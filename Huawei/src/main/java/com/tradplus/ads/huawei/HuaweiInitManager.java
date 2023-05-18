package com.tradplus.ads.huawei;

import static com.huawei.hms.ads.NonPersonalizedAd.ALLOW_ALL;
import static com.huawei.hms.ads.NonPersonalizedAd.ALLOW_NON_PERSONALIZED;
import static com.huawei.hms.ads.UnderAge.PROMISE_FALSE;
import static com.huawei.hms.ads.UnderAge.PROMISE_TRUE;
import static com.huawei.hms.ads.UnderAge.PROMISE_UNSPECIFIED;

import android.content.Context;
import android.util.Log;

import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.RequestOptions;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class HuaweiInitManager extends TPInitMediation {

    private static final String TAG = "Huawei";
    private static HuaweiInitManager sInstance;

    public synchronized static HuaweiInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new HuaweiInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {
        String key = RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_HUAWEI);

        removeInited(key);

        suportGDPR(context, userParams);

        if (isInited(key)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(key, initCallback)) {
            return;
        }

        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: ");

        boolean openPersonalizedAd = GlobalTradPlus.getInstance().isOpenPersonalizedAd();
        RequestOptions build = HwAds.getRequestOptions().toBuilder()
                .setNonPersonalizedAd(openPersonalizedAd ? ALLOW_ALL : ALLOW_NON_PERSONALIZED).build();
        Log.i("PersonalizeEnable", TAG + " openPersonalizedAd 个性化开关: " + openPersonalizedAd);
        HwAds.setRequestOptions(build);

        HwAds.init(context);

        sendResult(key, true);
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

        RequestOptions.Builder builder = HwAds.getRequestOptions().toBuilder();
        if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
            boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
            Log.i("privacylaws", "coppa: " + coppa);
            builder.setTagForChildProtection(coppa ? 1 : 0);
        } else {
            builder.setTagForChildProtection(-1);
        }

        if (userParams.containsKey(AppKeyManager.GDPR_CONSENT)) {
            Integer gdpr = (Integer) userParams.get(AppKeyManager.GDPR_CONSENT);
            Log.i("privacylaws", "gdpr: " + (gdpr == TradPlus.PERSONALIZED));
            builder.setTagForUnderAgeOfPromise(gdpr == TradPlus.PERSONALIZED ? PROMISE_TRUE : PROMISE_FALSE);
        } else {
            builder.setTagForUnderAgeOfPromise(PROMISE_UNSPECIFIED);
        }

        HwAds.setRequestOptions(builder.build());

    }

    @Override
    public String getNetworkVersionCode() {
        return HwAds.getSDKVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Huaiwei";
    }
}
