package com.hackzurich.carapp.carapphackzurich;

import android.hardware.SensorEvent;

import android.hardware.SensorManager;
import android.util.Log;
import java.lang.Math;


public class SensorsCalulations {

    private float[] gravity;
    private float[] linear_acceleration;
    private static final int INT_ROUNDVAL = 100;
    private static final float DOUBLE_ROUNDVAL = 100;
    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float[] deltaRotationVector;
    private float timestamp;
    private static final float EPSILON = 0.000001f;
    private SensorManager sensorManager;

    public SensorsCalulations(SensorManager sensorManager){
        this.sensorManager = sensorManager;
        gravity = new float[3];
        linear_acceleration = new float[3];
        deltaRotationVector = new float[4];
    }

    public float [] Calculate_Accel(SensorEvent event){

        final double alpha = 0.8;
        // Isolate the force of gravity with the low-pass filter.
        gravity[0] =(float) (alpha * gravity[0] + (1 - alpha) * event.values[0]);
        gravity[1] = (float) (alpha * gravity[1] + (1 - alpha) * event.values[1]);
        gravity[2] = (float) (alpha * gravity[2] + (1 - alpha) * event.values[2]);

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        linear_acceleration[0] = (float) Math.round(linear_acceleration[0] * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        linear_acceleration[1] = (float) Math.round(linear_acceleration[1] * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        linear_acceleration[2] = (float) Math.round(linear_acceleration[2] * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        return linear_acceleration;
    }

    public float [] Calculate_Gyro(SensorEvent event) {

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
        float axisO = 0;
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

        float rotationInRadians = deltaRotationVector[2];
        double rotationInDegrees = Math.toDegrees(rotationInRadians);
        rotationInDegrees = (360 + rotationInDegrees) % 360;
//        disp += "Orientation: " + rotationInDegrees + "\u00b0";
        axisX = Math.round(axisX * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        axisY = Math.round(axisY * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        axisZ = Math.round(axisZ * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        axisO = rotationInRadians;
        float [] gyro = new float[4];
        gyro[0] = axisX;
        gyro[1] = axisY;
        gyro[2] = axisZ;
        gyro[3] = axisO;
        return gyro;
    }

    public float [] Calculate_Gyro_Un(SensorEvent event) {

        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.

        float axisX = event.values[0];
        float axisY = event.values[1];
        float axisZ = event.values[2];


        // User code should concatenate the delta rotation we computed with the current rotation
        // in order to get the updated rotation.
        //  rotationCurrent = rotationCurrent * deltaRotationMatrix;

        float [] gyro = new float[3];
        gyro[0] = axisX;
        gyro[1] = axisY;
        gyro[2] = axisZ;
        return gyro;
    }

    public float [] Calculate_Gravity(SensorEvent event) {

        float axisX = event.values[0];
        float axisY = event.values[1];
        float axisZ = event.values[2];

        float [] grav = new float[3];
        grav[0] = Math.round(axisX * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        grav[1] = Math.round(axisY * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        grav[2] = Math.round(axisZ * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        return grav;
    }

    public float Calculate_Magnetometer(SensorEvent event) {

        double mT = event.values[0];
        return Math.round(mT * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
    }

    public float Calculate_MagnetometerUn(SensorEvent event) {

        double mT = event.values[0];
        return Math.round(mT * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
    }

    public float [] Calculate_LinearAccel(SensorEvent event) {

        float la = event.values[0];
        float la1 = event.values[1];
        float la2 = event.values[2];
        float [] lina = new float[3];
        lina[0] = Math.round(la * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        lina[1] = Math.round(la1 * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        lina[2] = Math.round(la2 * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        return lina;
    }

    public float [] Calculate_RotationVector(SensorEvent event) {

        float axisX = event.values[0];
        float axisY = event.values[1];
        float axisZ = event.values[2];
        float W = event.values[3];

        // User code should concatenate the delta rotation we computed with the current rotation
        // in order to get the updated rotation.
        //  rotationCurrent = rotationCurrent * deltaRotationMatrix;

        float [] rv = new float[4];
        rv[0] = Math.round(axisX * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        rv[1] = Math.round(axisY * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        rv[2] = Math.round(axisZ * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        rv[3] = Math.round(W * INT_ROUNDVAL) / DOUBLE_ROUNDVAL;
        return rv;
    }
}