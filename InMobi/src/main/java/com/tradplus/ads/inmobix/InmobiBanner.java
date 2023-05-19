package com.tradplus.ads.inmobix;

import android.content.Context;
import android.util.Log;


import androidx.annotation.NonNull;

import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.listeners.BannerAdEventListener;
import com.inmobi.sdk.InMobiSdk;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPTaskManager;
import com.tradplus.ads.common.util.Views;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

public class InmobiBanner extends TPBannerAdapter {

    private String mPlacementId;
    private InMobiBanner bannerAd;
    private static final String TAG = "InMobiBanner";
    private TPBannerAdImpl tpBannerAd;
    private boolean isC2SBidding;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) return;

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            setAdHeightAndWidthByService(mPlacementId, tpParams);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }
        setDefaultAdSize(320, 50);

        InmobiInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                if (bannerAd != null) {
                    requestInmobiBiddingBanner();
                } else {
                    requestInMobiBanner(context);
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

    private void requestInmobiBiddingBanner() {
        bannerAd.setListener(bannerAdEventListener);
        Map<String, String> parameters = InmobiInitManager.getInstance().getParameters();
        if (parameters != null) {
            bannerAd.setExtras(parameters);
        }
        bannerAd.getPreloadManager().load();
    }

    private BannerAdEventListener bannerAdEventListener = new BannerAdEventListener() {

        @Override
        public void onAdDisplayed(InMobiBanner inMobiBanner) {
            super.onAdDisplayed(inMobiBanner);
            Log.i(TAG, "onAdDisplayed: ");

        }

        @Override
        public void onAdDismissed(InMobiBanner inMobiBanner) {
            super.onAdDismissed(inMobiBanner);
            Log.i(TAG, "onAdDismissed: ");
        }

        @Override
        public void onUserLeftApplication(InMobiBanner inMobiBanner) {
            super.onUserLeftApplication(inMobiBanner);
            Log.i(TAG, "onUserLeftApplication: ");

        }

        @Override
        public void onAdFetchSuccessful(InMobiBanner inMobiBanner, AdMetaInfo adMetaInfo) {
            super.onAdFetchSuccessful(inMobiBanner, adMetaInfo);
            Log.i(TAG, "onAdFetchSuccessful: ");

            if (mLoadAdapterListener != null) {
                tpBannerAd = new TPBannerAdImpl(null, inMobiBanner);
                mLoadAdapterListener.loadAdapterLoaded(tpBannerAd);
            }
        }


        @Override
        public void onAdLoadSucceeded(InMobiBanner inMobiBanner, AdMetaInfo adMetaInfo) {
            super.onAdLoadSucceeded(inMobiBanner, adMetaInfo);
            Log.i(TAG, "onAdLoadSucceeded: ");

            if (isC2SBidding) {
                if (mLoadAdapterListener != null) {
                    tpBannerAd = new TPBannerAdImpl(null, inMobiBanner);
                    mLoadAdapterListener.loadAdapterLoaded(tpBannerAd);
                }
            }

        }

        @Override
        public void onAdLoadFailed(InMobiBanner inMobiBanner, InMobiAdRequestStatus inMobiAdRequestStatus) {
            super.onAdLoadFailed(inMobiBanner, inMobiAdRequestStatus);
            Log.i(TAG, "onAdLoadFailed: ");
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(InmobiErrorUtils.getTPError(inMobiAdRequestStatus));
            }
        }

        @Override
        public void onAdClicked(InMobiBanner inMobiBanner, Map<Object, Object> map) {
            super.onAdClicked(inMobiBanner, map);
            Log.i(TAG, "onAdClicked: ");
            if (tpBannerAd != null) tpBannerAd.adClicked();
        }

        @Override
        public void onAdImpression(@NonNull InMobiBanner inMobiBanner) {
            Log.i(TAG, "onAdImpression: ");
            if (tpBannerAd != null) tpBannerAd.adShown();
        }
    };

    private void requestInMobiBanner(Context context) {
        long pid;
        try {
            pid = Long.parseLong(mPlacementId);
        } catch (Throwable e) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(UNSPECIFIED));
            }
            return;
        }

        bannerAd = new InMobiBanner(context, pid);
        bannerAd.setEnableAutoRefresh(false);
        bannerAd.setAnimationType(InMobiBanner.AnimationType.ANIMATION_OFF);
        bannerAd.setListener(bannerAdEventListener);
        bannerAd.setBannerSize(mAdWidth, mAdHeight);
        Map<String, String> parameters = InmobiInitManager.getInstance().getParameters();
        if (parameters != null) {
            bannerAd.setExtras(parameters);
        }
        bannerAd.load();
    }

    @Override
    public void clean() {
        if (bannerAd != null) {
            Views.removeFromParent(bannerAd);
            bannerAd.setListener(null);
            bannerAd.destroy();
            bannerAd = null;
        }
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
                if (onC2STokenListener != null) onC2STokenListener.onC2SBiddingFailed(code, msg);
            }
        });
    }

    public void requestBid(final Context context, Map<String, Object> userParams, Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            setAdHeightAndWidthByService(mPlacementId, tpParams);
        }

        setDefaultAdSize(320, 50);
        //Load
        long pid;
        try {
            pid = Long.parseLong(mPlacementId);
        } catch (Throwable e) {
            Log.d(TAG, "Bid failed ：pid error ");
            onC2STokenListener.onC2SBiddingFailed("", e.getMessage());
            return;
        }

        final long finalPid = pid;
        TPTaskManager.getInstance().getThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                bannerAd = new InMobiBanner(context, finalPid);
                bannerAd.setEnableAutoRefresh(false);
                bannerAd.setAnimationType(InMobiBanner.AnimationType.ANIMATION_OFF);
                Log.i(TAG, "mAdWidth: " + mAdWidth + ",mAdHeight：" + mAdHeight);
                bannerAd.setBannerSize(mAdWidth, mAdHeight);
                bannerAd.setListener(new BannerAdEventListener() {
                    @Override
                    public void onAdFetchSuccessful(@NonNull InMobiBanner inMobiBanner, @NonNull AdMetaInfo adMetaInfo) {
                        Log.d(TAG, "Bid received : " + adMetaInfo.getBid());
                        isC2SBidding = true;
                        onC2STokenListener.onC2SBiddingResult(adMetaInfo.getBid());
                    }

                    @Override
                    public void onAdFetchFailed(@NonNull InMobiBanner inMobiBanner, @NonNull InMobiAdRequestStatus inMobiAdRequestStatus) {
                        Log.i(TAG, "onAdFetchFailed: " + inMobiAdRequestStatus.getMessage());
                        onC2STokenListener.onC2SBiddingFailed("", inMobiAdRequestStatus.getMessage());
                    }
                });
                Map<String, String> parameters = InmobiInitManager.getInstance().getParameters();
                if (parameters != null) {
                    bannerAd.setExtras(parameters);
                }
                bannerAd.getPreloadManager().preload();
            }
        });

    }
}
