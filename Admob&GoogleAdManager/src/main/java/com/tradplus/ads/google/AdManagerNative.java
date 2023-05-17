package com.tradplus.ads.google;

import static com.google.android.gms.ads.AdSize.BANNER;
import static com.google.android.gms.ads.AdSize.FLUID;
import static com.google.android.gms.ads.AdSize.LARGE_BANNER;
import static com.google.android.gms.ads.AdSize.LEADERBOARD;
import static com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE;
import static com.google.android.gms.ads.nativead.NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY;
import static com.google.android.gms.ads.nativead.NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE;
import static com.google.android.gms.ads.nativead.NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT;
import static com.google.android.gms.ads.nativead.NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_SQUARE;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.CONNECTION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NATIVE_ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_INVALID_REQUEST;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusDataConstants;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

class AdManagerNative extends TPNativeAdapter {

    private AdManagerAdRequest request;
    private AdManagerAdView mAdManagerAdView;
    private AdManagerNativeAd mAdManagerNativeAd;
    private String adUnitId;
    private int mIsTemplateRending;
    private static final String TAG = "GAMNative";
    private Boolean mVideoMute = true;
    private String mAdSize;
    private Integer adSize;
    private int adChoicesPosition = NativeAdOptions.ADCHOICES_TOP_RIGHT;
    private String id;
    private String mContentUrls;
    private ArrayList<String> mNeighboringUrls;

    @Override
    public void loadCustomAd(final Context context, final Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) return;

        String template;
        if (extrasAreValid(tpParams)) {
            adUnitId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            template = tpParams.get(AppKeyManager.IS_TEMPLATE_RENDERING);
            mAdSize = tpParams.get(AppKeyManager.ADSIZE + adUnitId);
            id = tpParams.get(GoogleConstant.ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (!TextUtils.isEmpty(template)) {
            mIsTemplateRending = Integer.parseInt(template);
        }


        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(GoogleConstant.NATIVE_VIDEO_MUTE)) {
                mVideoMute = (Boolean) userParams.get(GoogleConstant.NATIVE_VIDEO_MUTE);
            }

            if (userParams.containsKey(GoogleConstant.NATIVE_EXPRESS_SIZE)) {
                adSize = (Integer) userParams.get(GoogleConstant.NATIVE_EXPRESS_SIZE);
            }

            if (userParams.containsKey(GoogleConstant.ADCHOICES_POSITION)) {
                adChoicesPosition = (int) userParams.get(GoogleConstant.ADCHOICES_POSITION);
                Log.i(TAG, "adChoicesPosition: " + adChoicesPosition);
            }

        }

        if (userParams.containsKey(GoogleConstant.GOOGLE_CONTENT_URLS)) {
            Object contentUrl = userParams.get(GoogleConstant.GOOGLE_CONTENT_URLS);
            if (contentUrl instanceof ArrayList) {
                ArrayList<String> url = (ArrayList<String>) contentUrl;
                int size = url.size();
                Log.i(TAG, "contentUrl size : " + size);

                try {
                    if (size == 1) {
                        mContentUrls = url.get(0);
                    }

                    if (size >= 2) {
                        mNeighboringUrls = new ArrayList<String>();
                        mNeighboringUrls.addAll(url);
                    }
                }catch(Throwable e) {
                    e.printStackTrace();
                }

            }
        }

        request = AdManagerInit.getInstance().getAdmobAdRequest(userParams,mContentUrls,mNeighboringUrls);

