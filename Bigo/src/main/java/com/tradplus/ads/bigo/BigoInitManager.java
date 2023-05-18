package com.tradplus.ads.bigo;

import android.content.Context;
import android.util.Log;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;

import java.util.Map;

import sg.bigo.ads.BigoAdSdk;
import sg.bigo.ads.ConsentOptions;
import sg.bigo.ads.api.AdConfig;

public class BigoInitManager extends TPInitMediation {

    private static final String TAG = "Bigo";
    private static BigoInitManager sInstance;
    private String mAppId;

    public synchronized static BigoInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new BigoInitManager();
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

        if (BigoAdSdk.isInitialized()) {
            sendResult(mAppId, true);
            return;
        }

        suportGDPR(context, userParams);

        AdConfig config = new AdConfig.Builder()
                .setAppId(mAppId)
                .setDebug(TestDeviceUtil.getInstance().isNeedTestDevice())
                .build();

        BigoAdSdk.initialize(context, config, new BigoAdSdk.InitListener() {
            @Override
            public void onInitialized() {
                Log.i(TAG, "onInitialized: ");
                sendResult(mAppId, true);
            }
        });
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
                // “true”值表示用户已同意存储和处理个人信息，“false”值表示用户已拒绝同意。
                BigoAdSdk.setUserConsent(context, ConsentOptions.GDPR, need_set_gdpr);
            }

            if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean cppa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "cppa: " + cppa);
                // “true”值表示用户已同意存储和处理个人信息，“false”值表示用户已拒绝同意。
                BigoAdSdk.setUserConsent(context, ConsentOptions.CCPA, cppa);
            }
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return BigoAdSdk.getSDKVersionName();
    }

    @Override
    public String getNetworkVersionName() {
        return "Bigo";
    }
}
