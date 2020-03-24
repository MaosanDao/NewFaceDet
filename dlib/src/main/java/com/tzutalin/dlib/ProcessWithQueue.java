package com.tzutalin.dlib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
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

    private static String checkMode = null;

    private List<VisionDetRet> results;

    private Handler mInferenceHandler;
    private Context mContext;
    private FaceDet mFaceDet;
    private BitMapListener mWindow;
    private Paint mFaceLandmardkPaint;

    private int mframeNum = 0;

    private double ear = 0;
    private int x = 0;
    private boolean ear_array_removed = false;
    private boolean drop_array_appended = false;
    private double THRESH = 0.04;
    private double DROP_THRESH = 0.065;
    private ArrayList<Double> ear_array = new ArrayList<>();
    private ArrayList<Integer> ax = new ArrayList<>();
    private ArrayList<Double> ay = new ArrayList<>();
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


    public ProcessWithQueue(LinkedBlockingQueue<Bitmap> frameQueue
            , LinkedBlockingQueue<Bitmap> frameQueueForDisplay, Context context
            , Handler handler, BitMapListener frameLayout) {
        this.mContext = context;
        this.mInferenceHandler = handler;

        mQueue = frameQueue;
        frameForDisplay = frameQueueForDisplay;

        mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
        mWindow = frameLayout;

        mFaceLandmardkPaint = new Paint();
        mFaceLandmardkPaint.setColor(Color.RED);
        mFaceLandmardkPaint.setStrokeWidth(2);
        mFaceLandmardkPaint.setStyle(Paint.Style.STROKE);

        checkMode = EyesBlinkDetection;

        start();
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

        if (frameData != null) {
            mInferenceHandler.post(
                    new Runnable() {
                        @Override
                        public void run() {

                            if (!new File(Constants.getFaceShapeModelPath()).exists()) {
                                Log.v("wangpei", "Copying landmark model to " + Constants.getFaceShapeModelPath());
                                FileUtils.copyFileFromRawToOthers(mContext, R.raw.shape_predictor_68_face_landmarks, Constants.getFaceShapeModelPath());
                            }

                            mframeNum++;
//                          saveBitmap(frameData, "frames", String.valueOf(mframeNum) + ".jpg");

                            switch (checkMode) {

                                case NoDetection: {
                                    mWindow.onBitMap(framefordisplay);
                                }
                                break;
                                case EyesBlinkDetection: {
                                    results = mFaceDet.detect(frameData);
                                    if (results == null) {
                                        return;
                                    }

                                    if (results.size() != 0) {
                                        for (final VisionDetRet ret : results) {
                                            float resizeRatio = 4f;
                                            Canvas canvas = new Canvas(framefordisplay);

                                            ArrayList<Point> landmarks = ret.getFaceLandmarks();

                                            int i = 1;

                                            //从68_face_landmarks获得6个关键点
                                            Point[] leftEye = new Point[6];
                                            Point[] rightEye = new Point[6];

                                            for (Point point : landmarks) {
                                                if (i > 36 && i < 43) {
                                                    //为了提高处理效率，我们处理的数据被缩小了
                                                    //因此，必须放大该点才能在原始图像中正确显示。
                                                    int pointX = (int) (point.x * resizeRatio);
                                                    int pointY = (int) (point.y * resizeRatio);
                                                    leftEye[i - 37] = new Point(pointX, pointY);
                                                    //canvas.drawCircle(pointX, pointY, 2, mFaceLandmardkPaint);
                                                } else if (i > 42 && i < 49) {
                                                    int pointX = (int) (point.x * resizeRatio);
                                                    int pointY = (int) (point.y * resizeRatio);
                                                    rightEye[i - 43] = new Point(pointX, pointY);
                                                    //canvas.drawCircle(pointX, pointY, 2, mFaceLandmardkPaint);
                                                }
                                                if (i > 48) {
                                                    break;
                                                }
                                                i++;
                                            }

                                            canvas.drawPath(getPath(leftEye), mFaceLandmardkPaint);
                                            canvas.drawPath(getPath(rightEye), mFaceLandmardkPaint);
                                            //saveBitmap(frameData, "Pframes", String.valueOf(mframeNum) + ".jpg");

                                            double leftEAR = eye_aspect_ratio(leftEye);
                                            double rightEAR = eye_aspect_ratio(rightEye);
                                            ear = (leftEAR + rightEAR) / 2.0;
                                        }
                                    } else {
                                        frames_notFoundFace++;
                                        Log.i("frames_notFoundFace", String.valueOf(frames_notFoundFace));
                                    }

                                    if (ear != 0) {
                                        //下面的代码很难阅读，但确实有效
                                        x += 1;
                                        ear_array.add(ear);
                                        ax.add(x);
                                        ay.add(ear);
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

                                                    Log.e("wangpei", " 眨眼了");
                                                    blink += 1;
                                                    closeEyes_drop = -5;
                                                }
                                            }
                                        }

                                    }


                                    mWindow.onBitMap(framefordisplay);
                                }
                                break;
                                case headOrientationDetection: {

                                    long startTime = System.currentTimeMillis();

                                    results = mFaceDet.detect(frameData);

                                    if (results.size() != 0) {
                                        for (final VisionDetRet ret : results) {
                                            float resizeRatio = 4f;
                                            Canvas canvas = new Canvas(framefordisplay);

                                            ArrayList<Point> landmarks = ret.getFaceLandmarks();


                                            for (Point point : landmarks) {
                                                int pointX = (int) (point.x * resizeRatio);
                                                int pointY = (int) (point.y * resizeRatio);
                                                canvas.drawCircle(pointX, pointY, 2, mFaceLandmardkPaint);
                                            }

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
                                        Log.i("wangpei", "无人脸");
                                    }

                                    long currentTime = System.currentTimeMillis();
                                    //从无人脸到这里，必须要大于1秒，否则会有抖动值。防抖用的
                                    if (currentTime - isNoFaceTime > 1000) {
                                        if (ratio > 0.6 && ratio < 1) {
                                            headToward = "front";
                                        } else {
                                            //如果您的右脸比左脸大，那么您的头朝左
                                            headToward = rightHalfFace > leftHalfFace ? "left" : "right";


                                            if ("left".equals(headToward)) {
                                                isTurnLeft = true;
                                            }

                                            if ("right".equals(headToward)) {
                                                isTurnRight = true;
                                            }
                                            //必须要同时满足左右摇头，才算成功
                                            if (isTurnRight && isTurnLeft) {
                                                Log.e("wangpei", "摇头了");

                                                isTurnRight = false;
                                                isTurnLeft = false;
                                            }

                                        }

                                    } else {
                                        Log.d("wangpei", "抖动的");
                                    }
//                                    mTransparentTitleView.setText("头朝向:" + headToward + " 耗时: " + (endTime - startTime) / 1000f);
//                                    mWindow.setImageBitmap("总帧数: " + mframeNum);
//                                    mWindow.setMoreInformation("比率: " + ratio);
                                    mWindow.onBitMap(framefordisplay);
                                }
                                break;
                                case MouthOrientationDetection: {
                                    results = mFaceDet.detect(frameData);
                                    if (results.size() != 0) {
                                        for (final VisionDetRet ret : results) {
                                            float resizeRatio = 4f;
                                            Canvas canvas = new Canvas(framefordisplay);
                                            ArrayList<Point> landmarks = ret.getFaceLandmarks();

                                            int i = 1;

                                            //从68_face_landmarks获得6个关键点
                                            Point[] leftEye = new Point[8];

                                            for (Point point : landmarks) {
                                                if (i > 60 && i < 69) {
                                                    //为了提高处理效率，我们处理的数据被缩小了
                                                    //因此，必须放大该点才能在原始图像中正确显示。
                                                    int pointX = (int) (point.x * resizeRatio);
                                                    int pointY = (int) (point.y * resizeRatio);
                                                    leftEye[i - 61] = new Point(pointX, pointY);
                                                    //canvas.drawCircle(pointX, pointY, 2, mFaceLandmardkPaint);
                                                }
                                                if (i > 69) {
                                                    break;
                                                }
                                                i++;
                                            }

                                            canvas.drawPath(getPath(leftEye), mFaceLandmardkPaint);
                                            //saveBitmap(frameData, "Pframes", String.valueOf(mframeNum) + ".jpg");

                                            double leftEAR = mouth_aspect_ratio(leftEye);

                                            if (leftEAR != 0) {

                                                //下面的代码很难阅读，但确实有效
                                                x += 1;
                                                ear_array.add(leftEAR);
                                                ax.add(x);
                                                ay.add(leftEAR);
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
                                                        }
                                                    }
                                                }

                                            }


                                            long currentTime = System.currentTimeMillis();

                                            //从无人脸到这里，必须要大于1秒，否则会有抖动值。防抖用的
                                            if (currentTime - isNoFaceTime > 1000) {
                                                if (leftEAR >= 0.55) {
                                                    if (!isOpenMouth) {
                                                        Log.v("wangpei", "ear:" + leftEAR);
//                                                        mWindow.action(CameraConnectionFragment.MOUTH);
                                                        isOpenMouth = true;
                                                    }
                                                } else {
                                                    isOpenMouth = false;
                                                }
                                            } else {
                                                Log.d("wangpei", "抖动的");
                                            }
//                                            keyPoint_mouth_top = landmarks.get(63);
//                                            keyPoint_mouth_bottom = landmarks.get(67);
//
//                                            canvas.drawCircle(keyPoint_mouth_top.x, keyPoint_mouth_top.y, 2, mFaceLandmardkPaint);
//                                            canvas.drawCircle(keyPoint_mouth_bottom.x, keyPoint_mouth_bottom.y, 2, mFaceLandmardkPaint);

                                        }
                                    } else {
                                        frames_notFoundFace++;
                                        isNoFaceTime = System.currentTimeMillis();
                                        Log.i("wangpei", "无人脸");
                                    }


                                    long endTime = System.currentTimeMillis();

//                                    mTransparentTitleView.setText("头朝向:" + headToward + " 耗时: " + (endTime - startTime) / 1000f);
//                                    mWindow.setImageBitmap("总帧数: " + mframeNum);
//                                    mWindow.setMoreInformation("比率: " + ratio);
                                    mWindow.onBitMap(framefordisplay);
                                }
                                break;
                            }
                        }

                    });
        }
    }

    //眼睛的高和长的比值
    private double eye_aspect_ratio(Point[] eye) {
        double ear = 0;
        double A = euclidean(eye[1], eye[5]);
        double B = euclidean(eye[2], eye[4]);
        double C = euclidean(eye[0], eye[3]);
        ear = (A + B) / (2.0 * C);
        return ear;
    }

    //眼睛的高和长的比值
    private double mouth_aspect_ratio(Point[] eye) {
        double ear = 0;
        double A = euclidean(eye[1], eye[7]);
        double B = euclidean(eye[3], eye[5]);
        double C = euclidean(eye[0], eye[4]);
        ear = (A + B) / (2.0 * C);
        return ear;
    }

    //两点间的欧式距离
    private double euclidean(Point p1, Point p2) {
        double result = 0;
        result = Math.sqrt(Math.pow((p1.x - p2.x), 2) + Math.pow((p1.y - p2.y), 2));
        return result;
    }

    /**
     * 过滤异常值,以便更好的计算连续落差Calculate_continuous_drop()
     *
     * @param AL     存有ear值的ArrayList
     * @param ax     ear值对应的帧，AL看作是Y轴的话，那么ax就是X轴，当AL过滤某一ear值时，其对应的帧也应删除
     * @param THRESH 阈值，小于它才过滤
     * @return 返回此次调用是否发生了过滤，过滤了返回true,无需过滤返回false
     */
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

    //返回点的闭合路径，通过canvas可画出
    private Path getPath(Point[] points) {
        Path path = new Path();
        path.moveTo(points[0].x, points[0].y);//起点
        //添加中间连接点
        for (int i = 1; i < points.length; i++) {
            path.lineTo(points[i].x, points[i].y);
        }
        path.close(); // 使这些点构成封闭的多边形
        return path;
    }

    private void saveBitmap(Bitmap bm, String directory, String fileName) {
        String filePath = null;

        boolean hasSDCard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

        if (hasSDCard) {

            filePath = Environment.getExternalStorageDirectory().toString() + File.separator + directory + File.separator + fileName;

        } else

            filePath = Environment.getDownloadCacheDirectory().toString() + File.separator + directory + File.separator + fileName;
        try {
            File file = new File(filePath);

            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
