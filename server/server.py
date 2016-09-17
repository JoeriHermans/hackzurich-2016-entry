# Note: The API is totally crap.

from flask import Flask, request, Response
from threading import Lock
import threading
from Queue import Queue
import time
import numpy as np
import json
import requests
from math import cos, asin, sqrt

## BEGIN Utility functions. ####################################################

def distance(a, b):
    # Fetch the parameters of the first car.
    lon_1 = float(a["longitude"])
    lat_1 = float(a["latitude"])
    # Fetch the parameters of the second car.
    lon_2 = float(b["longitude"])
    lat_2 = float(b["latitude"])
    # Distance computation from StackOverflow (http://stackoverflow.com/questions/365826/calculate-distance-between-2-gps-coordinates)
    p = 0.017453292519943295
    a = 0.5 - cos((lat_2 - lat_1) * p)/2 + cos(lat_1 * p) * cos(lat_2 * p) * (1 - cos((lon_2 - lon_1) * p)) / 2
    distance = 12742 * asin(sqrt(a))

    return distance

## END Utility functions. ######################################################

## BEGIN Event Listeners. ######################################################

class EventListener(object):

    def handle(self, update):
        raise NotImplementedError

class EmergencyTrafficLightStopEventListener(EventListener):

    def __init__(self, cars, cars_mutex, events, events_mutex, smart_infrastructure, smart_infra_mutex, detection_range=1.0, event_timeout=30):
        self.cars = cars
        self.mutex_cars = cars
        self.events = events
        self.mutex_events = events_mutex
        self.smart_infrastructure = smart_infrastructure
        self.mutex_smart_infra = smart_infra_mutex
        self.detection_range = detection_range
        self.event_timeout = event_timeout

    def get_traffic_lights_on_route(self, update):
        traffic_lights = []
        with self.mutex_smart_infra:
            for infra in self.smart_infrastructure:
                # Compute the distance to the smart infrastructure.
                d = infra.distance_to(update)
                # Check if the type is a traffic light.
                if infra.get_type() == "traffic_light" and d <= 0.5 and infra.on_route(update):
                    traffic_lights.append(infra)

        return traffic_lights

    def get_closest_traffic_light(self, car, traffic_lights):
        traffic_light = traffic_lights[0]
        min_distance = traffic_light.distance_to(car)
        num_traffic_lights = len(traffic_lights)
        # Iterate through the rest.
        for i in range(1, num_traffic_lights):
            tf = traffic_lights[i]
            d = tf.distance_to(car)
            if d < min_distance:
                min_distance = d
                traffic_light = tf

        return traffic_light

    def create_event(self, update, traffic_light):
        event = {}

        event_type = "emergency_traffic_light_stop"
        event_car_id = int(update["car_id"])
        # Build a dictionary.
        event["event_type"] = event_type
        event["car_id"] = event_car_id
        event["expiration_timestamp"] = int(update["timestamp"]) + 20

        return event

    def handle(self, update):
        event = None

        traffic_lights = self.get_traffic_lights_on_route(update)
        if len(traffic_lights) > 0:
            traffic_light = self.get_closest_traffic_light(update, traffic_lights)
            if traffic_light.is_red(update):
                distance_to_intersection = traffic_light.distance_to(update)
                speed = float(update["sensors"]["speed"])
                stopping_distance = (speed * speed) / (2 * 0.7 * 9.81)
                # Convert to Kilometers
                stopping_distance /= 1000
                if stopping_distance >= (distance_to_intersection + 0.5):
                    event = self.create_event(update, traffic_light)

        print(event)

        return event

class EmergencyServiceEventListener(EventListener):

    def __init__(self, cars, cars_mutex, events, events_mutex, detection_range=1.0, event_timeout=180):
        self.detection_range = detection_range
        self.event_timeout = event_timeout
        self.cars = cars
        self.mutex_cars = cars_mutex
        self.events = events
        self.mutex_events = events_mutex

    def is_emergency_vehicle(self, update):
        vehicle_type = update["car_type"]

        return int(vehicle_type) == 1

    def in_emergency(self, update):
        in_emergency = update["in_emergency"]

        return int(in_emergency) == 1

    def not_in_events(self, update):
        in_events = False
        car_id = int(update["car_id"])
        with self.mutex_events:
            for event in self.events:
                event_car_id = int(event["car_id"])
                event_type = event["event_type"]
                if event_type == "emergency_alert" and event_car_id == car_id:
                    in_events = True
                    break

        return not in_events

    def create_event(self, update):
        event = {}

        # Create the attributes which are required to describe the event.
        event_type = "emergency_alert"
        event_timestamp = update["timestamp"]
        event_expiration_timestamp = event_timestamp + self.event_timeout
        event_car_id = int(update["car_id"])
        # Build an event using a dictionary.
        event = {}
        event["event_type"] = event_type
        event["timestamp"] = event_timestamp
        event["expiration_timestamp"] = event_expiration_timestamp
        event["car_id"] = event_car_id

        return event

    def handle(self, update):
        event = None
        if self.is_emergency_vehicle(update) and self.in_emergency(update) and self.not_in_events(update):
           event = self.create_event(update)

        return event

