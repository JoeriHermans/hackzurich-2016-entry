package com.hackzurich.carapp.carapphackzurich;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
//import android.util.Log;
import android.widget.TextView;
import android.content.BroadcastReceiver;

public class SensorsViewActivity extends Activity {

    private static final String TAG = "SensorsService";
    private Intent intent;

    private TextView tvAccel;
    private TextView tvGyro;
    private TextView tvGyroUn;
    private TextView tvGravity;
    private TextView tvMagnitometer;
    private TextView tvMagnitometerUn;
    private TextView tvLinearAccel;
    private TextView tvGPSLovation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensors_view);

        intent = new Intent(this, SensorsService.class);

        tvAccel = (TextView) findViewById(R.id.tvACCELEROMETER);
        tvGyro = (TextView) findViewById(R.id.tvGYROSCOPE);
        tvGyroUn = (TextView) findViewById(R.id.tvGYROSCOPE_UNCALIBRATED);
        tvGravity = (TextView) findViewById(R.id.tvGRAVITY);
        tvMagnitometer = (TextView) findViewById(R.id.tvMAGNETOMETER);
        tvMagnitometerUn = (TextView) findViewById(R.id.tvMAGNETOMETER_UNCALIBRATED);
        tvLinearAccel = (TextView) findViewById(R.id.tvLINEAR_ACCELERATION);
        tvGPSLovation = (TextView) findViewById(R.id.tvGPS);
    }

    private BroadcastReceiver sensorsSReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI(intent);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        startService(intent);
        registerReceiver(sensorsSReceiver, new IntentFilter(SensorsService.BROADCAST_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(sensorsSReceiver);
        stopService(intent);
    }

    private void updateUI(Intent intent) {
        tvAccel.setText(intent.getStringExtra("TYPE_ACCELEROMETER").toString());
        tvGyro.setText(intent.getStringExtra("TYPE_GYROSCOPE").toString());
        tvGyroUn.setText(intent.getStringExtra("TYPE_GYROSCOPE_UNCALIBRATED").toString());
        tvGravity.setText(intent.getStringExtra("TYPE_MAGNETIC_FIELD").toString());
        tvMagnitometer.setText(intent.getStringExtra("TYPE_MAGNETIC_FIELD_UNCALIBRATED").toString());
        tvMagnitometerUn.setText(intent.getStringExtra("TYPE_LINEAR_ACCELERATION").toString());
        tvLinearAccel.setText(intent.getStringExtra("TYPE_ROTATION_VECTOR").toString());
        tvGPSLovation.setText(intent.getStringExtra("GPS").toString());
//        Log.d(TAG, "TYPE_ACCELEROMETER");
//        Log.d(TAG, "TYPE_GYROSCOPE");
//        Log.d(TAG, "TYPE_GYROSCOPE_UNCALIBRATED");
//        Log.d(TAG, "TYPE_GRAVITY");
//        Log.d(TAG, "TYPE_MAGNETIC_FIELD");
//        Log.d(TAG, "TYPE_MAGNETIC_FIELD_UNCALIBRATED");
//        Log.d(TAG, "TYPE_LINEAR_ACCELERATION");
//        Log.d(TAG, "TYPE_ROTATION_VECTOR");
    }
}
