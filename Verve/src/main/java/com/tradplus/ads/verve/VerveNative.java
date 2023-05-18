package com.tradplus.ads.verve;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.util.Log;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;


import net.pubnative.lite.sdk.HyBid;
import net.pubnative.lite.sdk.models.NativeAd;
import net.pubnative.lite.sdk.request.HyBidNativeAdRequest;

import java.util.Map;

public class VerveNative extends TPNativeAdapter {

    private String mPlacementId;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private VerveNativeAd mVerveNativeAd;
    private OnC2STokenListener onC2STokenListener;
    private NativeAd mNativeAd;
    private HyBidNativeAdRequest nativeAdRequest;
    private boolean mNeedDownloadImg = false;
    private static final String TAG = "VerveNative";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(DataKeys.DOWNLOAD_IMG)) {
                String downLoadImg = (String) userParams.get(DataKeys.DOWNLOAD_IMG);
                if (downLoadImg.equals("true")) {
                    mNeedDownloadImg = true;
                }
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
            if (mNativeAd == null) {
                TPError tpError = new TPError(UNSPECIFIED);
                Log.i(TAG, "Load Failed, NativeAd == null");
                tpError.setErrorMessage("Load Failed, NativeAd == null");
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                return;
            }

            setFirstLoadedTime();
            mVerveNativeAd = new VerveNativeAd(context, mNativeAd);
            downloadAndCallback(mVerveNativeAd, mNeedDownloadImg);
        } else {
            nativeAdRequest = new HyBidNativeAdRequest();
            nativeAdRequest.load(mPlacementId, new HyBidNativeAdRequest.RequestListener() {
                @Override
                public void onRequestSuccess(NativeAd ad) {
                    if (isC2SBidding) {
                        Log.i(TAG, "onRequestSuccess: isC2SBidding BidPoints: " + ad.getBidPoints());
                        if (onC2STokenListener != null) {
                            onC2STokenListener.onC2SBiddingResult(ad.getBidPoints());
                        }
                        mNativeAd = ad;
                        isBiddingLoaded = true;
                        return;
                    }

                    mVerveNativeAd = new VerveNativeAd(context, ad);
                    downloadAndCallback(mVerveNativeAd, mNeedDownloadImg);
                }

                @Override
                public void onRequestFail(Throwable throwable) {
                    //Ad failed to load
                    Log.i(TAG, "onRequestFail: errormsg:" + throwable.getMessage());
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
            });
        }
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }

    @Override
    public void clean() {

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
