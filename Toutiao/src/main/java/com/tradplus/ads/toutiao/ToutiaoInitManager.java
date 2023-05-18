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
    private static int mPopConfirm;
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
            if (!isConfirmDownload || !TPDownloadConfirm.getInstance().isToutiaoConfirmDownload()) {
                return new int[]{
                        TTAdConstant.NETWORK_STATE_MOBILE, TTAdConstant.NETWORK_STATE_2G, TTAdConstant.NETWORK_STATE_3G,
                        TTAdConstant.NETWORK_STATE_WIFI, TTAdConstant.NETWORK_STATE_4G
                };
            } else {
                return new int[]{
                        TTAdConstant.NETWORK_STATE_2G
                };
            }
        } else if (popConfirm == 2) {
            return new int[]{
                    TTAdConstant.NETWORK_STATE_MOBILE, TTAdConstant.NETWORK_STATE_2G, TTAdConstant.NETWORK_STATE_3G,
                    TTAdConstant.NETWORK_STATE_WIFI, TTAdConstant.NETWORK_STATE_4G
            };
        } else {
            return new int[]{
                    TTAdConstant.NETWORK_STATE_2G
            };
        }
    }

    public static int popConfim(int popConfirm) {
        if (popConfirm == 0) {
            if (!isConfirmDownload || !TPDownloadConfirm.getInstance().isToutiaoConfirmDownload()) {
                return ToutiaoConstant.DOWNLOAD_TYPE_NO_POPUP;
            } else {
                return ToutiaoConstant.DOWNLOAD_TYPE_POPUP;
            }
        } else if (popConfirm == 2) {
            return ToutiaoConstant.DOWNLOAD_TYPE_NO_POPUP;
        } else {
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
