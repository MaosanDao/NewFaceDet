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
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraCaptureSession;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tzutalin.dlib.AutoFitTextureView;
import com.tzutalin.dlib.FaceDetectListener;
import com.tzutalin.dlibtest.FaceDetectSDK;
import com.tzutalin.dlibtest.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import timber.log.Timber;

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

        FaceDetectSDK.with()
                .setFaceDetectListener(this)
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
    public void onReady() {
        FaceDetectSDK.with()
                .setDetectMaxInterval(2000)
                .setDetectTimes(1);

        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        mHintText.setText("已初始化完毕");
                    }
                });
    }

    @Override
    public void onFaceDetected(List<Bitmap> bitmaps) {
        Log.d("wangpei", "已识别到人脸");

        for (int i = 0; i < bitmaps.size(); i++) {
            saveBitmap("bitmap" + i, bitmaps.get(i), this);
        }

        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        mHintText.setText("已识别到人脸");
                        finish();
                    }
                });
    }

    private void saveBitmap(String name, Bitmap bm, Context mContext) {
        Log.d("Save Bitmap", "Ready to save picture");
        //指定我们想要存储文件的地址
        String TargetPath = mContext.getExternalCacheDir() + "/images/";
        Log.d("Save Bitmap", "Save Path=" + TargetPath);
        //判断指定文件夹的路径是否存在
        if (!fileIsExist(TargetPath)) {
            Log.d("Save Bitmap", "TargetPath isn't exist");
        } else {
            //如果指定文件夹创建成功，那么我们则需要进行图片存储操作
            File saveFile = new File(TargetPath, name + ".png");

            try {
                FileOutputStream saveImgOut = new FileOutputStream(saveFile);
                // compress - 压缩的意思
                bm.compress(Bitmap.CompressFormat.JPEG, 80, saveImgOut);
                //存储完成后需要清除相关的进程
                saveImgOut.flush();
                saveImgOut.close();
                Log.d("Save Bitmap", "The picture is save to your phone!");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private boolean fileIsExist(String fileName) {
        //传入指定的路径，然后判断路径是否存在
        File file = new File(fileName);
        if (file.exists())
            return true;
        else {
            //file.mkdirs() 创建文件夹的意思
            return file.mkdirs();
        }
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
                Timber.tag("wangpei").d("bitmap:" + bitmap.getRowBytes());
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