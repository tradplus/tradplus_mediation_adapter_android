package com.tradplus.ads.yandex;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;


import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdEventListener;
import com.yandex.mobile.ads.nativeads.template.NativeBannerView;

import java.util.ArrayList;
import java.util.List;

public class YandexNativeAd extends TPBaseAd {

    private TPNativeAdView mTPNativeAdView;
    private Context mContext;
    private NativeAd mNativeAd;
    private NativeBannerView mNativeBannerView;
    private static final String TAG = "YandexNative";

    public YandexNativeAd(Context context, NativeAd nativeAd) {
        mNativeAd = nativeAd;
        mContext = context;
        nativeAd.setNativeAdEventListener(new NativeAdEventListener() {
            @Override
            public void onAdClicked() {
                Log.i(TAG, "onAdClicked: ");
                if (mShowListener != null) {
                    mShowListener.onAdClicked();
                }
            }

            @Override
            public void onLeftApplication() {

            }

            @Override
            public void onReturnedToApplication() {

            }

            @Override
            public void onImpression(ImpressionData impressionData) {
                Log.i(TAG, "onImpression: ");
                if (mShowListener != null) {
                    mShowListener.onAdShown();
                }
            }
        });
        mNativeBannerView = new NativeBannerView(context);
        mNativeBannerView.setAd(nativeAd);
    }

    @Override
    public Object getNetworkObj() {
        return mNativeAd;
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
        return AD_TYPE_NATIVE_EXPRESS;
    }

    @Override
    public View getRenderView() {
        return mNativeBannerView;
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
