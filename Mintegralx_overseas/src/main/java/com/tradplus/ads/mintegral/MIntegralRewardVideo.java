package com.tradplus.ads.mintegral;

import static com.tradplus.ads.base.adapter.TPInitMediation.INIT_STATE_BIDDING;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.mbbid.out.BidManager;
import com.mbridge.msdk.out.MBBidRewardVideoHandler;
import com.mbridge.msdk.out.MBConfiguration;
import com.mbridge.msdk.out.MBRewardVideoHandler;
import com.mbridge.msdk.out.MBridgeIds;
import com.mbridge.msdk.out.RewardInfo;
import com.mbridge.msdk.out.RewardVideoListener;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPTaskManager;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

/**
 * need appkey and appid params
 * <p>
 * Test can use anr check tools,
 */
public class MIntegralRewardVideo extends TPRewardAdapter {

    private MBRewardVideoHandler mMTGRewardVideoHandler;
    private MBBidRewardVideoHandler mMTGBidRewardVideoHandler;
    private String mPlacementId, userId, customData;
    private static final String TAG = "MTGOSRewardedVideo";
    private MIntegralInterstitialCallbackRouter mMTGICbR;
    private String mUnitId;
    private String payload;
    private Integer mVideoMute = 1; // 静音
    private boolean canAgain = false;
    private boolean alwaysReward;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            mUnitId = tpParams.get(AppKeyManager.UNIT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                alwaysReward = (Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD)) == AppKeyManager.ENFORCE_REWARD);
            }
            // 视频静音 指定自动播放时是否静音: 1 自动播放时静音；2 自动播放时有声
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.VIDEO_MUTE))) {
                mVideoMute = Integer.parseInt(tpParams.get(AppKeyManager.VIDEO_MUTE));
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        if (userParams != null && userParams.size() > 0) {
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

            if (userParams.containsKey(MTGConstant.VIDEO_MUTE)) {
                mVideoMute = (int) userParams.get(MTGConstant.VIDEO_MUTE);
            }

            if (userParams.containsKey(MTGConstant.AD_REWARD_AGAIN)) {
                canAgain = true;
            }

        }


        mMTGICbR = MIntegralInterstitialCallbackRouter.getInstance();
        mMTGICbR.addListener(mPlacementId + mUnitId, mLoadAdapterListener);

        MintegralInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitalVideo(context);
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


    private void requestInterstitalVideo(Context context) {
        RewardVideoListener rewardVideoListener = new RewardVideoListener() {
            @Override
            public void onVideoLoadSuccess(MBridgeIds mBridgeIds) {
                Log.i(TAG, "onVideoLoadSuccess PID :" + mBridgeIds.getPlacementId() + ", UnitId : " + mBridgeIds.getUnitId());
                if (mMTGICbR.getListener(mPlacementId + mUnitId) != null) {
                    setFirstLoadedTime();
                    setNetworkObjectAd(mMTGRewardVideoHandler != null ? mMTGRewardVideoHandler : mMTGBidRewardVideoHandler);

                    mMTGICbR.getListener(mPlacementId + mUnitId).loadAdapterLoaded(null);
                }
            }

            @Override
            public void onLoadSuccess(MBridgeIds mBridgeIds) {

            }

            @Override
            public void onVideoLoadFail(MBridgeIds mBridgeIds, String errorMsg) {
                Log.i(TAG, "onVideoLoadFail errorMsg:" + errorMsg);
                if (mMTGICbR.getListener(mPlacementId + mUnitId) != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage(errorMsg);
                    mMTGICbR.getListener(mPlacementId + mUnitId).loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void onAdShow(MBridgeIds mBridgeIds) {
                Log.i(TAG, "onAdShow");
                if (mMTGICbR.getShowListener(mPlacementId + mUnitId) != null) {
                    mMTGICbR.getShowListener(mPlacementId + mUnitId).onAdVideoStart();
                    mMTGICbR.getShowListener(mPlacementId + mUnitId).onAdShown();
                }
            }

            @Override
            public void onAdClose(MBridgeIds mBridgeIds, RewardInfo rewardInfo) {
                if (mMTGICbR.getShowListener(mPlacementId + mUnitId) == null)
                    return;

                if (rewardInfo.isCompleteView() || alwaysReward) {
                    Log.i(TAG, "isCompleteView: ");
                    mMTGICbR.getShowListener(mPlacementId + mUnitId).onReward();
                }

                Log.i(TAG, "onAdClose: ");
                mMTGICbR.getShowListener(mPlacementId + mUnitId).onAdClosed();
            }

            @Override
            public void onShowFail(MBridgeIds mBridgeIds, String errorMsg) {
                Log.i(TAG, "onShowFail :errorMsg :" + errorMsg);
                if (mMTGICbR.getShowListener(mPlacementId + mUnitId) != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage(errorMsg);
                    mMTGICbR.getShowListener(mPlacementId + mUnitId).onAdVideoError(tpError);
                }
            }

            @Override
            public void onVideoAdClicked(MBridgeIds mBridgeIds) {
                Log.i(TAG, "onVideoAdClicked");
                if (mMTGICbR.getShowListener(mPlacementId + mUnitId) != null) {
                    mMTGICbR.getShowListener(mPlacementId + mUnitId).onAdVideoClicked();
                }
            }

            @Override
            public void onVideoComplete(MBridgeIds mBridgeIds) {
                Log.i(TAG, "onVideoComplete");
                // 试玩广告不回调该监听，故在onAdClose中用isCompleteView作判断
                if (mMTGICbR.getShowListener(mPlacementId + mUnitId) != null) {
                    mMTGICbR.getShowListener(mPlacementId + mUnitId).onAdVideoEnd();
                }
            }

            @Override
            public void onEndcardShow(MBridgeIds mBridgeIds) {

            }

        };

        if (TextUtils.isEmpty(payload)) {
            mMTGRewardVideoHandler = new MBRewardVideoHandler(context, mPlacementId, mUnitId);
            mMTGRewardVideoHandler.setRewardVideoListener(rewardVideoListener);
            // 服务器默认 1静音 2 有声
            // MTG 参数 也是 1静音 2 有声
            mMTGRewardVideoHandler.playVideoMute(mVideoMute == 1 ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE);
            if (canAgain) {
                // 再看一次，设置也不一定会下方对应素材的广告；监听和普通广告是同一个
                mMTGRewardVideoHandler.setRewardPlus(true);
            }
            mMTGRewardVideoHandler.load();
        } else {
            mMTGBidRewardVideoHandler = new MBBidRewardVideoHandler(context, mPlacementId, mUnitId);
            mMTGBidRewardVideoHandler.setRewardVideoListener(rewardVideoListener);
            mMTGBidRewardVideoHandler.playVideoMute(mVideoMute == 1 ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE);
            if (canAgain) {
                // 再看一次，设置也不一定会下方对应素材的广告；监听和普通广告是同一个
                mMTGBidRewardVideoHandler.setRewardPlus(true);
            }
            mMTGBidRewardVideoHandler.loadFromBid(payload);
        }
    }

    @Override
    public void showAd() {
        if (mShowListener != null) {
            mMTGICbR.addShowListener(mPlacementId + mUnitId, mShowListener);
        }

        if (TextUtils.isEmpty(payload)) {
            Log.i(TAG, "showInterstitial: " + mMTGRewardVideoHandler.isReady());
            if (mMTGRewardVideoHandler.isReady()) {
                useServer(userId, customData);
            } else {
                if (mMTGICbR.getShowListener(mPlacementId + mUnitId) != null)
                    mMTGICbR.getShowListener(mPlacementId + mUnitId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        } else {
            Log.i(TAG, "showInterstitial: " + mMTGBidRewardVideoHandler.isBidReady());
            if (mMTGBidRewardVideoHandler.isBidReady()) {
                useServer(userId, customData);
            } else {
                if (mMTGICbR.getShowListener(mPlacementId + mUnitId) != null)
                    mMTGICbR.getShowListener(mPlacementId + mUnitId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        }
    }

    private void useServer(String userId, String customData) {
        Log.i(TAG, "RewardData: userId : " + userId + ", customData :" + customData);
        if (mMTGRewardVideoHandler != null) {
            if (!TextUtils.isEmpty(userId)) {
                mMTGRewardVideoHandler.show(userId, TextUtils.isEmpty(customData) ? "" : customData);
            } else {
                mMTGRewardVideoHandler.show();
            }
        }

        if (mMTGBidRewardVideoHandler != null) {
            if (!TextUtils.isEmpty(userId)) {
                // userId在服务器回调中用到
                mMTGBidRewardVideoHandler.showFromBid(userId, TextUtils.isEmpty(customData) ? "" : customData);
            } else {
                mMTGBidRewardVideoHandler.showFromBid();
            }
        }
    }

    @Override
    public boolean isReady() {
        if (TextUtils.isEmpty(payload)) {
            if (mMTGRewardVideoHandler != null) {
                return !isAdsTimeOut() && mMTGRewardVideoHandler.isReady();
            } else {
                return false;
            }
        } else {
            if (mMTGBidRewardVideoHandler != null) {
                return !isAdsTimeOut() && mMTGBidRewardVideoHandler.isBidReady();
            } else {
                return false;
            }
        }
    }

    @Override
    public void clean() {
        super.clean();
        if (mPlacementId != null) {
            mMTGICbR.removeListeners(mPlacementId + mUnitId);
        }

        try {
            if (mMTGRewardVideoHandler != null) {
                mMTGRewardVideoHandler.setRewardVideoListener(null);
                mMTGRewardVideoHandler = null;
            }

            if (mMTGBidRewardVideoHandler != null) {
                mMTGBidRewardVideoHandler.setRewardVideoListener(null);
                mMTGBidRewardVideoHandler = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_MTG);
    }

    @Override
    public String getNetworkVersion() {
        return MBConfiguration.SDK_VERSION;
    }

    @Override
    public void getBiddingToken(final Context context, final Map<String, String> tpParams, final Map<String, Object> localParams, final OnS2STokenListener onS2STokenListener) {
        TPTaskManager.getInstance().runOnMainThread(new Runnable() {
            @Override
            public void run() {
                boolean initSuccess = true;
                String appKey = tpParams.get(AppKeyManager.APP_KEY);
                String appId = tpParams.get(AppKeyManager.APP_ID);

                if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey)) {
                    initSuccess = MintegralInitManager.isInited(appKey + appId);
                }

                final boolean finalInitSuccess = initSuccess;
                MintegralInitManager.getInstance().initSDK(context, localParams, tpParams, new TPInitMediation.InitCallback() {
                    @Override
                    public void onSuccess() {
                        String token = BidManager.getBuyerUid(context);
                        if (!finalInitSuccess) {
                            // 第一次初始化 250
                            MintegralInitManager.getInstance().sendInitRequest(true, INIT_STATE_BIDDING);
                        }

                        if (onS2STokenListener != null) {
                            onS2STokenListener.onTokenResult(token, null);
                        }
                    }

                    @Override
                    public void onFailed(String code, String msg) {
                        MintegralInitManager.getInstance().sendInitRequest(false, INIT_STATE_BIDDING);
                        if (onS2STokenListener != null) {
                            onS2STokenListener.onTokenResult("", null);
                        }
                    }
                });
            }
        });

    }
}
