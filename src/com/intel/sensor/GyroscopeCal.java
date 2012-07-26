/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.sensor;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import java.io.FileWriter;
import java.io.IOException;

public class GyroscopeCal extends Activity implements OnClickListener, SensorEventListener{
    private final int DATA_COUNT = 100;
    private Button calButton;
    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private float data[][];
    private int dataCount;
    private Boolean inCalibration;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.gyroscope_cal);

        calButton = (Button)this.findViewById(R.id.calibration_button);
        calButton.setOnClickListener(this);


        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        data = new float[DATA_COUNT][3];
        dataCount = 0;
        inCalibration = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (inCalibration)
            sensorManager.unregisterListener(this, gyroSensor);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (inCalibration)
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.calibration_button:
            calButton.setEnabled(false);
            inCalibration = true;

            try {
                FileWriter fw = new FileWriter("/data/gyro.conf");
                String s = "0 0 0\n";
                fw.write(s);
                fw.flush();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
            break;
        }
    }

    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
        case Sensor.TYPE_GYROSCOPE:
            collectData(event);
            break;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void collectData(SensorEvent event) {
        if (dataCount < DATA_COUNT) {
            data[dataCount][0] = event.values[0];
            data[dataCount][1] = event.values[1];
            data[dataCount][2] = event.values[2];
            dataCount++;
        } else {
            sensorManager.unregisterListener(this, gyroSensor);
            inCalibration = false;
            gyroCalibration();
            dataCount = 0;
            calButton.setEnabled(true);
        }
    }

    public void gyroCalibration() {
        float x = 0, y = 0, z = 0;

        for (int index = 0; index < DATA_COUNT; index++) {
            x += data[index][0];
            y += data[index][1];
            z += data[index][2];
        }

        x /= DATA_COUNT;
        y /= DATA_COUNT;
        z /= DATA_COUNT;

        try {
            FileWriter fw = new FileWriter("/data/gyro.conf");
            String s = Float.toString(x) + " " + Float.toString(y) + " " + Float.toString(z) + "\n";
            fw.write(s);
            fw.flush();
            fw.close();
            showResultDialog(true);
        } catch (IOException e) {
            e.printStackTrace();
            showResultDialog(false);
        }
    }

    private void showResultDialog(boolean success) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(R.string.gyroscope_cal_alert_ok_btn, null);
            builder.setTitle(R.string.gyroscope_cal_alert_title);
            builder.setMessage(success? R.string.gyroscope_cal_alert_success: R.string.gyroscope_cal_alert_fail);
            builder.show();
    }
}
