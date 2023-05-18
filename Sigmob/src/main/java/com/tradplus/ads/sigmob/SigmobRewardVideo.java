package com.tradplus.ads.sigmob;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.sigmob.windad.WindAdError;
import com.sigmob.windad.WindAdOptions;
import com.sigmob.windad.WindAds;
import com.sigmob.windad.rewardVideo.WindRewardAdRequest;
import com.sigmob.windad.rewardVideo.WindRewardInfo;
import com.sigmob.windad.rewardVideo.WindRewardVideoAd;
import com.sigmob.windad.rewardVideo.WindRewardVideoAdListener;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.HashMap;
import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

public class SigmobRewardVideo extends TPRewardAdapter {

    private String mPlacementId;
    private String userId;
    private static final String TAG = "SigmobRewardVideo";
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private SigmobInterstitialCallbackRouter mSigmobICbR;
    private WindRewardVideoAd windRewardVideoAd;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        String bidToken;
        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            bidToken = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            userId = (String) userParams.get(AppKeyManager.CUSTOM_USERID);

            if (TextUtils.isEmpty(userId)) {
                userId = "";
            }
        }

        mSigmobICbR = SigmobInterstitialCallbackRouter.getInstance();
        mSigmobICbR.addListener(mPlacementId, mLoadAdapterListener);

        final String token = bidToken;
        SigmobInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitial(token);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestInterstitial(String bidToken) {
        WindRewardAdRequest request = new WindRewardAdRequest(mPlacementId, !TextUtils.isEmpty(userId) ? userId : null, null);
        Log.i(TAG, "RewardData: userId : " + userId);

        windRewardVideoAd = new WindRewardVideoAd(request);
        windRewardVideoAd.setWindRewardVideoAdListener(mWindRewardVideoAdListener);

        if (TextUtils.isEmpty(bidToken)) {
            windRewardVideoAd.loadAd();
        } else {
            windRewardVideoAd.loadAd(bidToken);
        }
    }

    @Override
    public void showAd() {
        if (mShowListener != null) {
            mSigmobICbR.addShowListener(mPlacementId, mShowListener);
        }

        if (windRewardVideoAd == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(UNSPECIFIED));
            }
            return;
        }

        if (windRewardVideoAd.isReady()) {
            windRewardVideoAd.show(new HashMap<String, String>());
        }else {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
            }
        }
    }

    @Override
    public boolean isReady() {
        Log.i(TAG, "isReady: ");
        if (windRewardVideoAd != null) {
            return !isAdsTimeOut() && windRewardVideoAd.isReady();
        } else {
            return false;
        }
    }

    private final WindRewardVideoAdListener mWindRewardVideoAdListener = new WindRewardVideoAdListener() {
        @Override
        public void onRewardAdLoadSuccess(String pid) {
            Log.i(TAG, "onRewardAdLoadSuccess: ");
            setFirstLoadedTime();
            if (mSigmobICbR.getListener(pid) != null) {
                setNetworkObjectAd(windRewardVideoAd);
                mSigmobICbR.getListener(pid).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onRewardAdPreLoadSuccess(String s) {

        }

        @Override
        public void onRewardAdPreLoadFail(String s) {

        }

        @Override
        public void onRewardAdPlayStart(String pid) {
            Log.i(TAG, "onRewardAdPlayStart: ");
            if (mSigmobICbR.getShowListener(pid) != null) {
                mSigmobICbR.getShowListener(pid).onAdVideoStart();
                mSigmobICbR.getShowListener(pid).onAdShown();
            }
        }

        @Override
        public void onRewardAdPlayEnd(String pid) {
            Log.i(TAG, "onRewardAdPlayEnd: ");
            if (mSigmobICbR.getShowListener(pid) != null) {
                mSigmobICbR.getShowListener(pid).onAdVideoEnd();
            }
        }

        @Override
        public void onRewardAdClicked(String pid) {
            Log.i(TAG, "onRewardAdClicked: ");
            if (mSigmobICbR.getShowListener(pid) != null) {
                mSigmobICbR.getShowListener(pid).onAdVideoClicked();
            }
        }

        @Override
        public void onRewardAdClosed(String pid) {
            if (mSigmobICbR.getShowListener(pid) == null) {
                return;
            }

            if (hasGrantedReward || alwaysRewardUser) {
                mSigmobICbR.getShowListener(pid).onReward();
            }

            Log.i(TAG, "onRewardAdClosed: ");
            mSigmobICbR.getShowListener(pid).onAdClosed();

        }

        @Override
        public void onRewardAdRewarded(WindRewardInfo windRewardInfo, String pid) {
            Log.i(TAG, "onRewardAdRewarded: ");
            if (windRewardInfo != null) {
                hasGrantedReward = windRewardInfo.isReward();
            }
        }

        @Override
        public void onRewardAdLoadError(WindAdError windAdError, String pid) {
            Log.i(TAG, "onRewardAdLoadError: ErrorCode :" + windAdError.getErrorCode() + ", ErrorMessage : " + windAdError.getMessage());
            if (mSigmobICbR.getListener(pid) != null) {
                mSigmobICbR.getListener(pid).loadAdapterLoadFailed(SimgobErrorUtil.getTradPlusErrorCode(windAdError));
            }
        }

        @Override
        public void onRewardAdPlayError(WindAdError windAdError, String pid) {
            Log.i(TAG, "onRewardAdPlayError: :" + windAdError.getErrorCode() + ", ErrorMessage : " + windAdError.getMessage());
            if (mSigmobICbR.getShowListener(pid) != null) {
                mSigmobICbR.getShowListener(pid).onAdVideoError(SimgobErrorUtil.getTradPlusErrorCode(windAdError));
            }
        }
    };


    @Override
    public void clean() {
        super.clean();
        if (mPlacementId != null)
            mSigmobICbR.removeListeners(mPlacementId);
    }

    @Override
    public String getBiddingToken(Context context, Map<String, String> tpParams) {
        if (tpParams == null) {
            return "";
        }

        String appId = tpParams.get(AppKeyManager.APP_ID);
        String appkey = tpParams.get(AppKeyManager.APP_KEY);
        WindAds windAds = WindAds.sharedAds();

        if (!SigmobInitManager.isInited(appId)) {
            windAds.startWithOptions(context, new WindAdOptions(appId, appkey));
        }

        return windAds.getSDKToken();
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_SIGMOB);
    }

    @Override
    public String getNetworkVersion() {
        return WindAds.getVersion();
    }
}
