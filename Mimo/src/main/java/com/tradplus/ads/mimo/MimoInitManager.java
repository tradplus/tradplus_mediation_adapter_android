package com.tradplus.ads.mimo;

import android.content.Context;
import android.util.Log;

import com.miui.zeus.mimo.sdk.BuildConfig;
import com.miui.zeus.mimo.sdk.MimoSdk;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class MimoInitManager extends TPInitMediation {

    private static final String TAG = "Mimo";
    private String mAppId;
    private static MimoInitManager sInstance;

    public synchronized static MimoInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new MimoInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {
        String customAs = RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_MIMO);

        removeInited(customAs);

        if (isInited(customAs)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(customAs, initCallback)) {
            return;
        }
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: customAs :" + customAs);
        MimoSdk.init(context, new MimoSdk.InitCallback() {
            @Override
            public void success() {
                sendResult(customAs, true);
            }

            @Override
            public void fail(int code, String msg) {
                sendResult(customAs, false,code+ "" ,msg);
            }
        });
        // 新版本设置Debug必须在MimoSdk后调用 MimoSdk & GlobalHolder must init before using
        MimoSdk.setDebugOn(TestDeviceUtil.getInstance().isNeedTestDevice()); //打开调试，输出调试信息
        MimoSdk.setStagingOn(TestDeviceUtil.getInstance().isNeedTestDevice());//打开请求小米测试服务器
        boolean openPersonalizedAd = GlobalTradPlus.getInstance().isOpenPersonalizedAd();
        MimoSdk.setPersonalizedAdEnabled(openPersonalizedAd); // 个性化广告，可以在任意位置设置
        Log.i("PersonalizeEnable",  TAG + " openPersonalizedAd 个性化开关: " + openPersonalizedAd);

    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

    }

    @Override
    public String getNetworkVersionCode() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public String getNetworkVersionName() {
        return "Mimo";
    }


}
