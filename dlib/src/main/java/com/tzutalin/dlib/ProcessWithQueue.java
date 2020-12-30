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

    private static int checkMode = 0;

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


    public ProcessWithQueue(LinkedBlockingQueue<Bitmap> frameQueue
            , LinkedBlockingQueue<Bitmap> frameQueueForDisplay, Context context
            , Handler handler, ArrayList<Integer> mLastMotions, FaceDetectListener faceDetectListener) {
        this.mContext = context;
        this.mInferenceHandler = handler;
        this.mLastMotions = mLastMotions;
        this.faceDetectListener = faceDetectListener;

        mQueue = frameQueue;
        frameForDisplay = frameQueueForDisplay;
        mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());

        start();
    }

    public void setMotionType(int motion) {
        checkMode = motion;
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
        if (frameData != null && mInferenceHandler != null) {
            mInferenceHandler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            Log.w("FaceDetectSDK", "mInferenceHandler");
                            if (mLastMotions.size() > 0) {
                                if (detectingIndex == mLastMotions.size()) {
                                    detectingIndex = 0;
                                    mInferenceHandler.removeCallbacks(this);
                                    faceDetectListener.onComplete();
                                }
                            }

                            checkMode = mLastMotions.get(detectingIndex);

                            switch (checkMode) {
                                case Constants.NO_SET: {
                                    faceDetectListener.onBitMap(framefordisplay);
                                }
                                break;
                                case Constants.MOTION_EYE: {
                                    results = mFaceDet.detect(frameData);
                                    if (results == null) {
                                        return;
                                    }

                                    if (!isCheckedEyeHint) {
                                        isCheckedEyeHint = true;
                                        faceDetectListener.onMotionCheckStart(Constants.MOTION_EYE);
                                    }

                                    if (results.size() != 0) {
                                        for (final VisionDetRet ret : results) {
                                            float resizeRatio = 4f;
                                            ArrayList<Point> landmarks = ret.getFaceLandmarks();

                                            int i = 1;

                                            Point[] leftEye = new Point[6];
                                            Point[] rightEye = new Point[6];

                                            for (Point point : landmarks) {
                                                if (i > 36 && i < 43) {
                                                    int pointX = (int) (point.x * resizeRatio);
                                                    int pointY = (int) (point.y * resizeRatio);
                                                    leftEye[i - 37] = new Point(pointX, pointY);
                                                } else if (i > 42 && i < 49) {
                                                    int pointX = (int) (point.x * resizeRatio);
                                                    int pointY = (int) (point.y * resizeRatio);
                                                    rightEye[i - 43] = new Point(pointX, pointY);
                                                }
                                                if (i > 48) {
                                                    break;
                                                }
                                                i++;
                                            }

                                            double leftEAR = eye_aspect_ratio(leftEye);
                                            double rightEAR = eye_aspect_ratio(rightEye);
                                            ear = (leftEAR + rightEAR) / 2.0;
                                        }
                                    } else {
                                        frames_notFoundFace++;
                                    }

                                    if (ear != 0) {
                                        x += 1;
                                        ear_array.add(ear);
                                        ax.add(x);
                                        ear_array_removed = filter_unexpected_values(ear_array, ax, THRESH);

                                        if (ear_array.size() > 2 && !ear_array_removed) {
                                            if (ear_array.get(ear_array.size() - 2) > ear_array.get(ear_array.size() - 3)) {
                                                continuous_Increment += 1;
                                                if (continuous_Decrement != 0) {
                                                    drop = ear_array.get(ear_array.size() - 3) - ear_array.get(ear_array.size() - 3 - continuous_Decrement);
                                                    if (continuous_Decrement != 1) {
                                                        drop_array.add(drop);
                                                        drop_array_appended = true;
                                                    }
                                                    temp = continuous_Decrement;
                                                    continuous_Decrement = 0;
                                                }
                                            } else if (ear_array.get(ear_array.size() - 2) < ear_array.get(ear_array.size() - 3)) {
                                                continuous_Decrement += 1;
                                                if (continuous_Increment != 0) {
                                                    drop = ear_array.get(ear_array.size() - 3) - ear_array.get(ear_array.size() - 3 - continuous_Increment);
                                                    if (continuous_Increment != 1) {
                                                        drop_array.add(drop);
                                                        drop_array_appended = true;
                                                    }
                                                    temp = continuous_Increment;
                                                    continuous_Increment = 0;
                                                }
                                            }
                                        }

                                        if (drop_array_appended) {
                                            if (drop_array.get(drop_array.size() - 1) < -DROP_THRESH) {
                                                closeEyes_drop = drop_array.get(drop_array.size() - 1);
                                                closeEyes_end = ax.get(ax.size() - 3);
                                            }
                                            if (drop_array.get(drop_array.size() - 1) > DROP_THRESH) {
                                                openEyes_drop = drop_array.get(drop_array.size() - 1);
                                                openEyes_start = ax.get(ax.size() - 3 - temp);
                                                if (Math.abs(closeEyes_drop + openEyes_drop) < 0.1 && ear_array.get(ear_array.size() - 3 - temp) < 0.21
                                                        && openEyes_start - closeEyes_end < 20) {

                                                    blink += 1;
                                                    closeEyes_drop = -5;
                                                    if (!isCheckedEye) {
                                                        isCheckedEye = true;
                                                        detectingIndex++;
                                                        Log.e("FaceDetectSDK", "MOTION_EYE");
                                                        faceDetectListener.onActionMotion(Constants.MOTION_EYE);
                                                    }
                                                }
                                            }
                                        }

                                    }

                                    faceDetectListener.onBitMap(framefordisplay);
                                }
                                break;
                                case Constants.MOTION_HEAD: {
                                    results = mFaceDet.detect(frameData);

                                    if (results == null) {
                                        return;
                                    }

                                    if (!isCheckedHeadHint) {
                                        isCheckedHeadHint = true;
                                        faceDetectListener.onMotionCheckStart(Constants.MOTION_HEAD);
                                    }

                                    if (results.size() != 0) {
                                        for (final VisionDetRet ret : results) {
                                            float resizeRatio = 4f;
                                            ArrayList<Point> landmarks = ret.getFaceLandmarks();

                                            keyPoint_left = landmarks.get(2);
                                            keyPoint_right = landmarks.get(14);
                                            keyPoint_nose = landmarks.get(30);

                                            rightHalfFace = euclidean(keyPoint_nose, keyPoint_right);
                                            leftHalfFace = euclidean(keyPoint_nose, keyPoint_left);

                                            ratio = Math.min(rightHalfFace, leftHalfFace) / Math.max(rightHalfFace, leftHalfFace);
                                        }
                                    } else {
                                        frames_notFoundFace++;
                                        isNoFaceTime = System.currentTimeMillis();
                                    }

                                    long currentTime = System.currentTimeMillis();
                                    if (currentTime - isNoFaceTime > 1000) {
                                        if (ratio > 0.6 && ratio < 1) {
                                            headToward = "front";
                                        } else {
                                            headToward = rightHalfFace > leftHalfFace ? "left" : "right";

                                            if ("left".equals(headToward)) {
                                                isTurnLeft = true;
                                            }

                                            if ("right".equals(headToward)) {
                                                isTurnRight = true;
                                            }
                                            if (isTurnRight && isTurnLeft) {
                                                isTurnRight = false;
                                                isTurnLeft = false;

                                                if (!isCheckedHead) {
                                                    isCheckedHead = true;
                                                    detectingIndex++;
                                                    Log.e("FaceDetectSDK", "MOTION_HEAD");
                                                    faceDetectListener.onActionMotion(Constants.MOTION_HEAD);
                                                }
                                            }

                                        }

                                    }
                                    faceDetectListener.onBitMap(framefordisplay);
                                }
                                break;
                                case Constants.MOTION_MOUTH: {
                                    results = mFaceDet.detect(frameData);
                                    if (results == null) {
                                        return;
                                    }

                                    if (!isCheckedMouthHint) {
                                        isCheckedMouthHint = true;
                                        faceDetectListener.onMotionCheckStart(Constants.MOTION_MOUTH);
                                    }

                                    if (results.size() != 0) {
                                        for (final VisionDetRet ret : results) {
                                            float resizeRatio = 4f;
                                            ArrayList<Point> landmarks = ret.getFaceLandmarks();

                                            int i = 1;

                                            Point[] leftEye = new Point[8];

                                            for (Point point : landmarks) {
                                                if (i > 60 && i < 69) {
                                                    int pointX = (int) (point.x * resizeRatio);
                                                    int pointY = (int) (point.y * resizeRatio);
                                                    leftEye[i - 61] = new Point(pointX, pointY);
                                                }
                                                if (i > 69) {
                                                    break;
                                                }
                                                i++;
                                            }

                                            double leftEAR = mouth_aspect_ratio(leftEye);
                                            long currentTime = System.currentTimeMillis();

                                            if (currentTime - isNoFaceTime > 1000) {
                                                if (leftEAR >= 0.50) {
                                                    if (!isOpenMouth) {
                                                        isOpenMouth = true;
                                                        if (!isCheckedMouth) {
                                                            isCheckedMouth = true;
                                                            detectingIndex++;
                                                            Log.e("FaceDetectSDK", "MOTION_MOUTH");
                                                            faceDetectListener.onActionMotion(Constants.MOTION_MOUTH);
                                                        }
                                                    }
                                                } else {
                                                    isOpenMouth = false;
                                                }
                                            }
                                        }
                                    } else {
                                        frames_notFoundFace++;
                                        isNoFaceTime = System.currentTimeMillis();
                                    }
                                    long endTime = System.currentTimeMillis();
                                    faceDetectListener.onBitMap(framefordisplay);
                                }
                                break;
                                default:
                            }
                        }
                    });
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
