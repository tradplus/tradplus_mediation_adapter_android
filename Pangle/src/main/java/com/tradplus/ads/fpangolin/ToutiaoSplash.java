package com.tradplus.ads.fpangolin;

import static com.tradplus.ads.base.adapter.TPInitMediation.INIT_STATE_BIDDING;
import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAd;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAdLoadListener;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenRequest;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class ToutiaoSplash extends TPSplashAdapter {

    private static final String TAG = "PangleSplash";
    private String placementId;
    private int timeout = 5000;
    private PAGAppOpenAd mPAGAppOpenAd;
    private String mPayload;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);

            String payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(payload)) {
                mPayload = payload;
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.TIME_DELTA)) {
                int localTimeOut = (int) userParams.get(AppKeyManager.TIME_DELTA);

                // App Open ad timeout recommended >=3000ms.
                if (localTimeOut >= 3000) {
                    timeout = localTimeOut;
                    Log.i(TAG, "timeout: " + timeout);
                }
            }

            if (userParams.containsKey(ToutiaoConstant.PANGLE_SPLASH_DIRECTION)) {
            }
        }


        PangleInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestSplash();
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

    private void requestSplash() {
        PAGAppOpenRequest request = new PAGAppOpenRequest();

        request.setTimeout(timeout);

        // bidding set payload
        if (!TextUtils.isEmpty(mPayload)) {
            request.setAdString(mPayload);
        }

        PAGAppOpenAd.loadAd(placementId, request, new PAGAppOpenAdLoadListener() {
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
            public void onAdLoaded(PAGAppOpenAd pagAppOpenAd) {
                mPAGAppOpenAd = pagAppOpenAd;
                Log.i(TAG, "onAdLoaded: ");
                if (mLoadAdapterListener != null) {
                    setNetworkObjectAd(pagAppOpenAd);
                    mLoadAdapterListener.loadAdapterLoaded(null);
                }
            }
        });
    }

    @Override
    public void showAd() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            Log.i(TAG, "showAd, activity == null");
            return;
        }

        if (mPAGAppOpenAd == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(UNSPECIFIED));
            }
            Log.i(TAG, "showAd, PAGAppOpenAd == null");
            return;
        }

        mPAGAppOpenAd.setAdInteractionListener(new PAGAppOpenAdInteractionListener() {
            @Override
            public void onAdShowed() {
                Log.i(TAG, "onAdShow: ");
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
            public void onAdDismissed() {
                Log.i(TAG, "onAdDismissed: ");
                if (mShowListener != null) {
                    mShowListener.onAdClosed();
                }
            }
        });

        mPAGAppOpenAd.show(activity);

    }

    @Override
    public void clean() {
        if (mPAGAppOpenAd != null) {
            mPAGAppOpenAd.setAdInteractionListener(null);
            mPAGAppOpenAd = null;
        }
    }

    @Override
    public boolean isReady() {
        return true;
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
