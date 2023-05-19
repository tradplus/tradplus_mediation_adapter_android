package com.tradplus.ads.txadnet;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.interstitial2.UnifiedInterstitialAD;
import com.qq.e.ads.interstitial2.UnifiedInterstitialADListener;
import com.qq.e.ads.interstitial2.UnifiedInterstitialMediaListener;
import com.qq.e.comm.compliance.DownloadConfirmListener;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.comm.managers.status.SDKStatus;
import com.qq.e.comm.util.AdError;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class TxAdnetInterstitialVideo extends TPInterstitialAdapter {
    private static final String TAG = "GDTInterstitial";
    private UnifiedInterstitialAD mUnifiedInterstitialAD;
    private String mPlacementId;
    private boolean isVideoSoundEnable = true;
    private boolean showPopUpWin = false;
    private TxAdnetInterstitialCallbackRouter mCallbackRouter;
    private int fullScreenType;
    private int videoMaxTime, autoPlayVideo;
    private String payload;
    private String price;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        String mAutoPlayVideo;
        String mVideoMute;
        String mVideoMaxTime;
        if (serverExtrasAreValid(serverExtras)) {
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = serverExtras.get(DataKeys.BIDDING_PAYLOAD);
            price = serverExtras.get(DataKeys.BIDDING_PRICE);
            mAutoPlayVideo = serverExtras.get(AppKeyManager.AUTO_PLAY_VIDEO);
            mVideoMute = serverExtras.get(AppKeyManager.VIDEO_MUTE);
            mVideoMaxTime = serverExtras.get(AppKeyManager.VIDEO_MAX_TIME);
            fullScreenType = Integer.parseInt(serverExtras.get(AppKeyManager.FULL_SCREEN_TYPE));


            if (!TextUtils.isEmpty(mVideoMute)) {
                if (!mVideoMute.equals(AppKeyManager.VIDEO_MUTE_YES)) {
                    isVideoSoundEnable = false;
                }
            }

            if (!TextUtils.isEmpty(mVideoMaxTime)) {
                videoMaxTime = Integer.parseInt(mVideoMaxTime);
            }

            if (!TextUtils.isEmpty(mAutoPlayVideo)) {
                autoPlayVideo = Integer.parseInt(mAutoPlayVideo);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(GDTConstant.GTD_POPUP)) {
                showPopUpWin = (boolean)localExtras.get(GDTConstant.GTD_POPUP);
            }
        }

        mCallbackRouter = TxAdnetInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);

        TencentInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                reqeustAd();
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });
    }

    private void reqeustAd() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mCallbackRouter.getListener(mPlacementId) != null) {
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (TextUtils.isEmpty(payload)) {
            mUnifiedInterstitialAD = new UnifiedInterstitialAD(activity, mPlacementId, mUnifiedInterstitialADListener);
        } else {
            mUnifiedInterstitialAD = new UnifiedInterstitialAD(activity, mPlacementId, mUnifiedInterstitialADListener, null, payload);
        }
        setVideoOption();
        mUnifiedInterstitialAD.setMediaListener(mUnifiedInterstitialMediaListener);

        if (fullScreenType == AppKeyManager.FULL_TYPE) {
            mUnifiedInterstitialAD.loadFullScreenAD();
        } else {
            mUnifiedInterstitialAD.loadAD();
        }
    }

    private void setVideoOption() {
        VideoOption.Builder builder = new VideoOption.Builder();
        Log.i(TAG, "PlacementId: " + mPlacementId + "， videoMute: " + isVideoSoundEnable);
        VideoOption option = builder.setAutoPlayMuted(isVideoSoundEnable)
                .setDetailPageMuted(isVideoSoundEnable)
                .setAutoPlayPolicy(autoPlayVideo == 1 ? VideoOption.AutoPlayPolicy.ALWAYS : VideoOption.AutoPlayPolicy.WIFI)
                .build();
        mUnifiedInterstitialAD.setVideoOption(option);

        if (videoMaxTime >= 5 && videoMaxTime <= 60) {
            mUnifiedInterstitialAD.setMaxVideoDuration(videoMaxTime);
        }
    }


    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);
        }

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (mUnifiedInterstitialAD != null) {
            setBidEcpm();
            if (fullScreenType == AppKeyManager.FULL_TYPE) {
                mUnifiedInterstitialAD.showFullScreenAD(activity);
            } else {
                if (showPopUpWin) {
                    mUnifiedInterstitialAD.showAsPopupWindow(activity);
                }else {
                    mUnifiedInterstitialAD.show(activity);
                }
            }
        } else {
            mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
        }
    }

    private final UnifiedInterstitialMediaListener mUnifiedInterstitialMediaListener = new UnifiedInterstitialMediaListener() {

        @Override
        public void onVideoInit() {

        }

        @Override
        public void onVideoLoading() {

        }

        @Override
        public void onVideoReady(long l) {

        }

        @Override
        public void onVideoStart() {
            Log.i(TAG, "onVideoStart: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoStart();
            }
        }

        @Override
        public void onVideoPause() {

        }

        @Override
        public void onVideoComplete() {
            Log.i(TAG, "onVideoComplete: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoEnd();
            }
        }

        @Override
        public void onVideoError(AdError adError) {
            Log.i(TAG, "onVideoError: code:" + adError.getErrorCode() + " msg: " + adError.getErrorMsg());
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorCode(adError.getErrorCode() + "");
                tpError.setErrorMessage(adError.getErrorMsg());
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(tpError);
            }
        }

        @Override
        public void onVideoPageOpen() {

        }

        @Override
        public void onVideoPageClose() {

        }
    };

    private final UnifiedInterstitialADListener mUnifiedInterstitialADListener = new UnifiedInterstitialADListener() {
        @Override
        public void onADReceive() {
        }

        @Override
        public void onVideoCached() {
            if (mUnifiedInterstitialAD.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
                if (mCallbackRouter.getListener(mPlacementId) != null) {
                    Log.i(TAG, "onVideoCached: " + mUnifiedInterstitialAD.getAdPatternType());
                    setNetworkObjectAd(mUnifiedInterstitialAD);
                    mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
                }
            }
        }

        @Override
        public void onNoAD(AdError adError) {
            Log.i(TAG, "onNoAD, errorcode :" + adError.getErrorCode() + ", errormessage :" + adError.getErrorMsg());
            if (mCallbackRouter.getListener(mPlacementId) != null) {
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(TxAdnetErrorUtil.getTradPlusErrorCode(adError));
            }
        }

        @Override
        public void onADOpened() {
            Log.i(TAG, "onADOpened: ");

        }

        @Override
        public void onADExposure() {
            Log.i(TAG, "onADExposure: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdShown();
            }
        }

        @Override
        public void onADClicked() {
            Log.i(TAG, "onADClicked: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoClicked();
            }
        }

        @Override
        public void onADLeftApplication() {
            Log.i(TAG, "onADLeftApplication: ");
        }

        @Override
        public void onADClosed() {
            Log.i(TAG, "onADClosed: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdClosed();
            }
        }

        @Override
        public void onRenderSuccess() {
            setFirstLoadedTime();
            if (mUnifiedInterstitialAD.getAdPatternType() != AdPatternType.NATIVE_VIDEO) {
                if (mCallbackRouter.getListener(mPlacementId) != null) {
                    Log.i(TAG, "onADReceive: " + mUnifiedInterstitialAD.getAdPatternType());
                    setNetworkObjectAd(mUnifiedInterstitialAD);
                    mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
                }
            }

        }

        @Override
        public void onRenderFail() {
            Log.i(TAG, "onRenderFail: ");
        }
    };


    @Override
    public void clean() {
        super.clean();
        if (mUnifiedInterstitialAD != null) {
            mUnifiedInterstitialAD.setMediaListener(null);
            mUnifiedInterstitialAD.destroy();
            mUnifiedInterstitialAD = null;
        }
    }

    @Override
    public boolean isReady() {
        Log.i(TAG, "isReady:" + mUnifiedInterstitialAD.isValid());
        if (mUnifiedInterstitialAD != null) {
            return !isAdsTimeOut() && mUnifiedInterstitialAD.isValid();
        }
        return false;
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_TENCENT);
    }

    @Override
    public String getNetworkVersion() {
        return SDKStatus.getIntegrationSDKVersion();
    }

    private boolean serverExtrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    @Override
    public void setNetworkExtObj(Object obj) {
        Log.i(TAG, "setNetworkExtObj: ");
        if (obj instanceof DownloadConfirmListener) {
            if (mUnifiedInterstitialAD != null) {
                mUnifiedInterstitialAD.setDownloadConfirmListener((DownloadConfirmListener) obj);
            }
        }

    }

    private void setBidEcpm() {
        try {
            float temp = Float.parseFloat(price);
            int price = (int) temp;
            Log.i(TAG, "setBidEcpm: " + price);
            mUnifiedInterstitialAD.setBidECPM(price);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public String getBiddingToken(Context context, Map<String, String> tpParams) {
        if (tpParams == null) {
            return "";
        }
        String appId = tpParams.get(AppKeyManager.APP_ID);
        if (!TencentInitManager.isInited(appId)) {
            GDTAdSdk.init(context, appId);
        }
        return GDTAdSdk.getGDTAdManger().getBuyerId(null);
    }

    @Override
    public String getBiddingNetworkInfo(Context context, Map<String, String> tpParams) {
        if (tpParams == null) {
            return "";
        }

        if (tpParams.containsKey(AppKeyManager.AD_PLACEMENT_ID)) {
            String placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            try {
                return GDTAdSdk.getGDTAdManger().getSDKInfo(placementId);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                return "";
            }
        }
        return "";
    }
}
