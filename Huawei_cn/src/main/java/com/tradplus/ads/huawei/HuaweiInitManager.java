package com.tradplus.ads.huawei;

import static com.huawei.hms.ads.NonPersonalizedAd.ALLOW_ALL;
import static com.huawei.hms.ads.NonPersonalizedAd.ALLOW_NON_PERSONALIZED;

import android.content.Context;
import android.util.Log;

import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.RequestOptions;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
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

    }

    @Override
    public String getNetworkVersionCode() {
        return HwAds.getSDKVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Huawei";
    }
}
