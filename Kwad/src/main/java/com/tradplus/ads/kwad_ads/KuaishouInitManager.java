package com.tradplus.ads.kwad_ads;

import android.content.Context;
import android.util.Log;

import com.kwad.sdk.api.KsAdSDK;
import com.kwad.sdk.api.KsCustomController;
import com.kwad.sdk.api.KsInitCallback;
import com.kwad.sdk.api.SdkConfig;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;

public class KuaishouInitManager extends TPInitMediation {

    private static final String TAG = "Kuaishou";
    private String mAppId;
    private static KuaishouInitManager sInstance;
    public KsCustomController mKsCustomController;

    public synchronized static KuaishouInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new KuaishouInitManager();
        }
        return sInstance;
    }


    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {
        boolean openPersonalizedAd = GlobalTradPlus.getInstance().isOpenPersonalizedAd();
        KsAdSDK.setPersonalRecommend(openPersonalizedAd);
        Log.i("PersonalizeEnable", TAG + " openPersonalizedAd : " + openPersonalizedAd);

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

        if (TestDeviceUtil.getInstance().isNeedTestDevice()) {
            KsAdSDK.isDebugLogEnable();
        }
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + mAppId);

        boolean privacyUserAgree = GlobalTradPlus.getInstance().isPrivacyUserAgree();
        Log.i(TAG, "mTTCustomController == null ? " + (mKsCustomController == null) + "，privacyUserAgree :" + privacyUserAgree);

        KsAdSDK.init(context, new SdkConfig.Builder()
                .appId(mAppId)
                .showNotification(true)
                .debug(TestDeviceUtil.getInstance().isNeedTestDevice())
                .customController(mKsCustomController == null ? new UserDataObtainController(privacyUserAgree) : getKsCustomController())
                .setInitCallback(new KsInitCallback() {
                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "onSuccess: ");
                        sendResult(mAppId, true);
                    }

                    @Override
                    public void onFail(int i, String s) {
                        Log.i(TAG, "onFail: code:" + i + ", msg:" + s);
                        sendResult(mAppId, false, i + "", s);
                    }
                })
                .build());
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

    }

    @Override
    public String getNetworkVersionCode() {
        return KsAdSDK.getSDKVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Kuaishou";
    }

    public KsCustomController getKsCustomController() {
        return mKsCustomController;
    }

    public void setKsCustomController(KsCustomController ksCustomController) {
        mKsCustomController = ksCustomController;
    }
}
