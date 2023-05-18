package com.tradplus.appnext;

import android.content.Context;
import android.util.Log;

import com.appnext.base.Appnext;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class AppNextInitManager extends TPInitMediation {

    private static final String TAG = "AppNext";
    private static AppNextInitManager sInstance;

    public synchronized static AppNextInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new AppNextInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {
        suportGDPR(context, userParams);
        String customAs = RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_APPNEXT);


        if(isInited(customAs)){
            initCallback.onSuccess();
            return;
        }

        if(hasInit(customAs,initCallback)){
            return;
        }
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: ");
        Appnext.init(context);

        sendResult(customAs,true);
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {
        if (userParams == null || userParams.size() <= 0) {
            return;
        }
        if (userParams.containsKey(AppKeyManager.GDPR_CONSENT) && userParams.containsKey(AppKeyManager.IS_UE)) {
            boolean need_set_gdpr = false;
            int consent = (int) userParams.get(AppKeyManager.GDPR_CONSENT);
            if (consent == TradPlus.PERSONALIZED) {
                need_set_gdpr = true;
            }

            boolean isEu = (boolean) userParams.get(AppKeyManager.IS_UE);
            Log.i("privacylaws", "suportGDPR: " + need_set_gdpr + ":isUe:" + isEu);
            //true:agree  false:deny
            Appnext.setParam("consent", need_set_gdpr ? "true" : "false");
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return "2.6.0.473";
    }

    @Override
    public String getNetworkVersionName() {
        return "AppNext";
    }
}
