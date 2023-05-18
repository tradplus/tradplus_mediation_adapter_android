package com.tradplus.ads.network;

import android.content.Context;
import android.util.Log;

import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;
import com.tradplus.crosspro.network.base.CPError;
import com.tradplus.crosspro.network.rewardvideo.CPRewardVideoAd;
import com.tradplus.crosspro.network.rewardvideo.CPRewardVideoAdListener;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;

public class CPADRewardVideoAdapter extends TPRewardAdapter {

    private String campaignId;
    private String adSourceId;
    private CPRewardVideoAd cpRewardVideoAd;
    private long timeoutValue = 3 * 60 * 60 * 1000; //3 小时
    private long mFirstLoadTime;
    public final static long TIME_DELTA = 30 * 1000;
    private static String TAG = "CrossPro";
    private int direction;
    private boolean isReward;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (extrasAreValid(tpParams)) {
            campaignId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
                return;
            }
        }
        if (tpParams.containsKey(AppKeyManager.ADSOURCE_PLACEMENT_ID)) {
            adSourceId = tpParams.get(AppKeyManager.ADSOURCE_PLACEMENT_ID);
        }

        Object d = userParams.get(AppKeyManager.KEY_DIRECTION);
        if (d != null) {
            direction = Integer.parseInt(String.valueOf(d));
        }
        cpRewardVideoAd = new CPRewardVideoAd(context, campaignId, adSourceId);
        cpRewardVideoAd.setCpRewardVideoAdListener(new CPRewardVideoAdListener() {
            @Override
            public void onVideoAdPlayStart() {
                Log.i(TAG, "onVideoAdPlayStart: ");
                if (mShowListener != null) {
                    mShowListener.onAdVideoStart();
                }
            }

            @Override
            public void onVideoAdPlayEnd() {
                Log.i(TAG, "onVideoAdPlayEnd: ");
                if (mShowListener != null) {
                    mShowListener.onAdVideoEnd();
                }
            }

            @Override
            public void onVideoShowFailed(CPError error) {
                Log.i(TAG, "onVideoShowFailed: ");
                if (mShowListener != null) {
                    mShowListener.onAdVideoError(CPErrorUtil.getTradPlusErrorCode(error));
                }
            }

            @Override
            public void onRewarded() {
                Log.i(TAG, "onRewarded: ");
                isReward = true;
            }

            @Override
            public void onInterstitialLoad() {

            }

            @Override
            public void onInterstitialLoaded() {
                Log.i(TAG, "onInterstitialLoaded: ");
                setTimeoutValue(cpRewardVideoAd.getExpreTime());
                mFirstLoadTime = System.currentTimeMillis();

                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoaded(null);
                }
            }

            @Override
            public void onInterstitialFailed(TPError error) {
                Log.i(TAG, "onInterstitialFailed: ");
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(error);
                }
            }

            @Override
            public void onInterstitialShown() {
                Log.i(TAG, "onInterstitialShown: ");
                if (mShowListener != null) {
                    mShowListener.onAdShown();
                }
            }

            @Override
            public void onInterstitialClicked() {
                Log.i(TAG, "onInterstitialClicked: ");
                if (mShowListener != null) {
                    mShowListener.onAdVideoClicked();
                }
            }

            @Override
            public void onLeaveApplication() {
                Log.i(TAG, "onLeaveApplication: ");

            }

            @Override
            public void onInterstitialDismissed() {
                Log.i(TAG, "onInterstitialDismissed: ");
                if (mShowListener != null && isReward) {
                    mShowListener.onReward();
                }
                if (mShowListener != null) {
                    mShowListener.onAdClosed();
                }
            }

            @Override
            public void onInterstitialRewarded(String currencyName, int amount) {
                Log.i(TAG, "onInterstitialRewarded: ");
            }
        });
        cpRewardVideoAd.setDirection(direction);
        cpRewardVideoAd.load();


    }

    @Override
    public void showAd() {
        if (cpRewardVideoAd != null) {
            cpRewardVideoAd.show();
        }
    }

    @Override
    public boolean isReady() {
        if (cpRewardVideoAd != null) {
            return cpRewardVideoAd.isReady() && !isAdsTimeOut();
        } else {
            return false;
        }
    }

    @Override
    public void clean() {
        super.clean();
        if (cpRewardVideoAd != null) {
            cpRewardVideoAd.setCpRewardVideoAdListener(null);
            cpRewardVideoAd = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_CPAD);
    }

    @Override
    public String getNetworkVersion() {
        return null;
    }

    private boolean extrasAreValid(final Map<String, String> tpParams) {
        final String placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    public void setTimeoutValue(long timeoutValue) {
        this.timeoutValue = timeoutValue;
    }

    public boolean isAdsTimeOut() {
        return System.currentTimeMillis() - mFirstLoadTime + TIME_DELTA > timeoutValue;
    }
}
