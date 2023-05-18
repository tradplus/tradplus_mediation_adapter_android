package com.tradplus.ads.bigo;

import static com.tradplus.ads.base.adapter.TPInitMediation.INIT_STATE_BIDDING;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.util.AppKeyManager.ISNATIVE;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.annotation.NonNull;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.Map;

import sg.bigo.ads.BigoAdSdk;
import sg.bigo.ads.api.AdError;
import sg.bigo.ads.api.AdLoadListener;
import sg.bigo.ads.api.NativeAd;
import sg.bigo.ads.api.NativeAdLoader;
import sg.bigo.ads.api.NativeAdRequest;

public class BigoNative extends TPNativeAdapter {

    private static final String TAG = "BigoNative";
    private String mPlacementId;
    private String serverBiddingAdm;
    private NativeAd mNativeAd;
    private BigoNativeAd mBgioNativeAd;
    private int mVideoMute = 1;
    private boolean isNativeBanner = false;
    private boolean mNeedDownloadImg = false;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);


            String isNative = tpParams.get(ISNATIVE);
            if (!TextUtils.isEmpty(isNative)) {
                isNativeBanner = Integer.parseInt(isNative) == 1 ? true : false;
            }
            if (!isNativeBanner) {
                String secType = tpParams.get(AppKeyManager.ADTYPE_SEC);

                if (!TextUtils.isEmpty(secType)) {
                    isNativeBanner = AppKeyManager.NATIVE_TYPE_NATIVEBANNER.equals(secType);
                }
            }

            String payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(payload)) {
                serverBiddingAdm = payload;
            }
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(DataKeys.DOWNLOAD_IMG)) {
                String downLoadImg = (String) userParams.get(DataKeys.DOWNLOAD_IMG);
                mNeedDownloadImg = "true".equals(downLoadImg);
            }

            if (userParams.containsKey(BigoConstant.VIDEO_MUTE)) {
                mVideoMute = (int) userParams.get(BigoConstant.VIDEO_MUTE);
                Log.i(TAG, "VideoMute: " + mVideoMute);
            }
        }

        BigoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestNative(context);
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorCode(code);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });
    }

    private void requestNative(Context context) {
        NativeAdRequest.Builder builder = new NativeAdRequest.Builder()
                .withSlotId(mPlacementId);

        NativeAdLoader nativeAdLoader = new NativeAdLoader.Builder().
                withAdLoadListener(new AdLoadListener<NativeAd>() {
                    @Override
                    public void onError(@NonNull AdError error) {
                        TPError tpError = new TPError(NETWORK_NO_FILL);
                        if (error != null) {
                            int code = error.getCode();
                            String message = error.getMessage();
                            tpError.setErrorMessage(message);
                            tpError.setErrorCode(code + "");
                            Log.i(TAG, "code :" + code + ", message :" + message);
                        }

                        if (mLoadAdapterListener != null)
                            mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }

                    @Override
                    public void onAdLoaded(@NonNull NativeAd ad) {
                        mNativeAd = ad;
                        mBgioNativeAd = new BigoNativeAd(context, ad, mVideoMute == 1, isNativeBanner);
                        downloadAndCallback(mBgioNativeAd, mNeedDownloadImg);
                    }
                }).build();

        if (!TextUtils.isEmpty(serverBiddingAdm)) {
            builder.withBid(serverBiddingAdm);
        }

        nativeAdLoader.loadAd(builder.build());
    }

    @Override
    public void clean() {
        if (mNativeAd != null) {
            mNativeAd.destroy();
            mNativeAd = null;
        }
    }

    @Override
    public String getNetworkName() {
        return "Bigo";
    }

    @Override
    public String getNetworkVersion() {
        return BigoAdSdk.getSDKVersionName();
    }

    @Override
    public void getBiddingToken(Context context, Map<String, String> tpParams, Map<String, Object> localParams, final OnS2STokenListener onS2STokenListener) {
        boolean initialized = BigoAdSdk.isInitialized();
        BigoInitManager.getInstance().initSDK(context, localParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                String bidderToken = BigoAdSdk.getBidderToken();
                boolean tokenEmpty = TextUtils.isEmpty(bidderToken);
                Log.i(TAG, "onSuccess bidderToken isEmpty " + tokenEmpty);
                if (!initialized) {
                    BigoInitManager.getInstance().sendInitRequest(true, INIT_STATE_BIDDING);
                }
                onS2STokenListener.onTokenResult(!tokenEmpty ? bidderToken : "", null);
            }

            @Override
            public void onFailed(String code, String msg) {
                BigoInitManager.getInstance().sendInitRequest(false, INIT_STATE_BIDDING);
                onS2STokenListener.onTokenResult("", null);
            }
        });
    }
}
