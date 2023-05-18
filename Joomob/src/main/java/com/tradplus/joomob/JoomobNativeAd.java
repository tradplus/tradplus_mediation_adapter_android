package com.tradplus.joomob;

import android.view.View;
import android.view.ViewGroup;

import com.joomob.sdk.common.ads.JMView;
import com.joomob.sdk.common.proxy.INativeAd;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;

import java.util.ArrayList;
import java.util.List;

public class JoomobNativeAd extends TPBaseAd {
    private INativeAd mAdView;
    private List<View> mJmViewList;
    private int mType;

    public JoomobNativeAd(INativeAd jmView, int type) {
        mAdView = jmView;
        mType = type;
    }

    public JoomobNativeAd(List<View> jmViews, int type) {
        mJmViewList = jmViews;
        mType = type;
    }

    @Override
    public Object getNetworkObj() {
        return mAdView;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {

    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return null;
    }

    @Override
    public int getNativeAdType() {
        if (mType == AD_TYPE_NATIVE_LIST) {
            return AD_TYPE_NATIVE_LIST;
        } else {
            return AD_TYPE_NATIVE_EXPRESS;
        }
    }

    @Override
    public View getRenderView() {
        if (mAdView != null && mAdView.getAdView() != null) {
            return mAdView.getAdView();
        }
        return null;
    }

    @Override
    public List<View> getMediaViews() {
        return mJmViewList;
    }

    @Override
    public ViewGroup getCustomAdContainer() {
        return null;
    }

    public void adClicked() {
        if (mShowListener != null) {
            mShowListener.onAdClicked();
        }
    }

    public void adShown() {
        if (mShowListener != null) {
            mShowListener.onAdShown();
        }
    }

    public void adClosed() {
        if (mShowListener != null) {
            mShowListener.onAdClosed();
        }
    }

    @Override
    public void clean() {

    }
}
