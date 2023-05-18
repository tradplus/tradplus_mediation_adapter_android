package com.tradplus.appnext;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.appnext.core.AppnextAdCreativeType;
import com.appnext.core.AppnextError;
import com.appnext.nativeads.NativeAd;
import com.appnext.nativeads.NativeAdListener;
import com.appnext.nativeads.NativeAdRequest;
import com.appnext.nativeads.PrivacyIcon;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.LARGEBANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.MEDIUMRECTANGLE;

public class AppNextNative extends TPNativeAdapter {

    public static final String TAG = "AppNextNative";
    private String mPID;
    private NativeAd mNativeAd;
    private AppNextNativeAd mAppNextNativeAd;
    private int mVideoMute = 1; // 默认静音播放
    private boolean mAutoPlayVideo = true; // 默认自动播放
    private boolean mNeedDownloadImg = false;
    private int adChoicesPosition = PrivacyIcon.PP_ICON_POSITION_TOP_RIGHT; // 默认 顶右

    @Override
    public void loadCustomAd(Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        String autoPlayVideo;
        String videomute;
        if (serverExtras != null && serverExtras.size() > 0) {
            mPID = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            // 1 播放时静音；2 播放时有声
            videomute = serverExtras.get(AppKeyManager.VIDEO_MUTE);
            // 自动播放视频
            autoPlayVideo = serverExtras.get(AppKeyManager.AUTO_PLAY_VIDEO);
            if (!AppKeyManager.VIDEO_MUTE_YES.equals(videomute)) {
                mVideoMute = 2;
            }

            if (!TextUtils.isEmpty(autoPlayVideo)) {
                // 1 总是自动播放
                mAutoPlayVideo = (Integer.parseInt(autoPlayVideo) == 1);
            }
        }
//        mPID = "f47aaeb4-302c-4733-a13a-22a2bb721aae";

        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(DataKeys.DOWNLOAD_IMG)) {
                String downLoadImg = (String) localExtras.get(DataKeys.DOWNLOAD_IMG);
                if (downLoadImg.equals("true")) {
                    mNeedDownloadImg = true;
                }
            }

            // 1 mute , 2 video
            if (localExtras.containsKey(AppKeyManager.VIDEO_MUTE)) {
                mVideoMute = (int) localExtras.get(AppKeyManager.VIDEO_MUTE);
                Log.i(TAG, "VideoMute: " + (mVideoMute == 1));
            }

            if (localExtras.containsKey(AppKeyManager.AUTO_PLAY_VIDEO)) {
                mAutoPlayVideo = (boolean) localExtras.get(AppKeyManager.AUTO_PLAY_VIDEO);
                Log.i(TAG, "AutoPlayVideo: " + mAutoPlayVideo);
            }

            if (localExtras.containsKey(AppnextConstant.ADCHOICES_POSITION)) {
                adChoicesPosition = (int) localExtras.get(AppnextConstant.ADCHOICES_POSITION);
                Log.i(TAG, "adChoicesPosition: " + adChoicesPosition);
            }
        }

        AppNextInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestNative(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestNative(Context context) {
        mNativeAd = new NativeAd(context, mPID);
        mNativeAd.setAdListener(nativeAdListener);
        mNativeAd.setPrivacyPolicyPosition(adChoicesPostition(adChoicesPosition));
        mNativeAd.setPrivacyPolicyColor(PrivacyIcon.PP_ICON_COLOR_LIGHT);
        // 以下设置均默认值,固不调用
//        NativeAdRequest nativeAdRequest = new NativeAdRequest()
//                .setCachingPolicy(NativeAdRequest.CachingPolicy.ALL) // 视频和图片素材均会缓存
//                .setCreativeType(NativeAdRequest.CreativeType.ALL) // 设置过滤广告内容
//                .setVideoLength(NativeAdRequest.VideoLength.SHORT) // 设置广告视频创意的首选长度
//                .setVideoQuality(NativeAdRequest.VideoQuality.LOW); // 设置广告视频创意的首选质量
        mNativeAd.loadAd(new NativeAdRequest());
    }

    final NativeAdListener nativeAdListener = new NativeAdListener() {
        @Override
        public void onAdClicked(NativeAd nativeAd) {
            super.onAdClicked(nativeAd);
            Log.i(TAG, "onAdClicked: ");
            if (mAppNextNativeAd != null) {
                mAppNextNativeAd.onAdClicked();
            }
        }

        @Override
        public void onAdLoaded(NativeAd nativeAd, AppnextAdCreativeType appnextAdCreativeType) {
            super.onAdLoaded(nativeAd, appnextAdCreativeType);
            if (nativeAd == null) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(UNSPECIFIED);
                    tpError.setErrorMessage("nativeAd == null");
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
                return;
            }
            mNativeAd = nativeAd;
            Log.i(TAG, "onAdLoaded: ");
            mAppNextNativeAd = new AppNextNativeAd(nativeAd, mVideoMute == 1, mAutoPlayVideo);
            downloadAndCallback(mAppNextNativeAd, mNeedDownloadImg);
        }

        @Override
        public void onError(NativeAd nativeAd, AppnextError appnextError) {
            super.onError(nativeAd, appnextError);
            Log.i(TAG, "onError: ");
            TPError tpError = new TPError(NETWORK_NO_FILL);
            if (appnextError != null) {
                String errorMessage = appnextError.getErrorMessage();
                if (!TextUtils.isEmpty(errorMessage)) {
                    Log.i(TAG, "onError: " + errorMessage);
                    tpError.setErrorMessage(errorMessage);
                }
            }

            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void adImpression(NativeAd nativeAd) {
            super.adImpression(nativeAd);
            Log.i(TAG, "adImpression: ");
            if (mAppNextNativeAd != null) {
                mAppNextNativeAd.onAdShown();
            }
        }
    };

    @Override
    public void clean() {
        Log.i(TAG, "onInvalidate: ");
        try {
            if (mNativeAd != null) {
                mNativeAd.setAdListener(null);
                mNativeAd.destroy();
                mNativeAd = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int adChoicesPostition(int postion) {
        // 位置按照google native角标定义：0 顶左;1 顶右;2 底右;3 底左
        if (postion == 0) {
            return PrivacyIcon.PP_ICON_POSITION_TOP_LEFT;
        } else if (postion == 2) {
            return PrivacyIcon.PP_ICON_POSITION_BOTTOM_RIGHT;
        } else if (postion == 3) {
            return PrivacyIcon.PP_ICON_POSITION_BOTTOM_LEFT;
        }
        return PrivacyIcon.PP_ICON_POSITION_TOP_RIGHT;
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_APPNEXT);
    }

    @Override
    public String getNetworkVersion() {
        return "2.6.5.473";
    }
}
