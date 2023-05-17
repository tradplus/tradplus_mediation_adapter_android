package com.tradplus.ads.google;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.HashMap;
import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;
import static com.tradplus.ads.google.GoogleConstant.PAID_CURRENCYCODE;
import static com.tradplus.ads.google.GoogleConstant.PAID_PRECISION;
import static com.tradplus.ads.google.GoogleConstant.PAID_VALUEMICROS;

import androidx.annotation.NonNull;


public class GooglePlayServicesInterstitialVideo extends TPRewardAdapter {

    private RewardedAd mRewardedAd;
    private RewardedInterstitialAd rewardedInterstitialAd;
    private int isRewardedInterstitialAd;
    private String mAdUnitId, customData, userid;
    private AdRequest request;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private Integer mVideoMute = 0;
    private String id;
    private static final String TAG = "AdmobRewardedVideo";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (extrasAreValid(tpParams)) {
            mAdUnitId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            isRewardedInterstitialAd = Integer.parseInt(tpParams.get(AppKeyManager.ADSOURCE_TYPE));
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
            id = tpParams.get(GoogleConstant.ID);
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.CUSTOM_DATA)) {
                customData = (String) userParams.get(AppKeyManager.CUSTOM_DATA);
                if (TextUtils.isEmpty(customData)) {
                    customData = "";
                }
            }

            if (userParams.containsKey(AppKeyManager.CUSTOM_USERID)) {
                userid = (String) userParams.get(AppKeyManager.CUSTOM_USERID);
                if (TextUtils.isEmpty(userid)) {
                    userid = "";
                }
            }

            if (userParams.containsKey(GoogleConstant.VIDEO_ADMOB_MUTE)) {
                mVideoMute = (int) userParams.get(GoogleConstant.VIDEO_ADMOB_MUTE);
            }

        }

        request = GoogleInitManager.getInstance().getAdmobAdRequest(userParams,null,null);

        GoogleInitManager.getInstance().initSDK(context, request, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                if (mVideoMute != 0) {
                    MobileAds.setAppMuted(mVideoMute == 1);
                }
                requestInterstitialVideo(context);
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

    private void requestInterstitialVideo(Context context) {
        try {

            if (isRewardedInterstitialAd == AppKeyManager.INTERACTION_TYPE) {
                // Rewarded interstitial
                // Use the test ad unit ID to load an ad.
                Log.i(TAG, "load RewardedInterstitialAd:");
                RewardedInterstitialAd.load(context, mAdUnitId, request, mRewardedInterstitialAdLoadCallback);
            } else {
                // Rewarded ads
                Log.i(TAG, "load RewardedAd:");
                RewardedAd.load(context, mAdUnitId, request, mRewardedAdLoadCallback);
            }

        } catch (Exception e) {
            Log.i("googleInterstitialVideo", "e: " + e.getLocalizedMessage());
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(UNSPECIFIED);
                tpError.setErrorMessage(e.getLocalizedMessage());
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

    }


    private final RewardedInterstitialAdLoadCallback mRewardedInterstitialAdLoadCallback = new RewardedInterstitialAdLoadCallback() {
        @Override
        public void onAdLoaded(RewardedInterstitialAd ad) {
            setFirstLoadedTime();
            rewardedInterstitialAd = ad;

            rewardedInterstitialAd.setOnPaidEventListener(new OnPaidEventListener() {
                @Override
                public void onPaidEvent(@NonNull AdValue adValue) {
                    Log.i(TAG, "onAdImpression: ");

                    long valueMicros = adValue.getValueMicros();
                    String currencyCode = adValue.getCurrencyCode();
                    int precision = adValue.getPrecisionType();

                    if (mShowListener != null) {
                        Map<String, Object> map = new HashMap<>();
                        Long value = new Long(valueMicros);
                        double dvalue = value.doubleValue();

                        map.put(PAID_VALUEMICROS, dvalue / 1000 / 1000);
                        map.put(PAID_CURRENCYCODE, currencyCode);
                        map.put(PAID_PRECISION, precision);


                        mShowListener.onAdImpPaid(map);
                    }
                }
            });
            Log.i(TAG, "onAdLoaded");
            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(ad);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }

            rewardedInterstitialAd.setFullScreenContentCallback(mFullScreenContentCallback);

        }

        @Override
        public void onAdFailedToLoad(LoadAdError loadAdError) {
            Log.i(TAG, "onAdFailedToLoad: code :" + loadAdError.getCode() + " , msg : " + loadAdError.getMessage());
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(GoogleErrorUtil.getTradPlusErrorCode(new TPError(NETWORK_NO_FILL), loadAdError));
                rewardedInterstitialAd = null;
            }
        }
    };

    private final RewardedAdLoadCallback mRewardedAdLoadCallback = new RewardedAdLoadCallback() {
        @Override
        public void onAdFailedToLoad(LoadAdError loadAdError) {
            // Handle the error.
            Log.i(TAG, "onAdFailedToLoad: code :" + loadAdError.getCode() + " , msg : " + loadAdError.getMessage());
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(GoogleErrorUtil.getTradPlusErrorCode(new TPError(NETWORK_NO_FILL), loadAdError));
                mRewardedAd = null;
            }
        }

        @Override
        public void onAdLoaded(RewardedAd rewardedAd) {
            Log.i(TAG, "onAdLoaded: ");
            setFirstLoadedTime();
            mRewardedAd = rewardedAd;

            mRewardedAd.setOnPaidEventListener(new OnPaidEventListener() {
                @Override
                public void onPaidEvent(@NonNull AdValue adValue) {
                    Log.i(TAG, "onAdImpression: ");

                    long valueMicros = adValue.getValueMicros();
                    String currencyCode = adValue.getCurrencyCode();
                    int precision = adValue.getPrecisionType();

                    if (mShowListener != null) {
                        Map<String, Object> map = new HashMap<>();
                        Long value = new Long(valueMicros);
                        double dvalue = value.doubleValue();

                        map.put(PAID_VALUEMICROS, dvalue / 1000 / 1000);
                        map.put(PAID_CURRENCYCODE, currencyCode);
                        map.put(PAID_PRECISION, precision);


                        mShowListener.onAdImpPaid(map);
                    }
                }
            });


            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(rewardedAd);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }

            mRewardedAd.setFullScreenContentCallback(mFullScreenContentCallback);
        }
    };

    private final FullScreenContentCallback mFullScreenContentCallback = new FullScreenContentCallback() {
        @Override
        public void onAdShowedFullScreenContent() {
            // Called when ad is shown.
            Log.i(TAG, "Ad was shown.");
            if (mShowListener != null) {
                mShowListener.onAdVideoStart();
            }

            if (mShowListener != null) {
                mShowListener.onAdShown();
            }

        }

        @Override
        public void onAdFailedToShowFullScreenContent(AdError adError) {
            // Called when ad fails to show.
            Log.i(TAG, "Ad failed to show, code : " + adError.getCode() + " , msg: " + adError.getMessage());
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
            }
        }

        @Override
        public void onAdDismissedFullScreenContent() {
            // Called when ad is dismissed.
            // Don't forget to set the ad reference to null so you
            // don't show the ad a second time.
            Log.i(TAG, "Ad was dismissed.");
            if (mShowListener != null) {
                mShowListener.onAdVideoEnd();
            }

            if (hasGrantedReward || alwaysRewardUser) {
                if (mShowListener != null) {
                    mShowListener.onReward();
                }
            }

            if (mShowListener != null) {
                mShowListener.onAdClosed();
            }
        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mShowListener != null) {
                mShowListener.onAdVideoClicked();
            }
        }
    };

    @Override
    public void showAd() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }
        if (isRewardedInterstitialAd == AppKeyManager.INTERACTION_TYPE) {

            // If you want to set the custom reward string, you must do so before showing the ad.
            if (!TextUtils.isEmpty(userid)) {
                Log.i(TAG, "RewardData: userid:" + userid + ",customData : " + customData);
                ServerSideVerificationOptions options = new ServerSideVerificationOptions.Builder()
                        .setCustomData(TextUtils.isEmpty(customData) ? "" : customData)
                        .setUserId(userid)
                        .build();
                rewardedInterstitialAd.setServerSideVerificationOptions(options);
            }

            // Rewarded interstitial
            Log.i(TAG, "show RewardedInterstitialAd: ");
            rewardedInterstitialAd.show(activity, mOnUserEarnedRewardListener);

        } else {
            // Rewarded
            if (mRewardedAd != null) {
                // If you want to set the custom reward string, you must do so before showing the ad.
                if (!TextUtils.isEmpty(userid)) {
                    Log.i(TAG, "RewardData: customData : " + customData);
                    ServerSideVerificationOptions options = new ServerSideVerificationOptions.Builder()
                            .setCustomData(TextUtils.isEmpty(customData) ? "" : customData)
                            .setUserId(userid)
                            .build();
                    mRewardedAd.setServerSideVerificationOptions(options);
                }
                Log.i(TAG, "show RewardedAd:");
                mRewardedAd.show(activity, mOnUserEarnedRewardListener);

            } else {
                Log.i("TAG", "The rewarded ad wasn't loaded yet.");
                if (mShowListener != null)
                    mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
            }

        }
    }


    @Override
    public boolean isReady() {
        if (isRewardedInterstitialAd != AppKeyManager.INTERACTION_TYPE) {
            if (mRewardedAd == null) return false;
            boolean isMainThread = Looper.myLooper() == Looper.getMainLooper();
            if (isMainThread) {
                return !isAdsTimeOut();
            } else return !isAdsTimeOut();
        } else {
            if (rewardedInterstitialAd == null) return false;
            boolean isMainThread = Looper.myLooper() == Looper.getMainLooper();
            if (isMainThread) {
                return !isAdsTimeOut();
            } else return !isAdsTimeOut();
        }
    }

    private final OnUserEarnedRewardListener mOnUserEarnedRewardListener = new OnUserEarnedRewardListener() {
        @Override
        public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
            Log.i(TAG, "onUserEarnedReward: ");
            hasGrantedReward = true;

        }
    };

    @Override
    public void clean() {
        if (mRewardedAd != null) {
            mRewardedAd.setFullScreenContentCallback(null);
            mRewardedAd = null;
        }

        if (rewardedInterstitialAd != null) {
            rewardedInterstitialAd.setFullScreenContentCallback(null);
            rewardedInterstitialAd = null;
        }
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

}
