package com.tradplus.ads.google;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;

import java.util.ArrayList;
import java.util.List;

public class AdManagerNativeAd extends TPBaseAd {

    private int isRender;
    private NativeAd mNativeAd;
    private NativeAdView mNativeAdView;
    private TPNativeAdView mTpNativeAdView;
    private MediaView mMediaView;
    private Context mContext;
    private AdManagerAdView mAdManagerAdView;
    private static final String TAG = "GAMNative";

    public AdManagerNativeAd(AdManagerAdView nativeAd, int templateRend) {
        mAdManagerAdView = nativeAd;
        isRender = templateRend;
    }

    public AdManagerNativeAd(Context context, NativeAd nativeAd, int templateRend) {
        this.mContext = context;
        this.mNativeAd = nativeAd;
        this.isRender = templateRend;
        mNativeAdView = new NativeAdView(context);
        mTpNativeAdView = new TPNativeAdView();


        List<NativeAd.Image> images = mNativeAd.getImages();
        MediaContent mediaContent = mNativeAd.getMediaContent();
        if (mediaContent != null) {
            mMediaView = new MediaView(context);
            mMediaView.setMediaContent(mediaContent);
            boolean videoContent = mediaContent.hasVideoContent();
            if (videoContent) {
                VideoController videoController = mediaContent.getVideoController();
                videoController.setVideoLifecycleCallbacks(videoLifecycleCallbacks);
            }
            mTpNativeAdView.setMediaView(mMediaView);
        }else if (images != null && images.size() > 0) {
            NativeAd.Image image = images.get(0);
            if (image != null) {
                Drawable drawable = image.getDrawable();
                if (drawable != null) {
                    mTpNativeAdView.setMainImage(drawable);
                }
            }
        }

        NativeAd.Image icon = mNativeAd.getIcon();
        if (icon != null) {
            Drawable drawable = icon.getDrawable();
            Uri uri = icon.getUri();
            if (drawable != null) {
                mTpNativeAdView.setIconImage(drawable);
            } else if (uri != null) {
                mTpNativeAdView.setIconImageUrl(uri.toString());
            } else {
                Log.i(TAG, "icon == null");
            }
        }

        String headline = mNativeAd.getHeadline();
        if (!TextUtils.isEmpty(headline)) {
            mTpNativeAdView.setTitle(headline);
        }

        String body = mNativeAd.getBody();
        if (!TextUtils.isEmpty(body)) {
            mTpNativeAdView.setSubTitle(body);
        }

        String callToAction = mNativeAd.getCallToAction();
        if (!TextUtils.isEmpty(callToAction)) {
            mTpNativeAdView.setCallToAction(callToAction);
        }

        String advertiser = mNativeAd.getAdvertiser();
        if (!TextUtils.isEmpty(advertiser)) {
            mTpNativeAdView.setAdvertiserName(advertiser);
        }

        Double starRating = mNativeAd.getStarRating();
        Log.i("StarRating", "GAM StarRating: " + starRating);
        if (starRating != null) {
            mTpNativeAdView.setStarRating(starRating);
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

    public void onAdViewClosed() {
        if (mShowListener != null) {
            mShowListener.onAdClosed();
        }
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        View imageView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_IMAGE);
        if (mTpNativeAdView.getMediaView() != null) {
            mNativeAdView.setMediaView((MediaView) mTpNativeAdView.getMediaView());
        } else if (imageView != null) {
            mNativeAdView.setImageView(imageView);
        }

        View iconView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_ICON);
        if (iconView != null) {
            mNativeAdView.setIconView(iconView);
        }

        View titleView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_TITLE);
        if (titleView != null) {
            mNativeAdView.setHeadlineView(titleView);
        }

        View subTitleView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_SUBTITLE);
        if (subTitleView != null) {
            mNativeAdView.setBodyView(subTitleView);
        }

        View callToAction = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_CALLTOACTION);
        if (callToAction != null) {
            mNativeAdView.setCallToActionView(callToAction);
        }


        mNativeAdView.setNativeAd(mNativeAd);


    }

    private VideoController.VideoLifecycleCallbacks videoLifecycleCallbacks =  new VideoController.VideoLifecycleCallbacks() {
        @Override
        public void onVideoEnd() {
            Log.i(TAG, "onVideoEnd: ");
            if (mShowListener != null) {
                mShowListener.onAdVideoEnd();
            }
        }

        @Override
        public void onVideoMute(boolean b) {

        }

        @Override
        public void onVideoPause() {

        }

        @Override
        public void onVideoPlay() {

        }

        @Override
        public void onVideoStart() {
            Log.i(TAG, "onVideoStart: ");
            if (mShowListener != null) {
                mShowListener.onAdVideoStart();
            }
        }
    };


    @Override
    public Object getNetworkObj() {
        if (mNativeAd != null) {
            return mNativeAd;
        } else if (mAdManagerAdView != null) {
            return mAdManagerAdView;
        }
        return null;
    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return mTpNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        if (isRender == TPBaseAd.AD_TYPE_NORMAL_NATIVE) {
            return TPBaseAd.AD_TYPE_NORMAL_NATIVE;
        } else {
            return TPBaseAd.AD_TYPE_NATIVE_EXPRESS;
        }
    }

    @Override
    public View getRenderView() {
        return mAdManagerAdView;
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
        if (mNativeAdView != null) {
            mNativeAdView.destroy();
        }
    }
}
