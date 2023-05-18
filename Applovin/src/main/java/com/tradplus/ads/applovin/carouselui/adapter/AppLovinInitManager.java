package com.tradplus.ads.applovin.carouselui.adapter;

import android.content.Context;
import android.util.Log;

import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkSettings;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class AppLovinInitManager extends TPInitMediation {
    private static final String TAG = "AppLovin";
    private static AppLovinInitManager sInstance;
    private String sdkKey;

    public synchronized static AppLovinInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new AppLovinInitManager();
        }
        return sInstance;
    }

    private AppLovinSdk appLovinSdk;

    public AppLovinSdk getAppLovinSdk() {
        return appLovinSdk;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, TPInitMediation.InitCallback initCallback) {
        final String customAs = RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_APPLOVIN);

        if (isInited(customAs)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(customAs, initCallback)) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            sdkKey = tpParams.get(AppKeyManager.SDK_KEY);
        }

        suportGDPR(context, userParams);

        AppLovinSdkSettings appLovinSdkSettings = new AppLovinSdkSettings(context);

        boolean needTestDevice = TestDeviceUtil.getInstance().isNeedTestDevice();
        appLovinSdk = AppLovinSdk.getInstance(sdkKey, appLovinSdkSettings, context);
        // Please make sure to set the mediation provider value to "max" to ensure proper functionality
        appLovinSdk.setMediationProvider("other");
        //Enable verbose logging to see the GAID to use for test devices
        appLovinSdk.getSettings().setVerboseLogging(needTestDevice);
        // To disable the creative debugger in code

        appLovinSdk.initializeSdk(new AppLovinSdk.SdkInitializationListener() {
            @Override
            public void onSdkInitialized(final AppLovinSdkConfiguration config) {
                Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: ");
                sendResult(customAs, true);
            }
        });
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> localExtras) {
        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(AppKeyManager.GDPR_CONSENT) && localExtras.containsKey(AppKeyManager.IS_UE)) {
                boolean need_set_gdpr = false;
                int consent = (int) localExtras.get(AppKeyManager.GDPR_CONSENT);
                if (consent == TradPlus.PERSONALIZED) {
                    need_set_gdpr = true;
                }

                boolean isEu = (boolean) localExtras.get(AppKeyManager.IS_UE);
                Log.i("privacylaws", "suportGDPR: " + need_set_gdpr + ":isUe:" + isEu);
                AppLovinPrivacySettings.setHasUserConsent(need_set_gdpr, context);
            }

            if (localExtras.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) localExtras.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa" + coppa);
                AppLovinPrivacySettings.setIsAgeRestrictedUser(coppa, context);
            }

            // false:consent;
            // true:not consent
            if (localExtras.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean ccpa = (boolean) localExtras.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "ccpa: " + ccpa);
                AppLovinPrivacySettings.setDoNotSell(!ccpa, context);
            }
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return AppLovinSdk.VERSION;
    }

    @Override
    public String getNetworkVersionName() {
        return "AppLovin";
    }
}
