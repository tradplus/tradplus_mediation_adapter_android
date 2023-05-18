package com.tradplus.ads.mintegral;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.mbridge.msdk.MBridgeSDK;
import com.mbridge.msdk.out.MBConfiguration;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.mbridge.msdk.out.SDKInitStatusListener;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;

public class MintegralInitManager extends TPInitMediation {

    private static final String TAG = "MTG";
    private String mAppId, mAppKey;
    private static MintegralInitManager sInstance;

    public synchronized static MintegralInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new MintegralInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, final InitCallback initCallback) {

        if (tpParams != null && tpParams.size() > 0) {
            mAppId = tpParams.get(AppKeyManager.APP_ID);
            mAppKey = tpParams.get(AppKeyManager.APP_KEY);
        }

        removeInited(mAppKey + mAppId);

        if (isInited(mAppKey + mAppId)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(mAppKey + mAppId, initCallback)) {
            return;
        }


        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + mAppId + ", Appkey :" + mAppKey);
        final MBridgeSDK mIntegralSDK = MBridgeSDKFactory.getMBridgeSDK();

        String wxAppId = GlobalTradPlus.getInstance().getWxAppId();
        Log.i("WxAppID", "wxAppId: " +wxAppId);

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
                // Note 跟我们定义相反
                // 如果设置为 TRUE，服务器将不会显示基于用户个人信息的个性化广告。
                boolean openPersonalizedAd = GlobalTradPlus.getInstance().isOpenPersonalizedAd();
                mIntegralSDK.setDoNotTrackStatus(!openPersonalizedAd);
                Log.i("PersonalizeEnable", TAG + " openPersonalizedAd 个性化开关: " + !openPersonalizedAd);
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
