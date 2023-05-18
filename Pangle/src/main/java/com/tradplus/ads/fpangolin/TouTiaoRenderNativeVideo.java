package com.tradplus.ads.fpangolin;

import static com.tradplus.ads.base.adapter.TPInitMediation.INIT_STATE_BIDDING;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdLoadListener;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeRequest;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class TouTiaoRenderNativeVideo extends TPNativeAdapter {

    public static final String TAG = "PangleNative";
    private String mPlacementId;
    private ToutiaoNativeAd mNativeAd;
    private PAGNativeAd mPagNativeAd;
    private boolean mNeedDownloadImg = false;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }
        String payload;
        if (serverExtras != null && serverExtras.size() > 0) {
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = serverExtras.get(DataKeys.BIDDING_PAYLOAD);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(DataKeys.DOWNLOAD_IMG)) {
                String downLoadImg = (String) localExtras.get(DataKeys.DOWNLOAD_IMG);
                if (downLoadImg.equals("true")) {
                    mNeedDownloadImg = true;
                }
            }
        }

        final String ttAdm = payload;
        PangleInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestNative(ttAdm);
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

    private void requestNative(String payload) {
        PAGNativeRequest request = new PAGNativeRequest();

        if (!TextUtils.isEmpty(payload)) {
            request.setAdString(payload);
        }

        PAGNativeAd.loadAd(mPlacementId, request, new PAGNativeAdLoadListener() {
            @Override
            public void onError(int code, String message) {
                Log.i(TAG, "onError: code ：" + code + ", message ：" + message);
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorCode(code + "");
                    tpError.setErrorMessage(message);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void onAdLoaded(PAGNativeAd pagNativeAd) {
                if (pagNativeAd == null) {
                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoadFailed(new TPError(UNSPECIFIED));
                    }
                    Log.i(TAG, "onAdLoaded ,but pagNativeAd == null");
                    return;
                }
                mPagNativeAd = pagNativeAd;

                mNativeAd = new ToutiaoNativeAd(pagNativeAd);
                downloadAndCallback(mNativeAd, mNeedDownloadImg);
            }
        });
    }

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
        if (mPagNativeAd != null) {
            mPagNativeAd = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_PANGLE);
    }

    @Override
    public String getNetworkVersion() {
        return PAGSdk.getSDKVersion();
    }

    @Override
    public void getBiddingToken(Context context, Map<String, String> tpParams, Map<String, Object> localParams, final OnS2STokenListener onS2STokenListener) {
        final boolean initSuccess = PAGSdk.isInitSuccess();
        PangleInitManager.getInstance().initSDK(context, localParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                if (!initSuccess) {
                    PangleInitManager.getInstance().sendInitRequest(true, INIT_STATE_BIDDING);
                }
                onS2STokenListener.onTokenResult(PAGSdk.getBiddingToken(), null);

            }

            @Override
            public void onFailed(String code, String msg) {
                PangleInitManager.getInstance().sendInitRequest(false, INIT_STATE_BIDDING);
                onS2STokenListener.onTokenResult("", null);
            }
        });
    }

}
