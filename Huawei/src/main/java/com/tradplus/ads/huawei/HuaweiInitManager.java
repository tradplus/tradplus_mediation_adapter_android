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

        // ALLOW_ALL = 0：个性化广告与非个性化广告
        // ALLOW_NON_PERSONALIZED = 1：非个性化广告
        // 当nonPersonalizedAd为0时，使用hwNonPersonalizedAd和thirdNonPersonalizedAd设置个性化广告是有效的。
        boolean openPersonalizedAd = GlobalTradPlus.getInstance().isOpenPersonalizedAd();
        RequestOptions build = HwAds.getRequestOptions().toBuilder()
                .setNonPersonalizedAd(openPersonalizedAd ? ALLOW_ALL : ALLOW_NON_PERSONALIZED).build();
        Log.i("PersonalizeEnable", TAG + " openPersonalizedAd 个性化开关: " + openPersonalizedAd);
        HwAds.setRequestOptions(build);

        // 初始化HUAWEI Ads SDK
        HwAds.init(context);

        sendResult(key, true);
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

        RequestOptions.Builder builder = HwAds.getRequestOptions().toBuilder();
        //PROTECTION_TRUE：表明您的广告内容需要符合COPPA的规定。
        //PROTECTION_FALSE：表明您的广告内容不需要符合COPPA的规定。
        //PROTECTION_UNSPECIFIED：表明您不希望明确您的广告内容是否需要符合COPPA的规定。
        if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
            boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
            Log.i("privacylaws", "coppa: " + coppa);
            builder.setTagForChildProtection(coppa ? 1 : 0);
        } else {
            builder.setTagForChildProtection(-1);
        }

        //PROMISE_TRUE = 1：表明您希望按适合未达到法定承诺年龄的用户的方式来处理广告请求。
        //PROMISE_FALSE = 0;：表明您不希望按适合未达到法定承诺年龄的用户的方式来处理广告请求。
        //PROMISE_UNSPECIFIED = -1：表明您未明确是否按适合未达到法定承诺年龄的用户的方式来处理广告请求。
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
