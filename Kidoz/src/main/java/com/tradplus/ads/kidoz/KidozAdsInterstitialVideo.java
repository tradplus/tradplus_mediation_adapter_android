package com.tradplus.ads.kidoz;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.kidoz.sdk.api.KidozInterstitial;
import com.kidoz.sdk.api.KidozSDK;
import com.kidoz.sdk.api.ui_views.interstitial.BaseInterstitial;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class KidozAdsInterstitialVideo extends TPRewardAdapter {

    private String appId, appToken, placementId;
    private KidozInterstitialCallbackRouter mKidozICaR;
    private KidozInterstitial mKidozInterstitial;
    private boolean onRewardReceived = false;
    private boolean alwaysRewardUser;
    private static final String TAG = "KidozInterstitialVideo";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(tpParams)) {
            appId = tpParams.get(AppKeyManager.APP_ID);
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            appToken = tpParams.get(AppKeyManager.APPTOKEN);
            Log.i(TAG, "loadInterstitial: appId： " + appId + "， placementId ：" + placementId + ", appToken :" + appToken);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        mKidozICaR = KidozInterstitialCallbackRouter.getInstance();
        mKidozICaR.addListener(placementId, mLoadAdapterListener);

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (!KidozSDK.isInitialised()) {
            KidozInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
                @Override
                public void onSuccess() {
                    requestInerstitialVideo(activity);
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
        } else {
            requestInerstitialVideo(activity);
        }

    }

    private void requestInerstitialVideo(Activity activity) {
        mKidozInterstitial = new KidozInterstitial(activity, KidozInterstitial.AD_TYPE.REWARDED_VIDEO);
        mKidozInterstitial.setOnInterstitialEventListener(new BaseInterstitial.IOnInterstitialEventListener() {
            @Override
            public void onClosed() {
                Log.i(TAG, "onClosed: ");
                if (mKidozICaR.getShowListener(placementId) != null) {
                    mKidozICaR.getShowListener(placementId).onAdVideoEnd();

                    if (onRewardReceived || alwaysRewardUser) {
                        mKidozICaR.getShowListener(placementId).onReward();
                    }

                    mKidozICaR.getShowListener(placementId).onAdClosed();
                }
            }

            @Override
            public void onOpened() {
                Log.i(TAG, "onOpened: ");
                if (mKidozICaR.getShowListener(placementId) != null)
                    mKidozICaR.getShowListener(placementId).onAdShown();

                if (mKidozICaR.getShowListener(placementId) != null)
                    mKidozICaR.getShowListener(placementId).onAdVideoStart();
            }

            @Override
            public void onReady() {
                Log.i(TAG, "onReady: ");
                if (mKidozICaR.getListener(placementId) != null) {
                    setNetworkObjectAd(mKidozInterstitial);
                    mKidozICaR.getListener(placementId).loadAdapterLoaded(null);
                }

            }

            @Override
            public void onLoadFailed() {
                Log.i(TAG, "onLoadFailed: ");
                if (mKidozICaR.getListener(placementId) != null)
                    mKidozICaR.getListener(placementId).loadAdapterLoadFailed(new TPError(NETWORK_NO_FILL));
            }

            @Override
            public void onNoOffers() {
                Log.i(TAG, "onNoOffers: ");
            }
        });

        mKidozInterstitial.setOnInterstitialRewardedEventListener(new BaseInterstitial.IOnInterstitialRewardedEventListener() {
            @Override
            public void onRewardReceived() {
                Log.i(TAG, "onRewardReceived: ");
                onRewardReceived = true;
            }

            @Override
            public void onRewardedStarted() {

            }
        });
        mKidozInterstitial.loadAd();
    }


    @Override
    public void showAd() {
        if (mShowListener != null)
            mKidozICaR.addShowListener(placementId, mShowListener);


        if (mKidozInterstitial != null && mKidozInterstitial.isLoaded()) {
            mKidozInterstitial.show();
        } else {
            if (mKidozICaR.getShowListener(placementId) != null)
                mKidozICaR.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));

        }
    }

    @Override
    public void clean() {
        super.clean();
        if (mKidozInterstitial != null) {
            mKidozInterstitial.setOnInterstitialRewardedEventListener(null);
            mKidozInterstitial.setOnInterstitialEventListener(null);
            mKidozInterstitial = null;
        }

        if (placementId != null)
            mKidozICaR.removeListeners(placementId);
    }

    @Override
    public boolean isReady() {
        if (mKidozInterstitial != null) {
            return mKidozInterstitial.isLoaded();
        } else {
            return false;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_KIDOZ);
    }

    @Override
    public String getNetworkVersion() {
        return KidozSDK.getSDKVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }
}
