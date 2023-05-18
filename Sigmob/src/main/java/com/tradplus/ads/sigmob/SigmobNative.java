package com.tradplus.ads.sigmob;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.sigmob.windad.WindAdError;
import com.sigmob.windad.WindAdOptions;
import com.sigmob.windad.WindAds;
import com.sigmob.windad.natives.WindNativeAdData;
import com.sigmob.windad.natives.WindNativeAdRequest;
import com.sigmob.windad.natives.WindNativeUnifiedAd;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.List;
import java.util.Map;

public class SigmobNative extends TPNativeAdapter {

    private WindNativeAdData mWindNativeAdData;
    private WindNativeUnifiedAd windNativeUnifiedAd;
    private String mPlacementId;
    private SigmobNativeAd mSigmobNativeAd;
    private final static String TAG = "SigmobNative";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) return;

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
                requestNative(context,token);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });
    }

    private void requestNative(final Context context,String bidToken) {
        WindNativeAdRequest windNativeAdRequest = new WindNativeAdRequest(mPlacementId, null, null);
        windNativeUnifiedAd = new WindNativeUnifiedAd(windNativeAdRequest);
        windNativeUnifiedAd.setNativeAdLoadListener(new WindNativeUnifiedAd.WindNativeAdLoadListener() {
            @Override
            public void onAdError(WindAdError windAdError, String placementId) {
                Log.i(TAG, "onError:ErrorCode == " + windAdError.getErrorCode() + ", Message == " + windAdError.getMessage());
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorCode(windAdError.getErrorCode() + "");
                    tpError.setErrorMessage(windAdError.getMessage());
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void onAdLoad(List<WindNativeAdData> list, String placementId) {
                if (list != null && list.size() > 0) {
                    WindNativeAdData windNativeAdData = list.get(0);
                    Log.i(TAG, "onFeedAdLoad: ");
                    mSigmobNativeAd = new SigmobNativeAd(context,windNativeAdData);
                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoaded(mSigmobNativeAd);
                    }
                }
            }
        });

        if (TextUtils.isEmpty(bidToken)) {
            windNativeUnifiedAd.loadAd(3);
        } else {
            windNativeUnifiedAd.loadAd(bidToken);
        }
    }

    @Override
    public void clean() {

        if (windNativeUnifiedAd != null) {
            windNativeUnifiedAd.destroy();
            windNativeUnifiedAd = null;
        }
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
