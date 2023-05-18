package com.tradplus.ads.facebook;

import static android.view.Gravity.CENTER;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;
import static com.tradplus.ads.base.util.AppKeyManager.PLACEMENT_AD_TYPE;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNERZERO;
import static com.tradplus.ads.base.util.TradPlusDataConstants.LARGEBANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.MEDIUMRECTANGLE;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSettings;
import com.facebook.ads.BidderTokenProvider;
import com.facebook.ads.BuildConfig;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdBase;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.NativeAdView;
import com.facebook.ads.NativeBannerAd;
import com.facebook.ads.NativeBannerAdView;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.common.util.DeviceUtils;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;


public class FacebookNative extends TPNativeAdapter {

    private NativeAd mFacebookNative;
    private FacebookNativeAd mFacebookNativeAd;
    private NativeBannerAd mNativeBannerAd;
    private String payload;
    private String placementId, mAdSize;
    private String secType;
    private boolean mNeedDownloadImg = false;
    private int mIsTemplateRending;
    private int mAdWidth, mAdHeight;
    private static final String TAG = "FacebookNative";
    private int isNative;
    private boolean mSingleIcon;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        String template;
        if (serverExtrasAreValid(tpParams)) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            template = tpParams.get(AppKeyManager.IS_TEMPLATE_RENDERING);
            secType = tpParams.get(AppKeyManager.ADTYPE_SEC);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(template)) {
                mIsTemplateRending = Integer.parseInt(template);
            }

            // 模版NativeBanner支持下发AdSize
            String adSize = tpParams.get(AppKeyManager.ADSIZE + placementId);
            Log.i(TAG, "adSize: " + adSize);
            if (!TextUtils.isEmpty(adSize) && !BANNERZERO.equals(adSize)) {
                mAdSize = adSize;
            } else {
                mAdSize = BANNER;
            }

            if (tpParams.containsKey(PLACEMENT_AD_TYPE)) {
                String isNativeBanner = tpParams.get(PLACEMENT_AD_TYPE);
                if (!TextUtils.isEmpty(isNativeBanner)) {
                    isNative = Integer.parseInt(isNativeBanner);
                }
            }

        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (userParams.size() > 0) {
//            Log.i(TAG, "suportGDPR ccpa: " + userParams.get(AppKeyManager.KEY_CCPA) + ":COPPA:" + localExtras.get(AppKeyManager.KEY_COPPA));
//            if (localExtras.containsKey(AppKeyManager.KEY_CCPA)) {
//                boolean cppa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
//                if (cppa) {
//                    AdSettings.setDataProcessingOptions(new String[]{});
//                } else {
//                    AdSettings.setDataProcessingOptions(new String[]{"LDU"}, 1, 1000);
//                }
//
//            } else {
//                AdSettings.setDataProcessingOptions(new String[]{"LDU"}, 0, 0);
//            }

            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + coppa);
                if (coppa) {
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(NETWORK_NO_FILL));
                    return;
                }
            }

            if (userParams.containsKey(DataKeys.AD_WIDTH)) {
                mAdWidth = (int) userParams.get(DataKeys.AD_WIDTH);
            }
            if (userParams.containsKey(DataKeys.AD_HEIGHT)) {
                mAdHeight = (int) userParams.get(DataKeys.AD_HEIGHT);
            }

            Log.i(TAG, "Width :" + mAdWidth + ", Height :" + mAdHeight);

            if (userParams.containsKey(DataKeys.DOWNLOAD_IMG)) {
                String downLoadImg = (String) userParams.get(DataKeys.DOWNLOAD_IMG);
                if (downLoadImg.equals("true")) {
                    mNeedDownloadImg = true;
                }

            }

            if (userParams.containsKey(FaceBookConstant.META_SINGLEICON)) {
                Object singleIcon = userParams.get(FaceBookConstant.META_SINGLEICON);
                if (singleIcon instanceof Boolean) {
                    mSingleIcon = (boolean) singleIcon;
                }
            }

            if (mAdHeight == 0 || mAdWidth == 0) {
                mAdHeight = 320;
                mAdWidth = 340;
            }
        }

        FacebookInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestAd(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });


    }

    private void requestAd(Context context) {
        if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES) {
            // 模版
            if (secType == null || !secType.equals(AppKeyManager.NATIVE_TYPE_NATIVEBANNER)) {
                if (isNative == 1) {
                    loadNativeBannerTemplateAd(context, placementId);
                } else {
                    loadNativeTemplateAd(context, placementId);
                }
            } else {
                loadNativeBannerTemplateAd(context, placementId);
            }
        } else {
            if (secType == null || !secType.equals(AppKeyManager.NATIVE_TYPE_NATIVEBANNER)) {
                if (isNative == 1) {
                    loadNativeBannerAd(context, placementId);
                } else {
                    loadNativeAd(context, placementId);
                }
            } else {
                loadNativeBannerAd(context, placementId);
            }
        }
    }

    private void loadNativeBannerTemplateAd(Context context, String placementId) {
        Log.i(TAG, "loadNativeBannerTemplateAd: ");
        mNativeBannerAd = new NativeBannerAd(context, placementId);
        mNativeBannerAd.loadAd(
                mNativeBannerAd.buildLoadAdConfig()
                        .withAdListener(nativeAdListener)
                        .withBid(TextUtils.isEmpty(payload) ? "" : payload)
                        .build());

    }

    private void loadNativeTemplateAd(Context context, String placementId) {
        Log.i(TAG, "loadNativeTemplateAd: ");
        mFacebookNative = new NativeAd(context, placementId);
        mFacebookNative.loadAd(
                mFacebookNative.buildLoadAdConfig()
                        .withAdListener(nativeAdListener)
                        .withMediaCacheFlag(NativeAdBase.MediaCacheFlag.ALL)
                        .withBid(TextUtils.isEmpty(payload) ? "" : payload)
                        .build());
    }

    private void loadNativeBannerAd(final Context context, String placementId) {
        mNativeBannerAd = new NativeBannerAd(context, placementId);
        mNativeBannerAd.loadAd(
                mNativeBannerAd.buildLoadAdConfig()
                        .withBid(TextUtils.isEmpty(payload) ? "" : payload)
                        .withMediaCacheFlag(NativeAdBase.MediaCacheFlag.ALL)
                        .withAdListener(nativeAdListener)
                        .build());
    }

    private void loadNativeAd(Context context, String placementId) {
        mFacebookNative = new NativeAd(context, placementId);
        mFacebookNative.loadAd(mFacebookNative.buildLoadAdConfig()
                .withBid(TextUtils.isEmpty(payload) ? "" : payload)
                .withAdListener(nativeAdListener).build());
    }


    NativeAdListener nativeAdListener = new NativeAdListener() {
        @Override
        public void onError(Ad ad, AdError adError) {
            Log.i(TAG, "onError: ErrorCode : " + adError.getErrorCode() + ", ErrorMessage : " + adError.getErrorMessage());
            if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(FacebookErrorUtil.getTradPlusErrorCode(adError));

        }

        @Override
        public void onAdLoaded(Ad ad) {
            Context context = GlobalTradPlus.getInstance().getContext();
            if (context == null) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                    tpError.setErrorMessage("context == null");
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
                return;
            }

            if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES) {
                if (!AppKeyManager.NATIVE_TYPE_NATIVEBANNER.equals(secType)) {
                    // Render the Native Ad Template
                    if(isNative == 1){
                        AdLoadedTemplateNativeBanner(context);

                    }else {
                        AdLoadedTemplateNative(context);
                    }
                } else {
                    // Render the Native Banner Ad Template
                    AdLoadedTemplateNativeBanner(context);
                }

            } else {
                if (!AppKeyManager.NATIVE_TYPE_NATIVEBANNER.equals(secType)) {
                    if(isNative == 1){
                        AdLoadedNativeBanner(context);
                    }else{
                        AdLoadedNative(context);
                    }

                } else {
                    AdLoadedNativeBanner(context);
                }
            }

        }

        @Override
        public void onAdClicked(Ad ad) {
            Log.i(TAG, "onAdClicked: ");
            if (mFacebookNativeAd != null)
                mFacebookNativeAd.onAdViewClicked();
        }

        @Override
        public void onLoggingImpression(Ad ad) {
            Log.i(TAG, "onLoggingImpression: ");
            if (mFacebookNativeAd != null)
                mFacebookNativeAd.onAdViewExpanded();
        }

        @Override
        public void onMediaDownloaded(Ad ad) {

        }
    };

    private void AdLoadedNativeBanner(Context context) {
        // 自渲染 NativeBanner
        if (mNativeBannerAd != null) {
            mNativeBannerAd.unregisterView();

            mFacebookNativeAd = new FacebookNativeAd(context, mNativeBannerAd, mSingleIcon);

            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoaded(mFacebookNativeAd);
            }
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(UNSPECIFIED));
            }
        }
    }

    private void AdLoadedNative(Context context) {
        // 自渲染 native
        if (mFacebookNative != null) {
            mFacebookNative.unregisterView();

            mFacebookNativeAd = new FacebookNativeAd(context, mFacebookNative, mSingleIcon);

            downloadAndCallback(mFacebookNativeAd, mNeedDownloadImg);
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(UNSPECIFIED));
            }
        }
    }

    private void AdLoadedTemplateNativeBanner(Context context) {
        // 模版NativeBanner
        if (mNativeBannerAd != null) {
            if (mNativeBannerAd.isAdLoaded() && !mNativeBannerAd.isAdInvalidated()) {

                Log.i(TAG, "TemplateNativeBannerAdLoaded: ");
                View adView = NativeBannerAdView.render(context, mNativeBannerAd, calculateAdSize(mAdSize));
                mFacebookNativeAd = new FacebookNativeAd(adView, AppKeyManager.TEMPLATE_RENDERING_YES);

                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoaded(mFacebookNativeAd);
                }
            } else {
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(UNSPECIFIED));
                }
            }
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(UNSPECIFIED));
            }
        }
    }

    private void AdLoadedTemplateNative(Context context) {
        // 模版Native
        if (mFacebookNative != null) {
            Log.i(TAG, "TemplateNativeAdLoaded: ");

            View adView = NativeAdView.render(context, mFacebookNative);
            // Add the Native Ad View to your ad container.
            // The recommended dimensions for the ad container are:
            // Width: 280dp - 500dp
            // Height: 250dp - 500dp
            if (mAdWidth != 0 && mAdHeight != 0) {
                RelativeLayout relativeLayout = new RelativeLayout(context);
                relativeLayout.addView(adView, new RelativeLayout.LayoutParams(DeviceUtils.dip2px(context, mAdWidth)
                        , DeviceUtils.dip2px(context, mAdHeight)));
                relativeLayout.setGravity(CENTER);

                mFacebookNativeAd = new FacebookNativeAd(relativeLayout, AppKeyManager.TEMPLATE_RENDERING_YES);
            } else {
                mFacebookNativeAd = new FacebookNativeAd(adView, AppKeyManager.TEMPLATE_RENDERING_YES);
            }

            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoaded(mFacebookNativeAd);
            }

        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(UNSPECIFIED));
            }
        }
    }

    @Override
    public void clean() {
        AdSettings.clearTestDevices();
        if (mFacebookNativeAd != null)
            mFacebookNativeAd.clean();
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_FACEBOOK);
    }

    @Override
    public String getNetworkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    private NativeBannerAdView.Type calculateAdSize(String adSize) {
        if (adSize.equals(BANNER)) {
            return NativeBannerAdView.Type.HEIGHT_50;
        } else if (adSize.equals(LARGEBANNER)) {
            return NativeBannerAdView.Type.HEIGHT_100;
        } else if (adSize.equals(MEDIUMRECTANGLE)) {
            return NativeBannerAdView.Type.HEIGHT_120;
        }
        return NativeBannerAdView.Type.HEIGHT_50;
    }

    private boolean serverExtrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    @Override
    public String getBiddingToken() {
        try {
            return BidderTokenProvider.getBidderToken(GlobalTradPlus.getInstance().getContext());
        } catch (Exception e) {

        }
        return null;
    }
}
