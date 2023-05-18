package com.tradplus.ads.helium;

import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chartboost.heliumsdk.HeliumSdk;
import com.chartboost.heliumsdk.ad.HeliumFullscreenAdListener;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;

public class HeliumInitManager extends TPInitMediation {

    private static HeliumInitManager sInstance;
    private static final String TAG = "Helium";
    private String appId;
    private String appSignature;

    public synchronized static HeliumInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new HeliumInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, final TPInitMediation.InitCallback initCallback) {
        if (tpParams != null && tpParams.size() > 0) {
            appId = tpParams.get(AppKeyManager.APP_ID);
            appSignature = tpParams.get(AppKeyManager.APP_HELIUM_SIGNATURE);
        }

        if (isInited(appId + appSignature)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(appId + appSignature, initCallback)) {
            return;
        }

        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + appId + ", appSignature :" + appSignature);
        HeliumSdk.start(context, appId, appSignature, new HeliumSdk.HeliumSdkListener() {
            @Override
            public void didInitialize(Error error) {
                if (error == null) {
                    Log.i(TAG, "didInitialize: onSuccess");
                    sendResult(appId + appSignature, true);
                } else {
                    Log.i(TAG, "didInitialize: onFailed msg :" + error.getMessage());
                    sendResult(appId + appSignature, false, "", error.getMessage());
                }
            }
        });
        suportGDPR(context, userParams);
        HeliumSdk.setTestMode(TestDeviceUtil.getInstance().isNeedTestDevice());
        HeliumSdk.setDebugMode(TestDeviceUtil.getInstance().isNeedTestDevice());

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
                HeliumSdk.setSubjectToGDPR(isEu);
                HeliumSdk.setUserHasGivenConsent(need_set_gdpr);
            }

            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                HeliumSdk.setSubjectToCoppa(coppa);
                Log.i("privacylaws", "coppa: " + coppa);
            }

            if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean ccpa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "ccpa: " + ccpa);
                HeliumSdk.setCCPAConsent(ccpa);
            }
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return HeliumSdk.getVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Helium";
    }

}
