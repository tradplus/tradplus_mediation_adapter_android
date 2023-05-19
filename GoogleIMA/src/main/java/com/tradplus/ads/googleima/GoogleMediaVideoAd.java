package com.tradplus.ads.googleima;

import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;

import java.util.ArrayList;
import java.util.List;

public class GoogleMediaVideoAd extends TPBaseAd {
    
    private AdsManager mAdsManager;
    private static final String TAG = "GoogleMediaVideo";
    
    public GoogleMediaVideoAd(AdsManager adsManager) {
        mAdsManager = adsManager;
    }

    @Override
    public Object getNetworkObj() {
        return mAdsManager;
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
        return 0;
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

    @Override
    public void pause() {
        if (mAdsManager != null) {
            Log.i(TAG, "AdsManager pause : ");
            mAdsManager.pause();
        }
    }

    @Override
    public void resume() {
        if (mAdsManager != null) {
            Log.i(TAG, "AdsManager resume : ");
            mAdsManager.resume();
        }
    }

    @Override
    public void start() {
        if (mAdsManager != null) {
            Log.i(TAG, "AdsManager start : ");
            mAdsManager.start();
        }
    }
}
