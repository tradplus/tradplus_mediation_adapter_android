package com.tradplus.ads.yandex;

import android.content.Context;
import android.util.Log;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.yandex.mobile.ads.common.InitializationListener;
import com.yandex.mobile.ads.common.MobileAds;

import java.util.Map;

public class YandexInitManager extends TPInitMediation {

    public static final String TAG_YANDEX = "Yandex";
    private static YandexInitManager sInstance;


    public synchronized static YandexInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new YandexInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {

        final String customAs = TAG_YANDEX;

        if (isInited(customAs)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(customAs, initCallback)) {
            return;
        }

        suportGDPR(context, userParams);

        MobileAds.enableLogging(TestDeviceUtil.getInstance().isNeedTestDevice());
        MobileAds.initialize(context, new InitializationListener() {
            @Override
            public void onInitializationCompleted() {
                Log.i(TAG_YANDEX, "onInitializationCompleted: ");
                sendResult(customAs, true);
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
                Log.i("privacylaws", "suportGDPR: "+ need_set_gdpr);
                MobileAds.setUserConsent(need_set_gdpr);
            }

        }
    }

    @Override
    public String getNetworkVersionCode() {
        return MobileAds.getLibraryVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Yandex";
    }
}
