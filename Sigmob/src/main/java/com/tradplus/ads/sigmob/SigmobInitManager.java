package com.tradplus.ads.sigmob;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.sigmob.windad.WindAdOptions;
import com.sigmob.windad.WindAds;
import com.sigmob.windad.WindConsentStatus;
import com.sigmob.windad.WindCustomController;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;

public class SigmobInitManager extends TPInitMediation {

    private static final String TAG = "Sigmob";
    private String mAppId, mAppKey;
    private static SigmobInitManager sInstance;
    private WindAds windAds;
    public WindCustomController mWindCustomController;

    public synchronized static SigmobInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new SigmobInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {

        if (tpParams != null && tpParams.size() > 0) {
            mAppKey = tpParams.get(AppKeyManager.APP_KEY);
            mAppId = tpParams.get(AppKeyManager.APP_ID);
        }

        boolean isAdult = true;
        if (userParams != null && userParams.size() > 0) {
            if(userParams.containsKey(SigmobConstant.SIGMOB_IS_ADULT)) {
                isAdult = (boolean) userParams.get(SigmobConstant.SIGMOB_IS_ADULT);
            }
        }
        Log.i(TAG, "isAdult: " + isAdult);

        removeInited(mAppKey + mAppId);

        if (isInited(mAppKey + mAppId)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(mAppKey + mAppId, initCallback)) {
            return;
        }

        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + mAppId + ", AppKey :" + mAppKey);
        windAds = WindAds.sharedAds();
        suportGDPR(context, userParams);

        windAds.setAdult(isAdult);
        boolean openPersonalizedAd = GlobalTradPlus.getInstance().isOpenPersonalizedAd();
        windAds.setPersonalizedAdvertisingOn(openPersonalizedAd);
        Log.i("PersonalizeEnable", TAG + " openPersonalizedAd : " + openPersonalizedAd);

        boolean privacyUserAgree = GlobalTradPlus.getInstance().isPrivacyUserAgree();
        Log.i(TAG, "WindCustomController == null ? " + (mWindCustomController == null) + "，privacyUserAgree :" + privacyUserAgree);

        WindAdOptions windAdOptions = new WindAdOptions(mAppId, mAppKey);
        windAdOptions.setCustomController(mWindCustomController == null ? new SigmobCustomController(privacyUserAgree) : getTTCustomController());
        windAds.startWithOptions(context, windAdOptions);

        sendResult(mAppKey + mAppId, true);
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
                Log.i("gdpr", "suportGDPR: " + need_set_gdpr + ":isUe:" + isEu);
                if (windAds != null) {
                    windAds.setUserGDPRConsentStatus(need_set_gdpr ? WindConsentStatus.ACCEPT : WindConsentStatus.DENIED);
                }
            }
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return WindAds.getVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Sigmob";
    }

    public WindCustomController getTTCustomController() {
        return mWindCustomController;
    }

    public void setTTCustomController(WindCustomController TTCustomController) {
        mWindCustomController = TTCustomController;
    }
}
