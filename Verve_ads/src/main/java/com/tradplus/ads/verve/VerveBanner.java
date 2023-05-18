package com.tradplus.ads.verve;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.LARGEBANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.MEDIUMRECTANGLE;

import android.content.Context;
import android.util.Log;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import net.pubnative.lite.sdk.HyBid;
import net.pubnative.lite.sdk.models.AdSize;
import net.pubnative.lite.sdk.models.ImpressionTrackingMethod;
import net.pubnative.lite.sdk.views.HyBidAdView;

import java.util.Map;

public class VerveBanner extends TPBannerAdapter {

    private String mPlacementId, mAdSize;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private TPBannerAdImpl mTpBannerAd;
    private HyBidAdView adViewAd;
    private int onAdClick = 0; // 0 表示没有点击
    private OnC2STokenListener onC2STokenListener;
    private static final String TAG = "VerveBanner";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (tpParams.containsKey(AppKeyManager.ADSIZE + mPlacementId)) {
                mAdSize = tpParams.get(AppKeyManager.ADSIZE + mPlacementId);
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

        VerveInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestAd(context);
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

    private void requestAd(Context context) {
        if (isC2SBidding && isBiddingLoaded) {
            if (adViewAd == null) {
                TPError tpError = new TPError(UNSPECIFIED);
                Log.i(TAG, "Load Failed, HyBidAdView == null");
                tpError.setErrorMessage("Load Failed, HyBidAdView == null");
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                return;
            }

            setFirstLoadedTime();
            mTpBannerAd = new TPBannerAdImpl(null, adViewAd);
            if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);

        } else {
            adViewAd = new HyBidAdView(context, calculateAdSize(mAdSize));
            adViewAd.setTrackingMethod(ImpressionTrackingMethod.AD_VIEWABLE);
            adViewAd.load(mPlacementId, mListener);
        }
    }

    private final HyBidAdView.Listener mListener = new HyBidAdView.Listener() {
        @Override
        public void onAdLoaded() {
            if (adViewAd == null) {
                TPError tpError = new TPError(UNSPECIFIED);
                Log.i(TAG, "Load Failed, mAdView == null");
                tpError.setErrorMessage("Load Failed, mAdView == null");
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                return;
            }

            if(isC2SBidding){
                Log.i(TAG, "onAdLoaded: isC2SBidding BidPoints: " + adViewAd.getBidPoints());
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingResult(adViewAd.getBidPoints());
                }
                isBiddingLoaded = true;
                return;
            }

            mTpBannerAd = new TPBannerAdImpl(null, adViewAd);
            mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);

        }

        @Override
        public void onAdLoadFailed(Throwable throwable) {
            Log.i(TAG, "onAdLoadFailed: errormsg:" + throwable.getMessage());
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",throwable.getMessage());
                }
                return;
            }

            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(throwable.getMessage());
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onAdImpression() {
            Log.i(TAG, "onAdImpression: ");
            if (mTpBannerAd != null) {
                mTpBannerAd.adShown();
            }
        }

        @Override
        public void onAdClick() {
            Log.i(TAG, "onAdClick: ");
            if (mTpBannerAd != null && onAdClick == 0) {
                onAdClick = 1;
                mTpBannerAd.adClicked();
            }
        }
    };

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }


    private AdSize calculateAdSize(String adSize) {
        if (adSize.equals(BANNER)) {
            return AdSize.SIZE_320x50; //320 * 50
        } else if (adSize.equals(LARGEBANNER)) {
            return AdSize.SIZE_300x250; //300x250
        } else if (adSize.equals(MEDIUMRECTANGLE)) {
            return AdSize.SIZE_728x90; //728x90
        }
        return AdSize.SIZE_320x50;
    }

    @Override
    public void clean() {
        if (adViewAd != null) {
            adViewAd.destroy();
            adViewAd = null;
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
