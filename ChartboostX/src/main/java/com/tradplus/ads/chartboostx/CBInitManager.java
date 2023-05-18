package com.tradplus.ads.chartboostx;


import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.callbacks.StartCallback;
import com.chartboost.sdk.events.StartError;
import com.chartboost.sdk.privacy.model.CCPA;
import com.chartboost.sdk.privacy.model.COPPA;
import com.chartboost.sdk.privacy.model.GDPR;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.Map;

public class CBInitManager extends TPInitMediation {

    private static CBInitManager sInstance;
    private static final String TAG = "chartboost";
    private String appId, appSignature;

    public synchronized static CBInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new CBInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {
        if (tpParams != null && tpParams.size() > 0) {
            appId = tpParams.get(AppKeyManager.APP_ID);
            appSignature = tpParams.get(AppKeyManager.APP_SIGNATURE);
        }

        if (isInited(appId)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(appId, initCallback)) {
            return;
        }
        suportGDPR(context, userParams);

        Chartboost.startWithAppId(context, appId, appSignature, new StartCallback() {
            @Override
            public void onStartCompleted(@Nullable StartError startError) {
                if (startError != null) {
                    Log.i(TAG, "StartError: " + startError.toString());
                    sendResult(appId, false, startError.getCode() + "", startError.getException() + "");
                } else {
                    Log.i(TAG, "onStartCompleted: ");
                    sendResult(appId, true);
                }
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
                if (need_set_gdpr) {
                    Chartboost.addDataUseConsent(context, new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL));
                } else {
                    Chartboost.addDataUseConsent(context, new GDPR(GDPR.GDPR_CONSENT.NON_BEHAVIORAL));
                }
            }

            if (localExtras.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean ccpa = (boolean) localExtras.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "ccpa: " + ccpa);
                // OPT_IN_SALE(1NN-) means the user consents (Behavioral and Contextual Ads)
                // OPT_OUT_SALE(1NY-) means the user does not consent to targeting (Contextual ads)
                if (ccpa) {
                    Chartboost.addDataUseConsent(context, new CCPA(CCPA.CCPA_CONSENT.OPT_IN_SALE));
                } else {
                    Chartboost.addDataUseConsent(context, new CCPA(CCPA.CCPA_CONSENT.OPT_OUT_SALE));
                }

            }

            if (localExtras.containsKey(AppKeyManager.KEY_COPPA) || localExtras.containsKey(AppKeyManager.DFF)) {
                boolean isChildDirected = false;
                if (localExtras.containsKey(AppKeyManager.KEY_COPPA)) {
                    isChildDirected = (boolean) localExtras.get(AppKeyManager.KEY_COPPA);
                    Log.i("privacylaws", "coppa: " + isChildDirected);
                } else {
                    isChildDirected = (boolean) localExtras.get(AppKeyManager.DFF);
                    Log.i("privacylaws", "dff:" + isChildDirected);
                }
                // false means that COPPA restrictions do not apply.
                Chartboost.addDataUseConsent(context, new COPPA(isChildDirected));

            }

        }
    }

    @Override
    public String getNetworkVersionCode() {
        return Chartboost.getSDKVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "ChartBoost";
    }

}