class CrashEventListener(EventListener):

    def __init__(self, crash_tolerance=4,
                 event_timeout=20):
        self.crash_tolerance = crash_tolerance
        self.event_timeout = event_timeout

    def contains_arguments(self, update):
        contains = True
        contains &= ("acceleration_x" in update["sensors"])
        contains &= ("acceleration_y" in update["sensors"])
        contains &= ("acceleration_z" in update["sensors"])

        return contains

    def create_crash_event(self, update, magnitude):
        # Create the attributes which are required to describe the event.
        event_type = "crash"
        timestamp = float(update["timestamp"])
        expiration_timestamp = timestamp + self.event_timeout
        car_id = int(update["car_id"])
        # Build an event using a dictionary.
        event = {}
        event["event_type"] = event_type
        event["timestamp"] = timestamp
        event["expiration_timestamp"] = expiration_timestamp
        event["car_id"] = car_id

        return event

    def analyse_update(self, update):
        # Fetch the parameters from the sensor readings.
        a_x = float(update["sensors"]["acceleration_x"])
        a_y = float(update["sensors"]["acceleration_y"])
        a_z = float(update["sensors"]["acceleration_z"])
        # Compute the magnitude.
        vector = np.asarray([a_x, a_y, a_z])
        powers = np.asarray([2, 2, 2])
        vector = np.power(vector, powers)
        sum = 0.0
        for v_i in vector:
            sum += v_i
        magnitude = np.sqrt(sum)
        # Check if the magnitude hits the crash tolerance.
        if magnitude > self.crash_tolerance:
            event = self.create_crash_event(update, magnitude)
        else:
            event = None

        return event

    def handle(self, update):
        event = None

        # Check if the update has the required sensor readings.
        if self.contains_arguments(update):
            event = self.analyse_update(update)

        return event


## END Event Listeners. ########################################################

## BEGIN Smart Infrastructure. #################################################

class SmartTrafficLight(object):

    def __init__(self, id, latitude, longitude, route_a, route_b, url):
        self.route_a = route_a
        self.route_b = route_b
        self.url = url
        self.id = id
        self.type = "traffic_light"
        self.latitude = latitude
        self.longitude = longitude

    def get_information(self):
        r = requests.get(self.url)
        data = r.json()

        return data

    def get_type(self):
        return self.type

    def distance_to(self, car):
        a = {}
        a["longitude"] = self.longitude
        a["latitude"] = self.latitude

        return distance(a, car['sensors'])

    def is_red(self, car):
        is_red = False
        data = self.get_information()
        states = data["states"]
        for state in states:
            if state["road"] == car["road"]:
                is_red = (state["state"] == "red")
                break

        return is_red

    def is_green(self, car):
        is_green = False
        data = self.get_information()
        states = data["states"]
        for state in states:
            if state["road"] == car["road"]:
                is_green = (state["state"] == "green")
                break

        return is_green

    def on_route(self, car):
        route = car["road"]

        return self.route_a == route or self.route_b == route

## END Smart Infrastructure. ###################################################

