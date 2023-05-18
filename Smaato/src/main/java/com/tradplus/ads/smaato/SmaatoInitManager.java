package com.tradplus.ads.smaato;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.smaato.sdk.core.Config;
import com.smaato.sdk.core.SmaatoSdk;
import com.smaato.sdk.core.log.LogLevel;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;

public class SmaatoInitManager extends TPInitMediation {

    private static final String TAG = "Smatto";
    private String mAppId;
    private static SmaatoInitManager sInstance;

    public synchronized static SmaatoInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new SmaatoInitManager();
        }
        return sInstance;
    }

    private boolean availableParams(Map<String, String> tpParams) {
        mAppId = tpParams.get(AppKeyManager.APP_ID);
        return (mAppId != null && mAppId.length() > 0);
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {
        if (!availableParams(tpParams)) {
            sendResult(TAG, false, "", TPError.EMPTY_INIT_CONFIGURATION);
            return;
        }

        if (isInited(mAppId)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(mAppId, initCallback)) {
            return;
        }

        Application application;
        if (context instanceof Activity) {
            application = ((Activity) context).getApplication();
        } else {
            try {
                application = (Application) context.getApplicationContext();
            } catch (Throwable e) {
                e.printStackTrace();
                Log.i(TAG, "Unable to initialize Smaato, error obtaining application context.");
                sendResult(mAppId, false, "", e.getLocalizedMessage());
                return ;
            }
        }

        suportGDPR(context, userParams);
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + mAppId);
        boolean needTestDevice = TestDeviceUtil.getInstance().isNeedTestDevice();
        Config config = Config.builder()
                .enableLogging(needTestDevice)
                .setLogLevel(LogLevel.DEBUG)
                .setHttpsOnly(!needTestDevice)
                .build();

        SmaatoSdk.init(application, config, mAppId);
        sendResult(mAppId, true);

    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {
        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + coppa);
                SmaatoSdk.setCoppa(coppa);
            }

            if (userParams.containsKey(AppKeyManager.KEY_LGPD)) {
                int lgpd = (int) userParams.get(AppKeyManager.KEY_LGPD);
                boolean consent = (lgpd == TradPlus.PERSONALIZED);
                Log.i("privacylaws", "lgpd: " + consent);
                //Set LGPD consent
                SmaatoSdk.setLgpdConsentEnabled(consent);
            }
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return SmaatoSdk.getVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Smaato";
    }
}
