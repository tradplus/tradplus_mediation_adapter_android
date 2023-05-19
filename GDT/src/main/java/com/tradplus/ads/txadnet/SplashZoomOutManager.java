package com.tradplus.ads.txadnet;

import android.animation.Animator;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import com.qq.e.ads.splash.SplashAD;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.common.util.PxUtils;


public class SplashZoomOutManager {
  private static final String TAG = "SplashZoomOutManager";
  private static final int LEFT = 0;
  private static final int RIGHT = 1;

  private int zoomOutWidth;
  private int zoomOutHeight;
  private int zoomOutMargin;
  private int zoomOutAbove;
  private int zoomOutPos;
  private int zoomOutAnimationTime;

  private SplashAD splashAD;
  private View splashView;

  private int originSplashWidth;
  private int originSplashHeight;
  private int[] originSplashPos = new int[2];
  private int decorViewWidth;
  private int decorViewHeight;

  public interface AnimationCallBack {
    void animationStart(int animationTime);

    void animationEnd();
  }

  private static final class Holder {
    private static SplashZoomOutManager instance = new SplashZoomOutManager();
  }

  public static SplashZoomOutManager getInstance() {
    return Holder.instance;
  }

  private SplashZoomOutManager() {
      Context context = GlobalTradPlus.getInstance().getContext();
      if (context != null) {
          int deviceWidth = Math.min(PxUtils.getDeviceHeightInPixel(context), PxUtils.getDeviceWidthInPixel(context));
          zoomOutWidth = Math.round(deviceWidth * 0.3f);
          zoomOutHeight = Math.round(zoomOutWidth * 16 / 9);

          zoomOutMargin = PxUtils.dpToPx(context, 6);
          zoomOutAbove = PxUtils.dpToPx(context, 100);
      }else {
          Log.i("GDTSplashAd", "SplashZoomOutManager: context == null");
          int deviceWidth = 768;
          zoomOutWidth = Math.round(deviceWidth * 0.3f);
          zoomOutHeight = Math.round(zoomOutWidth * 16 / 9);

          zoomOutMargin = 13;
          zoomOutAbove = 220;
      }
      zoomOutPos = RIGHT;
      zoomOutAnimationTime = 300;
  }

  public void setSplashInfo(SplashAD splashAD, View splashView, View decorView) {
    this.splashAD = splashAD;
    this.splashView = splashView;
    splashView.getLocationOnScreen(originSplashPos);
    originSplashWidth = splashView.getWidth();
    originSplashHeight = splashView.getHeight();
    decorViewWidth = decorView.getWidth();
    decorViewHeight = decorView.getHeight();
  }

  public void clearStaticData() {
    splashAD = null;
    splashView = null;
  }

  public SplashAD getSplashAD() {
    return splashAD;
  }

  public ViewGroup startZoomOut(final ViewGroup animationContainer,
                                final ViewGroup zoomOutContainer,
                                final AnimationCallBack callBack) {
    Log.d(TAG,"zoomOut startZoomOut activity");
    if (animationContainer == null || zoomOutContainer == null) {
      Log.d(TAG,"zoomOut animationContainer or zoomOutContainer is null");
      return null;
    }
    if (splashAD == null || splashView == null) {
      Log.d(TAG,"zoomOut splashAD or splashView is null");
      return null;
    }
    int[] animationContainerPos = new int[2];
    animationContainer.getLocationOnScreen(animationContainerPos);
    int x = originSplashPos[0] - animationContainerPos[0];
    int y = originSplashPos[1] - animationContainerPos[1];

    removeFromParent(splashView);
    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(originSplashWidth,
        originSplashHeight);
    animationContainer.addView(splashView, layoutParams);
    splashView.setX(x);
    splashView.setY(y);
    return startZoomOut(splashView, animationContainer, zoomOutContainer, callBack);
  }

  public static void removeFromParent(View view) {
    if (view != null) {
      ViewParent vp = view.getParent();
      if (vp instanceof ViewGroup) {
        ((ViewGroup) vp).removeView(view);
      }
    }
  }

