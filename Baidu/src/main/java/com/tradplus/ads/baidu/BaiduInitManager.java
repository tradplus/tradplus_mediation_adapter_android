package com.tradplus.ads.baidu;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.mobads.sdk.api.AdSettings;
import com.baidu.mobads.sdk.api.BDAdConfig;
import com.baidu.mobads.sdk.api.BDDialogParams;
import com.baidu.mobads.sdk.api.MobadsPermissionSettings;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;

public class BaiduInitManager extends TPInitMediation {

    private static final String TAG = "Baidu";
    private String mAppId;
    private static BaiduInitManager sInstance;
    private boolean isHttps = true; //Baidu SDK默认请求https广告，若需要请求http广告,设置false
    private int dlDialogType = BDDialogParams.TYPE_BOTTOM_POPUP; //弹窗样式类型，默认底部呼起
    private int dlDialogAnimStyle = BDDialogParams.ANIM_STYLE_NONE; //弹窗按钮动效类型，默认无动效
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public synchronized static BaiduInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new BaiduInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {
        boolean openPersonalizedAd = GlobalTradPlus.getInstance().isOpenPersonalizedAd();
        MobadsPermissionSettings.setLimitPersonalAds(openPersonalizedAd);
        Log.i("PersonalizeEnable", TAG +" openPersonalizedAd 个性化开关: " + openPersonalizedAd);

        if (tpParams != null && tpParams.size() > 0) {
            mAppId = tpParams.get(AppKeyManager.APP_ID);
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(BaiduConstant.BAIDU_HTTPS)) {
                isHttps = (boolean) userParams.get(BaiduConstant.BAIDU_HTTPS);
                Log.i(TAG, "isHttps:" + isHttps);
            }

            if (userParams.containsKey(BaiduConstant.BAIDU_DIALOG_TYPE)) {
                dlDialogType = (int) userParams.get(BaiduConstant.BAIDU_DIALOG_TYPE);
                Log.i(TAG, "dlDialogType: " + dlDialogType);
            }

            if (userParams.containsKey(BaiduConstant.BAIDU_DIALOG_ANIMSTYLE)) {
                dlDialogAnimStyle = (int) userParams.get(BaiduConstant.BAIDU_DIALOG_ANIMSTYLE);
                Log.i(TAG, "dlDialogAnimStyle: " + dlDialogAnimStyle);
            }
        }

        if (isInited(mAppId)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(mAppId, initCallback)) {
            return;
        }
        String wxAppId = GlobalTradPlus.getInstance().getWxAppId();
        Log.i("WxAppID", "wxAppId: " +wxAppId);
        boolean needTestDevice = TestDeviceUtil.getInstance().isNeedTestDevice();
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + mAppId);
        BDAdConfig bdAdConfig = new BDAdConfig.Builder()
                .setAppsid(mAppId)
                .setHttps(isHttps)
                .setDialogParams(new BDDialogParams.Builder()
                        .setDlDialogType(dlDialogType)
                        .setDlDialogAnimStyle(dlDialogAnimStyle)
                        .build())
                .setDebug(needTestDevice)
                .setWXAppid(!TextUtils.isEmpty(wxAppId) ? wxAppId : "")
                .build(context);

        bdAdConfig.init();

        sendResult(mAppId, true);
//        postDelayResult(mAppId,1000); Bidding C2S 会卡住
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

    }

    @Override
    public String getNetworkVersionCode() {
        return AdSettings.getSDKVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Baidu";
    }
}
