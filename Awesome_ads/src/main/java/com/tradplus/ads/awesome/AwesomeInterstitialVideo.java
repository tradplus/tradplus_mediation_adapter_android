package com.tradplus.ads.awesome;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import tv.superawesome.sdk.publisher.SAInterface;
import tv.superawesome.sdk.publisher.SAVersion;
import tv.superawesome.sdk.publisher.SAVideoAd;

public class AwesomeInterstitialVideo extends TPRewardAdapter {

    private String placementId;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private Integer mVideoMute = 1; // 静音
    private AwesomeInterstitialCallbackRouter mCallbackRouter;
    private static final String TAG = "AwesomeInterstitialVideo";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) return;

        if (extrasAreValid(tpParams)) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }

            // 视频静音 指定自动播放时是否静音: 1 自动播放时静音；2 自动播放时有声
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.VIDEO_MUTE))) {
                mVideoMute = Integer.parseInt(tpParams.get(AppKeyManager.VIDEO_MUTE));
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }
//        placementId = "30479";
        mCallbackRouter = AwesomeInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(placementId, mLoadAdapterListener);


        AwesomeInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitialVideo(context);
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

    private void requestInterstitialVideo(Context context) {
        if (mVideoMute == 1) {
            SAVideoAd.enableMuteOnStart();
        }
        SAVideoAd.setListener((SAInterface) (placementId, event) -> {
            String pId = String.valueOf(placementId);
            switch (event) {
                case adLoaded:
                    // called when an ad has finished loading
                    Log.i(TAG, "adLoaded: " + pId);
                    if (mCallbackRouter.getListener(pId) != null) {
                        mCallbackRouter.getListener(pId).loadAdapterLoaded(null);
                    }
                    break;
                case adEmpty:
                    Log.i(TAG, "adEmpty: ");
                    // called when the request was successful but the server returned no ad
                    if (mCallbackRouter.getListener(pId) != null) {
                        mCallbackRouter.getListener(pId).loadAdapterLoadFailed(new TPError(NETWORK_NO_FILL));
                    }
                    break;
                case adFailedToLoad:
                    Log.i(TAG, "adFailedToLoad: " + pId + ", msg : " + event.name());
                    // called when an ad could not be loaded
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage(event.name());
                    if (mCallbackRouter.getListener(pId) != null) {
                        mCallbackRouter.getListener(pId).loadAdapterLoadFailed(tpError);
                    }
                    break;
                case adShown:
                    Log.i(TAG, "adShown: " + pId);
                    // called when an ad is first shown
                    if (mCallbackRouter.getShowListener(pId) != null) {
                        mCallbackRouter.getShowListener(pId).onAdShown();
                    }

                    if (mCallbackRouter.getShowListener(pId) != null) {
                        mCallbackRouter.getShowListener(pId).onAdVideoStart();
                    }
                    break;
                case adFailedToShow:
                    Log.i(TAG, "adFailedToShow: " + pId + ", msg : " + event.name());
                    // called when an ad fails to show
                    if (mCallbackRouter.getShowListener(pId) != null) {
                        TPError tpErrorTwo = new TPError(SHOW_FAILED);
                        tpErrorTwo.setErrorMessage(event.name());
                        mCallbackRouter.getShowListener(pId).onAdVideoError(tpErrorTwo);
                    }
                    break;
                case adClicked:
                    Log.i(TAG, "adClicked: " + pId);
                    // called when an ad is clicked
                    if (mCallbackRouter.getShowListener(pId) != null) {
                        mCallbackRouter.getShowListener(pId).onAdVideoClicked();
                    }
                    break;
                case adEnded:
                    Log.i(TAG, "video adEnded: " + pId);
                    hasGrantedReward = true;
                    // called when a video ad has ended playing (but hasn't yet closed)
                    if (mCallbackRouter.getShowListener(pId) != null) {
                        mCallbackRouter.getShowListener(pId).onAdVideoEnd();
                    }
                    break;
                case adClosed:
                    Log.i(TAG, "adClosed: " + pId);
                    if (mCallbackRouter.getShowListener(pId) != null) {
                        if (hasGrantedReward || alwaysRewardUser) {
                            mCallbackRouter.getShowListener(pId).onReward();
                        }
                        // called when a fullscreen ad is closed
                        mCallbackRouter.getShowListener(pId).onAdClosed();
                    }
                    break;
            }
        });
        SAVideoAd.load(Integer.parseInt(placementId), context);
    }


    @Override
    public boolean isReady() {
        return placementId != null && SAVideoAd.hasAdAvailable(Integer.parseInt(placementId)) && !isAdsTimeOut();
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_AWESOME);
    }

    @Override
    public String getNetworkVersion() {
        return SAVersion.getSDKVersion("awesome");
    }


    @Override
    public void showAd() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (mShowListener != null) {
            mCallbackRouter.addShowListener(placementId, mShowListener);
        }

        SAVideoAd.play(Integer.parseInt(placementId), activity);
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }
}
