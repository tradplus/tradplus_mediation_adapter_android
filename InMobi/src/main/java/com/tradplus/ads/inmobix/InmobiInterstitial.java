package com.tradplus.ads.inmobix;

import android.content.Context;
import android.util.Log;


import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import com.inmobi.sdk.InMobiSdk;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.EC_NOTREADY;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

public class InmobiInterstitial extends TPInterstitialAdapter {

    private InmobiInterstitialCallbackRouter mInmobICbR;
    private String mPlacementId;
    private InMobiInterstitial interstitialAd;
    private static final String TAG = "InMobiInterstitial";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) return;

        if (extrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        mInmobICbR = InmobiInterstitialCallbackRouter.getInstance();
        mInmobICbR.addListener(mPlacementId, mLoadAdapterListener);


        InmobiInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                if (interstitialAd != null) {
                    requestInmobiBiddingIntertitial();
                } else {
                    requestInMobiInterstitial(context);
                }
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });

    }

    private void requestInmobiBiddingIntertitial() {
        interstitialAd.setListener(interstitialAdEventListener);
        Map<String, String> parameters = InmobiInitManager.getInstance().getParameters();
        if (parameters != null) {
            interstitialAd.setExtras(parameters);
        }
        interstitialAd.getPreloadManager().load();
    }

    private InterstitialAdEventListener interstitialAdEventListener = new InterstitialAdEventListener() {
        @Override
        public void onAdFetchFailed(InMobiInterstitial inMobiInterstitial, InMobiAdRequestStatus inMobiAdRequestStatus) {
            super.onAdFetchFailed(inMobiInterstitial, inMobiAdRequestStatus);
            Log.i(TAG, "onAdFetchFailed: ");
        }

        @Override
        public void onAdWillDisplay(InMobiInterstitial inMobiInterstitial) {
            super.onAdWillDisplay(inMobiInterstitial);
            Log.i(TAG, "onAdWillDisplay: ");
        }

        @Override
        public void onAdDisplayed(InMobiInterstitial inMobiInterstitial, AdMetaInfo adMetaInfo) {
            super.onAdDisplayed(inMobiInterstitial, adMetaInfo);
            Log.i(TAG, "onAdDisplayed: ");
            if (mInmobICbR.getShowListener(mPlacementId) != null)
                mInmobICbR.getShowListener(mPlacementId).onAdShown();
        }

        @Override
        public void onAdDisplayFailed(InMobiInterstitial inMobiInterstitial) {
            super.onAdDisplayFailed(inMobiInterstitial);
            Log.i(TAG, "onAdDisplayFailed: ");
            if (mInmobICbR.getShowListener(mPlacementId) != null)
                mInmobICbR.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));

        }

        @Override
        public void onAdDismissed(InMobiInterstitial inMobiInterstitial) {
            super.onAdDismissed(inMobiInterstitial);
            Log.i(TAG, "onAdDismissed: ");
            if (mInmobICbR.getShowListener(mPlacementId) != null)
                mInmobICbR.getShowListener(mPlacementId).onAdClosed();
        }

        @Override
        public void onUserLeftApplication(InMobiInterstitial inMobiInterstitial) {
            super.onUserLeftApplication(inMobiInterstitial);
            Log.i(TAG, "onUserLeftApplication: ");
        }

        @Override
        public void onRewardsUnlocked(InMobiInterstitial inMobiInterstitial, Map<Object, Object> map) {
            super.onRewardsUnlocked(inMobiInterstitial, map);
            Log.i(TAG, "onRewardsUnlocked: ");
        }

        @Override
        public void onAdFetchSuccessful(InMobiInterstitial inMobiInterstitial, AdMetaInfo adMetaInfo) {
            super.onAdFetchSuccessful(inMobiInterstitial, adMetaInfo);
            Log.i(TAG, "onAdFetchSuccessful: ");
        }

        @Override
        public void onAdLoadSucceeded(InMobiInterstitial inMobiInterstitial, AdMetaInfo adMetaInfo) {
            super.onAdLoadSucceeded(inMobiInterstitial, adMetaInfo);
            Log.i(TAG, "onAdLoadSucceeded: ");
            if (mInmobICbR.getListener(mPlacementId) != null) {
                setNetworkObjectAd(inMobiInterstitial);
                mInmobICbR.getListener(mPlacementId).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onAdLoadFailed(InMobiInterstitial inMobiInterstitial, InMobiAdRequestStatus inMobiAdRequestStatus) {
            super.onAdLoadFailed(inMobiInterstitial, inMobiAdRequestStatus);
            Log.i(TAG, "onAdLoadFailed: ");
            if (mInmobICbR.getListener(mPlacementId) != null) {
                mInmobICbR.getListener(mPlacementId).loadAdapterLoadFailed(InmobiErrorUtils.getTPError(inMobiAdRequestStatus));
            }

        }

        @Override
        public void onAdClicked(InMobiInterstitial inMobiInterstitial, Map<Object, Object> map) {
            super.onAdClicked(inMobiInterstitial, map);
            Log.i(TAG, "onAdClicked: ");
            if (mInmobICbR.getShowListener(mPlacementId) != null)
                mInmobICbR.getShowListener(mPlacementId).onAdVideoClicked();
        }

    };

    private void requestInMobiInterstitial(Context context) {
        //Load
        long pid;
        try {
            pid = Long.parseLong(mPlacementId);
        } catch (Throwable e) {
            if (mInmobICbR.getListener(mPlacementId) != null) {
                mInmobICbR.getListener(mPlacementId).loadAdapterLoadFailed(new TPError(INIT_FAILED));
            }
            return;
        }

        interstitialAd = new InMobiInterstitial(context, pid, interstitialAdEventListener);
        Map<String, String> parameters = InmobiInitManager.getInstance().getParameters();
        if (parameters != null) {
            interstitialAd.setExtras(parameters);
        }
        interstitialAd.load();

    }

    @Override
    public void showAd() {
        if (mShowListener != null) {
            mInmobICbR.addShowListener(mPlacementId, mShowListener);
        }

        if (interstitialAd == null) {
            if (mInmobICbR.getShowListener(mPlacementId) != null) {
                TPError tpError = new TPError(UNSPECIFIED);
                tpError.setErrorMessage("showAd,but interstitialAd == null");
                mInmobICbR.getShowListener(mPlacementId).onAdVideoError(tpError);
            }
            return;
        }

        if (!interstitialAd.isReady()) {
            if (mInmobICbR.getShowListener(mPlacementId) != null) {
                TPError tpError = new TPError(EC_NOTREADY);
                tpError.setErrorMessage("showAd,but interstitialAd not Ready");
                mInmobICbR.getShowListener(mPlacementId).onAdVideoError(tpError);
            }
            return;
        }

        interstitialAd.show();
    }


    @Override
    public boolean isReady() {
        return interstitialAd != null && interstitialAd.isReady() && !isAdsTimeOut();
    }

    @Override
    public void clean() {
        if (interstitialAd != null) {
            interstitialAd.setListener(null);
            interstitialAd = null;
        }

        if (mPlacementId != null) {
            mInmobICbR.removeListeners(mPlacementId);
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_INMOBI);
    }

    @Override
    public String getNetworkVersion() {
        return InMobiSdk.getVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.AD_PLACEMENT_ID);
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        InmobiInitManager.getInstance().initSDK(context, localParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestBid(context, localParams, tpParams, onC2STokenListener);
            }

            @Override
            public void onFailed(String code, String msg) {
                if (onC2STokenListener != null) onC2STokenListener.onC2SBiddingFailed(code, msg);
            }
        });
    }

    public void requestBid(final Context context, Map<String, Object> userParams, Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        if (extrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        }
        //Load
        long pid;
        try {
            pid = Long.parseLong(mPlacementId);
        } catch (Throwable e) {
            onC2STokenListener.onC2SBiddingFailed("", e.getMessage());
            return;
        }

        interstitialAd = new InMobiInterstitial(context, pid, new InterstitialAdEventListener() {
                    @Override
                    public void onAdFetchSuccessful(InMobiInterstitial ad, AdMetaInfo info) {
                        onC2STokenListener.onC2SBiddingResult(info.getBid());
                    }

                    /**
                     * Called to notify that an ad preload has failed. 
                     Note: This  * notification is given only when you use 
                     {@code preload()} in  
                     * {@link InMobiInterstitial#getPreloadManager()}
                     *
                     * @param ad Represents the ad which was preloaded  
                     * @param status Represents the {@link InMobiAdRequestStatus} status 
                    containing error reason  
                     */
                    @Override
                    public void onAdFetchFailed(InMobiInterstitial ad, InMobiAdRequestStatus status) {
                        onC2STokenListener.onC2SBiddingFailed("", status.getMessage());
                    }
                });

        Map<String, String> parameters = InmobiInitManager.getInstance().getParameters();
        if (parameters != null) {
            interstitialAd.setExtras(parameters);
        }
        // Step to preload interstitial
        interstitialAd.getPreloadManager().preload();
    }

}
