package com.tradplus.ads.bigo;

import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;

import java.util.ArrayList;
import java.util.List;

import sg.bigo.ads.api.AdError;
import sg.bigo.ads.api.AdInteractionListener;
import sg.bigo.ads.api.AdOptionsView;
import sg.bigo.ads.api.AdTag;
import sg.bigo.ads.api.MediaView;
import sg.bigo.ads.api.NativeAd;
import sg.bigo.ads.api.VideoController;

public class BigoNativeAd extends TPBaseAd {

    private NativeAd mNativeAd;
    private TPNativeAdView mTPNativeAdView;
    private Context mContext;
    private MediaView mediaView;
    private int onVideoEnd = 0;
    private boolean mIsNativeBanner;
    private boolean mMute = true;
    private static final String TAG = "BigoNative";

    public BigoNativeAd(Context context, NativeAd ad, boolean videomute, boolean adType) {
        this.mNativeAd = ad;
        this.mContext = context;
        this.mMute = videomute;
        this.mIsNativeBanner = adType;
        initNativeAd(context, ad);
    }

    private void initNativeAd(Context context, NativeAd ad) {
        mTPNativeAdView = new TPNativeAdView();

        String callToAction = ad.getCallToAction();
        if (!TextUtils.isEmpty(callToAction)) {
            mTPNativeAdView.setCallToAction(callToAction);
        }

        String description = ad.getDescription();
        if (!TextUtils.isEmpty(description)) {
            mTPNativeAdView.setSubTitle(description);
        }

        String title = ad.getTitle();
        if (!TextUtils.isEmpty(title)) {
            mTPNativeAdView.setTitle(title);
        }

        String advertiser = ad.getAdvertiser();
        if (!TextUtils.isEmpty(advertiser)) {
            mTPNativeAdView.setAdvertiserName(advertiser);
        }

        // 返回广告是否含有图标
        boolean hasIcon = ad.hasIcon();
        if (hasIcon) {
            mTPNativeAdView.setIconView(new ImageView(context));
        }

        mediaView = new MediaView(context);
        mTPNativeAdView.setMediaView(mediaView);

        ad.setAdInteractionListener(new AdInteractionListener() {
            @Override
            public void onAdError(AdError error) {
                TPError tpError = new TPError(SHOW_FAILED);
                if (error != null) {
                    int code = error.getCode();
                    String message = error.getMessage();
                    tpError.setErrorMessage(message);
                    tpError.setErrorCode(code + "");
                    Log.i(TAG, "code :" + code + ", message :" + message);
                }

                if (mShowListener != null) {
                    mShowListener.onAdVideoError(tpError);
                }
            }

            @Override
            public void onAdImpression() {
                Log.i(TAG, "onAdImpression: ");
                if (mShowListener != null) {
                    mShowListener.onAdShown();
                }
            }

            @Override
            public void onAdClicked() {
                Log.i(TAG, "onAdClicked: ");
                if (mShowListener != null) {
                    mShowListener.onAdClicked();
                }
            }

            @Override
            public void onAdOpened() {

            }

            @Override
            public void onAdClosed() {
                Log.i(TAG, "onAdClosed: ");
                if (mShowListener != null) {
                    mShowListener.onAdClosed();
                }
            }
        });

    }

    @Override
    public Object getNetworkObj() {
        return mNativeAd;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        TPError tpError = new TPError(UNSPECIFIED);
        if (mNativeAd == null) {
            if (mShowListener != null) {
                tpError.setErrorMessage("NativeAd == null");
                mShowListener.onAdVideoError(tpError);
            }
            return;
        }

        if (mNativeAd.isExpired()) {
            if (mShowListener != null) {
                tpError.setErrorMessage("NativeAd isExpired");
                mShowListener.onAdVideoError(tpError);
            }
            return;
        }

        FrameLayout adChoicesView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_ADCHOICES);
        AdOptionsView optionsView = new AdOptionsView(mContext);
        if (adChoicesView != null) {
            ViewParent parent = optionsView.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(optionsView);
            }
            adChoicesView.removeAllViews();
            adChoicesView.addView(optionsView, 0);
        }

        View titleView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_TITLE);
        if (titleView != null) {
            titleView.setTag(AdTag.TITLE);
        }

        View subTitleView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_SUBTITLE);
        if (subTitleView != null) {
            subTitleView.setTag(AdTag.DESCRIPTION);
        }

        View callToAction = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_CALLTOACTION);
        if (callToAction != null) {
            callToAction.setTag(AdTag.CALL_TO_ACTION);
        }

        View iconView = mTPNativeAdView.getIconView();
        // 原生横幅不需要注册mediaView,否则会报错导致无点击,无展示回调
        mNativeAd.registerViewForInteraction(viewGroup, mIsNativeBanner ? null : mediaView,
                iconView == null ? null : (ImageView) iconView, optionsView, clickViews);

        // 视频播放的控制与回调
        if (mNativeAd.getCreativeType() == NativeAd.CreativeType.VIDEO) {
            VideoController videoController = mNativeAd.getVideoController();
            videoController.mute(mMute);
            videoController.setVideoLifeCallback(new VideoController.VideoLifeCallback() {
                @Override
                public void onVideoStart() {
                    Log.i(TAG, "onVideoStart: ");
                    if (mShowListener != null) {
                        mShowListener.onAdVideoStart();
                    }
                }

                @Override
                public void onVideoPlay() {

                }

                @Override
                public void onVideoPause() {

                }

                @Override
                public void onVideoEnd() {
                    Log.i(TAG, "onVideoEnd: ");
                    if (mShowListener != null && onVideoEnd == 0) {
                        onVideoEnd = 1;
                        mShowListener.onAdVideoEnd();
                    }
                }

                @Override
                public void onMuteChange(boolean b) {
                    Log.i(TAG, "onMuteChange: " + b);
                }
            });

        }

    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return mTPNativeAdView;
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
        return null;
    }

    @Override
    public void clean() {

    }
}
