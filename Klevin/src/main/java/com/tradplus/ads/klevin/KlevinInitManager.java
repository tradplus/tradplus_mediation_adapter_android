package com.tradplus.ads.klevin;

import android.content.Context;
import android.util.Log;

import com.tencent.klevin.KlevinConfig;
import com.tencent.klevin.KlevinCustomController;
import com.tencent.klevin.KlevinManager;
import com.tencent.klevin.listener.InitializationListener;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;

public class KlevinInitManager extends TPInitMediation {

    private static final String TAG = "Klevin";
    private String mAppId;
    private String mPopConfirm = "0";
    private static KlevinInitManager sInstance;
    public KlevinCustomController mKlevinCustomController;

    public synchronized static KlevinInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new KlevinInitManager();
        }
        return sInstance;
    }
    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {

        if (tpParams != null && tpParams.size() > 0) {
            mAppId = tpParams.get(AppKeyManager.APP_ID);
            //二次弹窗确认 0  默认 ； 1 指定 是 ； 2 指定 否
            mPopConfirm = tpParams.get(AppKeyManager.POP_CONFIRM);
        }
        // 设置个性化开关
        boolean openPersonalizedAd = GlobalTradPlus.getInstance().isOpenPersonalizedAd();
        Log.i("PersonalizeEnable", TAG + " openPersonalizedAd 个性化开关: " + openPersonalizedAd);

        if(isInited(mAppId)){
            // SDK初始化后控制个性化开关 设置是否成功：true-成功，false-失败
            boolean personalizeEnabled = KlevinManager.setPersonalizeEnabled(openPersonalizedAd);
            Log.i(TAG, "initSuccess 个性化开关是否设置成功: "+ personalizeEnabled);
            initCallback.onSuccess();
            return;
        }

        if(hasInit(mAppId,initCallback)){
            return;
        }


//        mAppId = "30008";
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + mAppId);
        boolean privacyUserAgree = GlobalTradPlus.getInstance().isPrivacyUserAgree();
        Log.i(TAG, "mTTCustomController == null ? " + (mKlevinCustomController == null));
        Log.i(TAG, "privacyUserAgree 隐私权限: " + privacyUserAgree);
        KlevinConfig.Builder builder = new KlevinConfig.Builder()
                .appId(mAppId) //【必须】AppId，AndroidManifest.xml中配置后可不调用
                .debugMode(TestDeviceUtil.getInstance().isNeedTestDevice()) //打开调试日志，上线前关闭,【可选】
                .directDownloadNetworkType(getDownloadConfirm(mPopConfirm))
                .customController(mKlevinCustomController == null ? new UserDataObtainController(privacyUserAgree) : getKlevinCustomController())
                .personalizeEnabled(openPersonalizedAd) //SDK初始化时控制个性化开关
                .testEnv(TestDeviceUtil.getInstance().isNeedTestDevice());

        KlevinManager.init(GlobalTradPlus.getInstance().getContext(), builder.build(), new InitializationListener() {
            public void onSuccess() {
                //初始化成功
                Log.i(TAG, "onSuccess: ");
                sendResult(mAppId,true);
            }

            public void onError(int err, String msg) {
                // 初始化失败，err是错误码，msg是描述信息
                Log.i(TAG, "onError: err:" + err + ", msg :" + msg);
                sendResult(mAppId,false,err+"",msg);

            }

            public void onIdentifier(boolean support, String oaid) {
                // support为true时，oaid返回设备的oaid。false时返回为空或错误码
            }

        });
    }

    public int getDownloadConfirm(String popConfirm){
        int confirm = Integer.parseInt(popConfirm);
        switch (confirm){
            case 0:
                //默认在任何网络下都会弹出合规二次确认弹框
                return KlevinConfig.NETWORK_STATE_NONE;
            case 1:
                //默认在任何网络下都会弹出合规二次确认弹框
                return KlevinConfig.NETWORK_STATE_NONE;
            case 2:
                //直接下载
                return KlevinConfig.NETWORK_STATE_ALL;
        }
        return KlevinConfig.NETWORK_STATE_NONE;
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

    }

    @Override
    public String getNetworkVersionCode() {
        return KlevinManager.getVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Klevin";
    }

    public KlevinCustomController getKlevinCustomController() {
        return mKlevinCustomController;
    }

    public void setKlevinCustomController(KlevinCustomController klevinCustomController) {
        mKlevinCustomController = klevinCustomController;
    }
}
