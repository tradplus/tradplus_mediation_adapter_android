package com.tradplus.ads.bigo;

import static com.tradplus.ads.base.adapter.TPInitMediation.INIT_STATE_BIDDING;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.annotation.NonNull;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.Map;

import sg.bigo.ads.BigoAdSdk;
import sg.bigo.ads.api.AdError;
import sg.bigo.ads.api.AdLoadListener;
import sg.bigo.ads.api.RewardAdInteractionListener;
import sg.bigo.ads.api.RewardVideoAd;
import sg.bigo.ads.api.RewardVideoAdLoader;
import sg.bigo.ads.api.RewardVideoAdRequest;

public class BigoInterstitialVideo extends TPRewardAdapter {

    private static final String TAG = "BigoRewardVideo";
    private String mPlacementId;
    private String serverBiddingAdm;
    private RewardVideoAd mRewardVideoAd;
    private InterstitialCallbackRouter mCallbackRouter;
    private boolean hasGrantedReward = false;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);

            String payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(payload)) {
                serverBiddingAdm = payload;
            }
        }

        mCallbackRouter = InterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);
        mLoadAdapterListener = mCallbackRouter.getListener(mPlacementId);

        BigoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                request();
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

    private void request() {
        RewardVideoAdRequest.Builder builder = new RewardVideoAdRequest.Builder().withSlotId(mPlacementId);

        RewardVideoAdLoader rewardVideoAdAdLoader = new RewardVideoAdLoader.Builder().
                withAdLoadListener(new AdLoadListener<RewardVideoAd>() {
                    @Override
                    public void onError(@NonNull AdError error) {
                        TPError tpError = new TPError(NETWORK_NO_FILL);
                        if (error != null) {
                            int code = error.getCode();
                            String message = error.getMessage();
                            tpError.setErrorMessage(message);
                            tpError.setErrorCode(code+"");
                            Log.i(TAG, "code :" + code + ", message :" + message);
                        }

                        if (mLoadAdapterListener != null)
                            mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardVideoAd ad) {
                        mRewardVideoAd = ad;
                        ad.setAdInteractionListener(adInteractionListener);
                        if (mLoadAdapterListener != null) {
                            setNetworkObjectAd(ad);
                            mLoadAdapterListener.loadAdapterLoaded(null);
                        }
                    }
                }).build();

        if (!TextUtils.isEmpty(serverBiddingAdm)) {
            builder.withBid(serverBiddingAdm);
        }

        rewardVideoAdAdLoader.loadAd(builder.build());
    }

    private final RewardAdInteractionListener adInteractionListener = new RewardAdInteractionListener() {
        @Override
        public void onAdRewarded() {
            // 表示激励视频已播放完成，可下发奖励。
            Log.i(TAG, "onAdRewarded: ");
            hasGrantedReward = true;
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoEnd();
            }
        }

        @Override
        public void onAdError(@NonNull AdError error) {
            TPError tpError = new TPError(SHOW_FAILED);
            if (error != null) {
                int code = error.getCode();
                String message = error.getMessage();
                tpError.setErrorMessage(message);
                tpError.setErrorCode(code+"");
                Log.i(TAG, "code :" + code + ", message :" + message);
            }

            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(tpError);
            }
        }

        @Override
        public void onAdImpression() {
            // When the ad appears on the screen.
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdShown();
            }
        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdClicked();
            }
        }

        @Override
        public void onAdOpened() {
            Log.i(TAG, "onAdOpened: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoStart();
            }
        }

        @Override
        public void onAdClosed() {
            Log.i(TAG, "onAdClosed: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                if (hasGrantedReward) {
                    mCallbackRouter.getShowListener(mPlacementId).onReward();
                }
                mCallbackRouter.getShowListener(mPlacementId).onAdClosed();
            }
        }
    };

    @Override
    public boolean isReady() {
        return mRewardVideoAd != null && !mRewardVideoAd.isExpired();
    }


    @Override
    public void showAd() {
        if (mShowListener == null) return;
        mCallbackRouter.addShowListener(mPlacementId, mShowListener);

        TPError tpErrorShow = new TPError(SHOW_FAILED);
        if (mRewardVideoAd == null) {
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                tpErrorShow.setErrorMessage("showFailed: InterstitialAd == null");
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(tpErrorShow);
            }
            return;
        }

        if (mRewardVideoAd.isExpired()) {
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                tpErrorShow.setErrorMessage("Ad is Expired");
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(tpErrorShow);
            }
            return;
        }

        mRewardVideoAd.show();
    }

    @Override
    public void clean() {
        if (mRewardVideoAd != null) {
            mRewardVideoAd.setAdInteractionListener(null);
            mRewardVideoAd.destroy();
            mRewardVideoAd = null;
        }

        if (mPlacementId != null) {
            mCallbackRouter.removeListeners(mPlacementId);
        }
    }

    @Override
    public String getNetworkName() {
        return "Bigo";
    }

    @Override
    public String getNetworkVersion() {
        return BigoAdSdk.getSDKVersionName();
    }

    @Override
    public void getBiddingToken(Context context, Map<String, String> tpParams, Map<String, Object> localParams, final OnS2STokenListener onS2STokenListener) {
        boolean initialized = BigoAdSdk.isInitialized();
        BigoInitManager.getInstance().initSDK(context, localParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                String bidderToken = BigoAdSdk.getBidderToken();
                boolean tokenEmpty = TextUtils.isEmpty(bidderToken);
                Log.i(TAG, "onSuccess bidderToken isEmpty " + tokenEmpty);
                if (!initialized) {
                    // 第一次初始化打印250
                    BigoInitManager.getInstance().sendInitRequest(true, INIT_STATE_BIDDING);
                }
                onS2STokenListener.onTokenResult(!tokenEmpty ? bidderToken : "", null);
            }

            @Override
            public void onFailed(String code, String msg) {
                BigoInitManager.getInstance().sendInitRequest(false, INIT_STATE_BIDDING);
                onS2STokenListener.onTokenResult("", null);
            }
        });
    }
}
