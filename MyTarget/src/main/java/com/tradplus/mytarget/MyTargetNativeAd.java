package com.tradplus.mytarget;

import static com.tradplus.ads.base.util.AppKeyManager.PLACEMENT_AD_TYPE;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import com.my.target.common.MyTargetManager;
import com.my.target.common.MyTargetVersion;
import com.my.target.nativeads.AdChoicesPlacement;
import com.my.target.nativeads.NativeAd;
import com.my.target.nativeads.NativeBannerAd;
import com.my.target.nativeads.banners.NativeBanner;
import com.my.target.nativeads.banners.NativePromoBanner;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class MyTargetNativeAd extends TPNativeAdapter {

    public static final String TAG = "MyTargetNative";
    private MyTargetNativeData myTargetNativeData;
    private String mSlotId;
    private String secType;
    private NativeAd mNativeAd;
    private NativeBannerAd nativeBannerAd;
    private String payload;
    private int isNative;
    private int adChoicesPosition = 1;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mSlotId = tpParams.get(AppKeyManager.AD_SLOT_ID);
            secType = tpParams.get(AppKeyManager.ADTYPE_SEC);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            isNative = Integer.parseInt(tpParams.get(PLACEMENT_AD_TYPE));
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(MyTargetConstant.ADCHOICES_POSITION)) {
                adChoicesPosition = (int) userParams.get(MyTargetConstant.ADCHOICES_POSITION);
                Log.i(TAG, "adChoicesPosition: " + adChoicesPosition);
            }
        }


        MyTargetInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestNative(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestNative(Context context) {
        if (secType == null || !secType.equals(AppKeyManager.NATIVE_TYPE_NATIVEBANNER)) {
            if (isNative == 1) {
                loadNativeBannerAd(context, mSlotId);
            } else {
                loadNativeAd(context, mSlotId);
            }
        } else {
            loadNativeBannerAd(context, mSlotId);
        }
    }


    private void loadNativeAd(final Context context, String mSlotId) {
        mNativeAd = new NativeAd(Integer.parseInt(mSlotId), context);
        mNativeAd.setListener(new NativeAd.NativeAdListener() {
            @Override
            public void onLoad(NativePromoBanner nativePromoBanner, NativeAd nativeAd) {
                Log.i(TAG, "Native onLoad: ");
                if (mLoadAdapterListener == null) return;

                if (nativePromoBanner != null && nativeAd != null) {
                    myTargetNativeData = new MyTargetNativeData(context, nativePromoBanner, nativeAd);
                    mLoadAdapterListener.loadAdapterLoaded(myTargetNativeData);
                } else {
                    TPError tpError = new TPError(TPError.UNSPECIFIED);
                    tpError.setErrorMessage("Native onLoad,but  nativePromoBanner == null or nativeAd == null");
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }

            }

            @Override
            public void onNoAd(String s, NativeAd nativeAd) {
                Log.i(TAG, "onNoAd: " + s);
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(TPError.NETWORK_NO_FILL);
                    tpError.setErrorMessage(s);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void onClick(NativeAd nativeAd) {
                Log.i(TAG, "onClick: ");
                if (myTargetNativeData != null) {
                    myTargetNativeData.onAdViewClicked();
                }
            }

            @Override
            public void onShow(NativeAd nativeAd) {
                Log.i(TAG, "onShow: ");
                if (myTargetNativeData != null) {
                    myTargetNativeData.onAdViewExpanded();
                }
            }

            @Override
            public void onVideoPlay(NativeAd nativeAd) {
                Log.i(TAG, "onVideoPlay: ");
                if (myTargetNativeData != null) {
                    myTargetNativeData.onAdVideoStart();
                }
            }

            @Override
            public void onVideoPause(NativeAd nativeAd) {
                Log.i(TAG, "onVideoPause: ");
            }

            @Override
            public void onVideoComplete(NativeAd nativeAd) {
                Log.i(TAG, "onVideoComplete: ");
                if (myTargetNativeData != null) {
                    myTargetNativeData.onAdVideoEnd();
                }
            }
        });

        mNativeAd.setAdChoicesPlacement(adChoicesPostition(adChoicesPosition));
        if (TextUtils.isEmpty(payload)) {
            mNativeAd.load();
        } else {
            mNativeAd.loadFromBid(payload);
        }

    }

    private void loadNativeBannerAd(final Context context, String mSlotId) {
        nativeBannerAd = new NativeBannerAd(Integer.parseInt(mSlotId), context);
        nativeBannerAd.setListener(new NativeBannerAd.NativeBannerAdListener() {
            @Override
            public void onLoad(NativeBanner nativeBanner, NativeBannerAd nativeBannerAd) {
                Log.i(TAG, "NativeBanner onLoad: ");
                if (mLoadAdapterListener == null) return;

                if (nativeBanner != null && nativeBannerAd != null) {
                    myTargetNativeData = new MyTargetNativeData(context, nativeBanner, nativeBannerAd);
                    mLoadAdapterListener.loadAdapterLoaded(myTargetNativeData);
                }else {
                    TPError tpError = new TPError(TPError.UNSPECIFIED);
                    tpError.setErrorMessage("NativeBanner onLoad,but NativeBanner == null or NativeBannerAd == null");
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void onNoAd(String s, NativeBannerAd nativeBannerAd) {
                Log.i(TAG, "onNoAd: " + s);
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(TPError.NETWORK_NO_FILL);
                    tpError.setErrorMessage(s);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void onClick(NativeBannerAd nativeBannerAd) {
                Log.i(TAG, "onClick: ");
                if (myTargetNativeData != null) {
                    myTargetNativeData.onAdViewClicked();
                }
            }

            @Override
            public void onShow(NativeBannerAd nativeBannerAd) {
                Log.i(TAG, "onShow: ");
                if (myTargetNativeData != null) {
                    myTargetNativeData.onAdViewExpanded();
                }
            }
        });

        nativeBannerAd.setAdChoicesPlacement(adChoicesPostition(adChoicesPosition));
        if (TextUtils.isEmpty(payload)) {
            nativeBannerAd.load();
        } else {
            nativeBannerAd.loadFromBid(payload);
        }
    }

    @Override
    public void clean() {
        if (nativeBannerAd != null) {
            nativeBannerAd.setListener(null);
            nativeBannerAd.unregisterView();
            nativeBannerAd = null;
        }

        if (mNativeAd != null) {
            mNativeAd.setListener(null);
            mNativeAd.unregisterView();
            mNativeAd = null;
        }
    }

    private int adChoicesPostition(int postion) {
        if (postion == 0) {
            return AdChoicesPlacement.TOP_LEFT;
        } else if (postion == 2) {
            return AdChoicesPlacement.BOTTOM_RIGHT;
        } else if (postion == 3) {
            return AdChoicesPlacement.BOTTOM_LEFT;
        }
        return AdChoicesPlacement.TOP_RIGHT;
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_MYTARGET);
    }

    @Override
    public String getNetworkVersion() {
        return MyTargetVersion.VERSION;
    }

    @Override
    public String getBiddingToken() {
        Context context = GlobalTradPlus.getInstance().getContext();
        return MyTargetManager.getBidderToken(context);
    }
}
