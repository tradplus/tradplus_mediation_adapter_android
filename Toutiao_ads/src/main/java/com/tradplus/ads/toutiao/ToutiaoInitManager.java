package com.tradplus.ads.toutiao;

import android.content.Context;
import android.util.Log;

import com.bytedance.sdk.openadsdk.TTAdConfig;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTCustomController;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPDownloadConfirm;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class ToutiaoInitManager extends TPInitMediation {
    private static final String TAG = "Toutiao";
    private static ToutiaoInitManager sInstance;
    private static boolean isConfirmDownload = true;
    private String appId;
    private static int mPopConfirm; //二次弹窗确认 0  默认 ； 1 指定 是 ； 2 指定 否
    public TTCustomController mTTCustomController;

    public synchronized static ToutiaoInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new ToutiaoInitManager();
        }
        return sInstance;
    }


    public static void isConfirmDownload(boolean isConfirm) {
        isConfirmDownload = isConfirm;
    }

    private TTAdConfig buildConfig(Context context, String appId) {
        boolean privacyUserAgree = GlobalTradPlus.getInstance().isPrivacyUserAgree();
        Log.i(TAG, "mTTCustomController == null ? " + (mTTCustomController == null) + "，privacyUserAgree :" + privacyUserAgree);
        TTAdConfig.Builder ttAdConfig = new TTAdConfig.Builder();
        ttAdConfig.appId(appId)
                .appName(context.getPackageManager().getApplicationLabel(context.getApplicationInfo()).toString())
                .useTextureView(true)
                .debug(TestDeviceUtil.getInstance().isNeedTestDevice())
                .titleBarTheme(TTAdConstant.TITLE_BAR_THEME_DARK)
                .allowShowNotify(true)
                .supportMultiProcess(false)
                .directDownloadNetworkType(getDownloadConfirm(mPopConfirm))
                .needClearTaskReset()
                // 隐私信息开关
                .customController(mTTCustomController == null ? new UserDataCustomController(privacyUserAgree) : getTTCustomController())
                .data(getData());
        return ttAdConfig.build();

    }

    private String getData() {
        try {
            boolean openPersonalizedAd = GlobalTradPlus.getInstance().isOpenPersonalizedAd();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", "personal_ads_type");
            jsonObject.put("value", openPersonalizedAd ? "1" : "0");
            Log.i("PersonalizeEnable", TAG + " openPersonalizedAd 个性化开关: " + openPersonalizedAd);
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(jsonObject);
            return jsonArray.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private static int[] getDownloadConfirm(int popConfirm) {
        if (popConfirm == 0) {
            // 默认，SDK端用户设置
            if (!isConfirmDownload || !TPDownloadConfirm.getInstance().isToutiaoConfirmDownload()) {
                //不弹窗: 当isConfirmDownload ==false （Android原生） 或者 当isToutiaoConfirmDownload == false（Android Unity端）
                return new int[]{
                        TTAdConstant.NETWORK_STATE_MOBILE, TTAdConstant.NETWORK_STATE_2G, TTAdConstant.NETWORK_STATE_3G,
                        TTAdConstant.NETWORK_STATE_WIFI, TTAdConstant.NETWORK_STATE_4G
                };
            } else {
                // 二次弹窗确认
                return new int[]{
                        TTAdConstant.NETWORK_STATE_2G
                };
            }
        } else if (popConfirm == 2) {
            // 服务器下发：不弹窗
            return new int[]{
                    TTAdConstant.NETWORK_STATE_MOBILE, TTAdConstant.NETWORK_STATE_2G, TTAdConstant.NETWORK_STATE_3G,
                    TTAdConstant.NETWORK_STATE_WIFI, TTAdConstant.NETWORK_STATE_4G
            };
        } else {
            // 服务器下发：二次弹窗确认
            return new int[]{
                    TTAdConstant.NETWORK_STATE_2G
            };
        }
    }

    public static int popConfim(int popConfirm) {
        if (popConfirm == 0) {
            // 默认，SDK端用户设置
            if (!isConfirmDownload || !TPDownloadConfirm.getInstance().isToutiaoConfirmDownload()) {
                //不弹窗: 当isConfirmDownload ==false （Android原生） 或者 当isToutiaoConfirmDownload == false（Android Unity端）
                return ToutiaoConstant.DOWNLOAD_TYPE_NO_POPUP; // 对于应用的下载不做特殊处理；
            } else {
                // 二次弹窗确认
                return ToutiaoConstant.DOWNLOAD_TYPE_POPUP;// 应用每次下载都需要触发弹窗披露应用信息（不含跳转商店的场景），该配置优先级高于下载网络弹窗配置；
            }
        } else if (popConfirm == 2) {
            // 服务器下发：不弹窗
            return ToutiaoConstant.DOWNLOAD_TYPE_NO_POPUP;
        } else {
            // 服务器下发：二次弹窗确认
            return ToutiaoConstant.DOWNLOAD_TYPE_POPUP;
        }
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, final TPInitMediation.InitCallback initCallback) {

        if (tpParams != null && tpParams.size() > 0) {
            appId = tpParams.get(AppKeyManager.APP_ID);
            mPopConfirm = Integer.parseInt(tpParams.get(AppKeyManager.POP_CONFIRM));
        }

        if (TTAdSdk.isInitSuccess()) {
            // 头条初始化成功后，用户需要再次设置个性化，直接调用该方法，避免多次初始化
            // 是否屏蔽个性化推荐广告接口进行设置
            // 注意使用该方法会覆盖之前初始化sdk的配置的data值
            TTAdConfig ttAdConfig = new TTAdConfig.Builder()
                    .data(getData())
                    .build();
            TTAdSdk.updateAdConfig(ttAdConfig);
        }

        if (isInited(appId)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(appId, initCallback)) {
            return;
        }


//        appId = "5001121";
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + appId);
        TTAdSdk.init(context, buildConfig(context, appId), new TTAdSdk.InitCallback() {
            @Override
            public void success() {
                Log.i(TAG, "success: ");
                sendResult(appId, true);
            }

            @Override
            public void fail(int code, String msg) {
                Log.i(TAG, "fail: code :" + code + ",msg :" + code);
                sendResult(appId, false, code + "", msg);

            }
        });
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

    }

    @Override
    public String getNetworkVersionCode() {
        return TTAdSdk.getAdManager().getSDKVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Toutiao";
    }

    public TTCustomController getTTCustomController() {
        return mTTCustomController;
    }

    public void setTTCustomController(TTCustomController TTCustomController) {
        mTTCustomController = TTCustomController;
    }
}
