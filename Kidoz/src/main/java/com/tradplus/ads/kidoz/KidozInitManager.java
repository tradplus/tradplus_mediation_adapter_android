package com.tradplus.ads.kidoz;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.kidoz.sdk.api.KidozSDK;
import com.kidoz.sdk.api.interfaces.SDKEventListener;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;

public class KidozInitManager extends TPInitMediation {

    private static final String TAG = "Kidoz";
    private String appId,appToken;
    private static KidozInitManager sInstance;

    public synchronized static KidozInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new KidozInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {

        if (tpParams != null && tpParams.size() > 0) {
            appId = tpParams.get(AppKeyManager.APP_ID);
            appToken = tpParams.get(AppKeyManager.APPTOKEN);
        }

        if(!(context instanceof Activity)) {
            if (initCallback != null) {
                initCallback.onFailed("","Context is not Activity");
            }
            return;
        }

        if(isInited(appId+appToken)){
            initCallback.onSuccess();
            return;
        }

        if(hasInit(appId+appToken,initCallback)){
            return;
        }

        KidozSDK.setLoggingEnabled(TestDeviceUtil.getInstance().isNeedTestDevice());
        suportGDPR(context, userParams);

        String sdkVersion = KidozSDK.getSDKVersion();
        KidozSDK.setSDKListener(new SDKEventListener() {
            @Override
            public void onInitSuccess() {
                Log.i(TAG, "onInitSuccess: SdkVersion : " + sdkVersion);
                sendResult(appId+appToken,true);

            }

            @Override
            public void onInitError(String s) {
                Log.i(TAG, "onInitError: error :" + s);
                sendResult(appId+appToken,false,"",s);

            }
        });
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + appId + ", appToken :" +appToken);
        KidozSDK.initialize((Activity) context, appId, appToken);
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

    }

    @Override
    public String getNetworkVersionCode() {
        return KidozSDK.getSDKVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Kidoz";
    }
}
