from flask import Flask, request

from threading import Lock

import threading

from Queue import Queue

import time

import numpy as np

import json

## BEGIN Event definitions. ####################################################

class Event(object):

    def __init__(self, timestamp, expiration_time):
        self.timestamp = timestamp
        self.expiration_time = expiration_time
        self.expiration_timestamp = timestamp + expiration_time

    def get_timestamp(self):
        return self.timestamp

    def get_expiration_duration(self):
        return self.expiration_duration

    def get_expiration_timestamp(self):
        return self.expiration_timestamp

    def expired(self):
        timestamp = time.time()

        return timestamp > self.expiration_timestamp

## END Event definitions. ######################################################

## BEGIN Event Listeners. ######################################################

class EventListener(object):

    def handle(self, update):
        raise NotImplementedError

class CrashEventListener(object):

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
        timestamp = float(update["update_timestamp"])
        expiration_timestamp = timestamp + self.event_timeout
        car_id = int(update["car_id"])
        latitude = float(update["sensors"]["latitude"])
        longitude = float(update["sensors"]["longitude"])
        # Build an event using a dictionary.
        event = {}
        event["event_type"] = event_type
        event["timestamp"] = timestamp
        event["expiration_timestamp"] = expiration_timestamp
        event["car_id"] = car_id
        event["latitude"] = latitude
        event["longitude"] = longitude

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

class Application(object):

    def __init__(self):
        self.cars = {}
        self.update_queue = Queue()
        self.events = []
        self.event_listeners = []
        self.mutex_cars = Lock()
        self.mutex_events = Lock()
        self.mutex_event_listeners = Lock()
        self.thread_purge = None
        self.thread_event_consumers = None

    def add_car(self, car_id):
        with self.mutex_cars:
            self.cars[car_id] = {}

    def add_event(self, event):
        with self.mutex_events:
            self.events.append(event)

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
                num_events = len(self.events)
                if num_events > 0:
                    # Remove the elements which are expired.
                    for i in range(num_events, -1, -1):
                        event = self.events[i]
                        if event.expired():
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
                self.update_car(car_id, data)
                self.update_queue.put(data)

            return 'OK'

        @app.route("/stream/<car_id>")
        def stream(car_id):
            car = self.get_car(car_id)

            # TODO Implement.

            return str(car)

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
    # Run the application.
    application.run()

## END Application code. #######################################################

if __name__ == '__main__':
    main()
