package com.tradplus.startapp;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.startapp.sdk.ads.nativead.NativeAdDetails;
import com.startapp.sdk.ads.nativead.NativeAdDisplayListener;
import com.startapp.sdk.ads.nativead.NativeAdInterface;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;

import java.util.ArrayList;
import java.util.List;

public class StartappNativeAd extends TPBaseAd {

    private TPNativeAdView mTpNativeAdView;
    private NativeAdDetails mNativeAdDetail;
    public static final String TAG = "StartAppNative";

    public StartappNativeAd(Context context, NativeAdDetails nativeAdDetail) {
        this.mNativeAdDetail = nativeAdDetail;
        initNativeAd();
    }

    private void initNativeAd() {
        if (mNativeAdDetail != null) {
            mTpNativeAdView = new TPNativeAdView();
            mTpNativeAdView.setTitle(mNativeAdDetail.getTitle());
            mTpNativeAdView.setSubTitle(mNativeAdDetail.getDescription());
            mTpNativeAdView.setCallToAction(mNativeAdDetail.getCallToAction());
            mTpNativeAdView.setIconImageUrl(mNativeAdDetail.getSecondaryImageUrl());
            mTpNativeAdView.setMainImageUrl(mNativeAdDetail.getImageUrl());
        }
    }

    @Override
    public ArrayList<String> getDownloadImgUrls() {
        downloadImgUrls.clear();

        if (!TextUtils.isEmpty(mTpNativeAdView.getIconImageUrl())) {
            downloadImgUrls.add(mTpNativeAdView.getIconImageUrl());
        }

        if (!TextUtils.isEmpty(mTpNativeAdView.getMainImageUrl())) {
            downloadImgUrls.add(mTpNativeAdView.getMainImageUrl());
        }
        return super.getDownloadImgUrls();
    }

    @Override
    public Object getNetworkObj() {
        return mNativeAdDetail == null ? null : mNativeAdDetail;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        mNativeAdDetail.registerViewForInteraction(viewGroup, clickViews, new NativeAdDisplayListener() {
            @Override
            public void adHidden(NativeAdInterface nativeAdInterface) {
                Log.i(TAG, "adHidden: ");
            }

            @Override
            public void adDisplayed(NativeAdInterface nativeAdInterface) {
                Log.i(TAG, "adDisplayed: ");
                if (mShowListener != null) {
                    mShowListener.onAdShown();
                }
            }

            @Override
            public void adClicked(NativeAdInterface nativeAdInterface) {
                Log.i(TAG, "adClicked: ");
                if (mShowListener != null) {
                    mShowListener.onAdClicked();
                }
            }

            @Override
            public void adNotDisplayed(NativeAdInterface nativeAdInterface) {
                Log.i(TAG, "adNotDisplayed: ");
            }
        });
    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return mTpNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        return TPBaseAd.AD_TYPE_NORMAL_NATIVE;
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
        return null;
    }

    @Override
    public void clean() {

    }
}
