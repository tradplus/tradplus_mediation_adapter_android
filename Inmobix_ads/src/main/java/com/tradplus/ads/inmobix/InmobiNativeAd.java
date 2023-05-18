package com.tradplus.ads.inmobix;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.inmobi.ads.InMobiNative;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.common.TPImageLoader;
import com.tradplus.ads.common.util.DeviceUtils;

import java.util.ArrayList;
import java.util.List;

public class InmobiNativeAd extends TPBaseAd {

    private InMobiNative mInMobiNative;
    private TPNativeAdView mTpNativeAdView;
    private Context context;
    private static final String TAG = "InmobiNative";


    public InmobiNativeAd(Context context, InMobiNative inMobiNative) {
        this.mInMobiNative = inMobiNative;
        this.context = context;
        initNativeAd();
    }

    private void initNativeAd() {
        if (mInMobiNative == null) return;

        mTpNativeAdView = new TPNativeAdView();

        String adTitle = mInMobiNative.getAdTitle();
        if (!TextUtils.isEmpty(adTitle)) {
            mTpNativeAdView.setTitle(adTitle);
        }

        String adDescription = mInMobiNative.getAdDescription();
        if (!TextUtils.isEmpty(adDescription)) {
            mTpNativeAdView.setSubTitle(adDescription);
        }

        String adCtaText = mInMobiNative.getAdCtaText();
        if (!TextUtils.isEmpty(adCtaText)) {
            mTpNativeAdView.setCallToAction(adCtaText);
        }

        float rating = mInMobiNative.getAdRating();
        Log.i("StarRating", "InMoBi StarRating: " + rating);
        Double adRating = (double) rating;
        if (adRating != null){
            mTpNativeAdView.setStarRating(adRating);
        }

        String tempIconUrl = mInMobiNative.getAdIconUrl();
        if (!TextUtils.isEmpty(tempIconUrl)) {
            tempIconUrl = tempIconUrl.replace("http://", "https://");
            mTpNativeAdView.setIconImageUrl(tempIconUrl);
        }


//        TPImageLoader.getInstance().loadImage(null, mInMobiNative.getAdIconUrl());
    }

    @Override
    public ArrayList<String> getDownloadImgUrls() {
        downloadImgUrls.clear();

        if(!TextUtils.isEmpty(mTpNativeAdView.getIconImageUrl())) {
            downloadImgUrls.add(mTpNativeAdView.getIconImageUrl());
        }
        return super.getDownloadImgUrls();
    }

    public void beforeRender(ViewGroup adContainer) {
        if (adContainer != null) {
            FrameLayout frameLayout = new FrameLayout(context);
            View primaryViewOfWidth = mInMobiNative.getPrimaryViewOfWidth(context, frameLayout, adContainer, adContainer.getMeasuredWidth());

            if (primaryViewOfWidth == null) {
                TPError tpError = new TPError(TPError.SHOW_FAILED);
                tpError.setErrorMessage("getPrimaryViewOfWidth == null");
                mShowListener.onAdVideoError(tpError);
                return;
            }

            frameLayout.addView(primaryViewOfWidth, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            if (mTpNativeAdView != null) {
                mTpNativeAdView.setMediaView(frameLayout);
            }
        }
    }

    public void onAdViewExpanded() {
        if (mShowListener != null) {
            mShowListener.onAdShown();
        }
    }

    public void onAdViewClicked() {
        if (mShowListener != null) {
            mShowListener.onAdClicked();
        }
    }

    @Override
    public Object getNetworkObj() {
        return mInMobiNative == null ? null : mInMobiNative;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: ");
                if (mInMobiNative != null) {
                    mInMobiNative.reportAdClickAndOpenLandingPage();
                }
            }

        };

        for (View view : clickViews) {
            view.setOnClickListener(listener);
        }

    }

    private void registerView(View view, View.OnClickListener clickListener) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                registerView(child, clickListener);
            }
        } else {
            view.setOnClickListener(clickListener);
        }
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
        if (mInMobiNative != null) {
            mInMobiNative.setListener(null);
            mInMobiNative.setVideoEventListener(null);
            mInMobiNative.destroy();
            mInMobiNative = null;
        }
    }
}
