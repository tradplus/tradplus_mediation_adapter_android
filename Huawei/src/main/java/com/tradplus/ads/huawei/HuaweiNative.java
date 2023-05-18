package com.tradplus.ads.huawei;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.VideoConfiguration;
import com.huawei.hms.ads.nativead.DislikeAdListener;
import com.huawei.hms.ads.nativead.NativeAd;
import com.huawei.hms.ads.nativead.NativeAdConfiguration;
import com.huawei.hms.ads.nativead.NativeAdLoader;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class HuaweiNative extends TPNativeAdapter {

    private String mPlacementId;
    private HuaweiNativeAd huaiweiNativeAd;
    private static final String TAG = "HuaweiNative";
    private int mIsTemplateRending;
    private int mVideoMute = 1; // 默认静音播放
    private int isAdLeave = 0; // 0的时候是没有跳转过页面，1的时候是跳转出去了
    private int closePosition = NativeAdConfiguration.ChoicesPosition.INVISIBLE; // 默认不显示
    private int nativeTemplate = 1; // TP内置模板
    private int autoInstall = 0; //下载类广告，点击安装是否直接下载

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        String videomute;
        if (extrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            String template = (String) tpParams.get(AppKeyManager.IS_TEMPLATE_RENDERING);
            // 1 播放时静音；2 播放时有声
            videomute = tpParams.get(AppKeyManager.VIDEO_MUTE);

            if (!TextUtils.isEmpty(template)) {
                mIsTemplateRending = Integer.parseInt(template);
            }

            if (!AppKeyManager.VIDEO_MUTE_YES.equals(videomute)) {
                mVideoMute = 2;
            }
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.VIDEO_MUTE)) {
                mVideoMute = (int) userParams.get(AppKeyManager.VIDEO_MUTE);
                Log.i(TAG, "VideoMute: " + (mVideoMute == 1));
            }

            if (userParams.containsKey(HuaweiConstant.HUAWEI_CLOSE_POSITION)) {
                closePosition = (int) userParams.get(HuaweiConstant.HUAWEI_CLOSE_POSITION);
                Log.i(TAG, "closePosition: " + closePosition);
            }

            // 开发者使用华为模板类型，设置TP内置的模板样式
            if (userParams.containsKey(HuaweiConstant.HUAWEI_NATIVE_TEMPLATE_TYPE)) {
                nativeTemplate = (int) userParams.get(HuaweiConstant.HUAWEI_NATIVE_TEMPLATE_TYPE);
                Log.i(TAG, "nativetemple: " + nativeTemplate);
            }

            if (userParams.containsKey(HuaweiConstant.HUAWEI_AUTOINSTALL)) {
                autoInstall = (int) userParams.get(HuaweiConstant.HUAWEI_AUTOINSTALL);
                Log.i(TAG, "autoInstall: " + autoInstall);
            }
        }

        HuaweiInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
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

    private void requestNative(final Context context) {
        // "testy63txaom86"为测试专用的广告位ID，App正式发布时需要改为正式的广告位ID
        NativeAdLoader.Builder builder = new NativeAdLoader.Builder(context, mPlacementId);

        builder.setNativeAdLoadedListener(new NativeAd.NativeAdLoadedListener() {
            public void onNativeAdLoaded(NativeAd nativeAd) {
                Log.i(TAG, "onNativeAdLoaded ");
                if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES) {
                    huaiweiNativeAd = new HuaweiNativeAd(context, nativeAd, AppKeyManager.TEMPLATE_RENDERING_YES);
                    huaiweiNativeAd.setTemplateType(nativeTemplate);
                } else {
                    huaiweiNativeAd = new HuaweiNativeAd(context, nativeAd);
                }
                huaiweiNativeAd.setAppAutoInstall(autoInstall);
                nativeAd.setDislikeAdListener(new DislikeAdListener() {
                    @Override
                    public void onAdDisliked() {
                        Log.i(TAG, "onAdDisliked: ");
                        if (huaiweiNativeAd != null) {
                            huaiweiNativeAd.onAdClosed();
                        }
                    }
                });

                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoaded(huaiweiNativeAd);
                }

            }
        }).setAdListener(new AdListener() {
            @Override
            public void onAdFailed(int i) {
                Log.i(TAG, "onAdFailed " + i);
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorCode(i + "");
                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }

            @Override
            public void onAdLeave() {
                Log.i(TAG, "onAdLeave: ");
                isAdLeave = 1;
            }

            @Override
            public void onAdLoaded() {
                Log.i(TAG, "onAdLoaded: ");
            }

            @Override
            public void onAdClicked() {
                // 广告点击时调用
                Log.i(TAG, "onAdClicked: ");
                if (huaiweiNativeAd != null) huaiweiNativeAd.onAdViewClicked();
            }

            @Override
            public void onAdClosed() {
                // 落地页的关闭会回调AdClosed,不是真正的关闭
            }

            @Override
            public void onAdImpression() {
                // 点击跳转到落地页后会再次回调展示
                Log.i(TAG, "onAdImpression: ");
                if (huaiweiNativeAd != null && isAdLeave == 0) {
                    huaiweiNativeAd.onAdViewExpanded();
                }
            }
        });

        VideoConfiguration videoConfiguration = new VideoConfiguration.Builder().setStartMuted(mVideoMute == 1) // 默认静音播放
                .build();
        Log.i(TAG, "requestNative closePosition : " + closePosition);
        NativeAdConfiguration configuration = new NativeAdConfiguration.Builder().setChoicesPosition(closePosition) // 图标“x”的显示位置
                // 设置原生广告是否返回多张图片素材，如果请求的创意类型是原生三小图，则需要设置此方法
                .setRequestMultiImages(true)
                // 是否要自定义“不再显示该广告”的功能
                .setRequestCustomDislikeThisAd(closePosition == 4) // 默认不显示关闭按钮,同时设置自定义dislike，才会不显示X
                .setVideoConfiguration(videoConfiguration).build();

        builder.setNativeAdOptions(configuration).build().loadAd(new AdParam.Builder().build());

    }

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
        if (huaiweiNativeAd != null) huaiweiNativeAd.clean();
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_HUAWEI);
    }

    @Override
    public String getNetworkVersion() {
        return HwAds.getSDKVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

}
