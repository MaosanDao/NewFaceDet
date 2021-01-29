package com.tzutalin.dlib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class ProcessWithQueue extends Thread {
    private static final String TAG = "Queue";
    private static final String NoDetection = "noDetection";
    private static final String EyesBlinkDetection = "eyesBlinkDetection";
    private static final String headOrientationDetection = "headOrientationDetection";
    private static final String MouthOrientationDetection = "mouthOrientationDetection";

    private LinkedBlockingQueue<Bitmap> mQueue;
    private LinkedBlockingQueue<Bitmap> frameForDisplay;
    private List<VisionDetRet> results;

    private Handler mInferenceHandler;
    private Context mContext;
    private FaceDet mFaceDet;
    private FaceDetectListener faceDetectListener;
    private double ear = 0;
    private int x = 0;
    private boolean ear_array_removed = false;
    private boolean drop_array_appended = false;
    private double THRESH = 0.04;
    private double DROP_THRESH = 0.065;
    private ArrayList<Double> ear_array = new ArrayList<>();
    private ArrayList<Integer> ax = new ArrayList<>();
    private int continuous_Increment = 0;
    private int continuous_Decrement = 0;
    private double drop = 0;
    private ArrayList<Double> drop_array = new ArrayList<>();
    private int temp = 0;
    private double closeEyes_drop = -5;
    private double openEyes_drop = 0;
    private int closeEyes_end = 0;
    private int openEyes_start = 0;
    private int blink = 0;
    private int frames_notFoundFace = 0;
    private Point keyPoint_right = null;
    private Point keyPoint_left = null;
    private Point keyPoint_nose = null;
    private Point keyPoint_mouth_top = null;
    private Point keyPoint_mouth_bottom = null;
    private double rightHalfFace = 0;
    private double leftHalfFace = 0;
    private String headToward = "front";

    private boolean isOpenMouth = false;
    private double ratio = 0;


    private long isNoFaceTime;

    private boolean isTurnRight = false;
    private boolean isTurnLeft = false;


    private ArrayList<Integer> mLastMotions;

    private boolean isCheckedEye = false;
    private boolean isCheckedHead = false;
    private boolean isCheckedMouth = false;

    private boolean isCheckedEyeHint = false;
    private boolean isCheckedHeadHint = false;
    private boolean isCheckedMouthHint = false;

    private int detectingIndex = 0;
    private List<Bitmap> mBitMaps;
    private long mRecordTime = 0;
    private int MAX_INTERVAL = 1000;

    /**
     * 识别几次
     */
    private int maxDetectTimes = 3;

    public ProcessWithQueue(LinkedBlockingQueue<Bitmap> frameQueue
            , LinkedBlockingQueue<Bitmap> frameQueueForDisplay, Context context
            , Handler handler, ArrayList<Integer> mLastMotions, FaceDetectListener faceDetectListener) {
        this.mContext = context;
        this.mInferenceHandler = handler;
        this.mLastMotions = mLastMotions;
        this.faceDetectListener = faceDetectListener;
        this.mBitMaps = new ArrayList<>();

        mQueue = frameQueue;
        frameForDisplay = frameQueueForDisplay;
        mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());

        start();
    }

    /**
     * 设置识别的间隔时间
     */
    public void setDetectMaxInterval(int time) {
        MAX_INTERVAL = time;
    }

    public void setDetectTimes(int time) {
        maxDetectTimes = time;
    }

    public void release() {
        if (mFaceDet != null) {
            mFaceDet.release();
        }
    }

    @Override
    public void run() {
        while (true) {
            Bitmap frameData = null;
            Bitmap framefordisplay = null;
            try {
                frameData = mQueue.take();
                framefordisplay = frameForDisplay.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (frameData == null) {
                break;
            }
            processFrame(frameData, framefordisplay);
        }
    }

    private void processFrame(final Bitmap frameData, final Bitmap framefordisplay) {
        if (System.currentTimeMillis() - mRecordTime > MAX_INTERVAL) {
            mRecordTime = System.currentTimeMillis();
            if (frameData != null && mInferenceHandler != null) {
                Runnable face = new Runnable() {
                    @Override
                    public void run() {
                        results = mFaceDet.detect(frameData);
                        if (results == null) {
                            return;
                        }
                        if (results.size() != 0) {
                            for (final VisionDetRet ret : results) {
                                float resizeRatio = 4f;
                                ArrayList<Point> landmarks = ret.getFaceLandmarks();
                            }

                            if (detectingIndex < maxDetectTimes) {
                                mBitMaps.add(framefordisplay);
                                detectingIndex++;
                            } else {
                                faceDetectListener.onFaceDetected(mBitMaps);
                            }
                        }
                    }
                };


                mInferenceHandler.post(face);
            }
        }
    }

    private double eye_aspect_ratio(Point[] eye) {
        double ear;
        double A = euclidean(eye[1], eye[5]);
        double B = euclidean(eye[2], eye[4]);
        double C = euclidean(eye[0], eye[3]);
        ear = (A + B) / (2.0 * C);
        return ear;
    }

    private double mouth_aspect_ratio(Point[] eye) {
        double ear;
        double A = euclidean(eye[1], eye[7]);
        double B = euclidean(eye[3], eye[5]);
        double C = euclidean(eye[0], eye[4]);
        ear = (A + B) / (2.0 * C);
        return ear;
    }

    private double euclidean(Point p1, Point p2) {
        double result;
        result = Math.sqrt(Math.pow((p1.x - p2.x), 2) + Math.pow((p1.y - p2.y), 2));
        return result;
    }

    private static boolean filter_unexpected_values(ArrayList<Double> AL, ArrayList<Integer> ax, double THRESH) {
        if (AL.size() > 2) {
            if (AL.get(AL.size() - 3) < AL.get(AL.size() - 1)) {
                if (AL.get(AL.size() - 3) > AL.get(AL.size() - 2) && AL.get(AL.size() - 3) - AL.get(AL.size() - 2) > THRESH) {
                    AL.remove(AL.size() - 2);
                    ax.remove(AL.size() - 2);
                    return true;
                }
            } else if (AL.get(AL.size() - 3) > AL.get(AL.size() - 1)) {
                if (AL.get(AL.size() - 2) > AL.get(AL.size() - 3) && AL.get(AL.size() - 2) - AL.get(AL.size() - 3) > THRESH) {
                    AL.remove(AL.size() - 2);
                    ax.remove(AL.size() - 2);
                    return true;
                }
            }
        }
        return false;
    }

    private Path getPath(Point[] points) {
        Path path = new Path();
        path.moveTo(points[0].x, points[0].y);
        for (int i = 1; i < points.length; i++) {
            path.lineTo(points[i].x, points[i].y);
        }
        path.close();
        return path;
    }
}
