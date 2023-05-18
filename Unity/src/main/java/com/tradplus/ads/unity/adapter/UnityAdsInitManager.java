package com.tradplus.ads.unity.adapter;

import android.content.Context;
import android.util.Log;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MetaData;

import java.util.Map;

public class UnityAdsInitManager extends TPInitMediation {
    private static final String TAG = "Unityads";
    private static UnityAdsInitManager sInstance;
    private String appId;

    public synchronized static UnityAdsInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new UnityAdsInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, final InitCallback initCallback) {
        suportGDPR(context, userParams);

        if (tpParams != null && tpParams.size() > 0) {
            appId = tpParams.get(AppKeyManager.APP_ID);
        }

        if (isInited(appId)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(appId, initCallback)) {
            return;
        }

        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + appId);

        UnityAds.initialize(context, appId, TestDeviceUtil.getInstance().isNeedTestDevice(), new IUnityAdsInitializationListener() {

            @Override
            public void onInitializationComplete() {
                sendResult(appId, true);
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError unityAdsInitializationError, String s) {
                sendResult(appId, false, "", unityAdsInitializationError.name());
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

                MetaData gdprMetaData = new MetaData(context);
                gdprMetaData.set("gdpr.consent", need_set_gdpr);
                gdprMetaData.commit();
            }


            if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean ccpa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "ccpa: " + ccpa);
                // opts in "true" ; opts out of "false"
                MetaData privaceMetaData = new MetaData(context);
                privaceMetaData.set("privacy.consent", ccpa);
                privaceMetaData.commit();
            }

            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + coppa);
                MetaData agaGateMetaData = new MetaData(context);
                agaGateMetaData.set("privacy.useroveragelimit", coppa);
                agaGateMetaData.commit();
            }
        }

        if ((TradPlus.isCCPADoNotSell(context) == TradPlus.PRIVACY_DEFAULT_KEY)) {
            boolean openPersonalizedAd = GlobalTradPlus.getInstance().isOpenPersonalizedAd();
            MetaData privaceMetaData = new MetaData(context);
            privaceMetaData.set("privacy.consent", openPersonalizedAd);
            privaceMetaData.commit();

            MetaData piplMetaData = new MetaData(context.getApplicationContext());
            piplMetaData.set("pilp.consent", openPersonalizedAd);
            piplMetaData.commit();

            Log.i("PersonalizeEnable", TAG + " openPersonalizedAd : " + openPersonalizedAd);
        }

        // DFF Unity dashboard配置
    }

    @Override
    public String getNetworkVersionCode() {
        return UnityAds.getVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "UnityAds";
    }
}
