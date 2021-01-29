package com.tzutalin.dlib;

import android.graphics.Bitmap;

import java.util.List;

/**
 * Created on 2020/3/24.
 *
 * @author Wangpei
 * Email: wangpei@bamboocloud.cn
 */
public interface FaceDetectListener {

    void onReady();

    void onFaceDetected(List<Bitmap> bitmap);
}
