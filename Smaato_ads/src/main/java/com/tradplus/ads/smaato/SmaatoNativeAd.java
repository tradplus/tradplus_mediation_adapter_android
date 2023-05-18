package com.tradplus.ads.smaato;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.smaato.sdk.nativead.NativeAdAssets;
import com.smaato.sdk.nativead.NativeAdRenderer;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;

import java.util.ArrayList;
import java.util.List;

public class SmaatoNativeAd extends TPBaseAd {

    private NativeAdRenderer mRenderer;
    private TPNativeAdView mNativeAdView;
    private NativeAdAssets mNativeAdAssets;

    public SmaatoNativeAd(Context context, NativeAdRenderer renderer) {
        mRenderer = renderer;
        initNativeAd(context);
    }

    private void initNativeAd(Context context) {
        mNativeAdView = new TPNativeAdView();
        // Explicit rendering of raw assets into views
        mNativeAdAssets = mRenderer.getAssets();
        mNativeAdView.setCallToAction(mNativeAdAssets.cta());
        mNativeAdView.setTitle(mNativeAdAssets.text());
        mNativeAdView.setSubTitle(mNativeAdAssets.title());
        mNativeAdView.setAdSource(mNativeAdAssets.sponsored());

        if (mNativeAdAssets.icon() != null && mNativeAdAssets.icon().drawable() != null) {
            mNativeAdView.setIconImage(mNativeAdAssets.icon().drawable());
        }

        List<NativeAdAssets.Image> images = mNativeAdAssets.images();
        if (images != null && images.size() > 0) {
            mNativeAdView.setPicObject(new ArrayList<Object>(images));

            if (images.get(0) != null && images.get(0).drawable() != null) {
                mNativeAdView.setMainImage(images.get(0).drawable());
            }
        }

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

    @Override
    public Object getNetworkObj() {
        return mNativeAdAssets != null ? mNativeAdAssets : null;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        // Register ad view for impression tracking and user clicks handling
        mRenderer.registerForImpression(viewGroup);
        mRenderer.registerForClicks(clickViews);
    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return mNativeAdView;
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
}
