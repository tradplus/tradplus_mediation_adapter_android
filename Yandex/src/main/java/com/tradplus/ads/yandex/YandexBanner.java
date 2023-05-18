package com.tradplus.ads.yandex;

import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.yandex.mobile.ads.banner.AdSize;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.BidderTokenLoadListener;
import com.yandex.mobile.ads.common.BidderTokenLoader;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.MobileAds;

import java.util.Map;

public class YandexBanner extends TPBannerAdapter {

    private static final String TAG = "YandexBanner";
    private String mPlacementId,payload;
    private BannerAdView mBannerAdView;
    private TPBannerAdImpl mTpBannerAd;
    private int onAdShow = 0;
    private Integer mAdSize = 4;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            if (tpParams.containsKey(AppKeyManager.ADSIZE + mPlacementId)) {
                mAdSize = Integer.parseInt(tpParams.get(AppKeyManager.ADSIZE + mPlacementId));
                Log.i(TAG, "AdSize: " + mAdSize);
            }
        }

        YandexInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestBanner(context);
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

    private void requestBanner(Context context) {
        mBannerAdView = new BannerAdView(context);
        mBannerAdView.setAdSize(calculateAdSize(mAdSize));
        mBannerAdView.setAdUnitId(mPlacementId);
        mBannerAdView.setBannerAdEventListener(mBannerAdEventListener);
        // Creating an ad targeting object.
        AdRequest.Builder builder = new AdRequest.Builder();
        if (!TextUtils.isEmpty(payload)) {
            Log.i(TAG, "payload: " + payload);
            builder.setBiddingData(payload);
        }
        mBannerAdView.loadAd(builder.build());
    }

    private final BannerAdEventListener mBannerAdEventListener = new BannerAdEventListener() {
        @Override
        public void onAdLoaded() {
            if (mLoadAdapterListener != null) {
                Log.i(TAG, "onAdLoaded: ");
                mTpBannerAd = new TPBannerAdImpl(null, mBannerAdView);
                mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
            }
        }

        @Override
        public void onAdFailedToLoad( AdRequestError adRequestError) {
            Log.i(TAG, "onAdFailedToLoad: ");
            TPError tpError = new TPError(NETWORK_NO_FILL);
            if (adRequestError != null) {
                int code = adRequestError.getCode();
                String description = adRequestError.getDescription();
                tpError.setErrorMessage(description);
                tpError.setErrorCode(code+"");
                Log.i(TAG, "code :" + code + ", description :" + description);
            }
            if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mTpBannerAd != null) {
                mTpBannerAd.adClicked();
            }
        }

        @Override
        public void onLeftApplication() {

        }

        @Override
        public void onReturnedToApplication() {
            Log.i(TAG, "onReturnedToApplication: ");
        }

        @Override
        public void onImpression(ImpressionData impressionData) {
            if (mTpBannerAd != null && onAdShow == 0) {
                Log.i(TAG, "onImpression: ");
                onAdShow = 1;
                mTpBannerAd.adShown();
            }
        }
    };

    @Override
    public void clean() {
        if (mBannerAdView != null) {
            mBannerAdView.setBannerAdEventListener(null);
            mBannerAdView.destroy();
            mBannerAdView = null;
        }
    }

    //1, 240x400
    //
    //2, 300x250
    //
    //3, 320x100
    //
    //4, 320x50
    //
    //5, 400x240
    //
    //6, 728x90
    private AdSize calculateAdSize(int adSize) {
        if (adSize == 1) {
            return AdSize.BANNER_240x400;
        } else if (adSize == 2) {
            return AdSize.BANNER_300x250;
        } else if (adSize == 3) {
            return AdSize.BANNER_320x100;
        } else if (adSize == 4) {
            return AdSize.BANNER_320x50;
        } else if (adSize == 5) {
            return AdSize.BANNER_400x240;
        } else if (adSize == 6) {
            return AdSize.BANNER_728x90;
        }
        return AdSize.BANNER_320x50;
    }

    @Override
    public String getNetworkName() {
        return YandexInitManager.TAG_YANDEX;
    }

    @Override
    public String getNetworkVersion() {
        return MobileAds.getLibraryVersion();
    }

    @Override
    public void getBiddingToken(Context context, Map<String, String> tpParams, Map<String, Object> localParams, final OnS2STokenListener onS2STokenListener) {
        BidderTokenLoader.loadBidderToken(context, new BidderTokenLoadListener() {
            @Override
            public void onBidderTokenLoaded(final String bidderToken) {
                Log.i(TAG, "onBidderTokenLoaded: ");
                onS2STokenListener.onTokenResult(bidderToken, null);
            }

            @Override
            public void onBidderTokenFailedToLoad(final String failureReason) {
                Log.i(TAG, "onBidderTokenFailedToLoad: ");
                onS2STokenListener.onTokenResult("", null);
            }
        });
    }
}
