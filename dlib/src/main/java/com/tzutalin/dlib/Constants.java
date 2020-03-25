package com.tzutalin.dlib;

import android.os.Environment;

import java.io.File;

/**
 * Created by darrenl on 2016/4/22.
 */
public class Constants {


    public final static int MOTION_MOUTH = 1;
    public final static int MOTION_EYE = 2;
    public final static int MOTION_HEAD = 3;
    public final static int NO_SET = 4;

    /**
     * getFaceShapeModelPath
     *
     * @return default face shape model path
     */
    public static String getFaceShapeModelPath() {
        File sdcard = Environment.getExternalStorageDirectory();
        return sdcard.getAbsolutePath() + File.separator + "shape_predictor_68_face_landmarks.dat";
    }
}
