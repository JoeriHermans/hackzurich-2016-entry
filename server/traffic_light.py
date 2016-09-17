from flask import Flask, request, Response
from threading import Lock
import threading
from Queue import Queue
import time as tiem
import numpy as np
import json
import requests
from math import cos, asin, sqrt

## BEGIN Traffic light parameters. #############################################

latitude = 46.234518
longitude = 6.048213
time_interval = 30
red_a = False
route_a = "Route A Einstein"
route_b = "Route Rutherford"
port = 5001
time = 0
mutex_red_a = Lock()
mutex_time = Lock()

## END Traffic light parameters. ###############################################

def service():
    global time_interval
    global time
    global red_a

    while True:
        with mutex_time:
            time = time_interval
        while time > 0:
            tiem.sleep(1)
            with mutex_time:
                time -= 1
        with mutex_red_a:
            red_a = not red_a

app = Flask(__name__)
service_thread = threading.Thread(target=service).start()

@app.route("/state", methods=['GET'])
def send_state():
    global time
    global route_a
    global route_b

    with mutex_time:
        t = time
    state_a = ""
    state_b = ""
    with mutex_red_a:
        if red_a:
            state_a = "red"
            state_b = "green"
        else:
            state_a = "green"
            state_b = "red"
    # Prepare the data for route a.
    data_a = {}
    data_a["road"] = route_a
    data_a["state"] = state_a
    # Prepare the data for route b.
    data_b = {}
    data_b["road"] = route_b
    data_b["state"] = state_b
    # Prepare the dictionary with the data.
    data = {}
    data["time"] = t
    data["states"] = [data_a, data_b]

    return str(data)

app.run(host='0.0.0.0', port=port, threaded=True, use_reloader=True)
