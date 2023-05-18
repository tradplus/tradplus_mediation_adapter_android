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
            mPopConfirm = tpParams.get(AppKeyManager.POP_CONFIRM);
        }
        boolean openPersonalizedAd = GlobalTradPlus.getInstance().isOpenPersonalizedAd();
        Log.i("PersonalizeEnable", TAG + " openPersonalizedAd : " + openPersonalizedAd);

        if(isInited(mAppId)){
            boolean personalizeEnabled = KlevinManager.setPersonalizeEnabled(openPersonalizedAd);
            Log.i(TAG, "initSuccess : "+ personalizeEnabled);
            initCallback.onSuccess();
            return;
        }

        if(hasInit(mAppId,initCallback)){
            return;
        }


        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + mAppId);
        boolean privacyUserAgree = GlobalTradPlus.getInstance().isPrivacyUserAgree();
        Log.i(TAG, "mTTCustomController == null ? " + (mKlevinCustomController == null));
        Log.i(TAG, "privacyUserAgree : " + privacyUserAgree);
        KlevinConfig.Builder builder = new KlevinConfig.Builder()
                .appId(mAppId)
                .debugMode(TestDeviceUtil.getInstance().isNeedTestDevice())
                .directDownloadNetworkType(getDownloadConfirm(mPopConfirm))
                .customController(mKlevinCustomController == null ? new UserDataObtainController(privacyUserAgree) : getKlevinCustomController())
                .personalizeEnabled(openPersonalizedAd)
                .testEnv(TestDeviceUtil.getInstance().isNeedTestDevice());

        KlevinManager.init(GlobalTradPlus.getInstance().getContext(), builder.build(), new InitializationListener() {
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                sendResult(mAppId,true);
            }

            public void onError(int err, String msg) {
                Log.i(TAG, "onError: err:" + err + ", msg :" + msg);
                sendResult(mAppId,false,err+"",msg);

            }

            public void onIdentifier(boolean support, String oaid) {
            }

        });
    }

    public int getDownloadConfirm(String popConfirm){
        int confirm = Integer.parseInt(popConfirm);
        switch (confirm){
            case 0:
                return KlevinConfig.NETWORK_STATE_NONE;
            case 1:
                return KlevinConfig.NETWORK_STATE_NONE;
            case 2:
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
