package com.hackzurich.carapp.carapphackzurich;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.widget.Toast;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;

public class SensorsService extends Service implements SensorEventListener, LocationListener {

    private static final String TAG = "SensorsService";
    public static final String BROADCAST_ACTION = "com.hackzurich.carapp.carapphackzurich";
    private final Handler handler = new Handler();
    Intent intent;
    int counter = 0;
    int mStartMode;       // indicates how to behave if the service is killed

    private String CarID;
    private String CarType;
    private String InEmergenecy;

    private SensorManager mSensorManager;
    private SensorsCalulations calcs;
    private Sensor mAccel;
    private Sensor mGyro;
    private Sensor mGyroUn;
    private Sensor mGravity;
    private Sensor mMagnitometer;
    private Sensor mMagnitometerUn;
    private Sensor mLinearAccel;
    private Sensor mRotionVector;

    protected LocationManager locationManager;
    protected Location location;

    private float [] tvAccel;
    private float [] tvGyro;
    private float [] tvGyroUn;
    private float [] tvGravity;
    private float tvMagnitometer;
    private float tvMagnitometerUn;
    private float [] tvLinearAccel;
    private float [] tvRotationVector;

    private double mCurrentLatitude;
    private double mCurrentLongitude;
    private double sLongitude;
    private double sLatitude;
    private double sSpeed;
    private String Link;

    @Override
    public void onCreate() {
        super.onCreate();

        intent = new Intent(BROADCAST_ACTION);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        calcs = new SensorsCalulations(mSensorManager);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGyroUn = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mMagnitometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mMagnitometerUn = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);
        mLinearAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mRotionVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        handler.removeCallbacks(sendUpdatesToUI);
        CarID = intent.getStringExtra("CarID");
        CarType = intent.getStringExtra("CarType");
        InEmergenecy = intent.getStringExtra("InEmergency");
        Link = intent.getStringExtra("Link");

//        handler.postDelayed(sendUpdatesToUI, 1000); // 1 second
        handler.postDelayed(sendUpdatesToUI, 1000);

        mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroUn, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnitometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnitometerUn, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mLinearAccel, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mRotionVector, SensorManager.SENSOR_DELAY_NORMAL);

//        try{
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    mCurrentLatitude = latitude;
                    mCurrentLongitude = longitude;
                    sLongitude = latitude;
                    sLatitude = longitude;
                    sSpeed = location.getSpeed();
                }
            }
             else{
                Context context = this;
                Toast.makeText(context, "Please allow Location Services", Toast.LENGTH_SHORT).show();
            }

        return mStartMode;
    }

    public void SendJSON(){
        String s = "{";
        s += "\"car_id\": " + CarID + ",";
        s += "\"car_type\": " + CarType + ",";
        s += "\"in_emergency\": " + InEmergenecy + ",";
        s += "\"timestamp\": " + (System.currentTimeMillis() / 1000L) + ",";
        s += "\"sensors\":{";
        if (tvAccel != null) {
            s += "\"acceleration_x\": " + tvAccel[0] + ",";
            s += "\"acceleration_y\": " + tvAccel[1] + ",";
            s += "\"acceleration_z\": " + tvAccel[2] + ",";
        }
        if (tvGyro != null) {
            s += "\"gyro_x\": " + tvGyro[0] + ",";
            s += "\"gyro_y\": " + tvGyro[1] + ",";
            s += "\"gyro_z\": " + tvGyro[2] + ",";
        }
        s += "\"longitude\": " + sLongitude + ",";
        s += "\"latitude\": " + sLatitude + ",";
        s += "\"speed\": " + sSpeed;
        s += "}";
        s += "}";

        JSONObject j = null;
//        final String url = "http://172.31.4.246:5000/update";
        final String url = Link;
        try {
            j = new JSONObject(s);
            Log.i("JSON", s);
        } catch(Exception e) {
            e.printStackTrace();
        }
        RequestQueue q = Volley.newRequestQueue(this);
        JsonObjectRequest r = new JsonObjectRequest(url, j, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("Response", response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError er) {
                NetworkResponse errorRes = er.networkResponse;
                String stringData = "";
                if(errorRes != null && errorRes.data != null){
                    try {
                        stringData = new String(errorRes.data, "UTF-8");
                        VolleyLog.e("StringData", stringData);
                    }catch (UnsupportedEncodingException ex){
                        ex.printStackTrace();
                    }
                }
//                VolleyLog.e("Error", er.getMessage());
            }
        });
        q.add(r);
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
//            DisplayLoggingInfo();
//            handler.postDelayed(this, 10000); // 10 seconds
            SendJSON();
            handler.postDelayed(this, 1000); // 10 seconds
//            handler.post(this); // instantly post the obtained data to the caller class
        }
    };

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.

        // Do something with this sensor value.
        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                tvAccel = calcs.Calculate_Accel(event);
                break;

            case Sensor.TYPE_GYROSCOPE:
                tvGyro = calcs.Calculate_Gyro(event);
                break;

            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                tvGyroUn = calcs.Calculate_Gyro_Un(event);
                break;

            case Sensor.TYPE_GRAVITY:
                tvGravity = calcs.Calculate_Gravity(event);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                tvMagnitometer = calcs.Calculate_Magnetometer(event);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                tvMagnitometerUn = calcs.Calculate_MagnetometerUn(event);
                break;

            case Sensor.TYPE_LINEAR_ACCELERATION:
                tvLinearAccel = calcs.Calculate_LinearAccel(event);
                break;

            case Sensor.TYPE_ROTATION_VECTOR:
                tvRotationVector = calcs.Calculate_RotationVector(event);
                break;
        }



    }

    private void DisplayLoggingInfo() {
//        Log.d(TAG, "entered DisplayLoggingInfo");
        intent.putExtra("TYPE_ACCELEROMETER", tvAccel.toString());
        intent.putExtra("TYPE_GYROSCOPE", tvGyro.toString());
        intent.putExtra("TYPE_GYROSCOPE_UNCALIBRATED", tvGyroUn.toString());
        intent.putExtra("TYPE_GRAVITY", tvGravity.toString());
        intent.putExtra("TYPE_MAGNETIC_FIELD", tvMagnitometer);
        intent.putExtra("TYPE_MAGNETIC_FIELD_UNCALIBRATED", tvMagnitometerUn);
        intent.putExtra("TYPE_LINEAR_ACCELERATION", tvLinearAccel.toString());
        intent.putExtra("TYPE_ROTATION_VECTOR", tvRotationVector.toString());
        intent.putExtra("GPS", sLongitude + "\n" + sLatitude + "\n" + sSpeed);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(sendUpdatesToUI);
        mSensorManager.unregisterListener(this, mAccel);
        mSensorManager.unregisterListener(this, mGyro);
        mSensorManager.unregisterListener(this, mGyroUn);
        mSensorManager.unregisterListener(this, mGravity);
        mSensorManager.unregisterListener(this, mMagnitometer);
        mSensorManager.unregisterListener(this, mMagnitometerUn);
        mSensorManager.unregisterListener(this, mLinearAccel);
        mSensorManager.unregisterListener(this, mRotionVector);
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location loc) {
        double longitude = loc.getLongitude();
        double latitude = loc.getLatitude();
        mCurrentLatitude = latitude;
        mCurrentLongitude = longitude;
        sSpeed = loc.getSpeed();
        sLongitude = longitude;
        sLatitude = latitude;

    }

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

}
