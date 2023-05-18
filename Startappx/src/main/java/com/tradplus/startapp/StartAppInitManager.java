package com.tradplus.startapp;

import android.content.Context;
import android.util.Log;

import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;

public class StartAppInitManager extends TPInitMediation {

    private static final String TAG = "StartApp";
    private String mAppId;
    private static StartAppInitManager sInstance;

    public synchronized static StartAppInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new StartAppInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {

        if (tpParams != null && tpParams.size() > 0) {
            mAppId = tpParams.get(AppKeyManager.APP_ID);
        }

        if (isInited(mAppId)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(mAppId, initCallback)) {
            return;
        }
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + mAppId);
        Log.i(TAG, "initSDK: initSDK: appId :" + mAppId);
        suportGDPR(context, userParams);
        StartAppSDK.setTestAdsEnabled(TestDeviceUtil.getInstance().isNeedTestDevice());
        try {
            StartAppSDK.addWrapper(context, TradPlus.getTradPlusName(), TradPlus.getTradPlusVersion());
        } catch (Throwable throwable) {

        }
        StartAppSDK.init(context, mAppId, false);
        StartAppAd.disableSplash();
        StartAppAd.disableAutoInterstitial();

        sendResult(mAppId, true);
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {
        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.GDPR_CONSENT) && userParams.containsKey(AppKeyManager.IS_UE)) {
                boolean need_set_gdpr = false;
                int consent1 = (int) userParams.get(AppKeyManager.GDPR_CONSENT);
                if (consent1 == TradPlus.PERSONALIZED) {
                    need_set_gdpr = true;
                }

                boolean isEu = (boolean) userParams.get(AppKeyManager.IS_UE);
                Log.i("privacylaws", "suportGDPR: " + need_set_gdpr + ":isUe:" + isEu);
                //true:agree  false:deny
                StartAppSDK.setUserConsent(context, "pas", System.currentTimeMillis(), need_set_gdpr);
            }
            //ccpa ~~ false 加州用户均不上报数据 ；true 接受上报数据
            //If the user chooses NOT to opt out, and is ok with advertising as usual, you can use "1YNN". —— 正常上报数据
            //If the user chooses to restrict advertising and opt out, you can use "1YYN". ————限制广告数据上报
            if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean ccpa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "ccpa: " + ccpa);
                StartAppSDK.getExtras(context)
                        .edit()
                        .putString("IABUSPrivacy_String", ccpa ? "1YNN" : "1YYN")
                        .apply();
            }
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return StartAppSDK.getVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "StartApp";
    }
}
