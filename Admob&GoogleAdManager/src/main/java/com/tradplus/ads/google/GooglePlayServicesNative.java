package com.tradplus.ads.google;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusDataConstants;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

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
import static com.tradplus.ads.google.GoogleConstant.PAID_CURRENCYCODE;
import static com.tradplus.ads.google.GoogleConstant.PAID_PRECISION;
import static com.tradplus.ads.google.GoogleConstant.PAID_VALUEMICROS;

import androidx.annotation.NonNull;

class GooglePlayServicesNative extends TPNativeAdapter {

    private AdRequest request;
    private GoogleNativeAd mGoogleNativeAd;
    private String adUnitId;
    private static final String TAG = "AdmobNative";
    private Boolean mVideoMute = true;
    private String mAdSize;
    private int adChoicesPosition = NativeAdOptions.ADCHOICES_TOP_RIGHT;
    private String id;
    private String mContentUrls;
    private ArrayList<String> mNeighboringUrls;

    @Override
    public void loadCustomAd(final Context context, final Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) return;

        if (extrasAreValid(tpParams)) {
            adUnitId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            mAdSize = tpParams.get(AppKeyManager.ADSIZE + adUnitId);
            id = tpParams.get(GoogleConstant.ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(GoogleConstant.NATIVE_VIDEO_MUTE)) {
                mVideoMute = (Boolean) userParams.get(GoogleConstant.NATIVE_VIDEO_MUTE);
            }

            if (userParams.containsKey(GoogleConstant.ADCHOICES_POSITION)) {
                adChoicesPosition = (int) userParams.get(GoogleConstant.ADCHOICES_POSITION);
                Log.i(TAG, "adChoicesPosition: " + adChoicesPosition);
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
        }

        request = GoogleInitManager.getInstance().getAdmobAdRequest(userParams,mContentUrls,mNeighboringUrls);


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

    private void requestNative(Map<String, Object> userParams, Context context) {
        try {
            Object object = userParams.get(AppKeyManager.ADMOB_ADCHOICES);
            Log.i(TAG, "adchoices: " + object);
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

    private void loadAd(final Context context, String adUnitId) {
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

                                nativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                                    @Override
                                    public void onPaidEvent(@NonNull AdValue adValue) {
                                        Log.i(TAG, "onAdImpression: ");

                                        long valueMicros = adValue.getValueMicros();
                                        String currencyCode = adValue.getCurrencyCode();
                                        int precision = adValue.getPrecisionType();

                                        if (mGoogleNativeAd != null) {
                                            Map<String, Object> map = new HashMap<>();
                                            Long value = new Long(valueMicros);
                                            double dvalue = value.doubleValue();

                                            map.put(PAID_VALUEMICROS, dvalue / 1000 / 1000);
                                            map.put(PAID_CURRENCYCODE, currencyCode);
                                            map.put(PAID_PRECISION, precision);
                                            mGoogleNativeAd.onAdImpPaid(map);
                                        }


                                    }
                                });

                                mGoogleNativeAd = new GoogleNativeAd(context, nativeAd);
                                if (mLoadAdapterListener != null) {
                                    mLoadAdapterListener.loadAdapterLoaded(mGoogleNativeAd);
                                }
                            }
                        }).withAdListener(new AdListener() {
                    @Override
                    public void onAdClicked() {
                        Log.i(TAG, "onAdClicked: ");
                        if (mGoogleNativeAd != null)
                            mGoogleNativeAd.onAdViewClicked();
                    }

                    @Override
                    public void onAdImpression() {
                        Log.i(TAG, "onAdImpression: ");
                        if (mGoogleNativeAd != null) {
                            mGoogleNativeAd.onAdViewExpanded();
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.i(TAG, "onAdFailedToLoad: Code :" + loadAdError.getCode() + " , Message :" + loadAdError.getMessage());
                        if (mLoadAdapterListener != null)
                            switch (loadAdError.getCode()) {
                                case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                                    mLoadAdapterListener.loadAdapterLoadFailed(
                                            GoogleErrorUtil.getTradPlusErrorCode(new TPError(NATIVE_ADAPTER_CONFIGURATION_ERROR), loadAdError));
                                    break;
                                case AdRequest.ERROR_CODE_INVALID_REQUEST:
                                    mLoadAdapterListener.loadAdapterLoadFailed(
                                            GoogleErrorUtil.getTradPlusErrorCode(new TPError(NETWORK_INVALID_REQUEST), loadAdError));
                                    break;
                                case AdRequest.ERROR_CODE_NETWORK_ERROR:
                                    mLoadAdapterListener.loadAdapterLoadFailed(
                                            GoogleErrorUtil.getTradPlusErrorCode(new TPError(CONNECTION_ERROR), loadAdError));
                                    break;
                                case AdRequest.ERROR_CODE_NO_FILL:
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
