package com.tradplus.ads.baidu;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.baidu.mobads.sdk.api.AdSettings;
import com.baidu.mobads.sdk.api.AdSize;
import com.baidu.mobads.sdk.api.AdView;
import com.baidu.mobads.sdk.api.AdViewListener;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import com.tradplus.ads.base.util.TradPlusDataConstants;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import org.json.JSONObject;

import java.util.Map;

public class BaiduBanner extends TPBannerAdapter {

    private String mPlacementId;
    private TPBannerAdImpl mTpBannerAd;
    private AdView adView;
    private String mAdSize = TradPlusDataConstants.BANNER;
    private int scaledWidth;
    private int scaledHeight;
    private static final String TAG = "BaiduBanner";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (tpParams.get(AppKeyManager.ADSIZE + mPlacementId) != null) {
                mAdSize = tpParams.get(AppKeyManager.ADSIZE + mPlacementId);
            }
            Log.i(TAG, "BannerSize: " + mAdSize);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        BaiduInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestBanner();
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });
    }

    private void requestBanner() {
        /*cxt : 传入Activity的context
         *attrs : 传入null
         *autoplay : 是否自动播放 横幅会自动刷新，刷新间隔30秒 必须关掉
         *size : 枚举类型，用于指定横幅或方形
         *adPlaceId : 广告位
         */
        //注意：只有将AdView添加到布局中后，才会有广告返回
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }
        adView = new AdView(activity, null, false, AdSize.Banner, mPlacementId);
        adView.setListener(new AdViewListener() {
            @Override
            public void onAdReady(AdView adView) {
                // 资源已经缓存完毕，还没有渲染出来
                Log.i(TAG, "onAdReady: ");

            }

            @Override
            public void onAdShow(JSONObject jsonObject) {
                // 广告已经渲染出来
                if (mLoadAdapterListener != null) {
                    mTpBannerAd = new TPBannerAdImpl(null, adView);
                    mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "onAdShow: ");
                        if (mTpBannerAd != null) {
                            mTpBannerAd.adShown();
                        }
                    }
                }, 1000);


            }

            @Override
            public void onAdClick(JSONObject jsonObject) {
                Log.i(TAG, "onAdClick: ");
                if (mTpBannerAd != null) {
                    mTpBannerAd.adClicked();
                }
            }

            @Override
            public void onAdFailed(String s) {
                Log.i(TAG, "onAdFailed: " + s);

                if (mAdContainerView != null) {
                    mAdContainerView.removeView(adView);
                }

                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage(s);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void onAdSwitch() {
                Log.i(TAG, "onAdSwitch: ");
            }

            @Override
            public void onAdClose(JSONObject jsonObject) {
                Log.i(TAG, "onAdClose: ");
                if (mTpBannerAd != null) {
                    mTpBannerAd.adClosed();
                }
            }
        });

        calculateAdSize(Integer.parseInt(mAdSize));

        if (mAdContainerView != null) {
            DisplayMetrics dm = new DisplayMetrics();
            ((WindowManager) (activity).getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
            int winW = dm.widthPixels;
            int winH = dm.heightPixels;
            int width = Math.min(winW, winH);
            int height = width * scaledHeight / scaledWidth;
            // 将adView添加到父控件中(注：该父控件不一定为您的根控件，只要该控件能通过addView能添加广告视图即可)
            RelativeLayout.LayoutParams rllp = new RelativeLayout.LayoutParams(width, height);
            rllp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            mAdContainerView.addView(adView, rllp);
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(NETWORK_NO_FILL));
            }
        }


    }

    private void calculateAdSize(int adSize) {
        switch (adSize) {
            case 1:
                scaledWidth = 20;
                scaledHeight = 3;
                break;
            case 2:
                scaledWidth = 3;
                scaledHeight = 2;
                break;
            case 3:
                scaledWidth = 7;
                scaledHeight = 3;
                break;
            case 4:
                scaledWidth = 2;
                scaledHeight = 1;
                break;
            default:
                scaledWidth = 20;
                scaledHeight = 3;
        }
    }

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
        if (adView != null) {
            adView.destroy();
            adView.setListener(null);
            adView = null;
        }


    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_BAIDU);
    }

    @Override
    public String getNetworkVersion() {
        return AdSettings.getSDKVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }


}
