package com.tradplus.ads.verve;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import net.pubnative.lite.sdk.HyBid;
import net.pubnative.lite.sdk.rewarded.HyBidRewardedAd;

import java.util.Map;

public class VerveInterstitialVideo extends TPRewardAdapter {

    private String mPlacementId;
    private HyBidRewardedAd mRewarded;
    private InterstitialCallbackRouter mCallbackRouter;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    private static final String TAG = "VerveRewardedVideo";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }


        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
        }

        mCallbackRouter = InterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);

        VerveInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestAd();
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }

                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(code + "", msg);
                }
            }
        });
    }

    private void requestAd() {
        if (isC2SBidding && isBiddingLoaded) {
            setFirstLoadedTime();
            if (mCallbackRouter != null && mCallbackRouter.getListener(mPlacementId) != null) {
                setNetworkObjectAd(mRewarded);
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
            }
            Log.i(TAG, "isC2SBidding: ");
        } else {
            Activity activity = GlobalTradPlus.getInstance().getActivity();
            if (activity == null) {
                if (mCallbackRouter.getListener(mPlacementId) != null) {
                    mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
                }
                return;
            }
            mRewarded = new HyBidRewardedAd(activity, mPlacementId, mListener);
            mRewarded.load();
        }
    }

    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);
        }

        if (mRewarded == null) {
            TPError tpError = new TPError(UNSPECIFIED);
            tpError.setErrorMessage("showfailed，mRewardedVideoAd == null");
            mShowListener.onAdVideoError(tpError);
            return;
        }

        mRewarded.show();

    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }

    @Override
    public boolean isReady() {
        return mRewarded != null && mRewarded.isReady() && !isAdsTimeOut();
    }

    private final HyBidRewardedAd.Listener mListener = new HyBidRewardedAd.Listener() {
        @Override
        public void onRewardedLoaded() {
            if (isC2SBidding) {
                Log.i(TAG, "onRewardedLoaded isC2SBidding BidPoints: " + mRewarded.getBidPoints());
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingResult(mRewarded.getBidPoints());
                }
                isBiddingLoaded = true;
                return;
            }
            setFirstLoadedTime();

            if (mCallbackRouter.getListener(mPlacementId) != null) {
                setNetworkObjectAd(mRewarded);
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onRewardedLoadFailed(Throwable error) {
            Log.i(TAG, "onRewardedLoadFailed: msg:" + error.getMessage());

            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",error.getMessage());
                }
                return;
            }

            if (mCallbackRouter.getListener(mPlacementId) != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(error.getMessage());
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onRewardedOpened() {
            Log.i(TAG, "onRewardedOpened: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoStart();
                mCallbackRouter.getShowListener(mPlacementId).onAdShown();
            }
        }

        @Override
        public void onRewardedClosed() {
            if (mCallbackRouter.getShowListener(mPlacementId) == null) {
                return;
            }
            mCallbackRouter.getShowListener(mPlacementId).onAdVideoEnd();
            Log.i(TAG, "onRewardedClosed: ");
            if (hasGrantedReward || alwaysRewardUser) {
                mCallbackRouter.getShowListener(mPlacementId).onReward();
            }
            mCallbackRouter.getShowListener(mPlacementId).onAdClosed();

        }

        @Override
        public void onRewardedClick() {
            Log.i(TAG, "onRewardedClick: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoClicked();
            }
        }

        @Override
        public void onReward() {
            Log.i(TAG, "onReward: ");
            hasGrantedReward = true;
        }
    };

    @Override
    public void clean() {
        if (mRewarded != null) {
            mRewarded.destroy();
            mRewarded = null;
        }

        if (mPlacementId != null) {
            mCallbackRouter.removeListeners(mPlacementId);
        }
    }

    @Override
    public String getNetworkName() {
        return "Verve";
    }

    @Override
    public String getNetworkVersion() {
        return HyBid.getSDKVersionInfo();
    }
}
