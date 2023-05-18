package com.tradplus.ads.bigo;

import static com.tradplus.ads.base.adapter.TPInitMediation.INIT_STATE_BIDDING;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.annotation.NonNull;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.Map;

import sg.bigo.ads.BigoAdSdk;
import sg.bigo.ads.api.AdError;
import sg.bigo.ads.api.AdInteractionListener;
import sg.bigo.ads.api.AdLoadListener;
import sg.bigo.ads.api.AdSize;
import sg.bigo.ads.api.BannerAd;
import sg.bigo.ads.api.BannerAdLoader;
import sg.bigo.ads.api.BannerAdRequest;

public class BigoBanner extends TPBannerAdapter {

    private static final String TAG = "BigoBanner";
    private String mPlacementId;
    private TPBannerAdImpl mTpBannerAd;
    private BannerAd mBannerAd;
    private String serverBiddingAdm;
    private Integer mAdSize = 1; // 320 * 50

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);

            String payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(payload)) {
                serverBiddingAdm = payload;
            }

            String adSize = tpParams.get(AppKeyManager.ADSIZE + mPlacementId);
            if (!TextUtils.isEmpty(adSize)) {
                mAdSize = Integer.parseInt(adSize);
                Log.i(TAG, "AdSize: " + mAdSize);
            }

        }

        BigoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestBanner();
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

    private void requestBanner() {
        BannerAdRequest.Builder builder = new BannerAdRequest.Builder().withSlotId(mPlacementId).withAdSizes(calculateAdSize(mAdSize));

        BannerAdLoader bannerAdLoader = new BannerAdLoader.Builder().withAdLoadListener(new AdLoadListener<BannerAd>() {
            @Override
            public void onError(@NonNull AdError error) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                if (error != null) {
                    int code = error.getCode();
                    String message = error.getMessage();
                    tpError.setErrorMessage(message);
                    tpError.setErrorCode(code + "");
                    Log.i(TAG, "code :" + code + ", message :" + message);
                }

                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }

            @Override
            public void onAdLoaded(@NonNull BannerAd ad) {
                mBannerAd = ad;
                mBannerAd.setAdInteractionListener(adInteractionListener);

                View view = ad.adView();
                if (view == null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage("view == null");
                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }
                    return;
                }

                if (mLoadAdapterListener != null) {
                    Log.i(TAG, "onAdLoaded: ");
                    if (mTpBannerAd == null) {
                        mTpBannerAd = new TPBannerAdImpl(null, view);
                    }
                    mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
                }
            }
        }).build();

        if (!TextUtils.isEmpty(serverBiddingAdm)) {
            builder.withBid(serverBiddingAdm);
        }

        bannerAdLoader.loadAd(builder.build());

    }

    private final AdInteractionListener adInteractionListener = new AdInteractionListener() {
        @Override
        public void onAdError(@NonNull AdError error) {
            TPError tpError = new TPError(SHOW_FAILED);
            if (error != null) {
                int code = error.getCode();
                String message = error.getMessage();
                tpError.setErrorMessage(message);
                tpError.setErrorCode(code + "");
                Log.i(TAG, "code :" + code + ", message :" + message);
            }

            if (mTpBannerAd != null) {
                mTpBannerAd.onAdShowFailed(tpError);
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
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mTpBannerAd != null) {
                mTpBannerAd.adClicked();
            }
        }

        @Override
        public void onAdOpened() {

        }

        @Override
        public void onAdClosed() {
            Log.i(TAG, "onAdClosed: ");
            if (mTpBannerAd != null) {
                mTpBannerAd.adClosed();
            }
        }
    };

    @Override
    public void clean() {
        if (mBannerAd != null) {
            mBannerAd.setAdInteractionListener(null);
            mBannerAd.destroy();
            mBannerAd = null;
        }
    }

    private AdSize calculateAdSize(int adSize) {
        if (adSize == 1) {
            return AdSize.BANNER; // 320x50
        } else if (adSize == 2) {
            return AdSize.MEDIUM_RECTANGLE; // 300x250
        } else if (adSize == 3) {
            return AdSize.LARGE_BANNER; // 300x100
        } else if (adSize == 4) {
            return AdSize.LARGE_RECTANGLE; // 336x280
        }
        return AdSize.BANNER;
    }

    @Override
    public String getNetworkName() {
        return "Bigo";
    }

    @Override
    public String getNetworkVersion() {
        return BigoAdSdk.getSDKVersionName();
    }

    @Override
    public void getBiddingToken(Context context, Map<String, String> tpParams, Map<String, Object> localParams, final OnS2STokenListener onS2STokenListener) {
        // token在SDK未初始化或者异常情况下可能为空，开发者需要进⾏空值校验
        // 成功初始化BigoAds SDK后 获取token
        boolean initialized = BigoAdSdk.isInitialized();
        BigoInitManager.getInstance().initSDK(context, localParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                String bidderToken = BigoAdSdk.getBidderToken();
                boolean tokenEmpty = TextUtils.isEmpty(bidderToken);
                Log.i(TAG, "onSuccess bidderToken isEmpty " + tokenEmpty);
                if (!initialized) {
                    // 第一次初始化打印250
                    BigoInitManager.getInstance().sendInitRequest(true, INIT_STATE_BIDDING);
                }
                onS2STokenListener.onTokenResult(!tokenEmpty ? bidderToken : "",null);
            }

            @Override
            public void onFailed(String code, String msg) {
                BigoInitManager.getInstance().sendInitRequest(false, INIT_STATE_BIDDING);
                onS2STokenListener.onTokenResult("",null);
            }
        });
    }

}
