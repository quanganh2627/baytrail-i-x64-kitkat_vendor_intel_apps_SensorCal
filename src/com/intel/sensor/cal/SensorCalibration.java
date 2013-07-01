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

public class SensorCalibration {
    static final boolean PSH_SUPPORT = false;
    static final int PSH_COMP = 0;
    static final int PSH_GYRO = 1;

    static final int CAL_NONE = 0;
    static final int CAL_DONE = 0xFF;
    static final int CAL_VALUE_BEST = 10;
    static final int CAL_VALUE_MID = 50;
    static final int CAL_VALUE_MAX = 250;

    static final int MSG_START = 0;
    static final int MSG_GET = 1;
    static final int MSG_SET = 2;
    static final int MSG_STOP = 3;
    static final int MSG_CLOSE = 4;

    public int CalibrationOpen(int sensor_type) { return 0; }
    public int CalibrationStart(int handle) { return 0; }
    public int CalibrationGet(int handle) { return 0; }
    public int CalibrationSet(int handle) { return 0; }
    public int CalibrationStop(int handle) { return 0; }
    public void CalibrationClose(int handle) {}
}
