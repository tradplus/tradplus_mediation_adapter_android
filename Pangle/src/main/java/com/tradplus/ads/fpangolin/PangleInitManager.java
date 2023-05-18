package com.tradplus.ads.fpangolin;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.api.init.PAGConfig;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class PangleInitManager extends TPInitMediation {
    private static final String TAG = "Pangle";
    private static PangleInitManager sInstance;
    private String appId;
    private static int mAppIcon;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private boolean need_set_gdpr = false;
    private int ischild = 0;
    private boolean ccpa = false;

    public synchronized static PangleInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new PangleInitManager();
        }
        return sInstance;
    }


    private PAGConfig buildNewConfig(Map<String, Object> userParams, String appId) {

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.APPICON)) {
                int appIcon = (int) userParams.get(AppKeyManager.APPICON);
                mAppIcon = appIcon;
            }
        }

        String mediationName = null;
        String adapterVersion = null;
        mediationName = "tradplus";
        adapterVersion = "19.0.0";
        PAGConfig.Builder builder = new PAGConfig.Builder();
        builder.appId(appId);
        builder.useTextureView(true);
        builder.titleBarTheme(TTAdConstant.TITLE_BAR_THEME_DARK);
        builder.supportMultiProcess(false);
        builder.needClearTaskReset();
        builder.setGDPRConsent(need_set_gdpr ? 1 : 0);
        builder.setChildDirected(ischild);
        builder.setDoNotSell(ccpa ? 1 : 0);
        builder.setUserData(getDataString(mediationName, adapterVersion));
        builder.debugLog(TestDeviceUtil.getInstance().isNeedTestDevice());

        if (mAppIcon != 0) {
            builder.appIcon(mAppIcon);
        }

        return builder.build();
    }

    private static String getDataString(String mediationName, String adapterVersion) {
        if (TextUtils.isEmpty(mediationName) || TextUtils.isEmpty(adapterVersion)) return null;

        JSONArray jsonArray = new JSONArray();
        JSONObject mediationJson = new JSONObject();
        JSONObject versionJson = new JSONObject();
        try {
            mediationJson.put("name", "mediation");
            mediationJson.put("value", mediationName);
            versionJson.put("name", "adapter_version");
            versionJson.put("value", adapterVersion);
        } catch (Exception e) {
            e.printStackTrace();
        }
        jsonArray.put(mediationJson);
        jsonArray.put(versionJson);
        return jsonArray.toString();
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, final TPInitMediation.InitCallback initCallback) {
        if (tpParams != null && tpParams.size() > 0) {
            appId = tpParams.get(AppKeyManager.APP_ID);
        }

        suportGDPR(context, userParams);

        if (isInited(appId)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(appId, initCallback)) {
            return;
        }

        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + appId);
        if (PAGSdk.isInitSuccess()) {
            sendResult(appId, true);
            return;
        }

        PAGConfig pAGInitConfig = buildNewConfig(userParams, appId);
        PAGSdk.init(context, pAGInitConfig, new PAGSdk.PAGInitCallback() {
            @Override
            public void success() {
                Log.i(TAG, "success: ");
                sendResult(appId, true);
            }

            @Override
            public void fail(int code, String msg) {
                Log.i(TAG, "fail: code :" + code + ",msg :" + code);
                sendResult(appId, false, code + "", msg);
            }
        });
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {
        if (!PAGSdk.isInitSuccess()) {return;}

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.GDPR_CONSENT)) {
                int consent = (int) userParams.get(AppKeyManager.GDPR_CONSENT);
                if (consent == TradPlus.PERSONALIZED) {
                    need_set_gdpr = true;
                }
                Log.i("privacylaws", "suportGDPR: " + consent);
            }

            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + coppa);
                if (coppa) {
                    ischild = 1;
                }
            }

            if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
                ccpa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "ccpa: " + ccpa);
            }

        }
        // Set the configuration of COPPA, 0:adult, 1:child
        PAGConfig.setChildDirected(ischild);

        // 0: "sale" of personal information is permitted
        // 1: user has opted out of "sale" of personal information
        PAGConfig.setDoNotSell(ccpa ? 1 : 0);

        // 0:User doesn't grant consent, 1: User has granted the consent
        PAGConfig.setGDPRConsent(need_set_gdpr ? 1 : 0);
    }

    @Override
    public String getNetworkVersionCode() {
        return PAGSdk.getSDKVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Pangle";
    }

}
