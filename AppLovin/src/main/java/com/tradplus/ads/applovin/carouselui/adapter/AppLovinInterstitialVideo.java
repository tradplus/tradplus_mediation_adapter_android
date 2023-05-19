package com.tradplus.ads.applovin.carouselui.adapter;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinSdk;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.common.util.LogUtil;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class AppLovinInterstitialVideo extends TPRewardAdapter implements
        AppLovinAdLoadListener, AppLovinAdRewardListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener, AppLovinAdDisplayListener {

    public static final String TAG = "AppLovinRewardVideo";
    private AppLovinSdk mAppLovinSdk;
    private AppLovinAd loadedAd;
    private AppLovinIncentivizedInterstitial mAppLovinIncentivizedInterstitial;
    private AppLovinInterstitialCallbackRouter mAppLovinICBR;
    private String payload;
    private String zoneId, userId;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(tpParams)) {
            zoneId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = (String) userParams.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            userId = (String) userParams.get(AppKeyManager.CUSTOM_USERID);

            if (TextUtils.isEmpty(userId)) {
                userId = "";
            }
        }

        mAppLovinICBR = AppLovinInterstitialCallbackRouter.getInstance();

        if (mLoadAdapterListener != null) {
            mAppLovinICBR.addListener(zoneId, mLoadAdapterListener);
        }

        AppLovinInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                mAppLovinSdk = AppLovinInitManager.getInstance().getAppLovinSdk();

                if (!TextUtils.isEmpty(userId)) {
                    Log.i(TAG, "RewardData: userId : " + userId);
                    mAppLovinSdk.setUserIdentifier(userId);
                }

                loadWithSdkInitialized();
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    protected void loadWithSdkInitialized() {
        if (TextUtils.isEmpty(payload)) {
            LogUtil.ownShow("normal load");
            mAppLovinIncentivizedInterstitial = AppLovinIncentivizedInterstitial.create(zoneId, mAppLovinSdk);
            mAppLovinIncentivizedInterstitial.preload(this);
        } else {
            LogUtil.ownShow("bidding load");
            mAppLovinSdk.getAdService().loadNextAdForAdToken(payload, this);
        }
    }

    @Override
    public void showAd() {
        if (mAppLovinICBR != null && zoneId != null && mShowListener != null) {
            mAppLovinICBR.addShowListener(zoneId, mShowListener);
        }

        Context context = GlobalTradPlus.getInstance().getContext();
        if (context == null) {
            if (mShowListener != null) {
                TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                tpError.setErrorMessage("context == null");
                mShowListener.onAdVideoError(tpError);
            }
            return;
        }


        if (!mAppLovinIncentivizedInterstitial.isAdReadyToDisplay()) {
            if (zoneId != null) {
                if (mAppLovinICBR.getShowListener(zoneId) != null) {
                    TPError tpError = new TPError(SHOW_FAILED);
                    tpError.setErrorMessage("is not Ad Ready To Display");
                    mAppLovinICBR.getShowListener(zoneId).onAdVideoError(tpError);
                }
            }
            return;
        }
        mAppLovinIncentivizedInterstitial.show(context, this, this, this, this);


    }

    @Override
    public boolean isReady() {
        if (mAppLovinIncentivizedInterstitial == null) {
            return false;
        } else {
            return mAppLovinIncentivizedInterstitial.isAdReadyToDisplay() && !isAdsTimeOut();
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_APPLOVIN);
    }

    @Override
    public String getNetworkVersion() {
        return AppLovinSdk.VERSION;
    }

    @Override
    public void clean() {
        super.clean();
        if (mAppLovinIncentivizedInterstitial != null) {
            mAppLovinIncentivizedInterstitial = null;
        }
        if (zoneId != null)
            mAppLovinICBR.removeListeners(zoneId);

    }

    @Override
    public void adReceived(AppLovinAd appLovinAd) {
        Log.i(TAG, "adReceived:");
        if (mAppLovinICBR.getListener(zoneId) != null) {
            setNetworkObjectAd(appLovinAd);
            mAppLovinICBR.getListener(zoneId).loadAdapterLoaded(null);
        }
    }

    @Override
    public void failedToReceiveAd(int errorCode) {
        Log.i(TAG, "AppLovin rewarded ad failed to load with error code :" + errorCode);
        if (mAppLovinICBR.getListener(zoneId) != null) {
            mAppLovinICBR.getListener(zoneId).loadAdapterLoadFailed(AppLovinErrorUtil.getTradPlusErrorCode(errorCode));
        }
    }

    @Override
    public void userOverQuota(AppLovinAd appLovinAd, Map map) {
        Log.i(TAG, "userOverQuota: ");

    }

    @Override
    public void userRewardRejected(AppLovinAd appLovinAd, Map map) {
        Log.i(TAG, "userRewardRejected: ");

    }

    @Override
    public void userRewardVerified(AppLovinAd appLovinAd, Map map) {
        Log.i(TAG, "userRewardVerified: ");
        // Rewarded ad was displayed and user should receive the reward


    }

    @Override
    public void validationRequestFailed(AppLovinAd appLovinAd, int errorCode) {
        Log.i(TAG, "validationRequestFailed: ");

    }

    @Override
    public void videoPlaybackBegan(AppLovinAd appLovinAd) {
        Log.i(TAG, "videoPlaybackBegan: ");
        if (mAppLovinICBR.getShowListener(zoneId) != null) {
            mAppLovinICBR.getShowListener(zoneId).onAdVideoStart();
        }

    }

    @Override
    public void videoPlaybackEnded(AppLovinAd appLovinAd, double v, boolean b) {
        Log.i(TAG, "videoPlaybackEnded:");

        hasGrantedReward = b;
        if (mAppLovinICBR.getShowListener(zoneId) != null) {
            mAppLovinICBR.getShowListener(zoneId).onAdVideoEnd();
        }

    }

    @Override
    public void adDisplayed(AppLovinAd appLovinAd) {
        Log.i(TAG, "adDisplayed:");
        if (mAppLovinICBR.getShowListener(zoneId) != null) {
            mAppLovinICBR.getShowListener(zoneId).onAdShown();
        }

    }

    @Override
    public void adHidden(AppLovinAd appLovinAd) {
        Log.i(TAG, "adHidden:");
        if (mAppLovinICBR.getShowListener(zoneId) != null) {

            if (hasGrantedReward || alwaysRewardUser) {
                mAppLovinICBR.getShowListener(zoneId).onReward();
            }
            mAppLovinICBR.getShowListener(zoneId).onAdClosed();
        }
    }

    @Override
    public void adClicked(AppLovinAd appLovinAd) {
        Log.i(TAG, "adClicked:");
        if (mAppLovinICBR.getShowListener(zoneId) != null) {
            mAppLovinICBR.getShowListener(zoneId).onAdVideoClicked();
        }
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.AD_PLACEMENT_ID);
    }
}
