from flask import Flask, request, Response
from threading import Lock
import threading
from Queue import Queue
import time as tiem
import numpy as np
import json
import requests
from math import cos, asin, sqrt

# Define the positions of the first car.
a_position[0]['longitude'] = 6.048213
a_position[0]['latitude']  = 46.234518
a_position[1]['longitude'] = 6.048208
a_position[1]['latitude']  = 46.234519
a_position[2]['longitude'] = 6.045919
a_position[2]['latitude']  = 46.235168
a_speed = 20

def update_location(car):
    req = urllib2.Request('http://localhost:5000/update')
    req.add_header('Content-Type', 'application/json')
    urllib2.urlopen(req, json.dumps(car))
