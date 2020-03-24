package com.tzutalin.dlib;

import android.graphics.Bitmap;

/**
 * Function: #todo
 * Created on 2020/3/24.
 *
 * @author Wangpei
 * Email: wangpei@bamboocloud.cn
 */
public interface BitMapListener {

    void onBitMap(Bitmap bitmap);

    void action(int motion);
}
