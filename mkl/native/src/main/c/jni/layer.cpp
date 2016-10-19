#include "layer.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT
void JNICALL Java_com_intel_analytics_sparkdl_mkl_MKL_SetPrevFloat(
    JNIEnv *env, jclass thisClass, long prev, long curr)
{
  MKLLayer<float>::setPrev(prev, curr);
}

JNIEXPORT
void JNICALL Java_com_intel_analytics_sparkdl_mkl_MKL_SetPrevDouble(
    JNIEnv *env, jclass thisClass, long prev, long curr)
{
  MKLLayer<double>::setPrev(prev, curr);
}

JNIEXPORT
void JNICALL Java_com_intel_analytics_sparkdl_mkl_MKL_SetNextFloat(
    JNIEnv *env, jclass thisClass, long prev, long curr)
{
  MKLLayer<float>::setNext(prev, curr);
}

JNIEXPORT
void JNICALL Java_com_intel_analytics_sparkdl_mkl_MKL_SetNextDouble(
    JNIEnv *env, jclass thisClass, long prev, long curr)
{
  MKLLayer<double>::setNext(prev, curr);
}

JNIEXPORT
void JNICALL Java_com_intel_analytics_sparkdl_mkl_MKL_SetUseNextFloat(
    JNIEnv *env, jclass thisClass, long ptr, int value)
{
  MKLLayer<double>::setUseNext(ptr, value);
}

JNIEXPORT
void JNICALL Java_com_intel_analytics_sparkdl_mkl_MKL_SetUseNextDouble(
    JNIEnv *env, jclass thisClass, long ptr, int value)
{
  MKLLayer<double>::setUseNext(ptr, value);
}

#ifdef __cplusplus
}
#endif
