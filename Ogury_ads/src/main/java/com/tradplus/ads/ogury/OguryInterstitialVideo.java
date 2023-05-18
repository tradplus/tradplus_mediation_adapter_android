package com.tradplus.ads.ogury;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.ogury.core.OguryError;
import com.ogury.ed.OguryOptinVideoAd;
import com.ogury.ed.OguryOptinVideoAdListener;
import com.ogury.ed.OguryReward;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import io.presage.common.PresageSdk;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class OguryInterstitialVideo extends TPRewardAdapter {

    private String appId, mPlacementId, userId;
    private OguryInterstitialCallbackRouter mCallbackRouter;
    private OguryOptinVideoAd optinVideo;
    private static final String TAG = "OguryInterstitialVideo";
    private boolean hasGrantedReward;
    private boolean alwaysRewardUser;

    @Override
    public void loadCustomAd(Context context,
                             Map<String, Object> localExtras,
                             Map<String, String> serverExtras) {

        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(serverExtras)) {
            appId = serverExtras.get(AppKeyManager.APP_ID);
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            if (!TextUtils.isEmpty(serverExtras.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(serverExtras.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (localExtras != null && localExtras.size() > 0) {
            userId = (String) localExtras.get(AppKeyManager.CUSTOM_USERID);

            if (TextUtils.isEmpty(userId)) {
                userId = "";
            }
        }

//        appId = "OGY-955A72153B4A";
//        mPlacementId = "2189d8f0-321d-0139-77ec-0242ac120004";
        mCallbackRouter = OguryInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);

        OguryInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestRewardVideo(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestRewardVideo(Context context) {
        //Instantiate an Opt-in Video Ad
        optinVideo = new OguryOptinVideoAd(context, mPlacementId);

        if (!TextUtils.isEmpty(userId)) {
            // Create the rewarded ad data
            Log.i(TAG, "RewardData: userId : " + userId);
            optinVideo.setUserId(userId);
        }
        //Register a listener to reward the user
        optinVideo.setListener(optinVideoListener);
        //Load an Opt-in Video Ad
        optinVideo.load();

    }

    private final OguryOptinVideoAdListener optinVideoListener = new OguryOptinVideoAdListener() {

        @Override
        public void onAdLoaded() {
            Log.i(TAG, "onAdLoaded: ");
            if (mCallbackRouter.getListener(mPlacementId) != null) {
                setNetworkObjectAd(optinVideo);
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
            }

        }

        @Override
        public void onAdDisplayed() {
            Log.i(TAG, "onAdDisplayed: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdShown();
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoStart();
            }

        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null)
                mCallbackRouter.getShowListener(mPlacementId).onAdClicked();

        }

        @Override
        public void onAdClosed() {
            if (mCallbackRouter.getShowListener(mPlacementId) == null) {
                return;
            }
            mCallbackRouter.getShowListener(mPlacementId).onAdVideoEnd();

            if (hasGrantedReward || alwaysRewardUser) {
                mCallbackRouter.getShowListener(mPlacementId).onReward();
            }

            Log.i(TAG, "onAdClosed: ");
            mCallbackRouter.getShowListener(mPlacementId).onAdClosed();


        }

        @Override
        public void onAdError(OguryError oguryError) {
            if (oguryError.getErrorCode() == 2006 && appId != null) {
                AppKeyManager.getInstance().removeAppKey(appId);
            }

            Log.i(TAG, "onAdError: ErrorCode  :" + oguryError.getErrorCode() + " , Message :" + oguryError.getMessage());
            if (mCallbackRouter.getListener(mPlacementId) != null)
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(OguryErrorUtil.getTradPlusErrorCode(oguryError));


        }

        @Override
        public void onAdRewarded(OguryReward oguryReward) {
            // reward the user here
            Log.i(TAG, "onAdRewarded: ");
            hasGrantedReward = true;

        }

    };

    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);
        }
        if (optinVideo != null) {
            if (optinVideo.isLoaded()) {
                optinVideo.show();
            } else {
                Log.i(TAG, "showInterstitial: optinVideo.isLoaded == false");
                if (mCallbackRouter.getShowListener(mPlacementId) != null)
                    mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));

            }
        } else {
            Log.i(TAG, "showInterstitial: optinVideo == null");
            if (mCallbackRouter.getShowListener(mPlacementId) != null)
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));

        }
    }

    @Override
    public boolean isReady() {
        if (optinVideo == null) {
            return false;
        } else {
            return optinVideo.isLoaded();
        }
    }

    @Override
    public void clean() {
        if (optinVideo != null) {
            optinVideo.setListener(null);
            optinVideo = null;
        }

        if (mPlacementId != null) {
            mCallbackRouter.removeListeners(mPlacementId);
        }
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_OGURY);
    }

    @Override
    public String getNetworkVersion() {
        return PresageSdk.getAdsSdkVersion();
    }
}
