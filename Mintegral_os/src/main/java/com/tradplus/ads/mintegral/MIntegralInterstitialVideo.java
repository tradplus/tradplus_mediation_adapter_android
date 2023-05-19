package com.tradplus.ads.mintegral;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.newinterstitial.out.MBBidInterstitialVideoHandler;
import com.mbridge.msdk.mbbid.out.BidManager;
import com.mbridge.msdk.newinterstitial.out.MBNewInterstitialHandler;
import com.mbridge.msdk.newinterstitial.out.NewInterstitialListener;
import com.mbridge.msdk.out.MBConfiguration;
import com.mbridge.msdk.out.MBridgeIds;
import com.mbridge.msdk.out.RewardInfo;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
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


public class MIntegralInterstitialVideo extends TPInterstitialAdapter {

    private MBNewInterstitialHandler mbNewInterstitialHandler;
    private MBBidInterstitialVideoHandler mMBBidInterstitialVideoHandler;
    private String mPlacementId;
    private String mUnitId;
    private MIntegralInterstitialCallbackRouter mCallbackRouter;
    private String payload;
    private Integer mVideoMute = 1;
    private final static String TAG = "MTGOSInVideo";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            mUnitId = tpParams.get(AppKeyManager.UNIT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.VIDEO_MUTE))) {
                mVideoMute = Integer.parseInt(tpParams.get(AppKeyManager.VIDEO_MUTE));
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;

        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(MTGConstant.VIDEO_MUTE)) {
                mVideoMute = (int) userParams.get(MTGConstant.VIDEO_MUTE);
            }
        }

        mCallbackRouter = MIntegralInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId + mUnitId, mLoadAdapterListener);

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
        if (TextUtils.isEmpty(payload)) {
            loadNewInerstitial(context);
        } else {
            loadNewBidInterstitial(context);
        }

    }

    private void loadNewBidInterstitial(Context context) {
        mMBBidInterstitialVideoHandler = new MBBidInterstitialVideoHandler(context, mPlacementId, mUnitId);
        mMBBidInterstitialVideoHandler.setInterstitialVideoListener(mNewInterstitialListener);
        mMBBidInterstitialVideoHandler.playVideoMute(mVideoMute == 1 ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE);
        mMBBidInterstitialVideoHandler.loadFromBid(payload);
    }

    private void loadNewInerstitial(Context context) {
        mbNewInterstitialHandler = new MBNewInterstitialHandler(context, mPlacementId, mUnitId);
        mbNewInterstitialHandler.setInterstitialVideoListener(mNewInterstitialListener);
        mbNewInterstitialHandler.playVideoMute(mVideoMute == 1 ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE);
        mbNewInterstitialHandler.load();
    }

    private final NewInterstitialListener mNewInterstitialListener = new NewInterstitialListener() {
        @Override
        public void onLoadCampaignSuccess(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onLoadCampaignSuccess: ");
        }

        @Override
        public void onResourceLoadSuccess(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onResourceLoadSuccess: ");
            if (mCallbackRouter.getListener(mPlacementId + mUnitId) != null) {
                setFirstLoadedTime();
                setNetworkObjectAd(mMBBidInterstitialVideoHandler != null ? mMBBidInterstitialVideoHandler : mbNewInterstitialHandler);
                mCallbackRouter.getListener(mPlacementId + mUnitId).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onResourceLoadFail(MBridgeIds mBridgeIds, String errorMsg) {
            Log.i(TAG, "onResourceLoadFail: errorMsg :" + errorMsg);
            if (mCallbackRouter.getListener(mPlacementId + mUnitId) != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(errorMsg);
                mCallbackRouter.getListener(mPlacementId + mUnitId).loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onAdShow(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onAdShow: ");
            if (mCallbackRouter.getShowListener(mPlacementId + mUnitId) != null) {
                mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdShown();
            }
        }

        @Override
        public void onAdClose(MBridgeIds mBridgeIds, RewardInfo rewardInfo) {
            Log.i(TAG, "onAdClose:");
            if (mCallbackRouter.getShowListener(mPlacementId + mUnitId) != null) {
                mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdClosed();
            }
        }

        @Override
        public void onShowFail(MBridgeIds mBridgeIds, String errorMsg) {
            Log.i(TAG, "onShowFail: errorMsg :" + errorMsg);
            if (mCallbackRouter.getShowListener(mPlacementId + mUnitId) != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorMessage(errorMsg);
                mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdVideoError(tpError);
            }
        }

        @Override
        public void onAdClicked(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onAdClicked: ");
            if (mCallbackRouter.getShowListener(mPlacementId + mUnitId) != null) {
                mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdVideoClicked();
            }
        }

        @Override
        public void onVideoComplete(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onVideoComplete: ");
            if (mCallbackRouter.getShowListener(mPlacementId + mUnitId) != null) {
                mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdVideoEnd();
            }
        }

        @Override
        public void onAdCloseWithNIReward(MBridgeIds mBridgeIds, RewardInfo rewardInfo) {
            Log.i(TAG, "onAdCloseWithNIReward: ");
        }

        @Override
        public void onEndcardShow(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onEndcardShow: ");
        }
    };


    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mPlacementId + mUnitId, mShowListener);
        }

        if (TextUtils.isEmpty(payload)) {
            if (mbNewInterstitialHandler != null) {
                mbNewInterstitialHandler.show();
            } else {
                if (mCallbackRouter.getShowListener(mPlacementId + mUnitId) != null) {
                    mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdVideoError(new TPError(SHOW_FAILED));
                }
            }
        } else {
            if (mMBBidInterstitialVideoHandler != null) {
                mMBBidInterstitialVideoHandler.showFromBid();
            } else {
                if (mCallbackRouter.getShowListener(mPlacementId + mUnitId) != null) {
                    mCallbackRouter.getShowListener(mPlacementId + mUnitId).onAdVideoError(new TPError(SHOW_FAILED));
                }
            }
        }
    }


    @Override
    public boolean isReady() {
        if (mbNewInterstitialHandler != null) {
            return mbNewInterstitialHandler.isReady() && !isAdsTimeOut();
        } else if (mMBBidInterstitialVideoHandler != null) {
            return mMBBidInterstitialVideoHandler.isBidReady() && !isAdsTimeOut();
        }
        return false;
    }

    @Override
    public void clean() {
        super.clean();

        if (mPlacementId != null) {
            mCallbackRouter.removeListeners(mPlacementId + mUnitId);
        }

        if (mPlacementId != null) {
            mCallbackRouter.removeListeners(mPlacementId + mUnitId);
        }

        if (mbNewInterstitialHandler != null) {
            mbNewInterstitialHandler.setInterstitialVideoListener(null);
            mbNewInterstitialHandler = null;
        }

        if (mMBBidInterstitialVideoHandler != null) {
            mMBBidInterstitialVideoHandler.setInterstitialVideoListener(null);
            mMBBidInterstitialVideoHandler = null;
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
