
var map;

var myId = 1;

var cars = [];
var smartInfrastructure = [];

var frameWidth = 0.0001;
var lightScale = 1;
var offsetX = - 0.0001;
var offsetY = - 0.0002;

var carTypes = ['normal', 'ambulance'];

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
  },
  emergency_traffic_light_stop: {
    fill: '#4285f4',
    stroke: '#d53333'
  },
  emergency_stop_others: {
    fill: '#008000',
    stroke: '#d98c00'
  }
};

var events = {
  emergency_alert: 'Emergency vehicle nearby!',
  crash: 'Car crash!',
  emergency_traffic_light_stop: 'Stop or you will probably die!',
  emergency_stop_others: 'Dangerous car nearby.'
};

/**
 * @param {Object} info
 */
function refreshInfo (info) {
  $('.info-panel-wrapper .id span').html(info.car.car_id);
  $('.info-panel-wrapper .speed span').html(info.car.sensors.speed);
  $('.info-panel-wrapper .type span').html(carTypes[info.car.car_type]);
  $('.info-panel-wrapper .acceleration span').html(info.car.sensors.acceleration_x.toFixed(0)/100);

  $('.info-panel-wrapper .road-name span').html(info.car.road);

  if (info.events.length == 0) {
    $('.info-panel-wrapper .events').html('');
  } else {
    for (var i=0; i<info.events.length; i++) {
      renderEvent(info.events[i].event_type, events[info.events[i].event_type]);
    }
  }
}

/**
 * @param {string} type
 * @param {string} message
 */
function renderEvent (type, message) {
  if ($('.info-panel-wrapper .events .event.'+type).length == 0) {
    $('.info-panel-wrapper .events').append(
      '<div class="event ' + type + '">' + message + '</div>'
    );
  }
}

var done = false;

var refreshInterval = setInterval(function () {
  $.get('http://snf-648103.vm.okeanos.grnet.gr/stream/' + myId, function(data) {
    data = JSON.parse(data);
    refreshInfo(data);

    if (!done) {
      map.setCenter({ lat: data.car.sensors.latitude, lng: data.car.sensors.longitude });
      done = true;
    }

    drawCar(
      data.car.sensors.longitude,
      data.car.sensors.latitude,
      data.car.car_type,
      data.events,
      data.car.car_id,
      data.car.in_emergency
    );

    // Draw other cars:
    for (let i = 0; i < data.other_cars.length; i++) {
      drawCar(
        data.other_cars[i].sensors.longitude,
        data.other_cars[i].sensors.latitude,
        data.other_cars[i].car_type,
        data.events,
        data.other_cars[i].car_id,
        data.other_cars[i].in_emergency
      );
    }

    // Draw smart infrastructure:
    for (let i = 0; i < data.smart_infrastructure.length; i++) {
      drawTraficLights(
        data.smart_infrastructure[i].longitude,
        data.smart_infrastructure[i].latitude,
        data.smart_infrastructure[i].state,
        data.smart_infrastructure[i].id
      );
    }
  });
}, 100);

function drawCar (longitude, latitude, type, events, car_id, in_emergency) {
  var currentCar = _.find(cars, { car_id : car_id });

  var found = false;

  // Check if any of events refers to the car:
  for (let i=0; i<events.length; i++) {
    if (events[i].car_id == car_id) {
      found = true;

      if (events[i].event_type == 'crash' && car_id == myId) {
        type = 'crashed';
      } else if (events[i].event_type == 'emergency_stop_others') {
        type = 'emergency_stop_others';
      } else if (events[i].event_type == 'emergency_alert') {
        type = 'emergency';
      } else if (events[i].event_type == 'emergency_traffic_light_stop') {
        type = 'emergency_traffic_light_stop';
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

  if (currentCar) {
    if (car_id == myId && (currentCar.position.lng().toFixed(6) != longitude.toFixed(6) || currentCar.position.lat().toFixed(6) != latitude.toFixed(6))) {
      map.setCenter({ lat: latitude, lng: longitude });
    }

    _.find(cars, { car_id : car_id }).setPosition({
      lng: longitude,
      lat: latitude
    });
    _.find(cars, { car_id : car_id }).in_emergency = in_emergency;
    _.find(cars, { car_id : car_id }).type = type;
  } else {
    cars.push(new google.maps.Marker({
      car_id: car_id,
      in_emergency: in_emergency,
      type: type,
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
    }));
  }
}

function drawTraficLights (longitude, latitude, color, id) {
  var current = _.find(smartInfrastructure, { id : id });

  if (current) {
    _.find(smartInfrastructure, { id : 1 }).red.setIcon({
      path: google.maps.SymbolPath.CIRCLE,
      fillColor: '#ff4837',
      fillOpacity: color == 'red' ? 1 : 0.2,
      scale: 12 * lightScale,
      strokeColor: '#ff4837',
      strokeOpacity: color == 'red' ? 1 : 0.2,
      strokeWeight: 2
    });

    _.find(smartInfrastructure, { id : 1 }).green.setIcon({
      path: google.maps.SymbolPath.CIRCLE,
      fillColor: '#4caf50',
      fillOpacity: color == 'green' ? 1 : 0.2,
      scale: 12 * lightScale,
      strokeColor: '#4caf50',
      strokeOpacity: color == 'green' ? 1 : 0.2,
      strokeWeight: 2
    });
  } else {
    smartInfrastructure.push({
      id: id,
      frame: new google.maps.Marker({
        position: {
          lng: longitude - ( frameWidth / 2 ) + offsetX,
          lat: latitude - ( frameWidth * 2 / 2 ) - offsetY
        },
        icon: {
          path: "M 0,0 3,0 3,6 0,6 0,0 Z",
          fillColor: '#444',
          fillOpacity: 0.9,
          scale: 12,
          strokeColor: '#444',
          strokeWeight: 2
        },
        map: map
      }),
      green: new google.maps.Marker({
        position: {
          lng: longitude + ( frameWidth / 2 ) + offsetX - 0.000005,
          lat: latitude - ( frameWidth * 1.7 ) - offsetY
        },
        icon: {
          path: google.maps.SymbolPath.CIRCLE,
          fillColor: '#ff4837',
          fillOpacity: color == 'red' ? 1 : 0.2,
          scale: 12 * lightScale,
          strokeColor: '#ff4837',
          strokeOpacity: color == 'red' ? 1 : 0.2,
          strokeWeight: 2
        },
        map: map
      }),
      red: new google.maps.Marker({
        position: {
          lng: longitude + ( frameWidth / 2 ) + offsetX - 0.000005,
          lat: latitude - ( frameWidth * 3 ) - offsetY
        },
        icon: {
          path: google.maps.SymbolPath.CIRCLE,
          fillColor: '#4caf50',
          fillOpacity: color == 'green' ? 1 : 0.2,
          scale: 12 * lightScale,
          strokeColor: '#4caf50',
          strokeOpacity: color == 'green' ? 1 : 0.2,
          strokeWeight: 2
        },
        map: map
      })
    });
  }
}

function initMap () {
  map = new google.maps.Map(document.getElementById('map'), {
    center: { lat: 47.3673, lng: 8.55 },
    zoom: 18
  });
}
