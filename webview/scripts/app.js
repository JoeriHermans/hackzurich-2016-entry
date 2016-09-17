
var map;

var myId = 1;

var cars = [];

var carsColors = {
  main: {
    fill: '#4285f4',
    stroke: '#3569d7'
  },
  emergency: {
    fill: '#ffa500',
    stroke: '#d98c00'
  },
  normal: {
    fill: '#008000',
    stroke: '#005e00'
  },
  crashed: {
    fill: '#c03636',
    stroke: '#d53333'
  }
};

/**
 * @param {Object} info
 */
function refreshInfo (info) {
  $('.info-panel-wrapper .id span').html(info.car.car_id);
  $('.info-panel-wrapper .speed span').html(info.car.sensors.speed);
  $('.info-panel-wrapper .type span').html(info.car.car_type);
  $('.info-panel-wrapper .acceleration span').html(info.car.sensors.acceleration_x);

  $('.info-panel-wrapper .events').html('');

  for (var i=0; i<info.events.length; i++) {
    renderEvent(info.events[i].event_type, info.events[i].event_type == 'emergency_alert' ? 'Emergency vehicle nearby!' : 'Car crash nearby!' );
  }
}

/**
 * @param {string} type
 * @param {string} message
 */
function renderEvent (type, message) {
  $('.info-panel-wrapper .events').append(
    '<div class="event ' + type + '">' + message + '</div>'
  );
}

var refreshInterval = setInterval(function () {
  $.get('http://172.31.4.246:5000/stream/' + myId, function(data) {
    data = JSON.parse(data);
    refreshInfo(data);
    map.setCenter({ lat: data.car.sensors.latitude, lng: data.car.sensors.longitude });

    // Clear cars:
    for (let i = 0; i < cars.length; i++) {
      cars[i].setMap(null);
    }
    cars.length = 0;

    // Draw main car:
    cars.push(
      drawCar(
        map.getCenter().lng(),
        map.getCenter().lat(),
        data.car.car_type,
        data.events,
        data.car.car_id,
        data.car.in_emergency
      )
    );

    // Draw other cars:
    for (let i = 0; i < data.other_cars.length; i++) {
      cars.push(
        drawCar(
          data.other_cars[i].sensors.longitude,
          data.other_cars[i].sensors.latitude,
          data.other_cars[i].car_type,
          data.events,
          data.other_cars[i].car_id,
          data.other_cars[i].in_emergency
        )
      );
    }
  });
}, 1000);

function drawCar (longitude, latitude, type, events, car_id, in_emergency) {
  var found = false;

  // Check if any of events refers to the car:
  for (let i=0; i<events.length; i++) {
    if (events[i].car_id == car_id) {
      found = true;

      if (events[i].event_type == 'crash') {
        type = 'crashed';
      } else if (events[i].event_type == 'emergency_alert') {
        type = 'emergency';
      }
    }
  }

  // Check if any of events refers to the car:
  if (!found) {
    if (in_emergency == 1) {
      type = 'emergency';
    } else if (car_id == myId) {
      type = 'main';
    } else {
      type = 'normal';
    }
  }

  return new google.maps.Marker({
    position: {
      lng: longitude,
      lat: latitude
    },
    icon: {
      path: google.maps.SymbolPath.CIRCLE,
      fillColor: carsColors[type].fill,
      fillOpacity: 0.9,
      scale: 10,
      strokeColor: carsColors[type].stroke,
      strokeWeight: 3
    },
    map: map
  });
}

function initMap () {
  map = new google.maps.Map(document.getElementById('map'), {
    center: { lat: 47.3673, lng: 8.55 },
    zoom: 16
  });
}
