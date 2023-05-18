package com.tradplus.ads.tapjoy;

import android.content.Context;
import android.util.Log;

import com.tapjoy.TJActionRequest;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJPlacementVideoListener;
import com.tapjoy.Tapjoy;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class TapJoyInterstitial extends TPInterstitialAdapter {

    private String placementId;
    private TJPlacement mPlacement;
    private static final String TAG = "TapJoyInterstitial";
    private TapjoyInterstitialCallbackRouter mCallbackRouter;

    @Override
    public void loadCustomAd(final Context context,
                             final Map<String, Object> localExtras,
                             final Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(serverExtras)) {
            placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        mCallbackRouter = TapjoyInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(placementId, mLoadAdapterListener);


        TapjoyInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                mPlacement = Tapjoy.getPlacement(placementId, placementListener);
                mPlacement.setVideoListener(placementVideoListener);
                mPlacement.requestContent();
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


    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(placementId, mShowListener);
        }

        if (mPlacement == null) {
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));
            }
            return;
        }

        mPlacement.showContent();
    }

    @Override
    public boolean isReady() {
        return mPlacement != null && mPlacement.isContentAvailable() && !isAdsTimeOut();
    }

    private final TJPlacementListener placementListener = new TJPlacementListener() {
        @Override
        public void onRequestSuccess(TJPlacement tjPlacement) {
            if (!tjPlacement.isContentAvailable()) {
                Log.i(TAG, "onRequestFailure not Content Available");
                if (mCallbackRouter.getListener(tjPlacement.getName()) != null)
                    mCallbackRouter.getListener(tjPlacement.getName()).loadAdapterLoadFailed(new TPError(NETWORK_NO_FILL));

            }
        }

        @Override
        public void onRequestFailure(TJPlacement tjPlacement, TJError tjError) {
            Log.i(TAG, "onRequestFailure, code: " + tjError.code + ", msg:" + tjError.message);
            if (mCallbackRouter.getListener(tjPlacement.getName()) != null)
                mCallbackRouter.getListener(tjPlacement.getName()).
                        loadAdapterLoadFailed(TapjoyErrorUtil.getTradPlusErrorCode(NETWORK_NO_FILL, tjError));

        }

        @Override
        public void onContentReady(TJPlacement tjPlacement) {
            Log.i(TAG, "onContentReady: ");
            if (mCallbackRouter.getListener(tjPlacement.getName()) != null) {
                setNetworkObjectAd(mPlacement);
                mCallbackRouter.getListener(tjPlacement.getName()).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onContentShow(TJPlacement tjPlacement) {
        }

        @Override
        public void onContentDismiss(TJPlacement tjPlacement) {
            Log.i(TAG, "onContentDismiss: ");
            if (mCallbackRouter.getShowListener(tjPlacement.getName()) != null) {
                mCallbackRouter.getShowListener(tjPlacement.getName()).onAdClosed();
            }

        }

        @Override
        public void onPurchaseRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s) {}

        @Override
        public void onRewardRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s, int i) {}

        @Override
        public void onClick(TJPlacement tjPlacement) {
            Log.i(TAG, "onClick: ");
            if (mCallbackRouter.getShowListener(tjPlacement.getName()) != null)
                mCallbackRouter.getShowListener(tjPlacement.getName()).onAdClicked();

        }
    };

    private final TJPlacementVideoListener placementVideoListener = new TJPlacementVideoListener() {
        @Override
        public void onVideoComplete(final TJPlacement tjPlacement) {
            Log.i(TAG, "onVideoComplete: ");
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoEnd();
            }
        }

        @Override
        public void onVideoError(TJPlacement tjPlacement, String s) {
            Log.i(TAG, "onVideoError: " + s);
            if (mCallbackRouter.getShowListener(placementId) != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorMessage(s);
                mCallbackRouter.getShowListener(placementId).onAdVideoError(tpError);
            }
        }

        @Override
        public void onVideoStart(TJPlacement tjPlacement) {
            Log.i(TAG, "onVideoStart: ");
            if (mCallbackRouter.getShowListener(tjPlacement.getName()) != null) {
                mCallbackRouter.getShowListener(tjPlacement.getName()).onAdShown();
                mCallbackRouter.getShowListener(tjPlacement.getName()).onAdVideoStart();
            }
        }
    };

    @Override
    public void clean() {
        if (mPlacement != null) {
            mPlacement.setVideoListener(null);
            mPlacement = null;
        }

        if (placementId != null) {
            mCallbackRouter.removeListeners(placementId);
        }
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID) && serverExtras.containsKey(AppKeyManager.AD_PLACEMENT_ID);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_TAPJOY);
    }

    @Override
    public String getNetworkVersion() {
        return Tapjoy.getVersion();
    }
}
