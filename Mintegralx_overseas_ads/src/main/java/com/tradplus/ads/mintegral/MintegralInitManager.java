package com.tradplus.ads.mintegral;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.MBridgeSDK;
import com.mbridge.msdk.out.MBConfiguration;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.mbridge.msdk.out.SDKInitStatusListener;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;

public class MintegralInitManager extends TPInitMediation {
    private static final String TAG = "MTG";
    private String mAppId, mAppKey;
    private MBridgeSDK mIntegralSDK;
    private static MintegralInitManager sInstance;

    public synchronized static MintegralInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new MintegralInitManager();
        }
        return sInstance;
    }

    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, final TPInitMediation.InitCallback initCallback) {

        if (tpParams != null && tpParams.size() > 0) {
            mAppId = tpParams.get(AppKeyManager.APP_ID);
            mAppKey = tpParams.get(AppKeyManager.APP_KEY);
        }

        if (isInited(mAppKey + mAppId)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(mAppKey + mAppId, initCallback)) {
            return;
        }
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + mAppId + " , AppKey :" + mAppKey);
        mIntegralSDK = MBridgeSDKFactory.getMBridgeSDK();
        suportGDPR(context, userParams);

        String wxAppId = GlobalTradPlus.getInstance().getWxAppId();
        Log.i("WxAppID", "wxAppId: " + wxAppId);
        Map<String, String> configurationMap;
        if (TextUtils.isEmpty(wxAppId)) {
            configurationMap = mIntegralSDK.getMBConfigurationMap(mAppId, mAppKey);
        } else {
            configurationMap = mIntegralSDK.getMBConfigurationMap(mAppId, mAppKey, wxAppId);
        }

        mIntegralSDK.init(configurationMap, context, new SDKInitStatusListener() {
            @Override
            public void onInitSuccess() {
                Log.i(TAG, "onInitSuccess: ");
                sendResult(mAppKey + mAppId, true);
            }

            @Override
            public void onInitFail(String s) {
                sendResult(mAppKey + mAppId, false, "", s);
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
                int open = need_set_gdpr ? MBridgeConstans.IS_SWITCH_ON : MBridgeConstans.IS_SWITCH_OFF;
                mIntegralSDK.setConsentStatus(context, open);
            }

            if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean cppa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "cppa: " + cppa);
                mIntegralSDK.setDoNotTrackStatus(!cppa);
            }

            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + coppa);
                mIntegralSDK.setCoppaStatus(context, coppa);
            }
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return MBConfiguration.SDK_VERSION;
    }

    @Override
    public String getNetworkVersionName() {
        return "Mintegral";
    }
}
