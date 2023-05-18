package com.tradplus.mytarget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.my.target.common.NavigationType;
import com.my.target.common.models.ImageData;
import com.my.target.nativeads.NativeAd;
import com.my.target.nativeads.NativeBannerAd;
import com.my.target.nativeads.banners.NativeBanner;
import com.my.target.nativeads.banners.NativePromoBanner;
import com.my.target.nativeads.factories.NativeViewsFactory;
import com.my.target.nativeads.views.IconAdView;
import com.my.target.nativeads.views.MediaAdView;
import com.my.target.nativeads.views.NativeAdContainer;
import com.my.target.nativeads.views.NativeBannerAdView;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;

import java.util.ArrayList;
import java.util.List;

public class MyTargetNativeData extends TPBaseAd {

    private static final String TAG = "MyTargetNative";
    private TPNativeAdView mNativeAdView;
    private NativeBannerAd nativeBannerAd; //NativeBanner
    private NativeBanner nativeBanner; //NativeBanner
    private NativePromoBanner nativePromoBanner; //Native
    private NativeAd mNativeAd;//Native
    private NativeAdContainer mNativeAdContainer;

    public MyTargetNativeData(Context context, NativePromoBanner nativePromoBanner, NativeAd mNativeAd) {
        this.nativePromoBanner = nativePromoBanner;
        this.mNativeAd = mNativeAd;

        initNativeAd(context, nativePromoBanner);
    }

    public MyTargetNativeData(Context context, NativeBanner nativeBanner, NativeBannerAd nativeBannerAd) {
        this.nativeBannerAd = nativeBannerAd;
        this.nativeBanner = nativeBanner;
        initNativeBannerAd(context, nativeBanner);
    }

    private void initNativeAd(Context context, NativePromoBanner nativePromoBanner) {
        mNativeAdView = new TPNativeAdView();
        mNativeAdContainer = new NativeAdContainer(context);
        String title = nativePromoBanner.getTitle();
        if (!TextUtils.isEmpty(title)) {
            mNativeAdView.setTitle(title);
        }

        String description = nativePromoBanner.getDescription();
        if (!TextUtils.isEmpty(description)) {
            mNativeAdView.setSubTitle(description);
        }

        String ctaText = nativePromoBanner.getCtaText();
        if (!TextUtils.isEmpty(ctaText)) {
            mNativeAdView.setCallToAction(ctaText);
        }

        boolean hasVideo = nativePromoBanner.hasVideo();
        Log.i(TAG, "hasVideo: " + hasVideo);
        MediaAdView mediaAdView = new MediaAdView(context);
        mNativeAdView.setMediaView(mediaAdView);

        IconAdView iconAdView = new IconAdView(context);
        mNativeAdView.setIconView(iconAdView);

        String navigationType = nativePromoBanner.getNavigationType();
        if (!TextUtils.isEmpty(navigationType)) {
            if (NavigationType.WEB.equals(navigationType)) {
                String domain = nativePromoBanner.getDomain();
                if (!TextUtils.isEmpty(domain)) {
                    mNativeAdView.setNativeAdSocialContext(domain);
                }
            } else if (NavigationType.STORE.equals(navigationType)) {
                float rating = nativePromoBanner.getRating();
                Log.i("StarRating", "MyTarget StarRating: " + rating);
                Double adRating = (double) rating;
                if (adRating != null) {
                    mNativeAdView.setStarRating(adRating);
                }
            }
        }


    }

    private void initNativeBannerAd(Context context, NativeBanner nativeBanner) {
        mNativeAdView = new TPNativeAdView();
        mNativeAdContainer = new NativeAdContainer(context);
        String title = nativeBanner.getTitle();
        if (!TextUtils.isEmpty(title)) {
            mNativeAdView.setTitle(title);
        }

        String ctaText = nativeBanner.getCtaText();
        if (!TextUtils.isEmpty(ctaText)) {
            mNativeAdView.setCallToAction(ctaText);
        }

        String description = nativeBanner.getDescription();
        if (!TextUtils.isEmpty(description)) {
            mNativeAdView.setSubTitle(description);
        }

        IconAdView iconAdView = new IconAdView(context);
        mNativeAdView.setIconView(iconAdView);

        String navigationType = nativeBanner.getNavigationType();
        if (!TextUtils.isEmpty(navigationType)) {
            if (NavigationType.WEB.equals(navigationType)) {
                String domain = nativeBanner.getDomain();
                if (!TextUtils.isEmpty(domain)) {
                    mNativeAdView.setNativeAdSocialContext(domain);
                }
            } else if (NavigationType.STORE.equals(navigationType)) {
                float rating = nativeBanner.getRating();
                Log.i("StarRating", "MyTarget storeRating: " + rating);
                Double adRating = (double) rating;
                if (adRating != null) {
                    mNativeAdView.setStarRating(adRating);
                }
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

    public void onAdVideoStart() {
        if (mShowListener != null) {
            mShowListener.onAdVideoStart();
        }
    }

    public void onAdVideoEnd() {
        if (mShowListener != null) {
            mShowListener.onAdVideoEnd();
        }
    }

    @Override
    public Object getNetworkObj() {
        if (nativeBanner != null) {
            return nativeBanner;
        }
        if (nativePromoBanner != null) {
            return nativePromoBanner;
        }
        return null;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        if (mNativeAd != null) {
            mNativeAd.registerView(viewGroup, clickViews);
        }

        if (nativeBannerAd != null) {
            nativeBannerAd.registerView(viewGroup, clickViews);
        }

    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return mNativeAdView;
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
        return mNativeAdContainer;
    }

    @Override
    public void clean() {
        if (nativeBannerAd != null) {
            nativeBannerAd.setListener(null);
            nativeBannerAd.unregisterView();
            nativeBannerAd = null;
        }

        if (mNativeAd != null) {
            mNativeAd.setListener(null);
            mNativeAd.unregisterView();
            mNativeAd = null;
        }

    }
}
