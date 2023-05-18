package com.tradplus.appnext;

import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.appnext.ads.fullscreen.RewardedVideo;
import com.appnext.core.AppnextAdCreativeType;
import com.appnext.core.callbacks.OnAdClicked;
import com.appnext.core.callbacks.OnAdClosed;
import com.appnext.core.callbacks.OnAdError;
import com.appnext.core.callbacks.OnAdLoaded;
import com.appnext.core.callbacks.OnAdOpened;
import com.appnext.core.callbacks.OnVideoEnded;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class AppNextReward extends TPRewardAdapter implements OnAdLoaded, OnAdClicked, OnAdClosed, OnAdError, OnAdOpened {
    public static final String TAG = "AppNextReward";
    private String mPID, userId;
    private RewardedVideo mRewardedVideo;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private AppNextInterstitialCallbackRouter mAppNextInterstitialCallbackRouter;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        Log.i(TAG, "loadInterstitial: ");
        if (mLoadAdapterListener == null) {
            return;
        }
        if (serverExtras != null && serverExtras.size() > 0) {
            mPID = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            if (!TextUtils.isEmpty(serverExtras.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(serverExtras.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        }

        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(AppKeyManager.CUSTOM_USERID)) {
                userId = (String) localExtras.get(AppKeyManager.CUSTOM_USERID);
                if (TextUtils.isEmpty(userId)) {
                    userId = "";
                }
            }

        }
        mAppNextInterstitialCallbackRouter = AppNextInterstitialCallbackRouter.getInstance();
        mAppNextInterstitialCallbackRouter.addListener(mPID, mLoadAdapterListener);
        mLoadAdapterListener = mAppNextInterstitialCallbackRouter.getListener(mPID);

        AppNextInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requesInterstitialVideo(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requesInterstitialVideo(Context context) {
        if (mRewardedVideo == null)
            mRewardedVideo = new RewardedVideo(context, mPID);

        if (!TextUtils.isEmpty(userId)) {
            Log.i(TAG, "RewardData: userId : " + userId);
            mRewardedVideo.setRewardsUserId(userId);
        }
        mRewardedVideo.setOnAdClickedCallback(this);
        mRewardedVideo.setOnAdClosedCallback(this);
        mRewardedVideo.setOnAdErrorCallback(this);
        mRewardedVideo.setOnAdOpenedCallback(this);
        mRewardedVideo.setOnAdLoadedCallback(this);
        mRewardedVideo.setOnVideoEndedCallback(new OnVideoEnded() {
            @Override
            public void videoEnded() {
                Log.i(TAG, "videoEnded: ");
                hasGrantedReward = true;

                if (mShowListener != null) {
                    mShowListener.onAdVideoEnd();
                }

            }
        });
        mRewardedVideo.loadAd();
    }

    @Override
    public void showAd() {
        if (mAppNextInterstitialCallbackRouter != null && mShowListener != null) {
            mAppNextInterstitialCallbackRouter.addShowListener(mPID, mShowListener);
        }
        mShowListener = mAppNextInterstitialCallbackRouter.getShowListener(mPID);

        Log.i(TAG, "showInterstitial: ");
        if (mRewardedVideo.isAdLoaded()) {
            mRewardedVideo.showAd();
        }
    }

    @Override
    public void clean() {
        super.clean();
        if (mRewardedVideo != null) {
            mRewardedVideo.destroy();
            mRewardedVideo = null;
        }
    }

    @Override
    public boolean isReady() {
        if (mRewardedVideo != null) {
            return mRewardedVideo.isAdLoaded() && !isAdsTimeOut();
        }
        return false;
    }

    @Override
    public void adClicked() {
        Log.i(TAG, "adClicked: ");
        if (mShowListener != null) {
            mShowListener.onAdVideoClicked();
        }
    }

    @Override
    public void onAdClosed() {
        Log.i(TAG, "onAdClosed: ");
        if (mShowListener != null) {
            if (hasGrantedReward || alwaysRewardUser) {
                mShowListener.onReward();
            }
            mShowListener.onAdClosed();
        }
    }

    @Override
    public void adError(String s) {
        Log.i(TAG, "adError: " + s);
        if (mLoadAdapterListener != null) {
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorMessage(s);
            mLoadAdapterListener.loadAdapterLoadFailed(tpError);
        }
    }

    @Override
    public void adOpened() {
        Log.i(TAG, "adOpened: ");
        if (mShowListener != null) {
            mShowListener.onAdShown();
        }

        if (mShowListener != null) {
            mShowListener.onAdVideoStart();
        }
    }

    @Override
    public void adLoaded(String s, AppnextAdCreativeType appnextAdCreativeType) {
        Log.i(TAG, "adLoaded: " + s);
        if (mLoadAdapterListener != null) {
            setNetworkObjectAd(mRewardedVideo);
            mLoadAdapterListener.loadAdapterLoaded(null);
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_APPNEXT);
    }

    @Override
    public String getNetworkVersion() {
        return "2.6.5.473";
    }

}
