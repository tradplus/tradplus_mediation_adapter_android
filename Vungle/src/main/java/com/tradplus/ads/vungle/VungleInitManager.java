package com.tradplus.ads.vungle;

import android.content.Context;
import android.util.Log;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.vungle.warren.BuildConfig;
import com.vungle.warren.Plugin;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleApiClient;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.error.VungleException;

import java.util.HashMap;
import java.util.Map;

public class VungleInitManager extends TPInitMediation {

    private static final String TAG = "Vungle";
    private String mAppId;
    private static VungleInitManager sInstance;

    private Map<String , VungleBanner> vungleBanners = new HashMap<>();

    public synchronized static VungleInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new VungleInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, final InitCallback initCallback) {

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
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + mAppId);
        Plugin.addWrapperInfo(VungleApiClient.WrapperFramework.vunglehbs, "13.0.0");
        Vungle.init(mAppId, context.getApplicationContext(), new com.vungle.warren.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                sendResult(mAppId, true);

            }

            @Override
            public void onError(VungleException exception) {
                if (exception != null) {
                    Log.i(TAG, "InitCallback - onError: " + exception.getLocalizedMessage());
                    sendResult(mAppId, false, "", exception.getLocalizedMessage());
                } else {
                    sendResult(mAppId, false, "", "");
                    Log.i(TAG, "Throwable is null");
                }
            }

            @Override
            public void onAutoCacheAdAvailable(String placementId) {

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
                Vungle.updateConsentStatus(need_set_gdpr ? Vungle.Consent.OPTED_IN : Vungle.Consent.OPTED_OUT, "1.0.0");
            }

            if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean cppa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "cppa : " + cppa);
                Vungle.updateCCPAStatus(cppa ? Vungle.Consent.OPTED_IN : Vungle.Consent.OPTED_OUT);
            }

            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + coppa);
                Vungle.updateUserCoppaStatus(coppa);
            }
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public String getNetworkVersionName() {
        return "Vungle";
    }

    public VungleBanner getVungleBanner(String placementId) {
        return vungleBanners.get(placementId);
    }

    public void setVungleBanner(String placementId,VungleBanner vungleBanner) {
        this.vungleBanners.put(placementId,vungleBanner);
    }
    public void removeVungleBanner(String placementId) {
        this.vungleBanners.remove(placementId);
    }
}
