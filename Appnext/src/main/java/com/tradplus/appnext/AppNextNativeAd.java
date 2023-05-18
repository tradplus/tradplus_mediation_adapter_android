package com.tradplus.appnext;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.appnext.nativeads.MediaView;
import com.appnext.nativeads.NativeAd;
import com.appnext.nativeads.NativeAdView;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;

import java.util.ArrayList;
import java.util.List;

public class AppNextNativeAd extends TPBaseAd {

    private NativeAd mNtvAd;
    private TPNativeAdView mTpNativeAdView;
    private NativeAdView mNativeAdView;

    public AppNextNativeAd(NativeAd nativeAd,boolean videoMute,boolean autoPlayVideo) {
        mNtvAd = nativeAd;
        initAdViewData(nativeAd,videoMute,autoPlayVideo);
    }

    private void initAdViewData(NativeAd nativeAd,boolean videoMute,boolean autoPlayVideo) {
        Context context = GlobalTradPlus.getInstance().getContext();
        if (context == null) {
            if (mShowListener != null) {
                TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                tpError.setErrorMessage("context == null");
                mShowListener.onAdVideoError(tpError);
            }
            return;
        }

        mTpNativeAdView = new TPNativeAdView();
        mNativeAdView = new NativeAdView(context);

        String adTitle = nativeAd.getAdTitle();
        if (!TextUtils.isEmpty(adTitle)) {
            mTpNativeAdView.setTitle(adTitle);
        }

        String adDescription = nativeAd.getAdDescription();
        if (!TextUtils.isEmpty(adDescription)) {
            mTpNativeAdView.setSubTitle(adDescription);
        }

        String ctaText = nativeAd.getCTAText();
        if (!TextUtils.isEmpty(ctaText)) {
            mTpNativeAdView.setCallToAction(ctaText);
        }

        String iconURL = nativeAd.getIconURL();
        if (!TextUtils.isEmpty(iconURL)) {
            mTpNativeAdView.setIconImageUrl(iconURL);
        }

        String storeRating = nativeAd.getStoreRating();
        Log.i("StarRating", "AppNext StarRating: " + storeRating);
        if (!TextUtils.isEmpty(storeRating)) {
            mTpNativeAdView.setStarRating(Double.valueOf(storeRating));
        }

        MediaView mediaView = new MediaView(context);
        mediaView.setMute(videoMute);
        mediaView.setAutoPLay(autoPlayVideo);
//        mediaView.setClickEnabled(true); // 默认值是true
        nativeAd.setMediaView(mediaView);
        mTpNativeAdView.setMediaView(mediaView);

        mNtvAd.setNativeAdView(mNativeAdView);
    }

    @Override
    public ArrayList<String> getDownloadImgUrls() {
        downloadImgUrls.clear();

        if(!TextUtils.isEmpty(mTpNativeAdView.getIconImageUrl())) {
            downloadImgUrls.add(mTpNativeAdView.getIconImageUrl());
        }
        return super.getDownloadImgUrls();
    }

    @Override
    public Object getNetworkObj() {
        return mNtvAd == null ? null : mNtvAd;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        if (mNtvAd != null) {
            mNtvAd.registerClickableViews(clickViews);
        }
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
        return mNativeAdView;
    }

    @Override
    public void clean() {
        if (mNtvAd != null) {
            mNtvAd.destroy();
        }
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
}