class Application(object):

    def __init__(self):
        self.cars = {}
        self.update_queue = Queue()
        self.events = []
        self.event_listeners = []
        self.smart_infrastructure = []
        self.mutex_smart_infra = Lock()
        self.mutex_cars = Lock()
        self.mutex_events = Lock()
        self.mutex_event_listeners = Lock()
        self.thread_purge = None
        self.thread_event_consumers = None

    def add_car(self, car_id):
        with self.mutex_cars:
            sensors = {}
            sensors["latitude"] = 0
            sensors["longitude"] = 0
            sensors["speed"] = 0
            sensors["heading"] = 0
            c = {}
            c["car_id"] = int(car_id)
            c["car_type"] = 0
            c["in_emergency"] = 0
            c["timestamp"] = 0
            c["road"] = "Not available"
            c["sensors"] = sensors
            self.cars[car_id] = c

    def add_event(self, event):
        with self.mutex_events:
            self.events.append(event)

    def add_smart_infrastructure(self, infrastructure):
        with self.mutex_smart_infra:
            self.smart_infrastructure.append(infrastructure)

    def add_event_listener(self, event_listener):
        with self.mutex_event_listeners:
            self.event_listeners.append(event_listener)

    def update_car(self, car_id, data):
        with self.mutex_cars:
            if car_id in self.cars:
                self.cars[car_id] = data

    def purge_events(self):
        while True:
            with self.mutex_events:
                timestamp = time.time()
                num_events = len(self.events)
                if num_events > 0:
                    # Remove the elements which are expired.
                    for i in range(num_events - 1, -1, -1):
                        event = self.events[i]
                        if event["expiration_timestamp"] < timestamp:
                            del self.events[i]
            # Wait until the next purge.
            time.sleep(10)

    def consume_events(self):
        while True:
            while not self.update_queue.empty():
                update = self.update_queue.get()
                with self.mutex_event_listeners:
                    # Iterate through the event listeners.
                    for listener in self.event_listeners:
                        event = listener.handle(update)
                        if not event == None:
                            self.add_event(event)
            time.sleep(1)

    def run(self):
        self.thread_purge = threading.Thread(target=self.purge_events).start()
        self.thread_event_consumers = threading.Thread(target=self.consume_events).start()
        self.service()

    def get_car(self, id):
        with self.mutex_cars:
            if id in self.cars:
                car = self.cars[id]
            else:
                car = None

        return car

    def fetch_other_cars(self, car):
        cars = []

        with self.mutex_cars:
            num_cars = len(self.cars)
            for i in range(0, num_cars):
                c = self.cars[i]
                if car["car_id"] != c["car_id"] and distance(c["sensors"], car["sensors"]) <= 0.2:
                    cars.append(c)

        return cars

    def fetch_events(self, car):
        events = []

        with self.mutex_events:
            for e in self.events:
                event_car = self.get_car(e["car_id"])
                if distance(self.get_car(e["car_id"])["sensors"], car["sensors"]) <= 0.2:
                    events.append(e)

        return events

    def update_metadata(self, data):
        lat = float(data["sensors"]["latitude"])
        lon = float(data["sensors"]["longitude"])
        r = requests.get("http://maps.googleapis.com/maps/api/geocode/json?latlng=" + `lat` + "," + `lon` + "&sensor=true")
        metadata = r.json()
        address_components = metadata["results"][0]["address_components"]
        road = "Unknown"
        for component in address_components:
            if "route" in component["types"]:
                road = component["long_name"]
        # Add the metadata to the car information.
        data["road"] = road

    def service(self):
        app = Flask(__name__)

        ## BEGIN REST Routes. ##################################################

        @app.route("/update", methods=['POST'])
        def update():
            json_string = request.data
            data = json.loads(json_string)
            car_id = int(data['car_id'])
            car = self.get_car(car_id)
            # Check if the car exists.
            if not car == None:
                self.update_metadata(data)
                self.update_car(car_id, data)
                self.update_queue.put(data)

            return 'OK'

        @app.route("/stream/<car_id>")
        def stream(car_id):
            car_id = int(car_id)
            car = self.get_car(car_id)
            other_cars = self.fetch_other_cars(car)
            events = self.fetch_events(car)
            # Prepare the structure to be JSONified.
            data = {}
            data["car"] = car
            data["other_cars"] = other_cars
            data["events"] = events
            # Prepare the response.
            resp = Response(json.dumps(data))
            resp.headers['Access-Control-Allow-Origin'] = '*'

            return resp

        ## END REST Routes. ####################################################

        app.run(host='0.0.0.0', threaded=True, use_reloader=False)


## BEGIN Application code. #####################################################

def main():
    application = Application()
    # Add dummy cars (us).
    application.add_car(0) # Athanos
    application.add_car(1) # Przemek
    application.add_car(2) # Joeri
    # Add event listeners.
    application.add_event_listener(CrashEventListener(crash_tolerance=4))
    application.add_event_listener(EmergencyServiceEventListener(cars=application.cars,
                                                                 cars_mutex=application.mutex_cars,
                                                                 events=application.events,
                                                                 events_mutex=application.mutex_events))
    application.add_event_listener(EmergencyTrafficLightStopEventListener(cars=application.cars,
                                                              cars_mutex=application.mutex_cars,
                                                              events=application.events,
                                                              events_mutex=application.mutex_events,
                                                              smart_infrastructure=application.smart_infrastructure,
                                                              smart_infra_mutex=application.mutex_smart_infra))
    # Add smart infrastructure.
    traffic_light = SmartTrafficLight(id=1, latitude=46.234525, longitude=6.0482579, route_a="Route A Einstein", route_b="Route Rutherford", url="http://localhost:5001/state")
    application.add_smart_infrastructure(traffic_light)
    # Run the application.
    application.run()

## END Application code. #######################################################

if __name__ == '__main__':
    main()
