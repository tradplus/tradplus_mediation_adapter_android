package com.tradplus.criteo;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.criteo.publisher.advancednative.CriteoMedia;
import com.criteo.publisher.advancednative.CriteoMediaView;
import com.criteo.publisher.advancednative.CriteoNativeAd;
import com.criteo.publisher.advancednative.RendererHelper;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;

import java.util.ArrayList;
import java.util.List;

public class TPCriteoNativeAd extends TPBaseAd {

    private TPNativeAdView mTpNativeAdView;
    private CriteoNativeAd mNativeAD;
    private Context mContext;
    private ViewGroup mNativeContainer;
    private ViewGroup mViewGroup;
    private CriteoMediaView mCriteoMediaView;

    public TPCriteoNativeAd(CriteoNativeAd nativeAd, Context context) {
        if (nativeAd == null || context == null) {
            return;
        }
        mNativeAD = nativeAd;
        mContext = context;
        initAdViewData(nativeAd);
    }

    public void initData(CriteoMedia criteoMedia, RendererHelper helper) {
        helper.setMediaInView(criteoMedia, mCriteoMediaView);
    }

    private void initAdViewData(CriteoNativeAd nativeAd) {
        mTpNativeAdView = new TPNativeAdView();

        String title = nativeAd.getTitle();
        if (!TextUtils.isEmpty(title)) {
            mTpNativeAdView.setTitle(title);
        }

        String description = nativeAd.getDescription();
        if (!TextUtils.isEmpty(description)) {
            mTpNativeAdView.setSubTitle(description);
        }

        // Advertiser Name
        String advertiserDescription = nativeAd.getAdvertiserDescription();
        if (!TextUtils.isEmpty(advertiserDescription)) {
            mTpNativeAdView.setAdvertiserName(advertiserDescription);
        }

        String callToAction = nativeAd.getCallToAction();
        if (!TextUtils.isEmpty(callToAction)) {
            mTpNativeAdView.setCallToAction(callToAction);
        }


        mCriteoMediaView = new CriteoMediaView(mContext);
        mTpNativeAdView.setMediaView(mCriteoMediaView);
    }

    @Override
    public Object getNetworkObj() {
        return mNativeAD == null ? null : mNativeAD;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        if (viewGroup == null) {
            return;
        }
        this.mViewGroup = viewGroup;
        mNativeContainer = (ViewGroup) mNativeAD.createNativeRenderedView(mContext, viewGroup);
    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return mTpNativeAdView;
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
        return mNativeContainer;
    }

    @Override
    public void clean() {

    }

    public ViewGroup getmViewGroup() {
        return mViewGroup;
    }

    public void onAdShown() {
        if (mShowListener != null) {
            mShowListener.onAdShown();
        }
    }

    public void onAdClicked() {
        if (mShowListener != null) {
            mShowListener.onAdClicked();
        }
    }

    public void onAdClosed() {
        if (mShowListener != null) {
            mShowListener.onAdClosed();
        }
    }

    public void onAdVideoError(TPError tpError) {
        if (mShowListener != null) {
            mShowListener.onAdVideoError(tpError);
        }
    }
}
