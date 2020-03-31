package com.tzutalin.dlib;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;



/**
 * Function: #todo
 * Created on 2020/3/31.
 *
 * @author Wangpei
 * Email: wangpei@bamboocloud.cn
 */
public class RoundFrameLayout extends FrameLayout {
    private int radius = 0;

    public RoundFrameLayout(@NonNull Context context) {
        this(context, null);
    }

    public RoundFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                Rect rect = new Rect(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
                outline.setRoundRect(rect, radius);
            }
        });
        setClipToOutline(true);
    }

    public void turnRound() {
        invalidateOutline();
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public int getRadius() {
        return radius;
    }
}