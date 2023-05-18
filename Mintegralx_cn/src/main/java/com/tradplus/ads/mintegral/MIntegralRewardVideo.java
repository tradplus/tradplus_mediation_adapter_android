package com.tradplus.ads.mintegral;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.MBridgeSDK;
import com.mbridge.msdk.mbbid.out.BidManager;
import com.mbridge.msdk.out.MBBidRewardVideoHandler;
import com.mbridge.msdk.out.MBConfiguration;
import com.mbridge.msdk.out.MBRewardVideoHandler;
import com.mbridge.msdk.out.MBridgeIds;
import com.mbridge.msdk.out.MBridgeSDKFactory;
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

import static com.tradplus.ads.base.adapter.TPInitMediation.INIT_STATE_BIDDING;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class MIntegralRewardVideo extends TPRewardAdapter {
    private final static String TAG = "MTGCNRewardVideo";
    private MBRewardVideoHandler mMBRewardVideoHandler;
    private MBBidRewardVideoHandler mMTGBidRewardVideoHandler;
    private String mPlacementId, userId, customData;
    private MIntegralInterstitialCallbackRouter mCallbackRouter;
    private String mUnitId;
    private String payload;
    private Integer mVideoMute = 1;
    private boolean canAgain = false;
    private boolean alwaysReward;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (serverExtras != null && serverExtras.size() > 0) {
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            mUnitId = serverExtras.get(AppKeyManager.UNIT_ID);
            payload = serverExtras.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(serverExtras.get(AppKeyManager.ALWAYS_REWARD))) {
                alwaysReward = (Integer.parseInt(serverExtras.get(AppKeyManager.ALWAYS_REWARD)) == AppKeyManager.ENFORCE_REWARD);
            }
            if (!TextUtils.isEmpty(serverExtras.get(AppKeyManager.VIDEO_MUTE))) {
                mVideoMute = Integer.parseInt(serverExtras.get(AppKeyManager.VIDEO_MUTE));
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(AppKeyManager.CUSTOM_USERID)) {
                userId = (String) localExtras.get(AppKeyManager.CUSTOM_USERID);
                if (TextUtils.isEmpty(userId)) {
                    userId = "";
                }
            }


            if (localExtras.containsKey(AppKeyManager.CUSTOM_DATA)) {
                customData = (String) localExtras.get(AppKeyManager.CUSTOM_DATA);

                if (TextUtils.isEmpty(customData)) {
                    customData = "";
                }
            }

            if (localExtras.containsKey(MTGConstant.VIDEO_MUTE)) {
                mVideoMute = (int) localExtras.get(MTGConstant.VIDEO_MUTE);
            }

            if (localExtras.containsKey(MTGConstant.AD_REWARD_AGAIN)) {
                canAgain = true;
            }
        }


        mCallbackRouter = MIntegralInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId + mUnitId, mLoadAdapterListener);

        MintegralInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
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
        if (TextUtils.isEmpty(payload)) {
            mMBRewardVideoHandler = new MBRewardVideoHandler(context, mPlacementId, mUnitId);
            mMBRewardVideoHandler.setRewardVideoListener(rewardVideoListener);
            mMBRewardVideoHandler.playVideoMute(mVideoMute == 1 ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE);
            if (canAgain) {
                mMBRewardVideoHandler.setRewardPlus(true);
            }
            mMBRewardVideoHandler.load();

        } else {
            mMTGBidRewardVideoHandler = new MBBidRewardVideoHandler(context, mPlacementId, mUnitId);
            mMTGBidRewardVideoHandler.setRewardVideoListener(rewardVideoListener);
            mMTGBidRewardVideoHandler.playVideoMute(mVideoMute == 1 ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE);
            if (canAgain) {
                mMTGBidRewardVideoHandler.setRewardPlus(true);
            }
            mMTGBidRewardVideoHandler.loadFromBid(payload);
        }
    }

    RewardVideoListener rewardVideoListener = new RewardVideoListener() {
        @Override
        public void onVideoLoadSuccess(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onVideoLoadSuccess: ");
            if (mCallbackRouter.getListener(mPlacementId + mUnitId) != null) {
                setFirstLoadedTime();
                setNetworkObjectAd(mMBRewardVideoHandler != null ? mMBRewardVideoHandler : mMTGBidRewardVideoHandler);
                mCallbackRouter.getListener(mPlacementId + mUnitId).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onLoadSuccess(MBridgeIds mBridgeIds) {
        }

        @Override
        public void onVideoLoadFail(MBridgeIds mBridgeIds, String errorMsg) {
            Log.e(TAG, "onVideoLoadFail errorMsg:" + errorMsg);
            if (mCallbackRouter.getListener(mPlacementId + mUnitId) != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(errorMsg);
                mCallbackRouter.getListener(mPlacementId + mUnitId).loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onAdShow(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onAdShow");
            if (mCallbackRouter.getShowListener(mPlacementId + mUnitId) != null) {
                mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdVideoStart();
                mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdShown();
            }
        }

        @Override
        public void onAdClose(MBridgeIds mBridgeIds, RewardInfo rewardInfo) {
            if (mCallbackRouter.getShowListener(mPlacementId + mUnitId) == null) return;

            if (rewardInfo.isCompleteView() || alwaysReward) {
                Log.i(TAG, "isCompleteView: ");
                mCallbackRouter.getShowListener(mPlacementId + mUnitId).onReward();
            }

            Log.i(TAG, "onAdClose: ");
            mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdClosed();
        }

        @Override
        public void onShowFail(MBridgeIds mBridgeIds, String errorMsg) {
            Log.e(TAG, "onShowFail=" + errorMsg);
            if (mCallbackRouter.getShowListener(mPlacementId + mUnitId) != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorMessage(errorMsg);
                mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdVideoError(tpError);
            }
        }

        @Override
        public void onVideoAdClicked(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onVideoAdClicked");
            if (mCallbackRouter.getShowListener(mPlacementId + mUnitId) != null) {
                mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdVideoClicked();
            }

        }

        @Override
        public void onVideoComplete(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onVideoComplete");
            if (mCallbackRouter.getShowListener(mPlacementId + mUnitId) != null) {
                mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdVideoEnd();
            }
        }

        @Override
        public void onEndcardShow(MBridgeIds mBridgeIds) {

        }
    };

    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mPlacementId + mUnitId, mShowListener);
        }

        if (TextUtils.isEmpty(payload)) {
            Log.i(TAG, "showInterstitial: " + mMBRewardVideoHandler.isReady());
            if (mMBRewardVideoHandler.isReady()) {
                useServer(userId, customData);
            } else {
                if (mCallbackRouter.getShowListener(mPlacementId + mUnitId) != null)
                    mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        } else {
            Log.i(TAG, "showInterstitial Bid: " + mMTGBidRewardVideoHandler.isBidReady());
            if (mMTGBidRewardVideoHandler.isBidReady()) {
                useServer(userId, customData);
            } else {
                if (mCallbackRouter.getShowListener(mPlacementId + mUnitId) != null)
                    mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        }
    }

    private void useServer(String userId, String customData) {
        Log.i(TAG, "RewardData: userId : " + userId + ", customData :" + customData);
        if (mMBRewardVideoHandler != null) {
            if (!TextUtils.isEmpty(userId)) {
                mMBRewardVideoHandler.show(userId, TextUtils.isEmpty(customData) ? "" : customData);
            } else {
                mMBRewardVideoHandler.show();
            }
        }

        if (mMTGBidRewardVideoHandler != null) {
            if (!TextUtils.isEmpty(userId)) {
                mMTGBidRewardVideoHandler.showFromBid(userId, TextUtils.isEmpty(customData) ? "" : customData);
            } else {
                mMTGBidRewardVideoHandler.showFromBid();
            }
        }
    }

    @Override
    public void clean() {
        super.clean();

        if (mCallbackRouter.getListener(mPlacementId + mUnitId) != null) {
            mCallbackRouter.removeListeners(mPlacementId + mUnitId);
        }

        if (mMBRewardVideoHandler != null) {
            mMBRewardVideoHandler.setRewardVideoListener(null);
            mMBRewardVideoHandler = null;
        }

        if (mMTGBidRewardVideoHandler != null) {
            mMTGBidRewardVideoHandler.setRewardVideoListener(null);
            mMTGBidRewardVideoHandler = null;
        }


    }

    @Override
    public boolean isReady() {
        Log.i(TAG, "isReady: ");
        if (TextUtils.isEmpty(payload)) {
            if (mMBRewardVideoHandler != null) {
                return !isAdsTimeOut() && mMBRewardVideoHandler.isReady();
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
