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
import android.widget.ImageView;
import android.util.Log;
import java.io.FileWriter;
import java.io.IOException;
import android.content.pm.ActivityInfo;
import android.view.Surface;
import android.content.res.Configuration;
import android.content.DialogInterface;

public class CompassCal extends Activity implements OnClickListener, SensorEventListener{
    private TextView descText;
    private Button calButton;
    private SensorManager sensorManager;
    private Sensor compassSensor;
    private Boolean inCalibration;
    private int delay = SensorManager.SENSOR_DELAY_FASTEST;
    private boolean isTablet = false;
    private int originalOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    private SensorCalibration compassCal;
    private int handle;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        inCalibration = false;

        String boardType = getString(R.string.board_type);
        isTablet = boardType.equals("tablet");

        if (SensorCalibration.PSH_SUPPORT)
            compassCal = new SensorCalibration();

        delay = SensorManager.SENSOR_DELAY_FASTEST;
        setupLayout();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setupLayout();
    }

    private void setupLayout() {
        setContentView(R.layout.compass_cal);
        calButton = (Button)this.findViewById(R.id.calibration_button);
        if (calButton != null) {
            calButton.setOnClickListener(this);
            calButton.setEnabled(!inCalibration);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (inCalibration)
            sensorManager.unregisterListener(this, compassSensor);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (inCalibration)
            sensorManager.registerListener(this, compassSensor, delay);
    }

    private final void lockOrientation()
    {
        final int orientation = getResources().getConfiguration().orientation;
        final int rotation = getWindowManager().getDefaultDisplay().getOrientation();
        originalOrientation = getRequestedOrientation();

        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                // tablet is in reverse portrait mode when rotation is 90, crazy!!!
                if (isTablet)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                else
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            else if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else if (rotation == Surface.ROTATION_180 || rotation == Surface.ROTATION_270) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (isTablet)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                else
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            }
            else if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.calibration_button:
            lockOrientation();
            calButton.setEnabled(false);

            if (SensorCalibration.PSH_SUPPORT) {
                handle = compassCal.CalibrationOpen(1);
                compassCal.CalibrationStart(handle);
            }
            else
            {
                try {
                    FileWriter fw = new FileWriter("/data/compass.conf");
                    String s = "0 0 0 0 0 0 0\n";
                    fw.write(s);
                    fw.flush();
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            inCalibration = true;
            compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            sensorManager.registerListener(this, compassSensor, delay);

            break;
        }
    }

    public void onSensorChanged(SensorEvent event) {
        if (SensorCalibration.PSH_SUPPORT) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    if (compassCal.CalibrationFinishCheck(handle) == 1) {
                        compassCal.CalibrationClose(handle);

                        onCalibrationFinished();
                 }
                 break;
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (!SensorCalibration.PSH_SUPPORT)
            if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
                onCalibrationFinished();
    }

    public void onCalibrationFinished() {
        sensorManager.unregisterListener(this, compassSensor);
        inCalibration = false;
        calButton.setEnabled(true);

        setRequestedOrientation(originalOrientation);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(R.string.compass_cal_alert_ok_btn, null);

        builder.setTitle(R.string.compass_cal_alert_title);
        builder.setMessage(R.string.compass_cal_alert_success);
        builder.show();
    }
}
