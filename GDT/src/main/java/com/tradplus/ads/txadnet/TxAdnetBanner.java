package com.tradplus.ads.txadnet;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.qq.e.ads.banner2.UnifiedBannerADListener;
import com.qq.e.ads.banner2.UnifiedBannerView;
import com.qq.e.comm.compliance.DownloadConfirmListener;
import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.comm.managers.status.SDKStatus;
import com.qq.e.comm.util.AdError;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;


public class TxAdnetBanner extends TPBannerAdapter implements UnifiedBannerADListener {

    private static final String TAG = "GDTBanner";
    private String mPlacementId;
    private UnifiedBannerView mBannerView;

    private boolean isDestory;
    private TPBannerAdImpl nTPBannerAd;
    private String payload;
    private String price;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (serverExtrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            price = tpParams.get(DataKeys.BIDDING_PRICE);

            setAdHeightAndWidthByService(mPlacementId,tpParams);
            setDefaultAdSize(320,50);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        TencentInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                reqeustAd();
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void reqeustAd() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }
        if (TextUtils.isEmpty(payload)) {
            mBannerView = new UnifiedBannerView(activity, mPlacementId, this);
        } else {
            mBannerView = new UnifiedBannerView(activity, mPlacementId, this, null, payload);
        }
        mBannerView.setRefresh(0);
        mBannerView.loadAD();
    }

    @Override
    public void clean() {
        isDestory = true;
        if (mBannerView != null) {
            mBannerView.destroy();
            mBannerView = null;
        }
    }

    @Override
    public void onNoAD(AdError adError) {
        Log.i(TAG, "onNoAD: errorCode ：" + adError.getErrorCode() + ", errorMessage : " + adError.getErrorMsg());
        if (mLoadAdapterListener != null && !isDestory) {
            mLoadAdapterListener.loadAdapterLoadFailed(TxAdnetErrorUtil.getTradPlusErrorCode(adError));
        }
    }

    @Override
    public void onADReceive() {
        Log.i(TAG, "onADReceive: ");
        setBidEcpm();
        if (mLoadAdapterListener != null) {
            nTPBannerAd = new TPBannerAdImpl(null, mBannerView);
            mLoadAdapterListener.loadAdapterLoaded(nTPBannerAd);
        }
    }

    @Override
    public void onADExposure() {
        Log.i(TAG, "onADExposure: ");
        if (nTPBannerAd != null)
            nTPBannerAd.adShown();
    }

    @Override
    public void onADClosed() {
        Log.i(TAG, "onADClosed: ");
        if (nTPBannerAd != null)
            nTPBannerAd.adClosed();
    }

    @Override
    public void onADClicked() {
        Log.i(TAG, "onADClicked: ");
        if (nTPBannerAd != null) {
            nTPBannerAd.adClicked();
        }
    }

    @Override
    public void onADLeftApplication() {
        Log.i(TAG, "onADLeftApplication: ");
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_TENCENT);
    }

    @Override
    public String getNetworkVersion() {
        return SDKStatus.getIntegrationSDKVersion();
    }

    private boolean serverExtrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    @Override
    public void setNetworkExtObj(Object obj) {
        if (obj instanceof DownloadConfirmListener) {
            Log.i(TAG, "DownloadConfirmListener: ");
            if (mBannerView != null) {
                mBannerView.setDownloadConfirmListener((DownloadConfirmListener) obj);
            }
        }

    }

    private void setBidEcpm() {
        try {
            float temp = Float.parseFloat(price);
            int price = (int)temp;
            Log.i(TAG, "setBidEcpm: " + price);
            mBannerView.setBidECPM(price);
        } catch (Exception e) {

        }
    }

    @Override
    public String getBiddingToken(Context context, Map<String, String> tpParams) {
        if (tpParams == null) {
            return "";
        }
        String appId = tpParams.get(AppKeyManager.APP_ID);
        if(!TencentInitManager.isInited(appId)) {
            GDTAdSdk.init(context, appId);
        }
        return GDTAdSdk.getGDTAdManger().getBuyerId(null);
    }

    @Override
    public String getBiddingNetworkInfo(Context context, Map<String, String> tpParams) {
        if (tpParams == null) {
            return "";
        }

        if (tpParams.containsKey(AppKeyManager.AD_PLACEMENT_ID)) {
            String placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            try {
                return GDTAdSdk.getGDTAdManger().getSDKInfo(placementId);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }
        return "";
    }
}
