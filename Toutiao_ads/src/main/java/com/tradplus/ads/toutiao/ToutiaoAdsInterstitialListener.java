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
    private int onRewardVerify = 0; // 1 表示已经触发过奖励回调
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
        Log.i(TAG, "onAdClose: 视频广告关闭");

        // 在看一次，toutiao奖励没有回调，没有跳过广告（设置一直回调）
        if (isAgainReward && onRewardVerifyAgain == 0 && alwaysRewardUserAgain) {
            mToutiaoICbR.getShowListener(placementId).onPlayAgainReward();
        } else if (onRewardVerify == 0 && alwaysRewardUser) {
            // toutiao奖励没有回调，没有跳过广告（设置一直回调）
            mToutiaoICbR.getShowListener(placementId).onReward();
        }

        mToutiaoICbR.getShowListener(placementId).onAdClosed();
    }

    @Override
    public void onVideoComplete() {
        Log.i(TAG, "onVideoComplete: 视频播放完成");
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
        //视频播放完成后，奖励验证回调，rewardVerify：是否有效，rewardAmount：奖励梳理，rewardName：奖励名称，code：错误码，msg：错误信息
//        Log.i(TAG, "onRewardVerify 是否有效 :" + rewardVerify + ", 是否有错误code : " + code + " , msg : " + msg);
//        if (mToutiaoICbR.getShowListener(placementId) != null && !isRewardCallback && b) {
//            isRewardCallback = true;
//            mToutiaoICbR.getShowListener(placementId).onReward();
//        }
        // 不使用户服务端奖励验证的情况下onRewardVerify回调只校验视频播放状态或者进度
        // 视频播放90%或者因播放器异常导致出现播放失败，那么穿山甲都会回调onRewardVerify，并且rewardVerify=true
        // 需要服务器判断：可根据rewardVerify作为判断条件 进行奖励的发放
        // 无需服务器判断：rewardVerify默认是返回true 测试状态的代码位rewardVerify默认返回false
//        if (rewardVerify) {
//            if (mToutiaoICbR.getShowListener(placementId) != null) {
//                if (isAgainReward) {
//                    mToutiaoICbR.getShowListener(placementId).onPlayAgainReward();
//                } else {
//                    mToutiaoICbR.getShowListener(placementId).onReward();
//                }
//            }
//        }
    }

    /**
     * 激励视频播放完毕，验证是否有效发放奖励的回调 4400版本新增
     *
     * @param isRewardValid 奖励有效
     * @param rewardType    奖励类型，0:基础奖励 >0:进阶奖励
     * @param extraInfo     奖励的额外参数
     */
    @Override
    public void onRewardArrived(boolean isRewardValid, int rewardType, Bundle extraInfo) {
        Log.i(TAG, "onRewardVerify 是否有效 :" + isRewardValid + ", rewardType :" + rewardType + ", extraInfo:" + extraInfo);
        if(mToutiaoICbR.getShowListener(placementId) == null) return;

        if (isRewardValid) {
            Map<String, Object> mHashMap = new HashMap<>();

            mHashMap.put(ToutiaoConstant.REWARD_TYPE, rewardType);

            try {
                if (extraInfo != null) {
                    HashMap<String, Object> map = new HashMap();
                    Set<String> keySet = extraInfo.keySet();   // 得到bundle中所有的key
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
        Log.i(TAG, "onAdSkip: 用户点击跳过");
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
