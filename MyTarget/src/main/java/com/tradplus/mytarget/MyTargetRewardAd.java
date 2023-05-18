package com.tradplus.mytarget;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import com.my.target.ads.Reward;
import com.my.target.ads.RewardedAd;
import com.my.target.common.MyTargetManager;
import com.my.target.common.MyTargetVersion;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class MyTargetRewardAd extends TPRewardAdapter {
    public final static String TAG = "MyTargetRewardAd";
    private RewardedAd mRewardedAd;
    private String mSlotId;
    private MyTargetInterstitialCallbackRouter mMyTatgetICbR;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private String payload;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mSlotId = tpParams.get(AppKeyManager.AD_SLOT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        }
//        mSlotId = "854785";

        mMyTatgetICbR = MyTargetInterstitialCallbackRouter.getInstance();
        mMyTatgetICbR.addListener(mSlotId, mLoadAdapterListener);

        MyTargetInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitialVideo(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestInterstitialVideo(Context context) {
        mRewardedAd = new RewardedAd(Integer.parseInt(mSlotId), context);
        mRewardedAd.setListener(rewardedAdListener);
        if (TextUtils.isEmpty(payload)) {
            mRewardedAd.load();
        } else {
            mRewardedAd.loadFromBid(payload);
        }
    }


    RewardedAd.RewardedAdListener rewardedAdListener = new RewardedAd.RewardedAdListener() {
        @Override
        public void onLoad(RewardedAd rewardedAd) {
            Log.i(TAG, "onLoad: ");
            if (mMyTatgetICbR.getListener(mSlotId) != null) {
                setNetworkObjectAd(mRewardedAd);
                mMyTatgetICbR.getListener(mSlotId).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onNoAd(String s, RewardedAd rewardedAd) {
            Log.i(TAG, "onNoAd: " + s);
            if (mMyTatgetICbR.getListener(mSlotId) != null) {
                TPError tpError = new TPError(TPError.NETWORK_NO_FILL);
                tpError.setErrorMessage(s);
                mMyTatgetICbR.getListener(mSlotId).loadAdapterLoadFailed(tpError);
            }

        }

        @Override
        public void onClick(RewardedAd rewardedAd) {
            Log.i(TAG, "onClick: ");
            if (mMyTatgetICbR.getShowListener(mSlotId) != null) {
                mMyTatgetICbR.getShowListener(mSlotId).onAdVideoClicked();
            }
        }

        @Override
        public void onDismiss(RewardedAd rewardedAd) {
            if (mMyTatgetICbR.getShowListener(mSlotId) == null) {
                return;
            }
            Log.i(TAG, "onDismiss: ");
            mMyTatgetICbR.getShowListener(mSlotId).onAdVideoEnd();

            if (hasGrantedReward || alwaysRewardUser) {
                mMyTatgetICbR.getShowListener(mSlotId).onReward();
            }

            mMyTatgetICbR.getShowListener(mSlotId).onAdClosed();
        }

        @Override
        public void onReward(Reward reward, RewardedAd rewardedAd) {
            Log.i(TAG, "onReward: ");
            hasGrantedReward = true;
        }

        @Override
        public void onDisplay(RewardedAd rewardedAd) {
            Log.i(TAG, "onDisplay: ");
            if (mMyTatgetICbR.getShowListener(mSlotId) != null) {
                mMyTatgetICbR.getShowListener(mSlotId).onAdVideoStart();
                mMyTatgetICbR.getShowListener(mSlotId).onAdShown();
            }
        }
    };


    @Override
    public void showAd() {
        Log.i(TAG, "showAd: ");
        if (mShowListener != null)
            mMyTatgetICbR.addShowListener(mSlotId, mShowListener);


        if (mRewardedAd != null) {
            mRewardedAd.show();
        } else {
            if (mMyTatgetICbR.getShowListener(mSlotId) != null) {
                mMyTatgetICbR.getShowListener(mSlotId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        }
    }

    @Override
    public boolean isReady() {
        return !isAdsTimeOut();
    }

    @Override
    public void clean() {
        super.clean();
        if (mSlotId != null) {
            mMyTatgetICbR.removeListeners(mSlotId);
        }

        if (mRewardedAd != null) {
            mRewardedAd.setListener(null);
            mRewardedAd.destroy();
            mRewardedAd = null;
        }
    }


    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_MYTARGET);
    }

    @Override
    public String getNetworkVersion() {
        return MyTargetVersion.VERSION;
    }

    @Override
    public String getBiddingToken() {
        Context context = GlobalTradPlus.getInstance().getContext();
        return MyTargetManager.getBidderToken(context);
    }
}
