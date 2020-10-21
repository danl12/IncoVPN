#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_danl_incovpn_Native_getBaseUrl(JNIEnv *env, jobject thiz, jobject contextObject) {
    jclass contextClass = env->FindClass("android/content/Context");
    jmethodID getPackageNameMethodID = env->GetMethodID(contextClass, "getPackageName",
                                                        "()Ljava/lang/String;");
    auto packageName = (jstring) env->CallObjectMethod(contextObject, getPackageNameMethodID);
    jboolean isCopy;
    auto chars = env->GetStringUTFChars(packageName, &isCopy);
    if (strcmp(chars, "com.danl.incovpn") == 0) {
        std::string test = "http://167.99.245.210";
        return env->NewStringUTF(test.c_str());
    } else {
        return nullptr;
    }
}extern "C"
JNIEXPORT jstring JNICALL
Java_com_danl_incovpn_Native_getToken(JNIEnv *env, jobject thiz, jobject contextObject) {
    jclass contextClass = env->FindClass("android/content/Context");
    jmethodID getPackageNameMethodID = env->GetMethodID(contextClass, "getPackageName",
                                                        "()Ljava/lang/String;");
    auto packageName = (jstring) env->CallObjectMethod(contextObject, getPackageNameMethodID);
    jboolean isCopy;
    auto chars = env->GetStringUTFChars(packageName, &isCopy);
    if (strcmp(chars, "com.danl.incovpn") == 0) {
        std::string test = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJuYW1lIjoiaW5jb3ZwbiJ9.i2x1fUgSYlZO0WSngdXB4YUn9xJ4uBNSCbLqLmLUmko";
        return env->NewStringUTF(test.c_str());
    } else {
        return nullptr;
    }
}