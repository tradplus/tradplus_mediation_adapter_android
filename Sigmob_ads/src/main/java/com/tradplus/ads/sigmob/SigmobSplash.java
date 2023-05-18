package com.tradplus.ads.sigmob;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.sigmob.windad.Splash.WindSplashAD;
import com.sigmob.windad.Splash.WindSplashADListener;
import com.sigmob.windad.Splash.WindSplashAdRequest;
import com.sigmob.windad.WindAdError;
import com.sigmob.windad.WindAdOptions;
import com.sigmob.windad.WindAds;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;


public class SigmobSplash extends TPSplashAdapter {

    private String mPlacementId;
    private WindSplashAD mWindSplashAD;
    private String mLayoutName;
    private WindSplashAdRequest mSplashAdRequest;
    private static final String TAG = "SigmobSplash";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        String bidToken;
        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            bidToken = tpParams.get(DataKeys.BIDDING_PAYLOAD);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        final String token = bidToken;
        SigmobInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestSplash(token);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestSplash(String bidToken) {
        if (mSplashAdRequest == null)
            mSplashAdRequest = new WindSplashAdRequest(mPlacementId, null, null);

        /**
         *  广告结束，广告内容是否自动隐藏。
         *  若开屏和应用共用Activity，建议false。
         *  开屏是单独Activity ，建议true。
         */
        mSplashAdRequest.setDisableAutoHideAd(true);

        /**
         * 广告允许最大等待返回时间 : 默认5
         */
//        mSplashAdRequest.setFetchDelay(5);
        mWindSplashAD = new WindSplashAD(mSplashAdRequest, mWindSplshAdListener);

        if (TextUtils.isEmpty(bidToken)) {
            mWindSplashAD.loadAd();
        } else {
            mWindSplashAD.loadAd(bidToken);
        }


    }

    private final WindSplashADListener mWindSplshAdListener = new WindSplashADListener() {
        @Override
        public void onSplashAdShow(String s) {
            Log.i(TAG, "onSplashAdShow: ");
            if (mShowListener != null) {
                mShowListener.onAdShown();
            }
        }

        @Override
        public void onSplashAdLoadSuccess(String s) {
            Log.i(TAG, "onSplashAdSuccessLoad: ");
            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(mWindSplashAD);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
        }

        @Override
        public void onSplashAdLoadFail(WindAdError windAdError, String s) {
            Log.i(TAG, "onSplashAdLoadFail: code :" + windAdError.getErrorCode() + ", msg :" + windAdError.getMessage());
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorMessage(windAdError.getMessage());
            tpError.setErrorCode(windAdError.getErrorCode() + "");
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onSplashAdShowError(WindAdError windAdError, String s) {

        }

        @Override
        public void onSplashAdClick(String s) {
            Log.i(TAG, "onSplashAdClicked: ");
            if (mShowListener != null)
                mShowListener.onAdClicked();
        }

        @Override
        public void onSplashAdClose(String s) {
            Log.i(TAG, "onSplashClosed: ");
            if (mShowListener != null)
                mShowListener.onAdClosed();
        }

        @Override
        public void onSplashAdSkip(String s) {

        }
    };


    @Override
    public void showAd() {
        if (mWindSplashAD != null && mAdContainerView != null) {
            mWindSplashAD.show(mAdContainerView);
        } else {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(UNSPECIFIED));
            }
        }
    }

    @Override
    public void clean() {
        if (mSplashAdRequest != null) {
            mSplashAdRequest = null;
        }

        if (mAdContainerView != null) {
            mAdContainerView.removeAllViews();
            mAdContainerView = null;
        }
    }

    @Override
    public boolean isReady() {
        return mWindSplashAD != null && mWindSplashAD.isReady();
    }

    @Override
    public String getBiddingToken(Context context, Map<String, String> tpParams) {
        if (tpParams == null) {
            return "";
        }

        String appId = tpParams.get(AppKeyManager.APP_ID);
        String appkey = tpParams.get(AppKeyManager.APP_KEY);
        WindAds windAds = WindAds.sharedAds();

        if(!SigmobInitManager.isInited(appId)) {
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