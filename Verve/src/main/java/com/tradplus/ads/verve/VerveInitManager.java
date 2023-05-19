package com.tradplus.ads.verve;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;

import net.pubnative.lite.sdk.HyBid;
import net.pubnative.lite.sdk.UserDataManager;

import java.util.Map;

public class VerveInitManager extends TPInitMediation {

    private static final String TAG = "Verve";
    private static VerveInitManager sInstance;
    private String appToken;

    public synchronized static VerveInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new VerveInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {

        if (tpParams != null && tpParams.size() > 0) {
            appToken = tpParams.get(VerveConstant.APPTOKEN);
        }

        HyBid.setTestMode(TestDeviceUtil.getInstance().isNeedTestDevice());

        if (isInited(appToken)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(appToken, initCallback)) {
            return;
        }

        if (HyBid.isInitialized()) {
            Log.i(TAG, "HyBid isInitialized: ");
            sendResult(appToken, true);
            return;
        }

        suportGDPR(context, userParams);

        HyBid.initialize(appToken, (Application) context.getApplicationContext(), new HyBid.InitialisationListener() {
            @Override
            public void onInitialisationFinished(boolean b) {
                Log.i(TAG, "onInitialisationFinished: ");
                sendResult(appToken, true);
            }
        });
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {
        if (userParams != null && userParams.size() > 0) {
            UserDataManager userDataManager = HyBid.getUserDataManager();
            if (userParams.containsKey(AppKeyManager.GDPR_CONSENT)) {
                int consent = (int) userParams.get(AppKeyManager.GDPR_CONSENT);
                Log.i("privacylaws", "suportGDPR: " + consent);
                if (userDataManager != null && TextUtils.isEmpty(userDataManager.getIABGDPRConsentString())) {
                    Log.i("privacylaws", "IABGDPRConsentString is not empty");
                    userDataManager.setIABGDPRConsentString(consent == TradPlus.PERSONALIZED ? "1" : "0");
                }
            }

            //COPPA
            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa:" + coppa);
                HyBid.setCoppaEnabled(coppa);
            }

            // CCPA false:consent;true:not consent
            // If such a user opts out of the sale of their personal information, set the do-not-sell flag to true.
            if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean ccpa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "ccpa:" + ccpa);
                //NOTE: PubNative suggested this US Privacy String, so it does not match other adapters.
                if (!ccpa) {
                    userDataManager.setIABUSPrivacyString("1NYN");
                }
            }
        }

    }

    @Override
    public String getNetworkVersionCode() {
        return HyBid.getSDKVersionInfo();
    }

    @Override
    public String getNetworkVersionName() {
        return "Verve";
    }
}
