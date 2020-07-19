package com.hx.yolov5;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

public class YOLOv5 {
    static {
        System.loadLibrary("yolov5");
    }
    //通过JNI调用C++
    public static native void init(AssetManager manager);
    public static native Box[] detect(Bitmap bitmap, double threshold, double nms_threshold);
}
