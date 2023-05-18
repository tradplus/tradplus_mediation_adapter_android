package com.tradplus.ads.fpangolin;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdLoadListener;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerRequest;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerSize;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.adapter.TPInitMediation.INIT_STATE_BIDDING;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.LARGEBANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.MEDIUMRECTANGLE;

public class TouTiaoBanner extends TPBannerAdapter {

    private String mPlacementId;
    private PAGBannerAd mBannerAd;
    private TPBannerAdImpl mTpBannerAd;
    private String mAdSize = "1"; // 默认值1，320 * 50
    private static final String TAG = "PangleBanner";
    private String payload;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            if (tpParams.containsKey(AppKeyManager.ADSIZE + mPlacementId)) {
                mAdSize = tpParams.get(AppKeyManager.ADSIZE + mPlacementId);
            }
        }

        setAdHeightAndWidthByUser(userParams);


        final String ttAdm = payload;
        PangleInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestBanner(ttAdm);
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

    private void requestBanner(String bidAdm) {

        PAGBannerRequest bannerRequest = new PAGBannerRequest(calculateAdSize(mAdSize));

        if (!TextUtils.isEmpty(bidAdm)) {
            bannerRequest.setAdString(bidAdm);
        }

        PAGBannerAd.loadAd(mPlacementId, bannerRequest, new PAGBannerAdLoadListener() {
            @Override
            public void onError(int code, String message) {
                Log.i(TAG, "onError: code ：" + code + ", message ：" + message);
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(PangleErrorUtil.getTradPlusErrorCode(code, message));
                }
            }

            @Override
            public void onAdLoaded(PAGBannerAd bannerAd) {
                if (bannerAd == null) {
                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoadFailed(new TPError(UNSPECIFIED));
                    }
                    Log.i(TAG, "onAdLoaded, but bannerAd == null");
                    return;
                }
                mBannerAd = bannerAd;

                View bannerView = bannerAd.getBannerView();
                if (bannerView == null) {
                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoadFailed(new TPError(UNSPECIFIED));
                    }
                    Log.i(TAG, "onAdLoaded, but bannerView == null");
                    return;
                }

                bannerAd.setAdInteractionListener(new PAGBannerAdInteractionListener() {
                    @Override
                    public void onAdShowed() {
                        Log.i(TAG, "onAdShowed: ");
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
                    public void onAdDismissed() {
                        Log.i(TAG, "onAdDismissed: ");
                        if (mTpBannerAd != null) {
                            mTpBannerAd.adClosed();
                        }
                    }
                });

                mTpBannerAd = new TPBannerAdImpl(bannerAd, bannerView);
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
                }

            }
        });
    }

    @Override
    public void clean() {
        if (mBannerAd != null) {
            mBannerAd.destroy();
            mBannerAd = null;
        }
    }

    private PAGBannerSize calculateAdSize(String adSize) {
        if (adSize.equals(BANNER)) {
            return PAGBannerSize.BANNER_W_320_H_50; //320 * 50
        } else if (adSize.equals(LARGEBANNER)) {
            return PAGBannerSize.BANNER_W_300_H_250; //320 * 250
        } else if (adSize.equals(MEDIUMRECTANGLE)) {
            return PAGBannerSize.BANNER_W_728_H_90; // 729 * 90
        }
        return PAGBannerSize.BANNER_W_320_H_50;
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_PANGLE);
    }

    @Override
    public String getNetworkVersion() {
        return PAGSdk.getSDKVersion();
    }

    @Override
    public void getBiddingToken(Context context, Map<String, String> tpParams, Map<String, Object> localParams, final OnS2STokenListener onS2STokenListener) {
        final boolean initSuccess = PAGSdk.isInitSuccess();
        PangleInitManager.getInstance().initSDK(context, localParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                if (!initSuccess) {
                    PangleInitManager.getInstance().sendInitRequest(true, INIT_STATE_BIDDING);
                }
                onS2STokenListener.onTokenResult(PAGSdk.getBiddingToken(), null);

            }

            @Override
            public void onFailed(String code, String msg) {
                PangleInitManager.getInstance().sendInitRequest(false, INIT_STATE_BIDDING);
                onS2STokenListener.onTokenResult("", null);
            }
        });
    }

}
