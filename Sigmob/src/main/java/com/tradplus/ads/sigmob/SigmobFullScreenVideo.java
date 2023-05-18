package com.tradplus.ads.sigmob;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.sigmob.windad.WindAdError;
import com.sigmob.windad.WindAdOptions;
import com.sigmob.windad.WindAds;
import com.sigmob.windad.interstitial.WindInterstitialAd;
import com.sigmob.windad.interstitial.WindInterstitialAdListener;
import com.sigmob.windad.interstitial.WindInterstitialAdRequest;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.HashMap;
import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;


public class SigmobFullScreenVideo extends TPInterstitialAdapter {

    private String mPlacementId;
    private SigmobInterstitialCallbackRouter mSigmobICbR;
    private WindInterstitialAd mWindInterstitialAd;
    private final static String TAG = "SigmobFullScreenVideo";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
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
        WindInterstitialAdRequest mWindInterstitialAdRequest = new WindInterstitialAdRequest(mPlacementId, null, null);

        mWindInterstitialAd = new WindInterstitialAd(mWindInterstitialAdRequest);
        mWindInterstitialAd.setWindInterstitialAdListener(mInterstitialAdListener);

        if (TextUtils.isEmpty(bidToken)) {
            mWindInterstitialAd.loadAd();
        } else {
            mWindInterstitialAd.loadAd(bidToken);
        }
    }

    private final WindInterstitialAdListener mInterstitialAdListener =  new WindInterstitialAdListener() {
        @Override
        public void onInterstitialAdLoadSuccess(String placementId) {
            Log.i(TAG, "onInterstitialAdLoadSuccess: ");
            setFirstLoadedTime();
            if (mSigmobICbR.getListener(placementId) != null) {
                setNetworkObjectAd(mWindInterstitialAd);
                mSigmobICbR.getListener(placementId).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onInterstitialAdPreLoadSuccess(String s) {

        }

        @Override
        public void onInterstitialAdPreLoadFail(String s) {

        }

        @Override
        public void onInterstitialAdPlayStart(String placementId) {
            Log.i(TAG, "onInterstitialAdPlayStart: ");
            if (mSigmobICbR.getShowListener(placementId) != null) {
                mSigmobICbR.getShowListener(placementId).onAdVideoStart();
                mSigmobICbR.getShowListener(placementId).onAdShown();
            }
        }

        @Override
        public void onInterstitialAdPlayEnd(String placementId) {
            Log.i(TAG, "onInterstitialAdPlayEnd: ");
            if (mSigmobICbR.getShowListener(placementId) != null)
                mSigmobICbR.getShowListener(placementId).onAdVideoEnd();
        }

        @Override
        public void onInterstitialAdClicked(String placementId) {
            Log.i(TAG, "onInterstitialAdClicked: ");
            if (mSigmobICbR.getShowListener(placementId) != null)
                mSigmobICbR.getShowListener(placementId).onAdVideoClicked();
        }

        @Override
        public void onInterstitialAdClosed(String placementId) {
            Log.i(TAG, "onInterstitialAdClosed: ");
            if (mSigmobICbR.getShowListener(placementId) != null)
                mSigmobICbR.getShowListener(placementId).onAdClosed();
        }

        @Override
        public void onInterstitialAdLoadError(WindAdError windAdError, String placementId) {
            Log.i(TAG, "onInterstitialAdLoadError: code : " + windAdError.getErrorCode() + ", msg :" + windAdError.getMessage());
            if (mSigmobICbR.getListener(placementId) != null)
                mSigmobICbR.getListener(placementId).loadAdapterLoadFailed(SimgobErrorUtil.getTradPlusErrorCode(windAdError));

        }

        @Override
        public void onInterstitialAdPlayError(WindAdError windAdError, String placementId) {
            Log.i(TAG, "onInterstitialAdPlayError: code : " + windAdError.getErrorCode() + ", msg :" + windAdError.getMessage());
            if (mSigmobICbR.getShowListener(placementId) != null)
                mSigmobICbR.getShowListener(placementId).onAdVideoError(SimgobErrorUtil.getTradPlusErrorCode(windAdError));

        }
    };

    @Override
    public void showAd() {
        if (mShowListener != null) {
            mSigmobICbR.addShowListener(mPlacementId, mShowListener);
        }

        if (mWindInterstitialAd == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(UNSPECIFIED));
            }
            return;
        }

        if (mWindInterstitialAd.isReady()) {
            mWindInterstitialAd.show(new HashMap<String, String>());
        }else {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
            }
        }
    }

    @Override
    public boolean isReady() {
        if (mWindInterstitialAd != null) {
            return !isAdsTimeOut() && mWindInterstitialAd.isReady();
        }

        return false;
    }

    @Override
    public void clean() {
        if (mWindInterstitialAd != null) {
            mWindInterstitialAd.setWindInterstitialAdListener(null);
            mWindInterstitialAd.destroy();
            mWindInterstitialAd = null;
        }
        
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
