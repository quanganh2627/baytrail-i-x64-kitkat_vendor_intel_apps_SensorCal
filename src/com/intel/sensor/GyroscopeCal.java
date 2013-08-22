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
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
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
    private Handler mMainHandler, mThreadHandler;
    private int mCalResult;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.gyroscope_cal);

        calButton = (Button)this.findViewById(R.id.calibration_button);
        if(calButton != null)
            calButton.setOnClickListener(this);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        if (SensorCalibration.PSH_SUPPORT) {
            mCalResult = SensorCalibration.CAL_NONE;
            mMainHandler = new Handler() {
                public void handleMessage(Message msg){
                    switch (msg.what) {
                        case SensorCalibration.MSG_GET:
                            int result = (int)msg.arg1;
                            if (result != 0)
                                mCalResult = result;
                            break;
                    }
                }
            };

            new CalibrationThread().start();
        }
        else
        {
            data = new float[DATA_COUNT][3];
            dataCount = 0;
        }
        inCalibration = false;
    }

    class CalibrationThread extends Thread {
        private SensorCalibration gyroCal;
        private int CalHandle;
        private boolean NeedExit;

        public void run() {
            gyroCal = new SensorCalibration();
            CalHandle = gyroCal.CalibrationOpen(SensorCalibration.PSH_GYRO);

            Looper.prepare();

            mThreadHandler = new Handler() {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case SensorCalibration.MSG_START:
                            gyroCal.CalibrationStart(CalHandle);

                            NeedExit = false;
                            new GetCalibrationResult().start();

                            break;
                        case SensorCalibration.MSG_SET:
                            gyroCal.CalibrationSet(CalHandle);
                            break;
                        case SensorCalibration.MSG_STOP:
                            NeedExit = true;
                            gyroCal.CalibrationStop(CalHandle);
                            break;
                        case SensorCalibration.MSG_CLOSE:
                            gyroCal.CalibrationClose(CalHandle);
                            break;
                    }
                }
            };

            Looper.loop();
        }

        class GetCalibrationResult extends Thread {

            public void run () {
                while (!NeedExit) {
                    int result = gyroCal.CalibrationGet(CalHandle);

                    Message toMain = mMainHandler.obtainMessage(SensorCalibration.MSG_GET, result, 0);
                    mMainHandler.sendMessage(toMain);

                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (inCalibration) {
            sensorManager.unregisterListener(this, gyroSensor);

            if (SensorCalibration.PSH_SUPPORT) {
                Message threadMsg = mThreadHandler.obtainMessage(SensorCalibration.MSG_STOP);
                mThreadHandler.sendMessage(threadMsg);

                threadMsg = mThreadHandler.obtainMessage(SensorCalibration.MSG_SET);
                mThreadHandler.sendMessage(threadMsg);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (inCalibration) {
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);

            if (SensorCalibration.PSH_SUPPORT) {
                Message threadMsg = mThreadHandler.obtainMessage(SensorCalibration.MSG_START);
                mThreadHandler.sendMessage(threadMsg);
            }

	}
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.calibration_button:
            calButton.setEnabled(false);

            if (SensorCalibration.PSH_SUPPORT) {
                Message threadMsg = mThreadHandler.obtainMessage(SensorCalibration.MSG_START);
                mThreadHandler.sendMessage(threadMsg);
            }
            else
            {
                FileWriter fw = null;
                try {
                    fw = new FileWriter("/data/gyro.conf");
                    String s = "0 0 0\n";
                    fw.write(s);
                    fw.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    if (fw != null)
                        fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            inCalibration = true;
            gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
            break;
        }
    }

    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
        case Sensor.TYPE_GYROSCOPE:
            if (SensorCalibration.PSH_SUPPORT) {
                if (mCalResult == SensorCalibration.CAL_DONE) {
                    sensorManager.unregisterListener(this, gyroSensor);
                    inCalibration = false;
                    calButton.setEnabled(true);

                    Message threadMsg = mThreadHandler.obtainMessage(SensorCalibration.MSG_STOP);
                    mThreadHandler.sendMessage(threadMsg);

                    threadMsg = mThreadHandler.obtainMessage(SensorCalibration.MSG_SET);
                    mThreadHandler.sendMessage(threadMsg);

                    showResultDialog(true);
                }
            }
            else
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

        FileWriter fw = null;
        boolean success = false;
        try {
            fw = new FileWriter("/data/gyro.conf");
            String s = Float.toString(x) + " " + Float.toString(y) + " " + Float.toString(z) + "\n";
            fw.write(s);
            fw.flush();
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        try {
            if (fw != null)
                fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        showResultDialog(success);
    }

    private void showResultDialog(boolean success) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(R.string.gyroscope_cal_alert_ok_btn, null);
            builder.setTitle(R.string.gyroscope_cal_alert_title);
            builder.setMessage(success? R.string.gyroscope_cal_alert_success: R.string.gyroscope_cal_alert_fail);
            builder.show();
    }
}
