package com.tradplus.ads.tapjoy;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tapjoy.TJConnectListener;
import com.tapjoy.TJPrivacyPolicy;
import com.tapjoy.Tapjoy;
import com.tapjoy.TapjoyConnectFlag;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Hashtable;
import java.util.Map;

public class TapjoyInitManager extends TPInitMediation {

    private static final String TAG = "Tapjoy";
    private String mSdkKey, userId;
    private static TapjoyInitManager sInstance;

    public synchronized static TapjoyInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new TapjoyInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, final InitCallback initCallback) {

        if (tpParams != null && tpParams.size() > 0) {
            mSdkKey = tpParams.get(AppKeyManager.SDK_KEY);
        }

        if(isInited(mSdkKey)){
            initCallback.onSuccess();
            return;
        }

        if(hasInit(mSdkKey,initCallback)){
            return;
        }

        suportGDPR(context, userParams);

        Hashtable<String, Object> connectFlags = new Hashtable<String, Object>();
        if (userParams != null && userParams.size() > 0) {
            userId = (String) userParams.get(AppKeyManager.CUSTOM_USERID);
            if (TextUtils.isEmpty(userId)) {
                userId = "";
            }
            connectFlags.put(TapjoyConnectFlag.USER_ID, userId);
        }

        if(context instanceof Activity) {
            Tapjoy.setActivity((Activity) context);
        }


        Tapjoy.setDebugEnabled(TestDeviceUtil.getInstance().isNeedTestDevice());
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: SdkKey :" + mSdkKey);
        Tapjoy.connect(context, mSdkKey, connectFlags, new TJConnectListener() {
            @Override
            public void onConnectFailure() {
                Log.i(TAG, "onConnectFailure: ");
                sendResult(mSdkKey,false,"","onConnectFailure");
            }

            @Override
            public void onConnectSuccess() {
                sendResult(mSdkKey,true);

            }
        });
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {
        if (userParams != null && userParams.size() > 0) {
            TJPrivacyPolicy privacyPolicy = Tapjoy.getPrivacyPolicy();
            if (userParams.containsKey(AppKeyManager.GDPR_CONSENT) && userParams.containsKey(AppKeyManager.IS_UE)) {
                boolean need_set_gdpr = false;
                int consent1 = (int) userParams.get(AppKeyManager.GDPR_CONSENT);
                if (consent1 == TradPlus.PERSONALIZED) {
                    need_set_gdpr = true;
                }

                boolean isEu = (boolean) userParams.get(AppKeyManager.IS_UE);
                Log.i("privacylaws", "suportGDPR: " + need_set_gdpr + ":isUe:" + isEu);
                privacyPolicy.setSubjectToGDPR(isEu);
                //1:agree  0:deny
                String consent = need_set_gdpr ? "1" : "0";
                privacyPolicy.setUserConsent(consent);
            }

            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + coppa);
                privacyPolicy.setBelowConsentAge(coppa);
            }

            if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean ccpa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "ccpa: " + ccpa);
                if (ccpa) {
                    privacyPolicy.setUSPrivacy("1YYY");
                } else {
                    privacyPolicy.setUSPrivacy("1YNN");
                }
            }

            //Google 为家庭设计 (DFF) / 家庭政策
            if (userParams.containsKey(AppKeyManager.DFF)) {
                boolean dff = (boolean) userParams.get(AppKeyManager.DFF);
                Log.i("privacylaws", "dff:" + dff);
                Tapjoy.optOutAdvertisingID(context, dff);
            }
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return Tapjoy.getVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Tapjoy";
    }
}
