package com.tzutalin.dlib;

import android.graphics.Bitmap;

/**
 * Created on 2020/3/24.
 *
 * @author Wangpei
 * Email: wangpei@bamboocloud.cn
 */
public interface FaceDetectListener {

    /**
     * data For Each Frame
     *
     * @param bitmap Bitmap
     */
    void onBitMap(Bitmap bitmap);

    void onReady();

    void onFaceDetected();
}
