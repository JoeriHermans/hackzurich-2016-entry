package com.hackzurich.carapp.carapphackzurich;

import android.hardware.SensorEvent;

import android.hardware.SensorManager;
import android.util.Log;
import java.lang.Math;


public class SensorsCalulations {

    private double[] gravity;
    private double[] linear_acceleration;
    private static final int INT_ROUNDVAL = 100;
    private static final double DOUBLE_ROUNDVAL = 100.0;
    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float[] deltaRotationVector;
    private float timestamp;
    private static final float EPSILON = 0.000001f;
    private SensorManager sensorManager;

    public SensorsCalulations(SensorManager sensorManager){
        this.sensorManager = sensorManager;
        gravity = new double[3];
        linear_acceleration = new double[3];
        deltaRotationVector = new float[4];
    }

    public String Calculate_Accel(SensorEvent event){

        final double alpha = 0.8;
        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        String disp = event.sensor.getName() + "\n";
        disp += "X: " + Double.toString(Math.round(linear_acceleration[0] * INT_ROUNDVAL) / DOUBLE_ROUNDVAL) + " m/s²" + "\n"
                + "Y: " + Double.toString(Math.round(linear_acceleration[1]* INT_ROUNDVAL) / DOUBLE_ROUNDVAL) + " m/s²" + "\n"
                + "Z: " + Double.toString(Math.round(linear_acceleration[2]* INT_ROUNDVAL) / DOUBLE_ROUNDVAL) + " m/s²";

        return disp;
    }

    public String Calculate_Gyro(SensorEvent event) {

        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.

//        float axisX = event.values[0];
//        float axisY = event.values[1];
//        float axisZ = event.values[2];

        // User code should concatenate the delta rotation we computed with the current rotation
        // in order to get the updated rotation.
        //  rotationCurrent = rotationCurrent * deltaRotationMatrix;

        final float dT;
        float axisX = 0;
        float axisY = 0;
        float axisZ = 0;
        double omegaMagnitude;

        if (timestamp != 0) {
            dT = (event.timestamp - timestamp) * NS2S;
            // Axis of the rotation sample, not normalized yet.
            axisX = event.values[0];
            axisY = event.values[1];
            axisZ = event.values[2];

            // Calculate the angular speed of the sample
            omegaMagnitude = Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

            // Normalize the rotation vector if it's big enough to get the axis
            // (that is, EPSILON should represent your maximum allowable margin of error)
            if (omegaMagnitude > EPSILON) {
                axisX /= omegaMagnitude;
                axisY /= omegaMagnitude;
                axisZ /= omegaMagnitude;
            }

            // Integrate around this axis with the angular speed by the timestep
            // in order to get a delta rotation from this sample over the timestep
            // We will convert this axis-angle representation of the delta rotation
            // into a quaternion before turning it into the rotation matrix.
            float thetaOverTwo = (float) (omegaMagnitude * dT / 2.0f);
            float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
            float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
            deltaRotationVector[0] = sinThetaOverTwo * axisX;
            deltaRotationVector[1] = sinThetaOverTwo * axisY;
            deltaRotationVector[2] = sinThetaOverTwo * axisZ;
            deltaRotationVector[3] = cosThetaOverTwo;
        }
        timestamp = event.timestamp;
        float[] deltaRotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);

        String disp = event.sensor.getName() + "\n";
        disp += "X: " + Double.toString(Math.round(axisX * INT_ROUNDVAL) / DOUBLE_ROUNDVAL) + "  rad/s" + "\n"
                + "Y: " + Double.toString(Math.round(axisY * INT_ROUNDVAL) / DOUBLE_ROUNDVAL) + "  rad/s" + "\n"
                + "Z: " + Double.toString(Math.round(axisZ * INT_ROUNDVAL) / DOUBLE_ROUNDVAL) + "  rad/s" + "\n";
        float rotationInRadians = deltaRotationVector[2];
        double rotationInDegrees = Math.toDegrees(rotationInRadians);
        rotationInDegrees = (360 + rotationInDegrees) % 360;
        disp += "Orientation: " + rotationInDegrees + "\u00b0";

        return disp;
    }

    public String Calculate_Gyro_Un(SensorEvent event) {

        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.

        float axisX = event.values[0];
        float axisY = event.values[1];
        float axisZ = event.values[2];


        // User code should concatenate the delta rotation we computed with the current rotation
        // in order to get the updated rotation.
        //  rotationCurrent = rotationCurrent * deltaRotationMatrix;
        String disp = event.sensor.getName() + "\n";
        disp += "X: " + Double.toString(Math.round(axisX * INT_ROUNDVAL) / DOUBLE_ROUNDVAL) + "  rad/s" + "\n"
                + "Y: " + Double.toString(Math.round(axisY * INT_ROUNDVAL) / DOUBLE_ROUNDVAL) + "  rad/s" + "\n"
                + "Z: " + Double.toString(Math.round(axisZ * INT_ROUNDVAL) / DOUBLE_ROUNDVAL) + "  rad/s" + "\n";

        return disp;
    }

    public String Calculate_Gravity(SensorEvent event) {

        double axisX = event.values[0];
        double axisY = event.values[1];
        double axisZ = event.values[2];

        String disp = event.sensor.getName() + "\n";
        disp += "X: " + Double.toString(Math.round(axisX * INT_ROUNDVAL) / DOUBLE_ROUNDVAL) + " m/s²" + "\n"
                + "Y: " + Double.toString(Math.round(axisY * INT_ROUNDVAL) / DOUBLE_ROUNDVAL) + " m/s²" + "\n"
                + "Z: " + Double.toString(Math.round(axisZ * INT_ROUNDVAL) / DOUBLE_ROUNDVAL) + " m/s²" + "\n";

        return disp;
    }

    public double Calculate_Magnetometer(SensorEvent event) {
        return event.values[0];
    }

    public double Calculate_MagnetometerUn(SensorEvent event) {
        return event.values[0];
    }

    public float[] Calculate_LinearAccel(SensorEvent event) {
        return event.values;
    }

    public float[] Calculate_RotationVector(SensorEvent event) {
        return event.values;
    }
}
