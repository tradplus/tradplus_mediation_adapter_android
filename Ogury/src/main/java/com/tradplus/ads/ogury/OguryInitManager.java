package com.tradplus.ads.ogury;

import android.content.Context;
import android.util.Log;

import com.ogury.core.OguryLog;
import com.ogury.sdk.Ogury;
import com.ogury.sdk.OguryChildPrivacyTreatment;
import com.ogury.sdk.OguryConfiguration;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;

public class OguryInitManager extends TPInitMediation {

    private static final String TAG = "Ogury";
    private String mAppId;
    private static OguryInitManager sInstance;

    public synchronized static OguryInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new OguryInitManager();
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

        suportGDPR(context, userParams);
        if(TestDeviceUtil.getInstance().isNeedTestDevice()) {
            OguryLog.enable(OguryLog.Level.DEBUG);
        }

        Ogury.start((new OguryConfiguration.Builder(context, mAppId)).build());
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + mAppId);

        sendResult(mAppId, true);
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {
        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.GDPR_CONSENT) && userParams.containsKey(AppKeyManager.IS_UE)) {
                boolean need_set_gdpr = false;
                int consent = (int) userParams.get(AppKeyManager.GDPR_CONSENT);
                if (consent == TradPlus.PERSONALIZED) {
                    need_set_gdpr = true;
                }

                boolean isEu = (boolean) userParams.get(AppKeyManager.IS_UE);
                Log.i("privacylaws", "suportGDPR: " + need_set_gdpr + ":isUe:" + isEu);
                Ogury.applyChildPrivacy(need_set_gdpr ? OguryChildPrivacyTreatment.UNDER_AGE_OF_GDPR_CONSENT_TREATMENT_TRUE :
                        OguryChildPrivacyTreatment.UNDER_AGE_OF_GDPR_CONSENT_TREATMENT_FALSE);
            }

            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + coppa);
                // CHILD_UNDER_COPPA_TREATMENT_FALSE: not applicable
                // CHILD_UNDER_COPPA_TREATMENT_TRUE:
                Ogury.applyChildPrivacy(coppa ? OguryChildPrivacyTreatment.CHILD_UNDER_COPPA_TREATMENT_TRUE :
                        OguryChildPrivacyTreatment.CHILD_UNDER_COPPA_TREATMENT_FALSE);
            }
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return Ogury.getSdkVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Ogury";
    }
}
