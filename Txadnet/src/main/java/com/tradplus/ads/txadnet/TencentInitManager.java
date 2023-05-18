package com.tradplus.ads.txadnet;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.comm.managers.setting.GlobalSetting;
import com.qq.e.comm.managers.status.SDKStatus;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.HashMap;
import java.util.Map;

public class TencentInitManager extends TPInitMediation {
    private static final String TAG = "Tencent";
    private String mAppId;
    private static TencentInitManager sInstance;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public synchronized static TencentInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new TencentInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {
        boolean openPersonalizedAd = GlobalTradPlus.getInstance().isOpenPersonalizedAd();
        // 1 屏蔽个性化推荐广告
        // 0和所有非1的值: 不屏蔽个性化推荐广告
        GlobalSetting.setPersonalizedState(openPersonalizedAd ? 0 : 1);
        Log.i("PersonalizeEnable", TAG + " openPersonalizedAd 个性化开关: " + openPersonalizedAd);
        // false表示不允许获取，true表示允许获取，默认允许获取
        // 该接口只在应用第一次设置时生效，如需更改要重启应用重新设置
        boolean privacyUserAgree = GlobalTradPlus.getInstance().isPrivacyUserAgree();
//        GlobalSetting.setAgreePrivacyStrategy(privacyUserAgree); // 广告没有办法定向对收入有影响
        Log.i("PersonalizeEnable", TAG + " privacyUserAgree 隐私权限: " + privacyUserAgree);
        Map<String, Boolean> params = new HashMap();
        params.put("mac_address", privacyUserAgree); // 采集mac地址
        params.put("android_id", privacyUserAgree); // 采集android_id
        params.put("device_id", privacyUserAgree); // 采集device_id
        GlobalSetting.setAgreeReadPrivacyInfo(params);

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


        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + mAppId);

        GDTAdSdk.init(context, mAppId);

        sendResult(mAppId, true);
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

    }

    @Override
    public String getNetworkVersionCode() {
        return SDKStatus.getIntegrationSDKVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "GDT";
    }
}
