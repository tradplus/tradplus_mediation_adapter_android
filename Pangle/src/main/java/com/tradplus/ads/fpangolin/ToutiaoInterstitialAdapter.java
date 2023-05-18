package com.tradplus.ads.fpangolin;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdLoadListener;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialRequest;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.adapter.TPInitMediation.INIT_STATE_BIDDING;
import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

public class ToutiaoInterstitialAdapter extends TPInterstitialAdapter {

    public static final String TAG = "PangleInterstitial";
    //1:1
    public static final float EXPRESSVIEW_WIDTH1 = 300;
    public static final float EXPRESSVIEW_HEIGHT1 = 300;
    //2:3
    public static final float EXPRESSVIEW_WIDTH2 = 300;
    public static final float EXPRESSVIEW_HEIGHT2 = 450;
    //3:2
    public static final float EXPRESSVIEW_WIDTH3 = 450;
    public static final float EXPRESSVIEW_HEIGHT3 = 300;

    private ToutiaoInterstitialCallbackRouter mCallbackRouter;
    private String placementId;
    private PAGInterstitialAd mPAGInterstitialAd;


    @Override
    public void loadCustomAd(Context context,
                             final Map<String, Object> localExtras,
                             final Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        final String payload;
        if (extrasAreValid(serverExtras)) {
            placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = serverExtras.get(DataKeys.BIDDING_PAYLOAD);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        mCallbackRouter = ToutiaoInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(placementId, mLoadAdapterListener);

        PangleInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                intFullScreen(payload);
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

    private void intFullScreen(String payload) {
        PAGInterstitialRequest pagInterstitialRequest = new PAGInterstitialRequest();

        if (!TextUtils.isEmpty(payload)) {
            pagInterstitialRequest.setAdString(payload);
        }

        PAGInterstitialAd.loadAd(placementId, pagInterstitialRequest, new PAGInterstitialAdLoadListener() {
            @Override
            public void onError(int code, String message) {
                Log.i(TAG, "onError: code ：" + code + ", message ：" + message);
                if (mCallbackRouter.getListener(placementId) != null) {
                    mCallbackRouter.getListener(placementId).loadAdapterLoadFailed(PangleErrorUtil.getTradPlusErrorCode(code, message));
                }
            }

            @Override
            public void onAdLoaded(PAGInterstitialAd pagInterstitialAd) {
                if (pagInterstitialAd == null) {
                    if (mCallbackRouter.getListener(placementId) != null) {
                        mCallbackRouter.getListener(placementId).loadAdapterLoadFailed(new TPError(UNSPECIFIED));
                    }
                    Log.i(TAG, "onAdLoaded ,but pagInterstitialAd == null");
                    return;
                }
                mPAGInterstitialAd = pagInterstitialAd;

                if (mCallbackRouter.getListener(placementId) != null) {
                    setNetworkObjectAd(pagInterstitialAd);
                    mCallbackRouter.getListener(placementId).loadAdapterLoaded(null);
                }
            }
        });
    }

    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(placementId, mShowListener);
        }

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoError(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            Log.i(TAG, "showAd, activity == null");
            return;
        }

        if (mPAGInterstitialAd == null) {
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoError(new TPError(UNSPECIFIED));
            }
            Log.i(TAG, "showAd, PAGInterstitialAd == null");
            return;
        }

        mPAGInterstitialAd.setAdInteractionListener(new PAGInterstitialAdInteractionListener() {
            @Override
            public void onAdShowed() {
                Log.i(TAG, "onAdShowed: ");
                if (mCallbackRouter.getShowListener(placementId) != null) {
                    mCallbackRouter.getShowListener(placementId).onAdShown();
                }
            }

            @Override
            public void onAdClicked() {
                Log.i(TAG, "onAdClicked: ");
                if (mCallbackRouter.getShowListener(placementId) != null) {
                    mCallbackRouter.getShowListener(placementId).onAdClicked();
                }
            }

            @Override
            public void onAdDismissed() {
                Log.i(TAG, "onAdDismissed: ");
                if (mCallbackRouter.getShowListener(placementId) != null) {
                    mCallbackRouter.getShowListener(placementId).onAdClosed();
                }
            }
        });

        mPAGInterstitialAd.show(activity);
    }

    @Override
    public void clean() {
        super.clean();
        if (mPAGInterstitialAd != null) {
            mPAGInterstitialAd.setAdInteractionListener(null);
            mPAGInterstitialAd = null;
        }

        if (placementId != null) {
            mCallbackRouter.removeListeners(placementId);
        }
    }

    @Override
    public boolean isReady() {
        return !isAdsTimeOut();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID);
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