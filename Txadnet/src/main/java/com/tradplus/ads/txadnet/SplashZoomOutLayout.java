package com.tradplus.ads.txadnet;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

/*
 * created by timfeng 2020/11/14
 */
public class SplashZoomOutLayout extends FrameLayout {
  private float dX, dY;
  private int margin;
  private int maxX;
  private int maxY;

  private float moveAccumulateX, moveAccumulateY;
  private final int touchSlop;

  public SplashZoomOutLayout(Context context, int m) {
    super(context);
    GradientDrawable gd = new GradientDrawable();
    gd.setCornerRadius(10);
    this.setBackgroundDrawable(gd);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      setClipToOutline(true);
    }
    this.margin = m;
    touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    this.post(new Runnable() {
      @Override
      public void run() {
        View parent = (View) getParent();
        if (parent == null) {
          return;
        }
        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();
        maxY = parentHeight - SplashZoomOutLayout.this.getHeight() - margin;
        maxX = parentWidth - SplashZoomOutLayout.this.getWidth() - margin;
      }
    });
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    switch (event.getAction()) {

      case MotionEvent.ACTION_DOWN:
        dX = getX() - event.getRawX();
        dY = getY() - event.getRawY();
        moveAccumulateX = 0;
        moveAccumulateY = 0;
        break;

      case MotionEvent.ACTION_MOVE:
        float newX = event.getRawX() + dX;
        float newY = event.getRawY() + dY;

        moveAccumulateX += Math.abs(newX - getX());
        moveAccumulateY += Math.abs(newY - getY());
        newX = newX < margin ? margin : newX > maxX ? maxX : newX;
        newY = newY < margin ? margin : newY > maxY ? maxY : newY;
        animate()
            .x(newX)
            .y(newY)
            .setDuration(0)
            .start();
        break;
      case MotionEvent.ACTION_UP:
        float animationX;
        float upX = event.getRawX() + dX;
        if (upX * 2 > maxX) {
          animationX = maxX;
        } else {
          animationX = margin;
        }
        animate()
            .x(animationX)
            .setDuration(0)
            .start();
        if (moveAccumulateX > touchSlop || moveAccumulateY > touchSlop) {
          return true;
        }
      default:
    }
    return super.onInterceptTouchEvent(event);
  }
}
