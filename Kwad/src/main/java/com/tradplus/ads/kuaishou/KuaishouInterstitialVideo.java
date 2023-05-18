package com.tradplus.ads.kuaishou;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.kwad.sdk.api.KsAdSDK;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsRewardVideoAd;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.KsVideoPlayConfig;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.kwad_ads.KuaishouInitManager;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class KuaishouInterstitialVideo extends TPRewardAdapter implements KsRewardVideoAd.RewardAdInteractionListener {

    private String placementId, userId, customData, mBidResponseV2;
    private Integer direction;
    private KuaishouInterstitialCallbackRouter mRouter;
    private KsRewardVideoAd mRewardVideoAd;
    private boolean isVideoSoundEnable = true;
    private boolean hasGrantedReward = false;
    private static final String TAG = "KuaishouRewardVideo";
    private boolean canAgain;
    private boolean alwaysRewardUser;
    private boolean alwaysRewardUserAgain;
    private int onRewardVerify = 0;
    private int onRewardVerifyAgain = 0;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            mBidResponseV2 = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            String videoMute = tpParams.get(AppKeyManager.VIDEO_MUTE);
            String direct = tpParams.get(AppKeyManager.DIRECTION);

            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
                alwaysRewardUserAgain = alwaysRewardUser;
            }

            if (!TextUtils.isEmpty(videoMute)) {
                if (videoMute.equals(AppKeyManager.VIDEO_MUTE_YES)) {
                    isVideoSoundEnable = false;
                }
            }
            if (!TextUtils.isEmpty(direct)) {
                direction = Integer.valueOf(direct);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        getUseParams(userParams);

        mRouter = KuaishouInterstitialCallbackRouter.getInstance();
        mRouter.addListener(placementId, mLoadAdapterListener);

        KuaishouInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitialVideo();
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

    }

    private void getUseParams(Map<String, Object> userParams) {
        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.AD_REWARD_AGAIN)) {
                canAgain = true;
            }
            if (userParams.containsKey(AppKeyManager.CUSTOM_USERID)) {
                userId = (String) userParams.get(AppKeyManager.CUSTOM_USERID);
                if (TextUtils.isEmpty(userId)) {
                    userId = "";
                }
            }

            if (userParams.containsKey(AppKeyManager.CUSTOM_DATA)) {
                customData = (String) userParams.get(AppKeyManager.CUSTOM_DATA);
                if (TextUtils.isEmpty(customData)) {
                    customData = "";
                }
            }
        }
    }

    private KsScene getKsScene() {
        KsScene.Builder builder = new KsScene.Builder(Long.parseLong(placementId));
        if (canAgain) {
            builder.adNum(2);
        }
        if (!TextUtils.isEmpty(userId) || !TextUtils.isEmpty(customData)) {
            Log.i(TAG, "RewardData: userId : " + userId + " , customData : " + customData);
            Map<String, String> rewardCallbackExtraData = new HashMap<>();
            if (!TextUtils.isEmpty(userId)) {
                rewardCallbackExtraData.put("thirdUserId", userId);
            }
            if (!TextUtils.isEmpty(customData)) {
                rewardCallbackExtraData.put("extraData", customData);
            }
            builder.rewardCallbackExtraData(rewardCallbackExtraData);
        }

        if (!TextUtils.isEmpty(mBidResponseV2)) {
            builder.setBidResponseV2(mBidResponseV2);
        }

        return builder.build();
    }

    private void requestInterstitialVideo() {
        mRewardVideoAd = null;
        KsAdSDK.getLoadManager().loadRewardVideoAd(getKsScene(), new KsLoadManager.RewardVideoAdListener() {
            @Override
            public void onError(int code, String msg) {
                Log.i(TAG, "onError: errorCode ：" + code + ", errormessage :" + msg);
                if (mRouter.getListener(placementId) != null) {
                    mRouter.getListener(placementId).loadAdapterLoadFailed(KuaishouErrorUtil.geTpMsg(NETWORK_NO_FILL, code, msg));
                }
            }

            @Override
            public void onRewardVideoResult(List<KsRewardVideoAd> list) {
                Log.i(TAG, "onRequestResult: ");
            }

            @Override
            public void onRewardVideoAdLoad(List<KsRewardVideoAd> adList) {
                if (adList != null && adList.size() > 0) {
                    mRewardVideoAd = adList.get(0);
                    Log.i(TAG, "onRewardVideoAdLoad: ");
                    if (mRouter.getListener(placementId) != null) {
                        setFirstLoadedTime();
                        setNetworkObjectAd(mRewardVideoAd);
                        mRouter.getListener(placementId).loadAdapterLoaded(null);
                    }
                } else {
                    Log.i(TAG, "onRewardVideoAdLoad,but adList < 0");
                    if (mRouter.getListener(placementId) != null) {
                        TPError tpError = new TPError(NETWORK_NO_FILL);
                        tpError.setErrorMessage("onRewardVideoAdLoad,but adList < 0");
                        mRouter.getListener(placementId).loadAdapterLoadFailed(tpError);
                    }
                }

            }
        });

    }

    @Override
    public void showAd() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }
        if (mShowListener != null) {
            mRouter.addShowListener(placementId, mShowListener);
        }

        if (mRewardVideoAd != null && mRewardVideoAd.isAdEnable()) {
            mRewardVideoAd.setRewardAdInteractionListener(this);
            mRewardVideoAd.setRewardPlayAgainInteractionListener(new KsRewardVideoAd.RewardAdInteractionListener() {
                @Override
                public void onAdClicked() {
                    Log.i(TAG, "onAdAgainVideoClicked: ");
                    if (mRouter.getShowListener(placementId) != null) {
                        mRouter.getShowListener(placementId).onAdAgainVideoClicked();
                    }
                }

                @Override
                public void onPageDismiss() {
                    Log.i(TAG, "onPageDismiss: ");
                    if (mRouter.getShowListener(placementId) != null) {
                        if (onRewardVerifyAgain == 0 && alwaysRewardUserAgain) {
                            mRouter.getShowListener(placementId).onReward();
                        }
                        mRouter.getShowListener(placementId).onAdClosed();
                    }
                }

                @Override
                public void onVideoPlayError(int i, int i1) {

                }

                @Override
                public void onVideoPlayEnd() {
                    Log.i(TAG, "onAdAgainVideoEnd: ");
                    if (mRouter.getShowListener(placementId) != null) {
                        mRouter.getShowListener(placementId).onAdAgainVideoEnd();
                    }
                }

                @Override
                public void onVideoSkipToEnd(long l) {
                    Log.i(TAG, "onVideoSkipToEnd: ");
                    alwaysRewardUserAgain = false;
                    if (mRouter.getShowListener(placementId) != null) {
                        mRouter.getShowListener(placementId).onRewardSkip();
                    }
                }

                @Override
                public void onVideoPlayStart() {
                    Log.i(TAG, "onAdAgainVideoStart: ");
                    if (mRouter.getShowListener(placementId) != null) {
                        mRouter.getShowListener(placementId).onAdAgainVideoStart();
                        mRouter.getShowListener(placementId).onAdAgainShown();
                    }

                }

                @Override
                public void onRewardVerify() {

                }

                @Override
                public void onRewardStepVerify(int taskType, int currentTaskStatus) {
                    onRewardVerifyAgain = 1;
                    if (mRouter.getShowListener(placementId) != null) {
                        Map<String, Object> mHashMap = new HashMap<>();
                        mHashMap.put(KuaishouConstant.STEP_VERIFY_TASKTYPE, taskType);
                        mHashMap.put(KuaishouConstant.STEP_VERIFY_TASKSTATUS, currentTaskStatus);
                        mRouter.getShowListener(placementId).onPlayAgainReward(mHashMap);
                    }
                }

                @Override
                public void onExtraRewardVerify(int i) {

                }
            });
            mRewardVideoAd.showRewardVideoAd(activity, videoPlayConfig(activity));
        } else {
            if (mRouter.getShowListener(placementId) != null)
                mRouter.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));
        }
    }

    private KsVideoPlayConfig videoPlayConfig(Activity activity) {
        KsVideoPlayConfig.Builder builder = new KsVideoPlayConfig.Builder();
        if (direction == 1 || direction == 2) {
            builder.showLandscape(direction == 2);
        } else {
            int ori = activity.getResources().getConfiguration().orientation;
            builder.showLandscape(ori == ORIENTATION_LANDSCAPE);
        }

        Log.i(TAG, "videoSoundEnable: " + isVideoSoundEnable);
        builder.videoSoundEnable(isVideoSoundEnable);

        return builder.build();
    }

    @Override
    public boolean isReady() {
        if (mRewardVideoAd != null) {
            return !isAdsTimeOut() && mRewardVideoAd.isAdEnable();
        } else {
            return !isAdsTimeOut();
        }

    }

    @Override
    public void clean() {
        super.clean();
        if (mRewardVideoAd != null) {
            mRewardVideoAd.setRewardAdInteractionListener(null);
            mRewardVideoAd.setRewardPlayAgainInteractionListener(null);
            mRewardVideoAd = null;
        }

        if (placementId != null)
            mRouter.removeListeners(placementId);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_KUAISHOU);
    }

    @Override
    public String getNetworkVersion() {
        return KsAdSDK.getSDKVersion();
    }


    @Override
    public String getBiddingToken(Context context, Map<String, String> tpParams, Map<String, Object> userParams) {
        KuaishouInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "getBiddingToken onSuccess: ");

            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });
        return KsAdSDK.getLoadManager().getBidRequestTokenV2(new KsScene.Builder(0).build());
    }

    @Override
    public void onAdClicked() {
        if (mRouter.getShowListener(placementId) == null) {
            return;
        }
        Log.i(TAG, "onAdClicked: ");
        mRouter.getShowListener(placementId).onAdVideoClicked();
    }

    @Override
    public void onPageDismiss() {
        if (mRouter.getShowListener(placementId) == null) return;
        Log.i(TAG, "onPageDismiss: ");
        if (onRewardVerify == 0 && alwaysRewardUser) {
            mRouter.getShowListener(placementId).onReward();
        }
        mRouter.getShowListener(placementId).onAdClosed();
    }

    @Override
    public void onVideoPlayError(int code, int extra) {
        Log.i(TAG, "onVideoPlayError: code :" + code);
        if (mRouter.getShowListener(placementId) != null) {
            mRouter.getShowListener(placementId).onAdVideoError(KuaishouErrorUtil.getTradPlusErrorCode(code));
        }
    }

    @Override
    public void onVideoPlayEnd() {
        if (mRouter.getShowListener(placementId) == null) return;
        Log.i(TAG, "onVideoPlayEnd: ");
        mRouter.getShowListener(placementId).onAdVideoEnd();
    }

    @Override
    public void onVideoSkipToEnd(long l) {
        Log.i(TAG, "onVideoSkipToEnd: ");
        alwaysRewardUser = false;
        if (mRouter.getShowListener(placementId) != null) {
            mRouter.getShowListener(placementId).onRewardSkip();
        }
    }

    @Override
    public void onVideoPlayStart() {
        if (mRouter.getShowListener(placementId) == null) {
            return;
        }

        Log.i(TAG, "onVideoPlayStart: ");
        mRouter.getShowListener(placementId).onAdVideoStart();
        mRouter.getShowListener(placementId).onAdShown();

    }

    @Override
    public void onRewardVerify() {

    }


    @Override
    public void onRewardStepVerify(int taskType, int currentTaskStatus) {
        onRewardVerify = 1;
        if (mRouter.getShowListener(placementId) != null) {
            Map<String, Object> mHashMap = new HashMap<>();
            mHashMap.put(KuaishouConstant.STEP_VERIFY_TASKTYPE, taskType);
            mHashMap.put(KuaishouConstant.STEP_VERIFY_TASKSTATUS, currentTaskStatus);
            mRouter.getShowListener(placementId).onReward(mHashMap);
        }
    }


    @Override
    public void onExtraRewardVerify(int extraRewardType) {

    }

}
