# 人脸活体识别SDK使用指南

*当前文档版本：1.0.0*

*当前活体校验版本：1.8.6*

#### 引入

将提供的名为**dlib-release.aar**的文件夹放入到Android工程中的**libs**文件夹中，然后将其加入到项目中。

在**App级别**的build.gradle中，**dependencies**节点下新增：

```xml
implementation(name: 'dlib-release', ext: 'aar')
```

#### 文件导入【重要，必要检测条件】

由于该库使用了Dlib的方式进行的活体校验，所以需要一个名为：**shape_predictor_68_face_landmarks.dat**的人脸特征点训练文件，要将拷贝到手机存储根目录中。

同时SDK中提供了拷贝的方法：

```java
//从raw目录拷贝到手机存储根目录
//同时也可以自己定义逻辑，不强制使用，但是必须要在活体校验开始前，将这一步完成
FileUtils.copyFileFromRawToOthers(@NonNull final Context context
            , @RawRes int id, @NonNull final String targetPath);
```

#### 开始使用

##### 布局

```xml
<!-- 大小和尺寸自己定义即可 -->
<RelativeLayout
            android:id="@+id/content_layout"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_below="@+id/face_hint"
            android:layout_centerHorizontal="true"
            android:layout_marginTop="85dp">
			
        	<!-- 圆形外框（如果使用非圆形的预览，那么去掉即可）-->
            <com.tzutalin.dlib.RoundFrameLayout
                android:id="@+id/fr_camera_view_layout"
                android:layout_width="754dp"
                android:layout_height="754dp"
                android:layout_centerInParent="true">
                
				<!-- 初始化显示的控件（人脸识别的显示范围） -->
                <com.tzutalin.dlib.AutoFitTextureView
                    android:id="@+id/fr_camera_view"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content" />

            </com.tzutalin.dlib.RoundFrameLayout>
        </RelativeLayout>
```

##### 代码

1.在Fragment或者Activity中继承：

```java
implements ViewTreeObserver.OnGlobalLayoutListener
implements FaceDetectListener

//在onGlobalLayout回调中调用以下代码

//这个就是上述参考布局中的控件ID
fr_camera_view.viewTreeObserver.removeOnGlobalLayoutListener(this)
//如果是圆形预览的话，设置圆形的弧度
fr_camera_view_layout.radius = 33F
fr_camera_view_layout.turnRound()
```

2.调用

```java
//分别在对应的生命周期中加入
onStart() {
    super.onStart()
    FaceDetectSDK.with().onStart()
}

onResume() {
	 FaceDetectSDK.with()
            .setFaceDetectListener(this)
         	//第一个参数：motions为Constants类中的三个检测种类的集合
         	//第二个参数：是否随机顺序检测
            .setMotionList(motions, true)
            .init(
                getActivity(),
                fr_camera_view
            )
        fr_camera_view.viewTreeObserver.addOnGlobalLayoutListener(this)
}

onPause() {
    super.onPause()
    FaceDetectSDK.with().onPause()
}

onDestroy() {
    super.onDestroy()
    FaceDetectSDK.with().onDestroy()
}
```

3.部分回调释义

```java
//相机每一帧中的数据
void onBitMap(Bitmap bitmap);
//动作被触发
void onActionMotion(int motion);
//开始检测某个动作
void onMotionCheckStart(int motion);
//已检测到人脸
void onReady();
//全部动作检测完毕
void onComplete();

//其中包括的动作为：（均在Constants类中，自行调用即可）
public final static int MOTION_MOUTH = 1;//(张嘴)
public final static int MOTION_EYE = 2;//(眨眼)
public final static int MOTION_HEAD = 3;//(左右摇头)
```

#### 额外API方法

```java
//设置是否只检测张嘴动作
setMouthMotionType(boolean isOnlyMouth);
//设置是否只检测摇头动作
setHeadMotionType(boolean isOnlyHead);
//设置是否只检测眨眼动作
setEyeMotionType(boolean isOnlyEye);
```



