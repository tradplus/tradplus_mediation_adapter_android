package com.tradplus.ads.fpangolin;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardItem;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdLoadListener;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedRequest;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.HashMap;
import java.util.Map;

import static com.tradplus.ads.base.adapter.TPInitMediation.INIT_STATE_BIDDING;
import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.IF_FILL;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import org.json.JSONException;
import org.json.JSONObject;

public class ToutiaoRewardVideoAdapter extends TPRewardAdapter {

    private static final String TAG = "PangleRewardedVideo";
    private ToutiaoInterstitialCallbackRouter mCallbackRouter;
    private PAGRewardedAd mPAGRewardedAd;
    private String placementId, userId, customData;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;

    @Override
    public void loadCustomAd(final Context context, final Map<String, Object> localExtras, final Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        String payload;
        if (serverExtras != null && serverExtras.size() > 0) {
            placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = serverExtras.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(serverExtras.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(serverExtras.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
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
        }

        mCallbackRouter = ToutiaoInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(placementId, mLoadAdapterListener);


        final String ttAdm = payload;
        PangleInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitial(context, ttAdm);
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

    private void requestInterstitial(Context context, String payload) {
        PAGRewardedRequest pagRewardedRequest = new PAGRewardedRequest();

        if (!TextUtils.isEmpty(payload)) {
            pagRewardedRequest.setAdString(payload);
        }


        JSONObject jsonObject = null;
        if (!TextUtils.isEmpty(customData) || !TextUtils.isEmpty(userId)) {
            HashMap<String, Object> map = new HashMap();
            try {
                jsonObject = new JSONObject();

                if (!TextUtils.isEmpty(userId)) {
                    jsonObject.put("user_id", userId);
                }

                if (!TextUtils.isEmpty(customData)) {
                    jsonObject.put("custom_data", customData);
                    //The key is the fixed value media_extra,value is a string
                }
                map.put("media_extra", jsonObject.toString());
                Log.i(TAG, "RewardData: userId : " + userId + ", customData :" + customData);
                pagRewardedRequest.setExtraInfo(map);
            } catch (Throwable e) {
                e.printStackTrace();
            }

        }

        PAGRewardedAd.loadAd(placementId, pagRewardedRequest, new PAGRewardedAdLoadListener() {
            @Override
            public void onError(int code, String message) {
                Log.i(TAG, "onError: code ：" + code + ", message ：" + message);
                if (mCallbackRouter.getListener(placementId) != null) {
                    mCallbackRouter.getListener(placementId).loadAdapterLoadFailed(PangleErrorUtil.getTradPlusErrorCode(code, message));
                }
            }

            @Override
            public void onAdLoaded(PAGRewardedAd pagRewardedAd) {
                if (pagRewardedAd == null) {
                    if (mCallbackRouter.getListener(placementId) != null) {
                        mCallbackRouter.getListener(placementId).loadAdapterLoadFailed(new TPError(UNSPECIFIED));
                    }
                    Log.i(TAG, "onAdLoaded ,but pagRewardedAd == null");
                    return;
                }
                mPAGRewardedAd = pagRewardedAd;

                if (mCallbackRouter.getListener(placementId) != null) {
                    setNetworkObjectAd(pagRewardedAd);
                    mCallbackRouter.getListener(placementId).loadAdapterLoaded(null);
                }
            }
        });
    }

    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(placementId, mShowListener);
        }

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoError(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            Log.i(TAG, "showAd, activity == null");
            return;
        }

        if (mPAGRewardedAd == null) {
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoError(new TPError(UNSPECIFIED));
            }
            Log.i(TAG, "showAd, PAGRewardedAd == null");
            return;
        }

        mPAGRewardedAd.setAdInteractionListener(new PAGRewardedAdInteractionListener() {
            @Override
            public void onUserEarnedReward(PAGRewardItem pagRewardItem) {
                Log.i(TAG, "onUserEarnedReward: ");
                hasGrantedReward = true;
            }

            @Override
            public void onUserEarnedRewardFail(int i, String s) {
                Log.i(TAG, "onUserEarnedRewardFail: code :" + i + ",msg :" + s);
            }

            @Override
            public void onAdShowed() {
                Log.i(TAG, "onAdShowed: ");
                if (mCallbackRouter.getShowListener(placementId) != null) {
                    mCallbackRouter.getShowListener(placementId).onAdVideoStart();
                    mCallbackRouter.getShowListener(placementId).onAdShown();
                }
            }

            @Override
            public void onAdClicked() {
                Log.i(TAG, "onAdClicked: ");
                if (mCallbackRouter.getShowListener(placementId) != null) {
                    mCallbackRouter.getShowListener(placementId).onAdClicked();
                }
            }

            @Override
            public void onAdDismissed() {
                Log.i(TAG, "onAdDismissed: ");
                if (mCallbackRouter.getShowListener(placementId) != null) {
                    mCallbackRouter.getShowListener(placementId).onAdVideoEnd();

                    if (hasGrantedReward || alwaysRewardUser) {
                        mCallbackRouter.getShowListener(placementId).onReward();
                    }
                    mCallbackRouter.getShowListener(placementId).onAdClosed();
                }
            }
        });

        mPAGRewardedAd.show(activity);

    }


    @Override
    public boolean isReady() {
        return !isAdsTimeOut();
    }

    @Override
    public void clean() {
        super.clean();
        if (mPAGRewardedAd != null) {
            mPAGRewardedAd.setAdInteractionListener(null);
            mPAGRewardedAd = null;
        }

        if (placementId != null) {
            mCallbackRouter.removeListeners(placementId);
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_PANGLE);
    }

    @Override
    public String getNetworkVersion() {
        return PAGSdk.getSDKVersion();
    }

    @Override
    public void getBiddingToken(Context context, Map<String, String> tpParams, Map<String, Object> localParams, final OnS2STokenListener onS2STokenListener) {
        final boolean initSuccess = PAGSdk.isInitSuccess();
        PangleInitManager.getInstance().initSDK(context, localParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: " + initSuccess);
                if (!initSuccess) {
                    PangleInitManager.getInstance().sendInitRequest(true, INIT_STATE_BIDDING);
                }
                onS2STokenListener.onTokenResult(PAGSdk.getBiddingToken(), null);

            }

            @Override
            public void onFailed(String code, String msg) {
                PangleInitManager.getInstance().sendInitRequest(false, INIT_STATE_BIDDING);
                onS2STokenListener.onTokenResult("", null);
            }
        });
    }


}