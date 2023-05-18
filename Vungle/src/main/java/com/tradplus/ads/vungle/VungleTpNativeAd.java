package com.tradplus.ads.vungle;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.util.DeviceUtils;
import com.vungle.warren.BannerAdConfig;
import com.vungle.warren.Banners;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.error.VungleException;

import java.util.ArrayList;
import java.util.List;

import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class VungleTpNativeAd extends TPBaseAd {

    private VungleBanner mVungleBanner;
    private RelativeLayout mRelativeLayout;
    private static final String TAG = "VungleNative";


    public VungleTpNativeAd(Context context, VungleBanner vungleBanner) {
        mVungleBanner = vungleBanner;
        mRelativeLayout = new RelativeLayout(context);
        mRelativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, DeviceUtils.dip2px(context, 250)));
        mRelativeLayout.setGravity(Gravity.CENTER);
        mRelativeLayout.addView(vungleBanner);

    }


    @Override
    public Object getNetworkObj() {
        if (mVungleBanner != null) {
            return mVungleBanner;
        }
        return null;
    }

    public void onAdClosed() {
        if (mShowListener != null) {
            mShowListener.onAdClosed();
        }
    }

    public void onAdClicked() {
        if (mShowListener != null) {
            mShowListener.onAdClicked();
        }
    }

    public void onAdShown() {
        if (mShowListener != null) {
            mShowListener.onAdShown();
        }
    }

    public void onAdVideoError(TPError tpError) {
        if (mShowListener != null) {
            mShowListener.onAdVideoError(tpError);
        }
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
        if (mRelativeLayout != null) {
            return mRelativeLayout;
        }
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
        if (mVungleBanner != null && mRelativeLayout != null) {
            mVungleBanner.destroyAd();
            mVungleBanner = null;
            mRelativeLayout.removeAllViews();
        }
    }
}
