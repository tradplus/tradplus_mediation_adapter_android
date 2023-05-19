package com.tradplus.ads.ironsource;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.utils.IronSourceUtils;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;

public class IronSourceInitManager extends TPInitMediation {

    private static final String TAG = "IronSource";
    private String appKey;
    private static IronSourceInitManager sInstance;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public synchronized static IronSourceInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new IronSourceInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, final InitCallback initCallback) {

        if (tpParams != null && tpParams.size() > 0) {
            appKey = tpParams.get(AppKeyManager.APP_ID);
        }

        if (isInited(appKey)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(appKey, initCallback)) {
            return;
        }

        suportGDPR(context, userParams);
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + appKey);
        IronSource.setAdaptersDebug(TestDeviceUtil.getInstance().isNeedTestDevice());
        IronSource.initISDemandOnly(context, appKey, IronSource.AD_UNIT.INTERSTITIAL, IronSource.AD_UNIT.REWARDED_VIDEO);
        sendResult(appKey, true);
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
                IronSource.setConsent(need_set_gdpr);
            }

            if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean cppa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "suportGDPR ccpa: " + cppa);
                IronSource.setMetaData("do_not_sell", cppa ? "false" : "true");
            }

            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + coppa);
                // The indication of whether a specific end-user is a child should be done using a “is_child_directed” flag,
                // by setting its value to “true” or “false”.
                IronSource.setMetaData("is_child_directed", coppa ? "true" : "false");
            }


            if (userParams.containsKey(AppKeyManager.DFF)) {
                boolean dff = (boolean) userParams.get(AppKeyManager.DFF);
                Log.i("privacylaws", "dff:" + dff);
                if (dff) {
                    IronSource.setMetaData("is_deviceid_optout", "true");
                }
            }
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return IronSourceUtils.getSDKVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "IronSource";
    }
}
