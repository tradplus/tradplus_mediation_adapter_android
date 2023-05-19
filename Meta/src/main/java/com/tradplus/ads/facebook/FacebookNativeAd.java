package com.tradplus.ads.facebook;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.facebook.ads.AdOptionsView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdBase;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeBannerAd;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPImageLoader;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.ArrayList;
import java.util.List;


public class FacebookNativeAd extends TPBaseAd {

    private static final String TAG = "FacebookNative";
    private NativeAd mFacebookNative;
    private TPNativeAdView mNativeAdView;
    private NativeAdLayout mContainer;
    private NativeBannerAd mFacebookNativeBanner;
    private AdOptionsView adOptionsView;
    private View mFacebookView;
    private int isRender;
    private boolean mSingleIcon = false;

    public FacebookNativeAd(View facebookView, int templete) {
        mFacebookView = facebookView;
        isRender = templete;
    }

    public FacebookNativeAd(Context context, NativeAd facebookNative, boolean singleIcon) {
        mFacebookNative = facebookNative;
        mSingleIcon = singleIcon;
        initNativeAd(context);
    }

    public FacebookNativeAd(Context context, NativeBannerAd facebookNative, boolean singleIcon) {
        mFacebookNativeBanner = facebookNative;
        mSingleIcon = singleIcon;
        initNativeBannerAd(context);
    }

    private void initNativeAd(Context context) {
        mNativeAdView = new TPNativeAdView();
        mContainer = new NativeAdLayout(context);
        MediaView nativeAdMedia = new MediaView(context);
        adOptionsView = new AdOptionsView(context, mFacebookNative, mContainer);
        adOptionsView.setSingleIcon(mSingleIcon);
        mNativeAdView.setAdChoiceView(adOptionsView);

        String adCallToAction = mFacebookNative.getAdCallToAction();
        if (!TextUtils.isEmpty(adCallToAction)) {
            mNativeAdView.setCallToAction(adCallToAction);
        }

        String adHeadline = mFacebookNative.getAdHeadline();
        if (!TextUtils.isEmpty(adHeadline)) {
            mNativeAdView.setTitle(adHeadline);
        }

        String adBodyText = mFacebookNative.getAdBodyText();
        if (!TextUtils.isEmpty(adBodyText)) {
            mNativeAdView.setSubTitle(adBodyText);
        }

        NativeAdBase.Image adIcon = mFacebookNative.getAdIcon();
        if (adIcon != null) {
            String url = adIcon.getUrl();
            if (!TextUtils.isEmpty(url)) {
                mNativeAdView.setIconImageUrl(url);
            }
        }

        NativeAdBase.Rating adStarRating = mFacebookNative.getAdStarRating();
        Log.i("StarRating", "Meta adStarRating: " + adStarRating);
        if (adStarRating != null) {
            double starRating = adStarRating.getValue();
            if (starRating > 0) {
                Log.i("StarRating", "Meta starRating: " + starRating);
                mNativeAdView.setStarRating(starRating);
            }
        }

        String advertiserName = mFacebookNative.getAdvertiserName();
        if (!TextUtils.isEmpty(advertiserName)) {
            mNativeAdView.setAdvertiserName(advertiserName);
        }

        mNativeAdView.setMediaView(nativeAdMedia);

    }

    @Override
    public ArrayList<String> getDownloadImgUrls() {
        downloadImgUrls.clear();

        if (!TextUtils.isEmpty(mNativeAdView.getIconImageUrl())) {
            downloadImgUrls.add(mNativeAdView.getIconImageUrl());
        }

        return super.getDownloadImgUrls();
    }

