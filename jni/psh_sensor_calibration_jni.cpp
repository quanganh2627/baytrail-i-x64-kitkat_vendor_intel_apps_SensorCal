#include <stdlib.h>
#include <stdio.h>
#include <jni.h>
#include <assert.h>
#include <android/log.h>

#include "libsensorhub/libsensorhub.h"

#undef LOG_TAG
#define LOG_TAG    "JNI_sensorcal"

#define LOGV(...) __android_log_print(ANDROID_LOG_SILENT, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define JNIREG_CLASS "com/intel/sensor/SensorCalibration"

/* sensor_type :
 * 1. compass calibration
 * 2. gyro calibration
 * ......
 */
#define COMPASS_CAL	1
#define GYRO_CAL	2

/* This JNI function will open a sensor calibration.
 * return a sensor calibration handle for open success,
 * otherwise, retrun 0
 */
JNIEXPORT jint JNICALL psh_calibration_open(JNIEnv *env,
						jobject jobj,
						jint sensor_type)
{
	int ret;
	handle_t cal;

	if (sensor_type == COMPASS_CAL){
		LOGI("PSH_S_C: Start compass calibration");

		cal = psh_open_session(SENSOR_CALIBRATION_COMP);
		if (cal == NULL) {
			LOGE("PSH_S_C: Can not connect compass_cal");
			return 0;
		}
	} else if (sensor_type == GYRO_CAL) {
		LOGI("PSH_S_C: Start gyro calibration");

		cal = psh_open_session(SENSOR_CALIBRATION_GYRO);
		if (cal == NULL) {
			LOGE("PSH_S_C: Can not connect gyro_cal");
			return 0;
		}
	} else {
		LOGE("PSH_S_C: No this sensor type supported!");
		return 0;
	}

	return (int)cal;
}

/* This JNI function will trigger a new sensor calibration operation
 * Return 1 for success, 0 for fail
 */
JNIEXPORT jint JNICALL psh_calibration_start(JNIEnv *env,
						jobject jobj,
						jint handle)
{
	int ret;
	struct cmd_calibration_param param;
	handle_t cal = (handle_t)handle;

	if (cal == NULL) {
		LOGE("PSH_S_C: illegal sensor handle");
		return 0;
	}

	param.sub_cmd = SUBCMD_CALIBRATION_START;
	ret = psh_set_calibration(cal, &param);
	if (ret != 0) {
		LOGE("PSH_S_C: Reset calibration failed, ret is %d", ret);
		return 0;
	}

	return 1;
}

/* This JNI function check if calibration is finished
 * Return 1 for finished, 0 for not yet
 */
JNIEXPORT jint JNICALL psh_calibration_finish_check(JNIEnv *env,
						jobject jobj,
						jint handle)
{
	int ret;
	struct cmd_calibration_param param;
	handle_t cal = (handle_t)handle;

	if (cal == NULL) {
		LOGE("PSH_S_C: illegal sensor handle");
		return 0;
	}

	memset(&param, 0, sizeof(struct cmd_calibration_param));

	ret = psh_get_calibration(cal, &param);
	if (ret != 0) {
		LOGE("PSH_S_C: Get calibration failed, ret is %d", ret);
		return 0;
	}

	if (param.calibrated) {
		LOGI("PSH_S_C: Finish calibration");
		return 1;
	} else
		return 0;
}

JNIEXPORT void JNICALL psh_calibration_close(JNIEnv *env,
						jobject jobj,
						jint handle)
{
	struct cmd_calibration_param param;
	handle_t cal = (handle_t)handle;

	if (cal == NULL) {
		LOGE("PSH_S_C: illegal sensor handle");
		return;
	}

	param.sub_cmd = SUBCMD_CALIBRATION_STOP;
	psh_set_calibration(cal, &param);

	psh_close_session(cal);
}

static JNINativeMethod gMethods[] = {
	{"CalibrationOpen", "(I)I", (void*)psh_calibration_open},
	{"CalibrationStart", "(I)I", (void*)psh_calibration_start},
	{"CalibrationFinishCheck", "(I)I", (void*)psh_calibration_finish_check},
	{"CalibrationClose", "(I)V", (void*)psh_calibration_close},
};

static int registerNativeMethods(JNIEnv* env, const char* className,
        JNINativeMethod* gMethods, int numMethods)
{
	jclass clazz;
	clazz = (env)->FindClass(className);
	if (clazz == NULL) {
		return JNI_FALSE;
	}

	if ((env)->RegisterNatives( clazz, gMethods, numMethods) < 0) {
		return JNI_FALSE;
	}

	return JNI_TRUE;
}

static int registerNatives(JNIEnv* env)
{
	if (!registerNativeMethods(env, JNIREG_CLASS, gMethods,
				sizeof(gMethods) / sizeof(gMethods[0])))
		return JNI_FALSE;

	return JNI_TRUE;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv* env = NULL;
	jint result = -1;

	if ((vm)->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		return -1;
	}

	assert(env != NULL);

	if (!registerNatives(env)) {
		return -1;
	}

	result = JNI_VERSION_1_4;

	return result;
}

