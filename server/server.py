from flask import Flask, request

from threading import Lock

import threading

import Queue

import time

import json

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

    def add_event_listener(self, event_listener):
        with self.mutex_event_listeners:
            self.mutex_event_listeners.append(event_listener)

    def update_car(self, car_id, data):
        with self.mutex_cars:
            if car_id in self.cars:
                self.cars[car_id] = data

    def purge_events(self):
        while True:
            with self.mutex_events:
                num_events = len(self.events)
                # Remove the elements which are expired.
                for i in range(num_events, -1, -1, -1):
                    event = self.events[i]
                    if event.expired():
                        del self.events[i]
            # Wait until the next purge.
            time.sleep(10)

    def consume_events(self):
        pass

    def run(self):
        self.thread_purge = threading.Thread(target=self.pruge_events)
        self.thread_event_consumers = threading.Thread(target=self.consume_events)
        self.service()

    def get_car(id):
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
