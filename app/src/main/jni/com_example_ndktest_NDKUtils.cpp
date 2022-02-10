/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
#include <string>
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <opencv2/opencv.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgproc/types_c.h>
using namespace cv;
/* Header for class com_example_ndktest_NDKUtils */

extern "C" {
/*
 * Class:     com_example_ndktest_NDKUtils
 * Method:    invokeCmethod
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_example_ndktest_NDKUtils_invokeCmethod
  (JNIEnv *env, jobject obj) {
  return env -> NewStringUTF("NDK获取Native字符串");
  }

JNIEXPORT jintArray JNICALL Java_com_example_ndktest_NDKUtils_grayProc
    (JNIEnv *env, jclass clazz, jintArray buf, jint w, jint h, jint nW, jint nH) {
    jint *cbuf;
    cbuf = env->GetIntArrayElements(buf, JNI_FALSE );
    if (cbuf == NULL) {
       return 0;
    }
    Mat imgData(h, w, CV_8UC4, (unsigned char *) cbuf);
    Mat out;
    resize(imgData, out, Size(nW, nH), 0, 0, INTER_LINEAR);
    int size = nW * nH;
    env->ReleaseIntArrayElements(buf, cbuf, 0);
     jintArray result1 = env->NewIntArray(size);
     env->SetIntArrayRegion(result1, 0, size, (int*)out.data);
    return result1;
}
}