    private void initNativeBannerAd(Context context) {
        mNativeAdView = new TPNativeAdView();
        mContainer = new NativeAdLayout(context);
        adOptionsView = new AdOptionsView(context, mFacebookNativeBanner, mContainer);
        adOptionsView.setSingleIcon(mSingleIcon);
        mNativeAdView.setAdChoiceView(adOptionsView);

        String adCallToAction = mFacebookNativeBanner.getAdCallToAction();
        if (!TextUtils.isEmpty(adCallToAction)) {
            mNativeAdView.setCallToAction(adCallToAction);
        }

        String adHeadline = mFacebookNativeBanner.getAdHeadline();
        if (!TextUtils.isEmpty(adHeadline)) {
            mNativeAdView.setTitle(adHeadline);
        }

        String adBodyText = mFacebookNativeBanner.getAdBodyText();
        if (!TextUtils.isEmpty(adBodyText)) {
            mNativeAdView.setSubTitle(adBodyText);
        }

        String advertiserName = mFacebookNativeBanner.getAdvertiserName();
        if (!TextUtils.isEmpty(advertiserName)) {
            mNativeAdView.setAdvertiserName(advertiserName);
            mNativeAdView.setAdSource(advertiserName);
        }

        NativeAdBase.Rating adStarRating = mFacebookNativeBanner.getAdStarRating();
        Log.i("StarRating", "Meta adStarRating: " + adStarRating);
        if (adStarRating != null) {
            double starRating = adStarRating.getValue();
            if (starRating > 0) {
                Log.i("StarRating", "Meta starRating: " + starRating);
                mNativeAdView.setStarRating(starRating);
            }
        }

        NativeAdBase.Image adIcon = mFacebookNativeBanner.getAdIcon();
        if (adIcon != null) {
            String url = adIcon.getUrl();
            if (!TextUtils.isEmpty(url)) {
                mNativeAdView.setIconImageUrl(url);
            }
        }

        TPImageLoader.getInstance().loadImage(null, mFacebookNativeBanner.getAdIcon().getUrl());
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
        if (mFacebookView != null) {
            return mFacebookView;
        }

        if (mFacebookNative != null) {
            return mFacebookNative;
        }

        if (mFacebookNativeBanner != null) {
            return mFacebookNativeBanner;
        }

        return null;

    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        if (mFacebookNativeBanner != null) {
            View iconView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_ICON);
            FrameLayout adChoicesView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_ADCHOICES);
            if (adChoicesView != null && adOptionsView != null) {
                if (adOptionsView.getParent() != null) {
                    ((ViewGroup) adOptionsView.getParent()).removeView(adOptionsView);
                }
                adChoicesView.removeAllViews();
                adChoicesView.addView(adOptionsView, 0);
            }

            if (iconView instanceof ImageView) {
                mFacebookNativeBanner.registerViewForInteraction(
                        viewGroup,
                        (ImageView) iconView,
                        clickViews);
            }
            return;
        }

        if (mFacebookNative != null) {
            View iconView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_ICON);
            if (mNativeAdView != null && mNativeAdView.getMediaView() instanceof MediaView && iconView instanceof ImageView) {
                mFacebookNative.registerViewForInteraction(
                        viewGroup,
                        (MediaView) mNativeAdView.getMediaView(),
                        (ImageView) iconView,
                        clickViews);
            }

            FrameLayout adChoicesView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_ADCHOICES);
            if (adChoicesView != null && adOptionsView != null) {
                if (adOptionsView.getParent() != null) {
                    ((ViewGroup) adOptionsView.getParent()).removeView(adOptionsView);
                }
                adChoicesView.removeAllViews();
                adChoicesView.addView(adOptionsView, 0);
            }
        }
    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return mNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        Log.i(TAG, "isRender: " + isRender);
        if (isRender == AppKeyManager.TEMPLATE_RENDERING_YES) {
            return AD_TYPE_NATIVE_EXPRESS;
        } else {
            return AD_TYPE_NORMAL_NATIVE;
        }
    }

    @Override
    public View getRenderView() {
        if (mFacebookView != null) {
            return mFacebookView;
        } else {
            return null;
        }
    }

    @Override
    public List<View> getMediaViews() {
        return null;
    }

    @Override
    public ViewGroup getCustomAdContainer() {
        return mContainer;
    }

    @Override
    public void clean() {
        if (mFacebookNative != null) {
            mFacebookNative.unregisterView();
            mFacebookNative.destroy();
            mFacebookNative = null;
        }

        if (mFacebookNativeBanner != null) {
            mFacebookNativeBanner.unregisterView();
            mFacebookNativeBanner.destroy();
            mFacebookNativeBanner = null;
        }
        mNativeAdView = null;
        mContainer = null;
    }
}
