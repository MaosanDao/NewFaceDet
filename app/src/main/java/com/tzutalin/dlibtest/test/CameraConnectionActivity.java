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
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraCaptureSession;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tzutalin.dlib.AutoFitTextureView;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDetectListener;
import com.tzutalin.dlibtest.FaceDetectSDK;
import com.tzutalin.dlibtest.R;

import java.util.ArrayList;

public class CameraConnectionActivity extends Activity implements FaceDetectListener, ViewTreeObserver.OnGlobalLayoutListener {

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
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CameraConnectionActivity.this, text, Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private TextView mHintText;
    private int mSum = 0;

    /**
     * 正在检测的顺序
     */
    private ArrayList<Integer> mMotions = new ArrayList<>(3);

    public static CameraConnectionActivity newInstance() {
        return new CameraConnectionActivity();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_connection_fragment);

        textureView = findViewById(R.id.texture);
        mHintText = findViewById(R.id.hint_text);

        textureView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        mHintText.setText("正在初始化中，请不要点击");
        mMyHandler = new MyHanlder();
    }

    @Override
    public void onStart() {
        super.onStart();
        FaceDetectSDK.with().onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        mMotions.add(Constants.MOTION_MOUTH);
        mMotions.add(Constants.MOTION_EYE);
        mMotions.add(Constants.MOTION_HEAD);

        FaceDetectSDK.with()
                .setFaceDetectListener(this)
                .setMotionList(mMotions, true)
                .init(this, textureView);
    }

    @Override
    public void onPause() {
        FaceDetectSDK.with().onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FaceDetectSDK.with().onDestroy();
    }

    @Override
    public void onBitMap(Bitmap bitmap) {
        Message message = Message.obtain();
        message.what = 1;
        message.obj = bitmap;
        mMyHandler.sendMessage(message);
    }

    @Override
    public void onReady() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        mHintText.setText("已初始化完毕");
                    }
                });
    }

    @Override
    public void onFaceDetected() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        mHintText.setText("已识别到人脸");
                    }
                });
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
            if (msg.what == 1) {
                Bitmap bitmap = (Bitmap) msg.obj;
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