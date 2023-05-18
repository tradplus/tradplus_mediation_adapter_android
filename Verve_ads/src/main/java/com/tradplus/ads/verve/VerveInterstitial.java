package com.tradplus.ads.verve;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import net.pubnative.lite.sdk.HyBid;
import net.pubnative.lite.sdk.interstitial.HyBidInterstitialAd;

import java.util.Map;

public class VerveInterstitial extends TPInterstitialAdapter {

    private String mPlacementId;
    private HyBidInterstitialAd mInterstitial;
    private InterstitialCallbackRouter mCallbackRouter;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    private static final String TAG = "VerveInterstitial";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }
        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
        }

        mCallbackRouter = InterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);

        VerveInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestAd();
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }

                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(code + "", msg);
                }
            }
        });
    }

    private void requestAd() {
        if (isC2SBidding && isBiddingLoaded) {
            setFirstLoadedTime();
            if (mCallbackRouter != null && mCallbackRouter.getListener(mPlacementId) != null) {
                setNetworkObjectAd(mInterstitial);
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
            }
        } else {
            Activity activity = GlobalTradPlus.getInstance().getActivity();
            if (activity == null) {
                if (mCallbackRouter.getListener(mPlacementId) != null) {
                    mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
                }
                return;
            }
            mInterstitial = new HyBidInterstitialAd(activity, mPlacementId, mListener);
            mInterstitial.load();
        }
    }

    private final HyBidInterstitialAd.Listener mListener = new HyBidInterstitialAd.Listener() {
        @Override
        public void onInterstitialLoaded() {
            if (isC2SBidding) {
                Log.i(TAG, "onInterstitialLoaded isC2SBidding BidPoints: " + mInterstitial.getBidPoints());
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingResult(mInterstitial.getBidPoints());
                }
                isBiddingLoaded = true;
                return;
            }
            setFirstLoadedTime();

            if (mCallbackRouter.getListener(mPlacementId) != null) {
                setNetworkObjectAd(mInterstitial);
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onInterstitialLoadFailed(Throwable error) {
            Log.i(TAG, "onInterstitialLoadFailed: msg:" + error.getMessage());
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",error.getMessage());
                }
                return;
            }

            if (mCallbackRouter.getListener(mPlacementId) != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(error.getMessage());
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onInterstitialImpression() {
            Log.i(TAG, "onInterstitialImpression: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdShown();
            }
        }

        @Override
        public void onInterstitialDismissed() {
            Log.i(TAG, "onInterstitialDismissed: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdClosed();
            }
        }

        @Override
        public void onInterstitialClick() {
            Log.i(TAG, "onInterstitialClick: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoClicked();
            }
        }
    };

    @Override
    public boolean isReady() {
        return mInterstitial != null && mInterstitial.isReady() && !isAdsTimeOut();
    }

    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);
        }

        if (mInterstitial == null) {
            TPError tpError = new TPError(UNSPECIFIED);
            tpError.setErrorMessage("showfailed，mInterstitial == null");
            mShowListener.onAdVideoError(tpError);
            return;
        }

        mInterstitial.show();
    }


    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }


    @Override
    public void clean() {
        if (mInterstitial != null) {
            mInterstitial.destroy();
            mInterstitial = null;
        }

        if (mPlacementId != null) {
            mCallbackRouter.removeListeners(mPlacementId);
        }
    }

    @Override
    public String getNetworkName() {
        return "Verve";
    }

    @Override
    public String getNetworkVersion() {
        return HyBid.getSDKVersionInfo();
    }
}
