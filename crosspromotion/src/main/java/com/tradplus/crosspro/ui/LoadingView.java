package com.tradplus.crosspro.ui;

import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.tradplus.ads.base.CommonUtil;


public class LoadingView {

    private ViewGroup mRoot;
    private ImageView mLoadingIv;

    public LoadingView(ViewGroup container) {
        this.mRoot = container;

        mLoadingIv = new ImageView(mRoot.getContext());
        mLoadingIv.setId(CommonUtil.getResId(mRoot.getContext(), "cp_loading_id", "id"));
        mLoadingIv.setImageResource(CommonUtil.getResId(mRoot.getContext(), "cp_loading", "drawable"));
    }

    private void addView() {
        if(mLoadingIv != null) {
            mRoot.removeView(mLoadingIv);
        }

        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, mRoot.getResources().getDisplayMetrics());
        RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(size, size);
        rl.addRule(RelativeLayout.CENTER_IN_PARENT);
        mRoot.addView(mLoadingIv, rl);
    }

    public void startLoading() {
        addView();
        mLoadingIv.post(new Runnable() {
            @Override
            public void run() {
                mLoadingIv.setAlpha(1f);
                RotateAnimation ra = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                ra.setDuration(1000);
                ra.setInterpolator(new LinearInterpolator());
                ra.setRepeatCount(-1);
                mLoadingIv.startAnimation(ra);
            }
        });
    }

    public void hide() {
        if (mLoadingIv != null) {
            mRoot.post(new Runnable() {
                @Override
                public void run() {
                    mLoadingIv.clearAnimation();
                    mLoadingIv.setAlpha(0f);
                    mRoot.removeView(mLoadingIv);
                }
            });
        }
    }

}
