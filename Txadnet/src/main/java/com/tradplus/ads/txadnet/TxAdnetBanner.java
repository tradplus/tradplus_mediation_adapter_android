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
        //推荐您将 Banner 的宽高比固定为 6.4:1 以获得最佳的广告展示效果
//        mBannerView.setLayoutParams(new FrameLayout.LayoutParams(DeviceUtils.dip2px(activity, mAdWidth), DeviceUtils.dip2px(activity, mAdHeight)));
        //默认自动刷新频率30秒一次，0标识 不自动轮播
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
        // 广告加载失败，error 对象包含了错误码和错误信息
        Log.i(TAG, "onNoAD: errorCode ：" + adError.getErrorCode() + ", errorMessage : " + adError.getErrorMsg());
        if (mLoadAdapterListener != null && !isDestory) {
            mLoadAdapterListener.loadAdapterLoadFailed(TxAdnetErrorUtil.getTradPlusErrorCode(adError));
        }
    }

    @Override
    public void onADReceive() {
        // 广告加载成功回调，表示广告相关的资源已经加载完毕，Ready To Show
        Log.i(TAG, "onADReceive: ");
        setBidEcpm();
        if (mLoadAdapterListener != null) {
            nTPBannerAd = new TPBannerAdImpl(null, mBannerView);
            mLoadAdapterListener.loadAdapterLoaded(nTPBannerAd);
        }
    }

    @Override
    public void onADExposure() {
        // 当广告曝光时发起的回调
        Log.i(TAG, "onADExposure: ");
        if (nTPBannerAd != null)
            nTPBannerAd.adShown();
    }

    @Override
    public void onADClosed() {
        // 当广告关闭时调用
        Log.i(TAG, "onADClosed: ");
        if (nTPBannerAd != null)
            nTPBannerAd.adClosed();
    }

    @Override
    public void onADClicked() {
        // 当广告点击时发起的回调，由于点击去重等原因可能和平台最终的统计数据有差异
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