  public ViewGroup startZoomOut(final View splash, final ViewGroup animationContainer,
                                final ViewGroup zoomOutContainer,
                                final AnimationCallBack callBack) {
    clearStaticData();
    if (splash == null || zoomOutContainer == null) {
      return null;
    }
    final Context context = zoomOutContainer.getContext();
    final int[] splashScreenPos = new int[2];
    splash.getLocationOnScreen(splashScreenPos);

    int fromWidth = splash.getWidth();
    int fromHeight = splash.getHeight();
    int animationContainerWidth = animationContainer.getWidth();
    int animationContainerHeight = animationContainer.getHeight();

    if (animationContainerWidth == 0) {
      animationContainerWidth = decorViewWidth;
    }
    if (animationContainerHeight == 0) {
      animationContainerHeight = decorViewHeight;
    }
    float xScaleRatio = (float) zoomOutWidth / fromWidth;
    float yScaleRation = (float) zoomOutHeight / fromHeight;
    final float animationDistX = zoomOutPos == LEFT ? zoomOutMargin :
        animationContainerWidth - zoomOutMargin - zoomOutWidth;
    final float animationDistY = animationContainerHeight - zoomOutAbove - zoomOutHeight;

    Log.d(TAG,"zoomOut animationContainerWidth:" + animationContainerWidth + " " +
        "animationContainerHeight:" + animationContainerHeight);
    Log.d(TAG,"zoomOut splashScreenX:" + splashScreenPos[0] + " splashScreenY:" + splashScreenPos[1]);
    Log.d(TAG,"zoomOut splashWidth:" + fromWidth + " splashHeight:" + fromHeight);
    Log.d(TAG,"zoomOut width:" + zoomOutWidth + " height:" + zoomOutHeight);
    Log.d(TAG,"zoomOut animationDistX:" + animationDistX + " animationDistY:" + animationDistY);

    removeFromParent(splash);
    FrameLayout.LayoutParams animationParams = new FrameLayout.LayoutParams(fromWidth, fromHeight);
    animationContainer.addView(splash, animationParams);

    final ViewGroup zoomOutView = new SplashZoomOutLayout(context, zoomOutMargin);

    splash.setPivotX(0);
    splash.setPivotY(0);
    splash.animate()
        .scaleX(xScaleRatio)
        .scaleY(yScaleRation)
        .x(animationDistX)
        .y(animationDistY)
        .setInterpolator(new OvershootInterpolator(0))
        .setDuration(zoomOutAnimationTime)
        .setListener(new Animator.AnimatorListener() {
          @Override
          public void onAnimationStart(Animator animation) {
            Log.d(TAG,"zoomOut onAnimationStart");
            if (callBack != null) {
              callBack.animationStart(zoomOutAnimationTime);
            }
          }

          @Override
          public void onAnimationEnd(Animator animation) {
            Log.d(TAG,"zoomOut onAnimationEnd");
            removeFromParent(splash);
            splash.setScaleX(1);
            splash.setScaleY(1);
            splash.setX(0);
            splash.setY(0);
            int[] zoomOutContainerScreenPos = new int[2];
            zoomOutContainer.getLocationOnScreen(zoomOutContainerScreenPos);
            float distX = animationDistX - zoomOutContainerScreenPos[0] + splashScreenPos[0];
            float distY = animationDistY - zoomOutContainerScreenPos[1] + splashScreenPos[1];
            Log.d(TAG,"zoomOut distX:" + distX + " distY:" + distY);
            Log.d(TAG,"zoomOut containerScreenX:" + zoomOutContainerScreenPos[0] + " " +
                "containerScreenY:" + zoomOutContainerScreenPos[1]);
            zoomOutView.addView(splash, FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
            FrameLayout.LayoutParams zoomOutParams = new FrameLayout.LayoutParams(zoomOutWidth,
                zoomOutHeight);
            zoomOutContainer.addView(zoomOutView, zoomOutParams);
            zoomOutView.setTranslationX(distX);
            zoomOutView.setTranslationY(distY);
            if (callBack != null) {
              callBack.animationEnd();
            }
          }

          @Override
          public void onAnimationCancel(Animator animation) {
          }

          @Override
          public void onAnimationRepeat(Animator animation) {
          }
        });
    return zoomOutView;
  }

}
