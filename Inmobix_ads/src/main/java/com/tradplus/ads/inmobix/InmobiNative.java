package com.tradplus.ads.inmobix;

import android.content.Context;
import android.util.Log;


import androidx.annotation.NonNull;

import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiNative;
import com.inmobi.ads.listeners.NativeAdEventListener;
import com.inmobi.sdk.InMobiSdk;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;


public class InmobiNative extends TPNativeAdapter {

    private String mPlacementId;
    private InMobiNative inmobiNative;
    private InmobiNativeAd mInmobiNativeAd;
    private static final String TAG = "InmobiNative";
    private boolean mNeedDownloadImg = false;
    private Context mContext;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) return;

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(DataKeys.DOWNLOAD_IMG)) {
                String downLoadImg = (String) userParams.get(DataKeys.DOWNLOAD_IMG);
                if (downLoadImg.equals("true")) {
                    mNeedDownloadImg = true;
                }
            }
        }

//        mAccountId = "6d2e43a4d0694d8990f382629eeebe42";
//        mPlacementId = "1604506512084";
        this.mContext = context;
        InmobiInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                if (inmobiNative != null) {
                    requestInmobiBiddingNative();
                } else {
                    requestInmobiNative(context);
                }
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });


    }


    private void requestInmobiBiddingNative() {
        inmobiNative.setListener(nativeAdEventListener);
        Map<String, String> parameters = InmobiInitManager.getInstance().getParameters();
        if (parameters != null) {
            inmobiNative.setExtras(parameters);
        }
        inmobiNative.load();
    }

    private NativeAdEventListener nativeAdEventListener = new NativeAdEventListener() {
        @Override
        public void onAdFullScreenDismissed(InMobiNative inMobiNative) {
            super.onAdFullScreenDismissed(inMobiNative);
            Log.i(TAG, "onAdFullScreenDismissed: ");
        }

        @Override
        public void onAdFullScreenWillDisplay(InMobiNative inMobiNative) {
            super.onAdFullScreenWillDisplay(inMobiNative);
            Log.i(TAG, "onAdFullScreenWillDisplay: ");
        }

        @Override
        public void onAdFullScreenDisplayed(InMobiNative inMobiNative) {
            super.onAdFullScreenDisplayed(inMobiNative);
            Log.i(TAG, "onAdFullScreenDisplayed: ");
        }

        @Override
        public void onUserWillLeaveApplication(InMobiNative inMobiNative) {
            super.onUserWillLeaveApplication(inMobiNative);
            Log.i(TAG, "onUserWillLeaveApplication: ");
        }

        @Override
        public void onAdImpressed(InMobiNative inMobiNative) {
            super.onAdImpressed(inMobiNative);
            Log.i(TAG, "onAdImpressed: ");
            if (mInmobiNativeAd != null)
                mInmobiNativeAd.onAdViewExpanded();
        }

        @Override
        public void onAdClicked(InMobiNative inMobiNative) {
            super.onAdClicked(inMobiNative);
            Log.i(TAG, "onAdClicked: ");
            if (mInmobiNativeAd != null)
                mInmobiNativeAd.onAdViewClicked();
        }

        @Override
        public void onAdFetchSuccessful(InMobiNative inMobiNative, AdMetaInfo adMetaInfo) {
            super.onAdFetchSuccessful(inMobiNative, adMetaInfo);
            Log.i(TAG, "onAdFetchSuccessful: ");
        }

        @Override
        public void onAdLoadSucceeded(InMobiNative inMobiNative, AdMetaInfo adMetaInfo) {
            super.onAdLoadSucceeded(inMobiNative, adMetaInfo);

            //判断广告是否准备好展示
            mInmobiNativeAd = new InmobiNativeAd(mContext, inMobiNative);

            if (inMobiNative.isReady()) {
                if (mLoadAdapterListener != null) {
                    Log.i(TAG, "onAdLoadSucceeded: ");
                    downloadAndCallback(mInmobiNativeAd, mNeedDownloadImg);
                }
            } else {
                Log.i(TAG, "Not Ready: ");
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage("inMobiNative Not Ready");
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        }

        @Override
        public void onAdLoadFailed(InMobiNative inMobiNative, InMobiAdRequestStatus inMobiAdRequestStatus) {
            super.onAdLoadFailed(inMobiNative, inMobiAdRequestStatus);
            Log.i(TAG, "onAdLoadFailed: ");
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(InmobiErrorUtils.getTPError(inMobiAdRequestStatus));
            }

        }
    };

    private void requestInmobiNative(final Context context) {
        //Load

        long pid = 0;
        try {
            pid = Long.parseLong(mPlacementId);
        } catch (Throwable e) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(UNSPECIFIED));
                return;
            }
        }

        inmobiNative = new InMobiNative(context, pid, nativeAdEventListener);
        Map<String, String> parameters = InmobiInitManager.getInstance().getParameters();
        if (parameters != null) {
            inmobiNative.setExtras(parameters);
        }
        inmobiNative.load();

    }

    @Override
    public void clean() {

    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_INMOBI);
    }

    @Override
    public String getNetworkVersion() {
        return InMobiSdk.getVersion();
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        InmobiInitManager.getInstance().initSDK(context, localParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestBid(context, localParams, tpParams, onC2STokenListener);
            }

            @Override
            public void onFailed(String code, String msg) {
                if (onC2STokenListener != null)
                    onC2STokenListener.onC2SBiddingFailed(code, msg);
            }
        });
    }

    public void requestBid(final Context context, Map<String, Object> userParams, Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        }
        //Load
        long pid;
        try {
            pid = Long.parseLong(mPlacementId);
        } catch (Throwable e) {
            onC2STokenListener.onC2SBiddingFailed("", e.getMessage());
            return;
        }
        inmobiNative = new InMobiNative(context, pid, new NativeAdEventListener() {
            @Override
            public void onAdFetchSuccessful(@NonNull InMobiNative inMobiNative, @NonNull AdMetaInfo adMetaInfo) {
                onC2STokenListener.onC2SBiddingResult(adMetaInfo.getBid());
            }

        });

        Map<String, String> parameters = InmobiInitManager.getInstance().getParameters();
        if (parameters != null) {
            inmobiNative.setExtras(parameters);
        }
        // Step to preload interstitial
        inmobiNative.load();
    }
}
