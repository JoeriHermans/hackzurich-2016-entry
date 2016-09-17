package com.hackzurich.carapp.carapphackzurich;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.widget.Toast;

import org.w3c.dom.Text;

public class MainActivity extends Activity {

    private static final String TAG = "SensorsService";
    private Intent serviceIntent;
    private Intent backgroundIntent;
    private Intent webIntent;
    final Context context = this;

    private Button btCarID;
    private Button btCarType;
    private Button btStartSensorService;
    private Button btStopSensorService;
    private Button btDisplaySensors;
    private Button btInEmergency;
    private Button btSetlink;
    private TextView tvCarID;
    private TextView tvCarType;
    private TextView tvInEmergency;
    private TextView tvLink;
    private TextView tvLogArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        serviceIntent = new Intent(this, SensorsViewActivity.class);
        backgroundIntent = new Intent(this, SensorsService.class);
        webIntent = new Intent(this, FrontEndActivity.class);
        btCarID = (Button) findViewById(R.id.btCarID);
        btCarType = (Button) findViewById(R.id.btCarType);
        tvCarID = (TextView) findViewById(R.id.tvCarID);
        tvCarType = (TextView) findViewById(R.id.tvCarType);
        btStartSensorService = (Button) findViewById(R.id.btStarSensorService);
        btStopSensorService = (Button) findViewById(R.id.btStopSensorService);
        btDisplaySensors = (Button) findViewById(R.id.btDisplaySensors);
        btInEmergency = (Button) findViewById(R.id.btInEmergency);
        tvInEmergency = (TextView) findViewById(R.id.tvEmergenecy);
        btSetlink = (Button) findViewById(R.id.btSetLink);
        tvLink = (TextView) findViewById(R.id.tvLink);
        tvLogArea = (TextView) findViewById(R.id.tvLogArea);

    }

    private BroadcastReceiver sensorsSReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateLogArea(intent);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        tvCarID.setText("0");
        tvCarType.setText("0");
        tvInEmergency.setText("0");
        tvLogArea.setText("Test text");
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void showSensors(View view) {
        startActivity(serviceIntent);
    }

    public void setCarID(View view) {
        showInputDialog("ID");
    }

    public void setCarType(View view) {
        showInputDialog("Type");
    }

    public void setLink(View view) {
        showInputDialog("Link");
    }

    protected void showInputDialog(final String type) {

        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.prompts, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);

        final EditText editText = (EditText) promptView.findViewById(R.id.edittext);
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (type.equals("ID")) {
                            tvCarID.setText(editText.getText());
                        }
                        else if (type.equals("Type")){
                            if ((!editText.getText().toString().equals("0")) ||
                                    (!editText.getText().toString().equals("1"))){
                                tvCarType.setText(editText.getText());
                            }
                            else {
                                tvCarType.setText("0");
                            }

                        }
                        else if(type.equals("Link")){
                            tvLink.setText(editText.getText());
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    public void StarSensorService(View view) {
        backgroundIntent.putExtra("CarID", tvCarID.getText().toString());
        backgroundIntent.putExtra("CarType", tvCarType.getText().toString());
        backgroundIntent.putExtra("InEmergency", tvInEmergency.getText().toString());
        backgroundIntent.putExtra("Link", tvLink.getText().toString());
        startService(backgroundIntent);
        registerReceiver(sensorsSReceiver, new IntentFilter(SensorsService.BROADCAST_ACTION));
    }

    public void StopSensorService(View view) {
        try {
            unregisterReceiver(sensorsSReceiver);
            stopService(backgroundIntent);
        }
        catch (java.lang.IllegalArgumentException e){
            Toast.makeText(context, "Service Already Stoppped", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLogArea(Intent intent){
        tvLogArea.setText("");
        tvLogArea.setText("TYPE_ACCELEROMETER: " + intent.getStringExtra("TYPE_ACCELEROMETER") + "\n\n"
                + "TYPE_GYROSCOPE: " + intent.getStringExtra("TYPE_GYROSCOPE") + "\n\n"
                + "TYPE_GYROSCOPE_UNCALIBRATED: " + intent.getStringExtra("TYPE_GYROSCOPE_UNCALIBRATED") + "\n\n"
                + "TYPE_MAGNETIC_FIELD: " + intent.getStringExtra("TYPE_MAGNETIC_FIELD") + "\n\n"
                + "TYPE_MAGNETIC_FIELD_UNCALIBRATED: " + intent.getStringExtra("TYPE_MAGNETIC_FIELD_UNCALIBRATED") + "\n\n"
                + "TYPE_LINEAR_ACCELERATION: " + intent.getStringExtra("TYPE_LINEAR_ACCELERATION") + "\n\n"
                + "TYPE_ROTATION_VECTOR: " + intent.getStringExtra("TYPE_ROTATION_VECTOR") + "\n\n"
                + "GPS: \n" + intent.getStringExtra("GPS") + "\n");


    }

    public void startWebview(View view) {
        startActivity(webIntent);
    }

    public void setInEmergency(View view) {
        if (tvInEmergency.getText().toString().equals("0"))
            tvInEmergency.setText("1");
        else
            tvInEmergency.setText("0");
    }

}
