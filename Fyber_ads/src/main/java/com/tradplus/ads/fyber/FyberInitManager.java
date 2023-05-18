package com.tradplus.ads.fyber;

import android.content.Context;
import android.util.Log;

import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;

import static com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener.FyberInitStatus.SUCCESSFULLY;

public class FyberInitManager extends TPInitMediation {

    private static final String TAG = "Fyber";
    private static FyberInitManager sInstance;
    private String appId;

    public synchronized static FyberInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new FyberInitManager();
        }
        return sInstance;
    }


    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {

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
        suportGDPR(context, userParams);

        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + appId);
        InneractiveAdManager.initialize(context, appId, new OnFyberMarketplaceInitializedListener() {
            @Override
            public void onFyberMarketplaceInitialized(FyberInitStatus fyberInitStatus) {
                if (fyberInitStatus == SUCCESSFULLY) {
                    Log.i(TAG, "onFyberMarketplaceInitialized: SUCCESSFULLY");
                    sendResult(appId, true);
                    return;
                }

                Log.i(TAG, "Fyber SDK has been failed initialized," + fyberInitStatus.name());
                sendResult(appId, false, "", fyberInitStatus.name());
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
                InneractiveAdManager.setGdprConsent(need_set_gdpr);
            }

            if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean ccpa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "ccpa: " + ccpa);
                //If the user chooses NOT to opt out, and is ok with advertising as usual, you can use "1YNN". —— 正常上报数据
                //If the user chooses to restrict advertising and opt out, you can use "1YYN". ————限制广告数据上报
                InneractiveAdManager.setUSPrivacyString(ccpa ? "1YNN" : "1YYN");
            }

            //  Google Play Families Ads program
            if (userParams.containsKey(AppKeyManager.DFF)) {
                boolean dff = (boolean) userParams.get(AppKeyManager.DFF);
                Log.i("privacylaws", "dff: " + dff);
                if (dff) {
                    InneractiveAdManager.currentAudienceIsAChild();
                }
            }

            // 巴西隐私政策
            if (userParams.containsKey(AppKeyManager.KEY_LGPD)) {
                int lgpd = (int) userParams.get(AppKeyManager.KEY_LGPD);
                boolean consent = (lgpd == TradPlus.PERSONALIZED);
                Log.i("privacylaws", "lgpd: " + consent);
                //Set LGPD consent
                InneractiveAdManager.setLgpdConsent(consent);
            }
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return InneractiveAdManager.getVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Fyber";
    }
}
