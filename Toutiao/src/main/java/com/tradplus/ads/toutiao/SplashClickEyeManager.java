package com.tradplus.ads.toutiao;

import android.animation.Animator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import com.bytedance.sdk.openadsdk.CSJSplashAd;
import com.bytedance.sdk.openadsdk.TTSplashAd;



public class SplashClickEyeManager {
    private static final String TAG = "ToutiaoSplashAd";
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private int mClickEyeViewWidth;
    private int mClickEyeViewHeight;
    private int mClickEyeViewMargin;
    private int mClickEyeViewMarginBottom;
    private int mClickEyeViewPos;
    private int mClickEyeViewAnimationTime;
    private CSJSplashAd mSplashAd;
    private View mSplashShowView;
    private int[] mOriginSplashPos = new int[2];
    private int mDecorViewWidth;
    private int mDecorViewHeight;
    private volatile static SplashClickEyeManager mInstance;
    private boolean mIsSupportSplashClickEye = false;
    private static Context mContext;

    public interface AnimationCallBack {
        void animationStart(int animationTime);

        void animationEnd();
    }


    /**
     *
     * @return
     */
    public static SplashClickEyeManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (SplashClickEyeManager.class) {
                if (mInstance == null) {
                    mContext = context;
                    mInstance = new SplashClickEyeManager();
                }
            }
        }
        return mInstance;
    }

    private SplashClickEyeManager() {
        initClickEyeViewData(mContext);
        mClickEyeViewMargin = UIUtils.dp2px(mContext, 16);
        mClickEyeViewMarginBottom = UIUtils.dp2px(mContext, 100);
        mClickEyeViewPos = RIGHT;
        mClickEyeViewAnimationTime = 300;
    }

    private void initClickEyeViewData(Context context) {
        int deviceWidth = Math.min(UIUtils.getScreenHeightInPx(context), UIUtils.getScreenWidthInPx(context));
        if (mSplashAd != null && mSplashAd.getSplashClickEyeSizeToDp() != null) {
            mClickEyeViewWidth = UIUtils.dp2px(context, mSplashAd.getSplashClickEyeSizeToDp()[0]);
            mClickEyeViewHeight = UIUtils.dp2px(context, mSplashAd.getSplashClickEyeSizeToDp()[1]);
        } else {
            mClickEyeViewWidth = Math.round(deviceWidth * 0.3f);
            mClickEyeViewHeight = Math.round(mClickEyeViewWidth * 16 / 9);
        }
    }

    public void setSplashInfo(CSJSplashAd splashAd, View splashView, View decorView) {
        this.mSplashAd = splashAd;
        this.mSplashShowView = splashView;
        splashView.getLocationOnScreen(mOriginSplashPos);
        mDecorViewWidth = decorView.getWidth();
        mDecorViewHeight = decorView.getHeight();
        initClickEyeViewData(mContext);
    }

    public void clearSplashStaticData() {
        mSplashAd = null;
        mSplashShowView = null;
    }

    public CSJSplashAd getSplashAd() {
        return mSplashAd;
    }

    public ViewGroup startSplashClickEyeAnimationInTwoActivity(final ViewGroup decorView,
                                                               final ViewGroup splashViewContainer,
                                                               final AnimationCallBack callBack) {
        if (decorView == null || splashViewContainer == null) {
            return null;
        }
        if (mSplashAd == null || mSplashShowView == null) {
            return null;
        }
        return startSplashClickEyeAnimation(mSplashShowView, decorView, splashViewContainer, callBack);
    }

    public ViewGroup startSplashClickEyeAnimation(final View splash, final ViewGroup decorView,
                                                  final ViewGroup splashViewContainer,
                                                  final AnimationCallBack callBack) {
        if (splash == null || splashViewContainer == null) {
            return null;
        }
        final int[] splashScreenPos = new int[2];
        splash.getLocationOnScreen(splashScreenPos);
        final Context context = splashViewContainer.getContext();
        int splashViewWidth = splash.getWidth();
        int splashViewHeight = splash.getHeight();
        int animationContainerWidth = decorView.getWidth();
        int animationContainerHeight = decorView.getHeight();

        if (animationContainerWidth == 0) {
            animationContainerWidth = mDecorViewWidth;
        }
        if (animationContainerHeight == 0) {
            animationContainerHeight = mDecorViewHeight;
        }
        float xScaleRatio = (float) mClickEyeViewWidth / splashViewWidth;
        float yScaleRation = (float) mClickEyeViewHeight / splashViewHeight;
        final float animationDistX = mClickEyeViewPos == LEFT ? mClickEyeViewMargin :
                animationContainerWidth - mClickEyeViewMargin - mClickEyeViewWidth;
        final float animationDistY = animationContainerHeight - mClickEyeViewMarginBottom - mClickEyeViewHeight;  //最终位于container的y坐标
        UIUtils.removeFromParent(splash);
        FrameLayout.LayoutParams animationParams = new FrameLayout.LayoutParams(splashViewWidth, splashViewHeight);
        decorView.addView(splash, animationParams);
        final FrameLayout splashViewLayout = new FrameLayout(context);
        splash.setPivotX(0);
        splash.setPivotY(0);
        splash.animate()
                .scaleX(xScaleRatio)
                .scaleY(yScaleRation)
                .x(animationDistX)
                .y(animationDistY)
                .setInterpolator(new OvershootInterpolator(0))
                .setDuration(mClickEyeViewAnimationTime)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        if (callBack != null) {
                            callBack.animationStart(mClickEyeViewAnimationTime);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        UIUtils.removeFromParent(splash);
                        splash.setScaleX(1);
                        splash.setScaleY(1);
                        splash.setX(0);
                        splash.setY(0);
                        int[] clickEyeContainerScreenPos = new int[2];
                        splashViewContainer.getLocationOnScreen(clickEyeContainerScreenPos);
                        float distX = animationDistX - clickEyeContainerScreenPos[0] + splashScreenPos[0];
                        float distY = animationDistY - clickEyeContainerScreenPos[1] + splashScreenPos[1];

                        splashViewLayout.addView(splash, FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT);
                        FrameLayout.LayoutParams clickEyeParams = new FrameLayout.LayoutParams(mClickEyeViewWidth,
                                mClickEyeViewHeight);
                        splashViewContainer.addView(splashViewLayout, clickEyeParams);
                        splashViewLayout.setTranslationX(distX);
                        splashViewLayout.setTranslationY(distY);
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
        return splashViewLayout;
    }

    public boolean isSupportSplashClickEye() {
        return mIsSupportSplashClickEye;
    }

    public void setSupportSplashClickEye(boolean isSupportSplashClickEye) {
        this.mIsSupportSplashClickEye = isSupportSplashClickEye;
    }
}
