/*
 * Copyright 2016-present Tzutalin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tzutalin.dlibtest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tzutalin.dlib.AutoFitTextureView;
import com.tzutalin.dlib.BitMapListener;
import com.tzutalin.dlib.Constants;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class CameraConnectionFragment extends Fragment {

    private static final String TAG = "CameraConnectionFragment";

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final String FRAGMENT_DIALOG = "dialog";

    private MyHanlder mMyHandler;

    /**
     * {@link android.view.TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(
                        final SurfaceTexture texture, final int width, final int height) {
                    FaceDetectSDK.with().openCamera(textureView, getActivity(), width, height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(
                        final SurfaceTexture texture, final int width, final int height) {
                    FaceDetectSDK.with().configureTransform(getActivity(), textureView, width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(final SurfaceTexture texture) {
                }
            };

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView textureView;
    private ImageView frameLayout;

    /**
     * 用于运行不应阻塞UI的任务的附加线程。
     */
    private HandlerThread backgroundThread;

    /**
     * 用于在后台运行任务。
     */
    private Handler backgroundHandler;

    /**
     * 用于运行推理的附加线程，以免阻塞相机。
     */
    private HandlerThread inferenceThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler inferenceHandler;


    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private TextView mHintText;
    private int mSum = 0;

    private Button mCheckEye;
    private Button mCheckMouth;
    private Button mCheckHead;
    private Button mSetZero;

    public static CameraConnectionFragment newInstance() {
        return new CameraConnectionFragment();
    }

    @Override
    public View onCreateView(
            final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.camera_connection_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        textureView = view.findViewById(R.id.texture);
        frameLayout = view.findViewById(R.id.frameLayout);
        mHintText = view.findViewById(R.id.hint_text);
        mCheckEye = view.findViewById(R.id.check_eye);
        mCheckHead = view.findViewById(R.id.check_head);
        mCheckMouth = view.findViewById(R.id.check_mouth);
        mSetZero = view.findViewById(R.id.set_zero);

        mHintText.setText("正在初始化中，请不要点击");
        mCheckEye.setEnabled(false);
        mCheckHead.setEnabled(false);
        mCheckMouth.setEnabled(false);
        mSetZero.setEnabled(false);

        mCheckEye.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showToast("已切换到眨眼");
                mHintText.setText("已清零，请再次触发动作");
                mSum = 0;
                mOnGetPreviewListener.setMotionType(Constants.MOTION_EYE);
            }
        });

        mCheckHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showToast("已切换到摇头");
                mHintText.setText("已清零，请再次触发动作");
                mSum = 0;
                mOnGetPreviewListener.setMotionType(Constants.MOTION_HEAD);
            }
        });

        mCheckMouth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showToast("已切换到张嘴");
                mHintText.setText("已清零，请再次触发动作");
                mSum = 0;
                mOnGetPreviewListener.setMotionType(Constants.MOTION_MOUTH);
            }
        });

        mSetZero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHintText.setText("已清零，请再次触发动作");
                mSum = 0;
            }
        });
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mMyHandler = new MyHanlder();
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable()) {
            FaceDetectSDK.with().openCamera(textureView, getActivity(), textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }

        FaceDetectSDK.with().init(mOnGetPreviewListener,
                getActivity().getApplicationContext(), inferenceHandler, backgroundHandler, new BitMapListener() {

                    @Override
                    public void onBitMap(Bitmap bitmap) {
                        Message message = Message.obtain();
                        message.what = 1;
                        message.obj = bitmap;
                        mMyHandler.sendMessage(message);
                    }

                    @Override
                    public void action(int motion) {
                        Message message = Message.obtain();
                        message.what = 2;
                        message.obj = motion;
                        mMyHandler.sendMessage(message);
                    }

                    @Override
                    public void onReady() {
                        final Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            mHintText.setText("已初始化完毕，请选择底部的检测类型");
                                            mCheckEye.setEnabled(true);
                                            mCheckHead.setEnabled(true);
                                            mCheckMouth.setEnabled(true);
                                            mSetZero.setEnabled(true);
                                        }
                                    });
                        }
                    }
                });
    }

    @Override
    public void onPause() {
        FaceDetectSDK.with().closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FaceDetectSDK.with().destroy();
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    @DebugLog
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("ImageListener");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        inferenceThread = new HandlerThread("InferenceThread");
        inferenceThread.start();
        inferenceHandler = new Handler(inferenceThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    @SuppressLint("LongLogTag")
    @DebugLog
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        inferenceThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;

            inferenceThread.join();
            inferenceThread = null;
            inferenceHandler = null;
        } catch (final InterruptedException e) {
            Timber.tag(TAG).e("error", e);
        }
    }

    private final OnGetImageListener mOnGetPreviewListener = new OnGetImageListener();


    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private class MyHanlder extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    Bitmap bitmap = (Bitmap) msg.obj;

                    frameLayout.setImageBitmap(bitmap);
                    break;
                case 2:
                    mSum++;
                    int motion = (int) msg.obj;
                    switch (motion) {
                        case Constants.MOTION_EYE:
                            mHintText.setText("眨眼次数：" + mSum);
                            break;
                        case Constants.MOTION_MOUTH:
                            mHintText.setText("张嘴次数：" + mSum);
                            break;
                        case Constants.MOTION_HEAD:
                            mHintText.setText("摇头次数：" + mSum);
                            break;
                    }

                    break;
                default:
            }
        }
    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {
        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(final String message) {
            final ErrorDialog dialog = new ErrorDialog();
            final Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(
                            android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialogInterface, final int i) {
                                    activity.finish();
                                }
                            })
                    .create();
        }
    }
}