package com.tradplus.ads.toutiao;

import android.os.Bundle;
import android.util.Log;

import com.bytedance.sdk.openadsdk.TTRewardVideoAd;
import com.tradplus.ads.base.common.TPError;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ToutiaoAdsInterstitialListener implements TTRewardVideoAd.RewardAdInteractionListener {
    private String placementId;
    private ToutiaoInterstitialCallbackRouter mToutiaoICbR;
    private static final String TAG = "ToutiaoRewardVideo";
    private boolean isAgainReward;
    private boolean alwaysRewardUser;
    private boolean alwaysRewardUserAgain;
    private int onRewardVerify = 0;
    private int onRewardVerifyAgain = 0;

    public ToutiaoAdsInterstitialListener(String id, boolean rewardUser) {
        placementId = id;
        alwaysRewardUser = rewardUser;
        mToutiaoICbR = ToutiaoInterstitialCallbackRouter.getInstance();
    }

    public ToutiaoAdsInterstitialListener(String id, boolean isAgainReward, boolean rewardUser) {
        placementId = id;
        this.isAgainReward = isAgainReward;
        alwaysRewardUserAgain = rewardUser;
        mToutiaoICbR = ToutiaoInterstitialCallbackRouter.getInstance();
    }

    @Override
    public void onAdShow() {
        if (mToutiaoICbR.getShowListener(placementId) == null) {
            return;
        }
        if (isAgainReward) {
            mToutiaoICbR.getShowListener(placementId).onAdAgainShown();
            mToutiaoICbR.getShowListener(placementId).onAdAgainVideoStart();
        } else {
            mToutiaoICbR.getShowListener(placementId).onAdVideoStart();
            mToutiaoICbR.getShowListener(placementId).onAdShown();
        }
    }

    @Override
    public void onAdVideoBarClick() {
        Log.i(TAG, "onAdVideoBarClick: ");
        if (mToutiaoICbR.getShowListener(placementId) != null) {
            if (isAgainReward) {
                mToutiaoICbR.getShowListener(placementId).onAdAgainVideoClicked();
            } else {
                mToutiaoICbR.getShowListener(placementId).onAdVideoClicked();
            }
        }
    }

    @Override
    public void onAdClose() {
        if (mToutiaoICbR.getShowListener(placementId) == null) return;
        Log.i(TAG, "onAdClose: ");

        if (isAgainReward && onRewardVerifyAgain == 0 && alwaysRewardUserAgain) {
            mToutiaoICbR.getShowListener(placementId).onPlayAgainReward();
        } else if (onRewardVerify == 0 && alwaysRewardUser) {
            mToutiaoICbR.getShowListener(placementId).onReward();
        }

        mToutiaoICbR.getShowListener(placementId).onAdClosed();
    }

    @Override
    public void onVideoComplete() {
        Log.i(TAG, "onVideoComplete: ");
        if (mToutiaoICbR.getShowListener(placementId) != null) {
            if (isAgainReward) {
                mToutiaoICbR.getShowListener(placementId).onAdAgainVideoEnd();
            } else {
                mToutiaoICbR.getShowListener(placementId).onAdVideoEnd();
            }
        }
    }

    @Override
    public void onVideoError() {
        Log.i(TAG, "onVideoError: ");
        if (mToutiaoICbR.getShowListener(placementId) != null) {
            mToutiaoICbR.getShowListener(placementId).onAdVideoError(new TPError(TPError.SHOW_FAILED));
        }
    }

    @Deprecated
    @Override
    public void onRewardVerify(boolean rewardVerify, int rewardAmount, String rewardName, int code, String msg) {

    }

    @Override
    public void onRewardArrived(boolean isRewardValid, int rewardType, Bundle extraInfo) {
        Log.i(TAG, "onRewardVerify  :" + isRewardValid + ", rewardType :" + rewardType + ", extraInfo:" + extraInfo);
        if(mToutiaoICbR.getShowListener(placementId) == null) return;

        if (isRewardValid) {
            Map<String, Object> mHashMap = new HashMap<>();

            mHashMap.put(ToutiaoConstant.REWARD_TYPE, rewardType);

            try {
                if (extraInfo != null) {
                    HashMap<String, Object> map = new HashMap();
                    Set<String> keySet = extraInfo.keySet();
                    Iterator<String> iter = keySet.iterator();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        map.put(key, extraInfo.get(key));
                    }
                    mHashMap.put(ToutiaoConstant.EXTRA, map);
                }
            } catch (Throwable throwable) {

            }

            if (isAgainReward) {
                onRewardVerifyAgain = 1;
                mToutiaoICbR.getShowListener(placementId).onPlayAgainReward(mHashMap);
            } else {
                onRewardVerify = 1;
                mToutiaoICbR.getShowListener(placementId).onReward(mHashMap);
            }
        }
    }


    @Override
    public void onSkippedVideo() {
        Log.i(TAG, "onAdSkip: ");
        if (isAgainReward) {
            alwaysRewardUserAgain = false;
        } else {
            alwaysRewardUser = false;
        }

        if (mToutiaoICbR.getShowListener(placementId) != null) {
            mToutiaoICbR.getShowListener(placementId).onRewardSkip();
        }
    }
}
