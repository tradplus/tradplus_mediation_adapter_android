package com.tradplus.ads.google;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED;

public class GoogleInitManager extends TPInitMediation {

    private static final String TAG = "Admob";
    private static GoogleInitManager sInstance;
    private String id;

    public synchronized static GoogleInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new GoogleInitManager();
        }
        return sInstance;
    }

    public void initSDK(Context context, AdRequest request, Map<String, Object> userParams, Map<String, String> tpParams, final InitCallback initCallback) {
        final String customAs = RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_ADMOB);

        removeInited(customAs);

        suportGDPR(context, userParams);

        if (isInited(customAs)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(customAs, initCallback)) {
            return;
        }

        setTestDevice(request, context);

        MobileAds.disableMediationAdapterInitialization(context);
        String isInitNoCallBack = "1";
        if (tpParams != null && tpParams.size() > 0) {
            if (tpParams.containsKey(GoogleConstant.INIT_NO_CALLBACK)) {
                isInitNoCallBack = (String) tpParams.get(GoogleConstant.INIT_NO_CALLBACK);
            }
            id = tpParams.get(GoogleConstant.ID);
        }

        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: ");

        Log.i(TAG, "isInitNoCallBack: " + isInitNoCallBack);
        if ("0".equals(isInitNoCallBack)) {
            MobileAds.initialize(context, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                    AdapterStatus status = initializationStatus.getAdapterStatusMap().get("com.google.android.gms.ads.MobileAds");
                    AdapterStatus.State state = status != null ? status.getInitializationState() : null;

                    if (state == AdapterStatus.State.READY) {
                        Log.i(TAG, "onInitializationComplete: ");
                        sendResult(customAs, true);
                    } else {
                        sendResult(customAs, false, "", "NOT READY");
                    }
                }
            });
        } else {
            MobileAds.initialize(context);
            sendResult(customAs, true);
        }
    }

    public void setTestDevice(AdRequest request, Context context) {
        if (TestDeviceUtil.getInstance().isNeedTestDevice()) {
            String admobTestDevice = TestDeviceUtil.getInstance().getAdmobTestDevice();
            if (!TextUtils.isEmpty(admobTestDevice)) {
                List<String> testDeviceIds = Arrays.asList(admobTestDevice);
                RequestConfiguration configuration = new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build();
                MobileAds.setRequestConfiguration(configuration);

            }
            boolean testDevice = request.isTestDevice(context);
            Log.i(TAG, "isTestDevice  : " + testDevice);
        }
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, TPInitMediation.InitCallback initCallback) {
        initSDK(context, getAdmobAdRequest(userParams,null,null), userParams, tpParams, initCallback);
    }

    public AdRequest getAdmobAdRequest(Map<String, Object> userParams, String contentUrls, ArrayList<String> neighboringUrls) {
        if (userParams == null || userParams.size() <= 0) {
            return new AdRequest.Builder().build();
        }
        Bundle networkExtrasBundle = new Bundle();

        boolean openPersonalizedAd = GlobalTradPlus.getInstance().isOpenPersonalizedAd();
        if (!openPersonalizedAd) {
            networkExtrasBundle.putString("npa", "1");
        }
        Log.i("PersonalizeEnable", TAG + " openPersonalizedAd 个性化开关: " + openPersonalizedAd);

        if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
            boolean ccpa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
            Log.i("privacylaws", "ccpa: " + ccpa);
            if (ccpa) {
                networkExtrasBundle.putInt("rdp", 1);
            }
        }

        if (userParams.containsKey(AppKeyManager.GDPR_CONSENT) && userParams.containsKey(AppKeyManager.IS_UE)) {
            boolean need_set_gdpr = false;
            int consent = (int) userParams.get(AppKeyManager.GDPR_CONSENT);
            if (consent == TradPlus.NONPERSONALIZED || consent == TradPlus.UNKNOWN) {
                need_set_gdpr = true;
            }

            boolean isEu = (boolean) userParams.get(AppKeyManager.IS_UE);
            Log.i("privacylaws", "Admob suportGDPR: " + need_set_gdpr + "(true)" + ":isUe:" + isEu);
            if ((need_set_gdpr && isEu)) {
                networkExtrasBundle.putString("npa", "1");
            }
        }

        AdRequest.Builder builder = new AdRequest.Builder();
        if (!TextUtils.isEmpty(contentUrls)) {
            builder.setContentUrl(contentUrls);
            Log.i(TAG, "contentUrls: " + contentUrls);
        }else if (neighboringUrls != null) {
            builder.setNeighboringContentUrls(neighboringUrls);
            Log.i(TAG, "neighboringUrls: " + neighboringUrls);
        }

        return builder.addNetworkExtrasBundle(AdMobAdapter.class, networkExtrasBundle).build();
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {
        if (userParams != null && userParams.size() > 0) {

            RequestConfiguration requestConfiguration = MobileAds.getRequestConfiguration();
            RequestConfiguration.Builder builder = requestConfiguration.toBuilder();

            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + coppa);
                builder.setTagForChildDirectedTreatment(coppa ? TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE : TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE);
            }


            if (userParams.containsKey(AppKeyManager.IS_UE)) {
                if (userParams.containsKey(AppKeyManager.KEY_GDPR_CHILD)) {
                    boolean gdprchild = (boolean) userParams.get(AppKeyManager.KEY_GDPR_CHILD);
                    Log.i("privacylaws", "gdprchild: " + gdprchild);
                    builder.setTagForUnderAgeOfConsent(gdprchild ? TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE : TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE);
                } else {
                    // 未知
                    builder.setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED);
                }

            }
            if (userParams.containsKey(GoogleConstant.MAX_AD_CONTENT_RATING)) {
                String maxAdContentRating = (String) userParams.get(GoogleConstant.MAX_AD_CONTENT_RATING);
                Log.i("privacylaws", "maxAdContentRating: " + maxAdContentRating);
                if (!TextUtils.isEmpty(maxAdContentRating)) {
                    builder.setMaxAdContentRating(maxAdContentRating);
                }
            }

            builder.build();
            MobileAds.setRequestConfiguration(requestConfiguration);
        }
    }

    @Override
    public String getNetworkVersionCode() {
        VersionInfo version = MobileAds.getVersion();
        int majorVersion = version.getMajorVersion();
        int minorVersion = version.getMinorVersion();
        int microVersion = version.getMicroVersion();
        return majorVersion + "." + minorVersion + "." + microVersion + "";
    }

    @Override
    public String getNetworkVersionName() {
        return RequestUtils.getInstance().getCustomAs(id);
    }
}
