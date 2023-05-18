package com.tradplus.startapp;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.adsbase.model.AdPreferences;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class StartAppInterstitialVideo extends TPRewardAdapter {

    private StartAppAd rewardedVideo;
    private String mPlacementId;
    private StartAppInterstitialCallbackRouter mStartAppICbR;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private static final String TAG = "StartAppRewardVideo";

    @Override
    public void loadCustomAd(final Context context,
                             Map<String, Object> userParams,
                             Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.containsKey(AppKeyManager.AD_PLACEMENT_ID)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        mStartAppICbR = StartAppInterstitialCallbackRouter.getInstance();
        mStartAppICbR.addListener(mPlacementId, mLoadAdapterListener);

        StartAppInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestAd(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }


    private void requestAd(Context context) {
        AdPreferences prefs = new AdPreferences();
        prefs.setAdTag(mPlacementId);

        rewardedVideo = new StartAppAd(context);
        rewardedVideo.setVideoListener(new com.startapp.sdk.adsbase.adlisteners.VideoListener() {
            @Override
            public void onVideoCompleted() {
                Log.i(TAG, "onVideoCompleted: ");
                hasGrantedReward = true;
                if (mStartAppICbR.getShowListener(mPlacementId) != null) {
                    mStartAppICbR.getShowListener(mPlacementId).onAdVideoEnd();
                }
            }
        });


        rewardedVideo.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, prefs, new AdEventListener() {
            @Override
            public void onReceiveAd(Ad ad) {
                Log.i(TAG, "onReceiveAd: ");
                if (mStartAppICbR.getListener(mPlacementId) != null) {
                    setNetworkObjectAd(ad);
                    mStartAppICbR.getListener(mPlacementId).loadAdapterLoaded(null);
                }
            }

            @Override
            public void onFailedToReceiveAd(Ad ad) {
                Log.i(TAG, "onFailedToReceiveAd: ad : " + ad.getErrorMessage() + "，mPlacementId ：" + mPlacementId);
                if (mStartAppICbR.getListener(mPlacementId) != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    if (ad.getErrorMessage() != null) {
                        tpError.setErrorMessage(ad.getErrorMessage());
                        mStartAppICbR.getListener(mPlacementId).loadAdapterLoadFailed(tpError);
                    } else {
                        tpError.setErrorMessage("StartApp has not error msg.");
                        mStartAppICbR.getListener(mPlacementId).loadAdapterLoadFailed(tpError);
                    }
                }

            }
        });
    }

    @Override
    public void showAd() {
        if (mShowListener != null)
            mStartAppICbR.addShowListener(mPlacementId, mShowListener);

        if (rewardedVideo != null && rewardedVideo.isReady() && mPlacementId != null) {
            rewardedVideo.showAd(mPlacementId, new AdDisplayListener() {
                @Override
                public void adHidden(Ad ad) {
                    //广告页面中点击关闭时候会回调
                    Log.i(TAG, "adHidden: ");
                    if (mStartAppICbR.getShowListener(mPlacementId) == null) {
                        return;
                    }
                    if (hasGrantedReward || alwaysRewardUser) {
                        mStartAppICbR.getShowListener(mPlacementId).onReward();
                    }
                    mStartAppICbR.getShowListener(mPlacementId).onAdClosed();
                }

                @Override
                public void adDisplayed(Ad ad) {
                    Log.i(TAG, "adDisplayed : ad ：" + ad.getAdId());
                    if (mStartAppICbR.getShowListener(mPlacementId) != null) {
                        mStartAppICbR.getShowListener(mPlacementId).onAdVideoStart();
                        mStartAppICbR.getShowListener(mPlacementId).onAdShown();
                    }
                }

                @Override
                public void adClicked(Ad ad) {
                    Log.i(TAG, "adClicked: ");
                    if (mStartAppICbR.getShowListener(mPlacementId) != null)
                        mStartAppICbR.getShowListener(mPlacementId).onAdVideoClicked();
                }

                @Override
                public void adNotDisplayed(Ad ad) {
                    Log.i(TAG, "adNotDisplayed: ");
                    if (mStartAppICbR.getShowListener(mPlacementId) != null) {
                        TPError tpError = new TPError(NETWORK_NO_FILL);
                        tpError.setErrorMessage(ad.getErrorMessage());
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                        mStartAppICbR.getShowListener(mPlacementId).onAdVideoError(tpError);
                    }
                }
            });

        } else {
            if (mStartAppICbR.getShowListener(mPlacementId) != null) {
                mStartAppICbR.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        }
    }


    @Override
    public boolean isReady() {
        if (rewardedVideo != null) {
            return rewardedVideo.isReady() && !isAdsTimeOut();
        } else {
            return false;
        }
    }

    @Override
    public void clean() {
        super.clean();
        if (mShowListener != null) {
            mStartAppICbR.removeListeners(mPlacementId);
        }
        if (rewardedVideo != null) {
            rewardedVideo.setVideoListener(null);
            rewardedVideo = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_STARTAPP);
    }

    @Override
    public String getNetworkVersion() {
        return StartAppSDK.getVersion();
    }

}
