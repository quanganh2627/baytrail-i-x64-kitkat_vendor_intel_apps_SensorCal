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

import android.util.Log;

public class SensorCalibration {
    static final boolean PSH_SUPPORT = true;
    static final int PSH_COMP = 0;
    static final int PSH_GYRO = 1;

    static final int CAL_NONE = 0;
    static final int CAL_DONE = 100;
    static final int CAL_VALUE_MID = 50;

    static final int MSG_START = 0;
    static final int MSG_GET = 1;
    static final int MSG_SET = 2;
    static final int MSG_STOP = 3;
    static final int MSG_CLOSE = 4;

    static {
        try {
            System.loadLibrary("PSHSensorCal_JNI");
        }
        catch(UnsatisfiedLinkError ule) {
            Log.e("PSH_S_C:","ERROR, Load PSHSensorCal_JNI failed");
        }
    }

    public native int CalibrationOpen(int sensor_type);
    public native int CalibrationStart(int handle);
    public native int CalibrationGet(int handle);
    public native int CalibrationSet(int handle);
    public native int CalibrationStop(int handle);
    public native void CalibrationClose(int handle);
}
