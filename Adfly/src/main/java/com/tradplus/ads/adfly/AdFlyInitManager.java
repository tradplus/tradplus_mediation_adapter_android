package com.tradplus.ads.adfly;

import android.app.Application;
import android.content.Context;

import com.adfly.sdk.core.AdFlySdk;
import com.adfly.sdk.core.SdkConfiguration;
import com.adfly.sdk.core.SdkInitializationListener;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.Map;

public class AdFlyInitManager extends TPInitMediation {

    private static final String TAG = "AdFly";
    private static AdFlyInitManager sInstance;
    private String appKey, appSecret;

    public synchronized static AdFlyInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new AdFlyInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {

        if (tpParams != null && tpParams.size() > 0) {
            appSecret = tpParams.get(AdFlyConstant.APPSECRET);
            appKey = tpParams.get(AppKeyManager.APP_KEY);
        }



        if (isInited(appKey + appSecret)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(appKey+ appSecret, initCallback)) {
            return;
        }

        if (AdFlySdk.isInitialized()) {
            sendResult(appKey+ appSecret, true);
            return;
        }

        SdkConfiguration configuration = new SdkConfiguration.Builder()
                .appKey(appKey)
                .appSecret(appSecret)
                .build();
        Context applicationContext = context.getApplicationContext();
        AdFlySdk.initialize((Application)applicationContext , configuration, new SdkInitializationListener() {
            @Override
            public void onInitializationFinished() {
                sendResult(appKey + appSecret, true);
            }
        });
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

    }

    @Override
    public String getNetworkVersionCode() {
        return AdFlySdk.getVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "AdFly";
    }
}
