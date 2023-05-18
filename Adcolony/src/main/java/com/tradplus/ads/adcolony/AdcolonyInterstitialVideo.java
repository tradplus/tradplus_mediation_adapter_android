package com.tradplus.ads.adcolony;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyReward;
import com.adcolony.sdk.AdColonyRewardListener;
import com.adcolony.sdk.AdColonyZone;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;


import java.util.Map;

public class AdcolonyInterstitialVideo extends TPRewardAdapter {

    private String zoneId, zoneIds, userId;
    private AdColonyInterstitial mAdColonyInterstitial;
    private AdcolonyInterstitialCallbackRouter mCallbackRouter;
    private boolean hasReward = false;
    private boolean alwaysRewardUser;
    private static final String TAG = "AdColonyRewardedVideo";


    @Override
    public void loadCustomAd(final Context context,
                             final Map<String, Object> localExtras,
                             final Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(serverExtras)) {
            zoneId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            if (!TextUtils.isEmpty(serverExtras.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(serverExtras.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        mCallbackRouter = AdcolonyInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(zoneId, mLoadAdapterListener);

        AdColonyInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                AdColony.setRewardListener(mAdColonyRewardListener);
                AdColony.requestInterstitial(zoneId, mAdColonyInterstitialListener, new AdColonyAdOptions());
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

    private final AdColonyRewardListener mAdColonyRewardListener = new AdColonyRewardListener() {
        @Override
        public void onReward(AdColonyReward reward) {
            // Used to retrieve information about whether or not the reward was successful.
            if (reward.success()) {
                hasReward = true;
                Log.i(TAG, "onReward: ");
            } else {
                Log.i(TAG, "not onReward: ");
            }

        }
    };

    private final AdColonyInterstitialListener mAdColonyInterstitialListener = new AdColonyInterstitialListener() {
        @Override
        public void onRequestFilled(AdColonyInterstitial ad) {
            mAdColonyInterstitial = ad;
            Log.i(TAG, "onRequestFilled: ");
            if (mCallbackRouter.getListener(ad.getZoneID()) != null) {
                setNetworkObjectAd(ad);
                mCallbackRouter.getListener(ad.getZoneID()).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onOpened(AdColonyInterstitial ad) {
            Log.i(TAG, "onOpened: ");
            if (mCallbackRouter.getShowListener(ad.getZoneID()) != null) {
                mCallbackRouter.getShowListener(ad.getZoneID()).onAdShown();
                mCallbackRouter.getShowListener(ad.getZoneID()).onAdVideoStart();
            }

        }

        @Override
        public void onExpiring(AdColonyInterstitial ad) {
        }

        @Override
        public void onClosed(AdColonyInterstitial ad) {
            if (mCallbackRouter.getShowListener(ad.getZoneID()) != null) {
                if (hasReward || alwaysRewardUser) {
                    mCallbackRouter.getShowListener(ad.getZoneID()).onReward();
                }
                mCallbackRouter.getShowListener(ad.getZoneID()).onAdVideoEnd();

                mCallbackRouter.getShowListener(ad.getZoneID()).onAdClosed();
            }
            Log.i(TAG, "onClosed: ");
        }

        /** Ad request was not filled */
        @Override
        public void onRequestNotFilled(AdColonyZone zone) {
            Log.i(TAG, "onRequestNotFilled: ");
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorMessage("No Fill");
            if (mCallbackRouter.getListener(zone.getZoneID()) != null)
                mCallbackRouter.getListener(zone.getZoneID()).loadAdapterLoadFailed(tpError);
        }
    };


    @Override
    public boolean isReady() {
        if (mAdColonyInterstitial == null) {
            return false;
        } else {
            return !mAdColonyInterstitial.isExpired() && !isAdsTimeOut();
        }
    }


    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(zoneId, mShowListener);
        }
        if (hasAdAvailable()) {
            mAdColonyInterstitial.show();
        } else {
            if (mCallbackRouter.getShowListener(mAdColonyInterstitial.getZoneID()) != null)
                mCallbackRouter.getShowListener(mAdColonyInterstitial.getZoneID()).onAdVideoError(new TPError(SHOW_FAILED));
        }

    }

    protected boolean hasAdAvailable() {
        return mAdColonyInterstitial != null && !mAdColonyInterstitial.isExpired();
    }


    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID) && serverExtras.containsKey(AppKeyManager.AD_PLACEMENT_ID);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_ADCOLONY);
    }

    @Override
    public String getNetworkVersion() {
        return AdColony.getSDKVersion();
    }

}
