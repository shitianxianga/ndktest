package com.example.ndktest;

public class NDKUtils {
    static {
        System.loadLibrary("OpenCV");//导入生成的链接库文件
    }
    public native String invokeCmethod();

    public static native int[] grayProc(int[] pixels, int w, int h, int nW, int nH);
}