        GoogleInitManager.getInstance().initSDK(context, request, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestNative(userParams, context);
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

    private void requestNative(Map<String, Object> userParams, final Context context) {
        if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES) {
            Log.i(TAG, "requestExpressNative: adUnitId:" + adUnitId);
            final AdManagerAdView adManagerAdView = new AdManagerAdView(context);
            adManagerAdView.setAdUnitId(adUnitId);
            adManagerAdView.setAdSize(calculateAdSize(adSize));
            adManagerAdView.setAdListener(new AdListener() {
                @Override
                public void onAdClicked() {
                    Log.i(TAG, "onAdClicked: ");
                    if (mAdManagerNativeAd != null)
                        mAdManagerNativeAd.onAdViewClicked();
                }

                @Override
                public void onAdClosed() {
                    Log.i(TAG, "onAdClosed: ");
                    if (mAdManagerNativeAd != null) {
                        mAdManagerNativeAd.onAdViewClosed();
                    }
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.i(TAG, "onAdFailedToLoad: Code :" + loadAdError.getCode() + ", msg : " + loadAdError.getMessage());
                    if (mLoadAdapterListener != null) {
                        TPError tpError = new TPError(NETWORK_NO_FILL);
                        tpError.setErrorCode(loadAdError.getCode() + "");
                        tpError.setErrorMessage(loadAdError.getMessage());
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }
                }

                @Override
                public void onAdImpression() {
                    Log.i(TAG, "onAdImpression: ");
                    if (mAdManagerNativeAd != null) {
                        mAdManagerNativeAd.onAdViewExpanded();
                    }
                }

                @Override
                public void onAdLoaded() {
                    Log.i(TAG, "onAdLoaded: ");
                    mAdManagerNativeAd = new AdManagerNativeAd(adManagerAdView, TPBaseAd.AD_TYPE_NATIVE_EXPRESS);
                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoaded(mAdManagerNativeAd);
                    }
                }

                @Override
                public void onAdOpened() {
                    super.onAdOpened();
                }
            });
            adManagerAdView.loadAd(request);
        } else {
            try {
                Object object = userParams.get(AppKeyManager.ADMOB_ADCHOICES);
                Log.i(TAG, "adchoices:" + object);
                if (object instanceof Integer) {
                    adChoicesPosition = (int) object;
                }
            } catch (Exception e) {
                Log.i(TAG, "requestNative: Exception : " + e.getLocalizedMessage());
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(UNSPECIFIED);
                    tpError.setErrorMessage(e.getLocalizedMessage());
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }

            loadAd(context, adUnitId);
        }
    }

    private void loadAd(final Context context, String adUnitId) {
        Log.i(TAG, "requestNative adUnitId:" + adUnitId);
        final AdLoader.Builder builder = new AdLoader.Builder(context, adUnitId);

        final NativeAdOptions.Builder optionsBuilder = new NativeAdOptions.Builder();

        optionsBuilder.setRequestMultipleImages(false);

        optionsBuilder.setReturnUrlsForImageAssets(false);

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(mVideoMute)
                .build();

        optionsBuilder.setVideoOptions(videoOptions);

        Log.i(TAG, "adchoices adChoicesPosition: " + adChoicesPosition);
        optionsBuilder.setAdChoicesPlacement(adChoicesPosition);

        optionsBuilder.setMediaAspectRatio(calculateAdRatio(mAdSize));

        NativeAdOptions adOptions = optionsBuilder.build();

        AdLoader adLoader =
                builder.forNativeAd(
                        new NativeAd.OnNativeAdLoadedListener() {
                            @Override
                            public void onNativeAdLoaded(NativeAd nativeAd) {
                                if (!isValidUnifiedAd(nativeAd)) {

                                    if (mLoadAdapterListener != null)
                                        mLoadAdapterListener.loadAdapterLoadFailed(
                                                new TPError(NETWORK_NO_FILL));
                                    return;
                                }
                                Log.i(TAG, "onNativeAdLoaded: ");
                                mAdManagerNativeAd = new AdManagerNativeAd(context, nativeAd, TPBaseAd.AD_TYPE_NORMAL_NATIVE);
                                if (mLoadAdapterListener != null) {
                                    mLoadAdapterListener.loadAdapterLoaded(mAdManagerNativeAd);
                                }
                            }
                        }).withAdListener(new AdListener() {
                    @Override
                    public void onAdClicked() {
                        Log.i(TAG, "onAdClicked: ");
                        if (mAdManagerNativeAd != null)
                            mAdManagerNativeAd.onAdViewClicked();
                    }

                    @Override
                    public void onAdImpression() {
                        Log.i(TAG, "onAdImpression: ");
                        if (mAdManagerNativeAd != null) {
                            mAdManagerNativeAd.onAdViewExpanded();
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.i(TAG, "onAdFailedToLoad: Code :" + loadAdError.getCode() + " , Message :" + loadAdError.getMessage());
                        if (mLoadAdapterListener != null)
                            switch (loadAdError.getCode()) {
                                case AdManagerAdRequest.ERROR_CODE_INTERNAL_ERROR:
                                    mLoadAdapterListener.loadAdapterLoadFailed(
                                            GoogleErrorUtil.getTradPlusErrorCode(new TPError(NATIVE_ADAPTER_CONFIGURATION_ERROR), loadAdError));
                                    break;
                                case AdManagerAdRequest.ERROR_CODE_INVALID_REQUEST:
                                    mLoadAdapterListener.loadAdapterLoadFailed(
                                            GoogleErrorUtil.getTradPlusErrorCode(new TPError(NETWORK_INVALID_REQUEST), loadAdError));
                                    break;
                                case AdManagerAdRequest.ERROR_CODE_NETWORK_ERROR:
                                    mLoadAdapterListener.loadAdapterLoadFailed(
                                            GoogleErrorUtil.getTradPlusErrorCode(new TPError(CONNECTION_ERROR), loadAdError));
                                    break;
                                case AdManagerAdRequest.ERROR_CODE_NO_FILL:
                                    mLoadAdapterListener.loadAdapterLoadFailed(
                                            GoogleErrorUtil.getTradPlusErrorCode(new TPError(NETWORK_NO_FILL), loadAdError));
                                    break;
                                default:
                                    mLoadAdapterListener.loadAdapterLoadFailed(
                                            GoogleErrorUtil.getTradPlusErrorCode(new TPError(UNSPECIFIED), loadAdError));
                            }
                    }
                }).withNativeAdOptions(adOptions).build();


        adLoader.loadAd(request);
    }

    private boolean isValidUnifiedAd(NativeAd unifiedNativeAd) {
        return (unifiedNativeAd.getHeadline() != null && unifiedNativeAd.getBody() != null
                && unifiedNativeAd.getImages() != null && unifiedNativeAd.getImages().size() > 0
                && unifiedNativeAd.getImages().get(0) != null
                && unifiedNativeAd.getIcon() != null
                && unifiedNativeAd.getCallToAction() != null);
    }


    @Override
    public void clean() {

    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(id);
    }

    @Override
    public String getNetworkVersion() {
        VersionInfo version = MobileAds.getVersion();
        int majorVersion = version.getMajorVersion();
        int minorVersion = version.getMinorVersion();
        int microVersion = version.getMicroVersion();
        return majorVersion + "." + minorVersion + "." + microVersion + "";
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.AD_PLACEMENT_ID);
    }

    private AdSize calculateAdSize(int adSize) {
        Log.i(TAG, "calculateAdSize: " + adSize);
        if (2 == adSize) {
            return LARGE_BANNER;
        } else if (3 == adSize) {
            return MEDIUM_RECTANGLE;
        } else if (4 == adSize) {
            return FLUID;
        } else if (5 == adSize) {
            return LEADERBOARD;
        } else {
            return BANNER;
        }
    }


    // Native素材尺寸
    private int calculateAdRatio(String adSize) {
        Log.i(TAG, "calculateAdRatio: " + adSize);
        if (TradPlusDataConstants.MEDIUMRECTANGLE.equals(adSize)) {
            return NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE;
        } else if (TradPlusDataConstants.FULLSIZEBANNER.equals(adSize)) {
            return NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT;
        } else if (TradPlusDataConstants.LEADERBOAD.equals(adSize)) {
            return NATIVE_MEDIA_ASPECT_RATIO_SQUARE;
        } else {
            return NATIVE_MEDIA_ASPECT_RATIO_ANY;
        }
    }
}
