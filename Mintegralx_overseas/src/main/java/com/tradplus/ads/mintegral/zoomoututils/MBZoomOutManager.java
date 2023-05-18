package com.tradplus.ads.mintegral.zoomoututils;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;


import com.mbridge.msdk.out.MBSplashHandler;
import com.mbridge.msdk.out.ZoomOutTypeEnum;

public class MBZoomOutManager {
    private static final String TAG = "MBZoomOutManager";
    private static final int LEFT = 0;
    private static final int RIGHT = 1;

    private int zoomOutWidth;
    private int zoomOutHeight;
    private int zoomOutMargin;
    private int zoomOutAbove;
    private int zoomOutPos;
    private int zoomOutAnimationTime;
    private int zoomOutAnimationAlphaTime;

    private View splashView;
    private ZoomOutTypeEnum typeEnum;
    private MBSplashHandler mbSplashHandler;
    private ViewGroup zoomOutViewLayout;

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
        private static MBZoomOutManager mInstance = new MBZoomOutManager();
    }

    public static MBZoomOutManager getInstance() {
        return Holder.mInstance;
    }

    private MBZoomOutManager() {
    }

    private void updateZoomOutWAndH(ZoomOutTypeEnum zoomOutTypeEnum, Context context) {

        if (decorViewWidth==0) {
            decorViewWidth = DimenUtils.getDisplayWidth(context);
        }
        if(decorViewHeight==0){
            decorViewHeight = DimenUtils.getDisplayHeight(context);
        }
        typeEnum = zoomOutTypeEnum;
        zoomOutMargin = DimenUtils.dip2px(context, 6);
        zoomOutAbove = DimenUtils.dip2px(context, 10);
        zoomOutPos = RIGHT;
        zoomOutAnimationTime = 500;
        zoomOutAnimationAlphaTime = 500;
        switch (zoomOutTypeEnum) {
            case FloatBall:
                zoomOutWidth = DimenUtils.dip2px(context, 96);
                zoomOutHeight = DimenUtils.dip2px(context, 96);
                break;
            case SmallView:
                zoomOutWidth = Math.round(decorViewWidth);
                zoomOutHeight = DimenUtils.dip2px(context, 49);
                break;
            case MediumView:
                zoomOutWidth = Math.round(decorViewWidth);
                zoomOutHeight = DimenUtils.dip2px(context, 58);
                break;
            case BigView:
                zoomOutWidth = Math.round(decorViewWidth);
                zoomOutHeight = DimenUtils.dip2px(context, 201);
                break;
        }
    }

    public void setSplashInfo(MBSplashHandler mbSplashHandler, View splashView, View decorView) {
        this.mbSplashHandler = mbSplashHandler;
        this.splashView = splashView;
        splashView.getLocationOnScreen(originSplashPos);
        originSplashWidth = splashView.getWidth();
        originSplashHeight = splashView.getHeight();
        decorViewWidth = decorView.getWidth();
        decorViewHeight = decorView.getHeight();
    }

    public void clearStaticData() {
        splashView = null;
        mbSplashHandler.onDestroy();
        mbSplashHandler = null;
    }


    public ViewGroup startZoomOut(final ViewGroup animationContainer,
                                  final ViewGroup zoomOutContainer,
                                  final AnimationCallBack callBack, ZoomOutTypeEnum zoomOutType) {
        Log.d(TAG, "zoomOut startZoomOut activity");
        if (animationContainer == null || zoomOutContainer == null) {
            Log.d(TAG, "zoomOut animationContainer or zoomOutContainer is null");
            return null;
        }
        if (splashView == null) {
            Log.d(TAG, "zoomOut  splashView is null");
            return null;
        }
        int[] animationContainerPos = new int[2];
        animationContainer.getLocationOnScreen(animationContainerPos);
        int x = originSplashPos[0] - animationContainerPos[0];
        int y = originSplashPos[1] - animationContainerPos[1];

        ViewUtils.removeFromParent(splashView);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(originSplashWidth,
                originSplashHeight);

        animationContainer.addView(splashView, layoutParams);
        splashView.setX(x);
        splashView.setY(y);
        return startZoomOut(splashView, animationContainer, zoomOutContainer, callBack, zoomOutType);
    }


    public ViewGroup startZoomOut(final View splash, final ViewGroup animationContainer,
                                  final ViewGroup zoomOutContainer,
                                  final AnimationCallBack callBack, ZoomOutTypeEnum zoomOutType) {
        if (splash == null || zoomOutContainer == null) {
            return null;
        }
        mbSplashHandler.allowClickSplash(false);

        final Context context = zoomOutContainer.getContext();
        final int[] splashScreenPos = new int[2];
        splash.getLocationOnScreen(splashScreenPos);
        final LinearLayout animationLinearLayout = new LinearLayout(context);
        animationContainer.addView(animationLinearLayout, new android.view.WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final int fromWidth = splash.getWidth();
        final int fromHeight = splash.getHeight();
        splash.setLayoutParams(new LinearLayout.LayoutParams(fromWidth, fromHeight));
        int animationContainerWidth = animationLinearLayout.getWidth();
        int animationContainerHeight = animationLinearLayout.getHeight();
        updateZoomOutWAndH(zoomOutType, zoomOutContainer.getContext());
        if (animationContainerWidth == 0) {
            animationContainerWidth = decorViewWidth;
        }
        if (animationContainerHeight == 0) {
            animationContainerHeight = decorViewHeight;
        }
        final ViewGroup zoomOutView;
        if (zoomOutType == ZoomOutTypeEnum.FloatBall) {
            zoomOutViewLayout = new ZoomOutLayout(context, zoomOutMargin);
            zoomOutView = mbSplashHandler.createZoomOutByType(zoomOutType);
            zoomOutViewLayout.addView(zoomOutView, zoomOutWidth,
                    zoomOutHeight);
        } else {
            zoomOutViewLayout = mbSplashHandler.createZoomOutByType(zoomOutType);
        }

        final float animationDistX = zoomOutPos == LEFT ? zoomOutMargin :
                animationContainerWidth - zoomOutMargin - zoomOutWidth;
        final float animationDistY = animationContainerHeight - zoomOutAbove - zoomOutHeight;  //最终位于container的y坐标

        Log.d(TAG, "zoomOut animationContainerWidth:" + animationContainerWidth + " " +
                "animationContainerHeight:" + animationContainerHeight);
        Log.d(TAG, "zoomOut splashScreenX:" + splashScreenPos[0] + " splashScreenY:" + splashScreenPos[1]);
        Log.d(TAG, "zoomOut splashWidth:" + fromWidth + " splashHeight:" + fromHeight);
        Log.d(TAG, "zoomOut width:" + zoomOutWidth + " height:" + zoomOutHeight);
        Log.d(TAG, "zoomOut animationDistX:" + animationDistX + " animationDistY:" + animationDistY);

        ViewUtils.removeFromParent(splash);
        FrameLayout.LayoutParams animationParams = new FrameLayout.LayoutParams(fromWidth, fromHeight);
        animationLinearLayout.addView(splash, animationParams);


        animationLinearLayout.setPivotX(0);
        animationLinearLayout.setPivotY(0);
        int[] zoomOutContainerScreenPos = new int[2];
        zoomOutContainer.getLocationOnScreen(zoomOutContainerScreenPos);
        final float distX = animationDistX - zoomOutContainerScreenPos[0] + splashScreenPos[0];
        final float distY = animationDistY - zoomOutContainerScreenPos[1] + splashScreenPos[1];
        Log.d(TAG, "zoomOut distX:" + distX + " distY:" + distY);
        Log.d(TAG, "zoomOut containerScreenX:" + zoomOutContainerScreenPos[0] + " " +
                "containerScreenY:" + zoomOutContainerScreenPos[1]);

        animationLinearLayout.animate()
                .setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        Log.e(TAG, "getDuration:" + animation.getDuration());
                        float progress = (float) animation.getCurrentPlayTime() / animation.getDuration();
                        if(progress>1){
                            progress =1;
                        }
                        final Rect originRect = new Rect();
                        animationLinearLayout.getGlobalVisibleRect(originRect);
                        final Rect zoomOutRect = new Rect();
                        zoomOutContainer.getGlobalVisibleRect(zoomOutRect);
                        Log.e(TAG,"zoomOutRect:"+zoomOutRect.toString());
                        float animationLinearLayoutTop = originRect.top;
                        float animationLinearLayoutLeft = originRect.left;
                        float animationLinearLayoutBottom= originRect.bottom;
                        float animationLinearLayoutRight = originRect.right;
                        float zoomOutContainerTop = 0;
                        float zoomOutContainerLeft = 0;
                        float zoomOutContainerRight;
                        float zoomOutContainerBottom;
                        if (typeEnum == ZoomOutTypeEnum.FloatBall) {
                            int rightMargin = (int) (zoomOutRect.right-distX-zoomOutWidth>zoomOutMargin?zoomOutRect.right-distX-zoomOutWidth:zoomOutMargin);
                            int bottomMargin = (int) (zoomOutRect.bottom-distY-zoomOutHeight>zoomOutMargin?zoomOutRect.bottom-distY-zoomOutHeight:zoomOutMargin);
                            if(bottomMargin+zoomOutHeight> zoomOutRect.bottom- zoomOutRect.top){
                                bottomMargin = zoomOutRect.bottom-zoomOutRect.top-zoomOutHeight;
                            }
                            zoomOutContainerBottom = zoomOutRect.bottom-bottomMargin;
                            zoomOutContainerRight = zoomOutRect.right-rightMargin;
                            zoomOutContainerTop = zoomOutContainerBottom-zoomOutHeight;
                            zoomOutContainerLeft = zoomOutContainerRight-zoomOutWidth;
                        }else {
                            zoomOutContainerTop = zoomOutRect.top;
                            zoomOutContainerLeft = zoomOutRect.left;
                            zoomOutContainerBottom =  zoomOutContainerTop+zoomOutHeight;
                            zoomOutContainerRight = zoomOutContainerLeft+zoomOutWidth;
                        }

                        Log.e(TAG,"distY:"+distY+"distX"+distX);
                        Log.e(TAG,"originRect:"+originRect.toString());


                        Log.e(TAG,"zoomOutContainer:"+zoomOutContainerLeft+"-"+zoomOutContainerTop+"-"+zoomOutContainerRight+"-"+zoomOutContainerBottom);
                        Rect rect = new Rect((int)(animationLinearLayoutLeft+(zoomOutContainerLeft - animationLinearLayoutLeft)*progress),(int)(animationLinearLayoutTop+(zoomOutContainerTop - animationLinearLayoutTop)*progress),(int)(animationLinearLayoutRight-(animationLinearLayoutRight - zoomOutContainerRight)*progress),(int)(animationLinearLayoutBottom-(animationLinearLayoutBottom - zoomOutContainerBottom)*progress));
                        Log.e(TAG,"rect:"+rect.toString());
                        animationLinearLayout.setClipBounds(rect);

                    }
                })
                .x(0)
                .y(0)
                .setInterpolator(new OvershootInterpolator(0))
                .setDuration(zoomOutAnimationTime)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        Log.d(TAG, "zoomOut onAnimationStart");
                        if (callBack != null) {
                            callBack.animationStart(zoomOutAnimationTime);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        Log.d(TAG, "zoomOut onAnimationEnd");


                        animationLinearLayout.animate().alpha(0).setDuration(zoomOutAnimationAlphaTime).setUpdateListener(null).setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                ViewUtils.removeFromParent(animationLinearLayout);
                                animationLinearLayout.setScaleX(1);
                                animationLinearLayout.setScaleY(1);
                                animationLinearLayout.setX(0);
                                animationLinearLayout.setY(0);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        }).start();


                        final Rect originRect = new Rect();
                        animationLinearLayout.getGlobalVisibleRect(originRect);
                        RelativeLayout.LayoutParams zoomOutParams = new RelativeLayout.LayoutParams(zoomOutWidth,
                                zoomOutHeight);
                        if (typeEnum == ZoomOutTypeEnum.FloatBall) {
                            zoomOutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                            zoomOutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                            final Rect zoomOutRect = new Rect();
                            zoomOutContainer.getGlobalVisibleRect(zoomOutRect);
                            Log.e(TAG,"zoomOutRect:"+zoomOutRect.toString());
                            int rightMargin = (int) (zoomOutRect.right-distX-zoomOutWidth>zoomOutMargin?zoomOutRect.right-distX-zoomOutWidth:zoomOutMargin);
                            int bottomMargin = (int) (zoomOutRect.bottom-distY-zoomOutHeight>zoomOutMargin?zoomOutRect.bottom-distY-zoomOutHeight:zoomOutMargin);
                            if(bottomMargin+zoomOutHeight> zoomOutRect.bottom- zoomOutRect.top){
                                bottomMargin = zoomOutRect.bottom-zoomOutRect.top-zoomOutHeight;
                            }
                            zoomOutParams.rightMargin = rightMargin;
                            zoomOutParams.bottomMargin = bottomMargin;
                        }
                        zoomOutContainer.addView(zoomOutViewLayout, zoomOutParams);

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
        return zoomOutViewLayout;
    }

    public void dismissZoomView() {
        if (zoomOutViewLayout != null) {
            ViewUtils.removeFromParent(zoomOutViewLayout);
        }
        mbSplashHandler.zoomOutPlayFinish();
    }
}
