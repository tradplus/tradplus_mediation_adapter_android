package com.tradplus.ads.awesome;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import tv.superawesome.sdk.publisher.AwesomeAds;
import tv.superawesome.sdk.publisher.SAVersion;
import tv.superawesome.sdk.publisher.SAVideoAd;

public class AwesomeInitManager extends TPInitMediation {

    private static final String TAG = "Awesome";
    private String mAppId;
    private static AwesomeInitManager sInstance;

    public synchronized static AwesomeInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new AwesomeInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {

        final String customAs = RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_AWESOME);

        if (isInited(customAs)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(customAs, initCallback)) {
            return;
        }

        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + customAs);

        // 参数2 ：是否开启log日志
        boolean needTestDevice = TestDeviceUtil.getInstance().isNeedTestDevice();
        AwesomeAds.init((Application) context.getApplicationContext(), needTestDevice);
        if (needTestDevice) {
            SAVideoAd.enableTestMode();
        }
        sendResult(customAs, true);
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

    }

    @Override
    public String getNetworkVersionCode() {
        return SAVersion.getSDKVersion("awesome");
    }

    @Override
    public String getNetworkVersionName() {
        return "Awesome";
    }
}
