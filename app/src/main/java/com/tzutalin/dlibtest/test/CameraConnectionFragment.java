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

package com.tzutalin.dlibtest.test;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraCaptureSession;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tzutalin.dlib.AutoFitTextureView;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDetectListener;
import com.tzutalin.dlibtest.FaceDetectSDK;
import com.tzutalin.dlibtest.R;

import java.util.ArrayList;

public class CameraConnectionFragment extends Fragment implements FaceDetectListener, ViewTreeObserver.OnGlobalLayoutListener {

    private static final String TAG = "CameraConnectionFragment";

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final String FRAGMENT_DIALOG = "dialog";

    private MyHanlder mMyHandler;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView textureView;
    private ImageView frameLayout;

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

    /**
     * 正在检测的顺序
     */
    private ArrayList<Integer> mMotions = new ArrayList<>(3);

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
        mHintText = view.findViewById(R.id.hint_text);
        mCheckEye = view.findViewById(R.id.check_eye);
        mCheckHead = view.findViewById(R.id.check_head);
        mCheckMouth = view.findViewById(R.id.check_mouth);
        mSetZero = view.findViewById(R.id.set_zero);

        textureView.getViewTreeObserver().addOnGlobalLayoutListener(this);

        mHintText.setText("正在初始化中，请不要点击");
        mCheckEye.setEnabled(false);
        mCheckHead.setEnabled(false);
        mCheckMouth.setEnabled(false);
        mSetZero.setEnabled(false);
        mSetZero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHintText.setText("已清零，请再次触发动作");
                mSum = 0;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        FaceDetectSDK.with().onStart();
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mMyHandler = new MyHanlder();
    }

    @Override
    public void onResume() {
        super.onResume();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).

        textureView.setRadius(300);
        textureView.turnRound();

        mMotions.add(Constants.MOTION_MOUTH);
        mMotions.add(Constants.MOTION_EYE);
        mMotions.add(Constants.MOTION_HEAD);

        FaceDetectSDK.with()
                .setFaceDetectListener(this)
                .setMotionList(mMotions, true)
                .init(getActivity(), textureView);
    }

    @Override
    public void onPause() {
        FaceDetectSDK.with().onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FaceDetectSDK.with().destroy();
    }


    @Override
    public void onBitMap(Bitmap bitmap) {
        Message message = Message.obtain();
        message.what = 1;
        message.obj = bitmap;
        mMyHandler.sendMessage(message);
    }

    @Override
    public void onActionMotion(int motion) {
        Message message = Message.obtain();
        message.what = 2;
        message.obj = motion;
        mMyHandler.sendMessage(message);
    }

    @Override
    public void onMotionCheckStart(int motion) {
        switch (motion) {
            case Constants.MOTION_MOUTH:
                Log.w("wangpei", "开始检测：张嘴");
                break;
            case Constants.MOTION_EYE:
                Log.w("wangpei", "开始检测：眨眼");
                break;
            case Constants.MOTION_HEAD:
                Log.w("wangpei", "开始检测：摇头");
                break;
        }

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

    @Override
    public void onComplete() {
        this.getActivity().finish();
    }

    @Override
    public void onGlobalLayout() {
        textureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }


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