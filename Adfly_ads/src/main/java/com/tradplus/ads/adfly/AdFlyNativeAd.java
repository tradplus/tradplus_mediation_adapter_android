package com.tradplus.ads.adfly;


import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.adfly.sdk.nativead.MediaView;
import com.adfly.sdk.nativead.NativeAd;
import com.adfly.sdk.nativead.NativeAdView;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPAdError;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;

import java.util.ArrayList;
import java.util.List;

public class AdFlyNativeAd extends TPBaseAd {

    private static final String TAG = "AdFlyNative";
    private TPNativeAdView mTPNativeAdView;
    private NativeAdView mNativeAdView;
    private NativeAd mNativeAd;
    private MediaView mMediaView;
    private Activity mActivity;

    public AdFlyNativeAd(Activity activity, NativeAd nativeAd) {
        mActivity = activity;
        mNativeAd = nativeAd;
        initView(activity, nativeAd);
    }

    private void initView(Activity activity, NativeAd nativeAd) {
        mNativeAdView = new NativeAdView(activity);
        mTPNativeAdView = new TPNativeAdView();

        String body = nativeAd.getBody();
        if (!TextUtils.isEmpty(body)) {
            mTPNativeAdView.setSubTitle(body);
        }

        String callToAction = nativeAd.getCallToAction();
        if (!TextUtils.isEmpty(callToAction)) {
            mTPNativeAdView.setCallToAction(callToAction);
        }

        String title = nativeAd.getTitle();
        if (!TextUtils.isEmpty(title)) {
            mTPNativeAdView.setTitle(title);
        }

        String sponsor = nativeAd.getSponsor();
        if (!TextUtils.isEmpty(sponsor)) {
            Log.i(TAG, "initView sponsor: " + sponsor);
            mTPNativeAdView.setAuthorName(sponsor);
        }

        String tag = nativeAd.getTag();
        if (!TextUtils.isEmpty(tag)) {
            Log.i(TAG, "initView tag: " + tag);
            mTPNativeAdView.setAdChoiceUrl(tag);
        }

        // 无ICON
        mMediaView = new MediaView(activity);
        mTPNativeAdView.setMediaView(mMediaView);

    }

    public void onAdViewClicked() {
        if (mShowListener != null) {
            mShowListener.onAdClicked();
        }
    }

    public void onAdViewExpanded() {
        if (mShowListener != null) {
            mShowListener.onAdShown();
        }
    }

    public void onAdVideoError(TPError tpAdError) {
        if (mShowListener != null) {
            mShowListener.onAdVideoError(tpAdError);
        }
    }

    @Override
    public Object getNetworkObj() {
        if (mNativeAd != null) {
            return mNativeAd;
        }

        return null;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        if (mNativeAd != null && mNativeAdView != null)
            mNativeAd.showView(mNativeAdView, mMediaView, clickViews);
    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return mTPNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        return AD_TYPE_NORMAL_NATIVE;
    }

    @Override
    public View getRenderView() {
          return null;
    }

    @Override
    public List<View> getMediaViews() {
        return null;
    }

    @Override
    public ViewGroup getCustomAdContainer() {
        return mNativeAdView;
    }

    @Override
    public void clean() {
        if (mNativeAd != null) {
            mNativeAd.setAdListener(null);
            mNativeAd.destroy();
            mNativeAd.destroyView();
            mNativeAd = null;
        }
    }
}
