//package com.tradplus.ads.mintegral;
//
//
//import android.app.Activity;
//import android.content.Context;
//import android.text.TextUtils;
//import android.util.Log;
//
//import com.mintegral.msdk.MIntegralConstans;
//import com.mintegral.msdk.MIntegralSDK;
//import com.mintegral.msdk.out.MIntegralSDKFactory;
//import com.mintegral.msdk.out.MtgWallHandler;
//import com.tradplus.ads.common.LifecycleListener;
//import com.tradplus.ads.mobileads.CustomEventInterstitial;
//import com.tradplus.ads.base.TradPlus;
//import com.tradplus.ads.base.common.TPError;
//import com.tradplus.ads.base.util.AppKeyManager;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class MitegralOfferWall extends CustomEventInterstitial  {
//
//
//    private String mAppKey,mUnitId,mAppId,mPlacementId;
//    private MIntegralInterstitialCallbackRouter mCallbackRouter;
//    private MtgWallHandler mOfferWallHandler;
//    private Map<String, Object> properties;
//
//    @Override
//    protected void loadInterstitial(Context context,
//                                    CustomEventInterstitialListener customEventInterstitialListener,
//                                    Map<String, Object> localExtras,
//                                    Map<String, String> serverExtras) {
//
//        Activity activity = (Activity) context;
//        mCallbackRouter = MIntegralInterstitialCallbackRouter.getInstance();
//
//        if (serverExtras != null && serverExtras.size() > 0) {
//            mAppKey = (String) serverExtras.get(AppKeyManager.APP_KEY);
//            mAppId = (String) serverExtras.get(AppKeyManager.APP_ID);
//            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
//            mUnitId = serverExtras.get(AppKeyManager.UNIT_ID);
//        } else {
//            customEventInterstitialListener.onInterstitialFailed(TradPlusErrorCode.ADAPTER_CONFIGURATION_ERROR);
//            return;
//        }
//
//        mAppKey = "7c22942b749fe6a6e361b675e96b3ee9";
//        mAppId = "118690";
//        mPlacementId = "138789";
//        mUnitId = "146877";
//
//        mCallbackRouter.addListener(mPlacementId,customEventInterstitialListener);
//
//        if (!TextUtils.isEmpty(mAppId) && !TextUtils.isEmpty(mAppKey)) {
//            if (!AppKeyManager.getInstance().isInited(mAppId + mAppKey, AppKeyManager.AdType.OFFERWALL)) {
//                MIntegralSDK mIntegralSDK = MIntegralSDKFactory.getMIntegralSDK();
//                suportGDPR(mIntegralSDK,context,localExtras);
//                Map<String, String> configurationMap = mIntegralSDK.getMTGConfigurationMap(mAppId, mAppKey);
//                mIntegralSDK.init(configurationMap, context);
//                AppKeyManager.getInstance().addAppKey(mAppKey + mAppKey, AppKeyManager.AdType.OFFERWALL);
//
//            }
//        }
//
//
//        properties = MtgWallHandler.getWallProperties(mPlacementId, mUnitId);
//
//        mOfferWallHandler = new MtgWallHandler(properties, context);
//        // user bitmap resId as the logo
//
//        // user bitmap resId as the logo
////        properties.put(MIntegralConstans.PROPERTIES_WALL_TITLE_LOGO, bitmap);
//
//        // use bitmap or text as the appwall title
//        properties.put(MIntegralConstans.PROPERTIES_WALL_TITLE_LOGO_TEXT, "text");
////         initHandler();
//
//        mOfferWallHandler.startWall();
//    }
//
//    private void initHandler() {
////        // user bitmap resId as the logo
////        properties.put(MIntegralConstans.PROPERTIES_WALL_TITLE_LOGO, bitmap);
////        // user drawable resId as the logo
////        properties.put(MIntegralConstans.PROPERTIES_WALL_TITLE_LOGO_ID, R.drawable.ic_launcher);
////
////        // use bitmap or text as the appwall title
////        properties.put(MIntegralConstans.PROPERTIES_WALL_TITLE_LOGO_TEXT, "text");
////        properties.put(MIntegralConstans.PROPERTIES_WALL_TITLE_LOGO_TEXT, bitmap);
////        // use color resId as the appwall title
////        properties.put(MIntegralConstans.PROPERTIES_WALL_TITLE_BACKGROUND_COLOR, R.color.mintegral_green);
////        // use drawable resid as the appwall title
////        properties.put(MIntegralConstans.PROPERTIES_WALL_TITLE_BACKGROUND_ID, R.drawable.ic_launcher);
////
////        // wall main background must be color
////        properties.put(MIntegralConstans.PROPERTIES_WALL_MAIN_BACKGROUND_ID, R.color.mintegral_bg_main);
////
////        // wall tab background must be in color
////        properties.put(MIntegralConstans.PROPERTIES_WALL_TAB_BACKGROUND_ID, R.color.mintegral_bg_main);
////
////        // wall tab indicator line must be in color
////        properties.put(MIntegralConstans.PROPERTIES_WALL_TAB_INDICATE_LINE_BACKGROUND_ID,
////                R.color.mintegral_wall_tab_line);
////
////        // wall button color must be drawable
////        properties.put(MIntegralConstans.PROPERTIES_WALL_BUTTON_BACKGROUND_ID, R.drawable.mintegral_shape_btn);
////
////        // wall loading view
////        properties.put(MIntegralConstans.PROPERTIES_WALL_LOAD_ID, R.layout.mintegral_demo_wall_click_loading);
////
////        properties.put(MIntegralConstans.PROPERTIES_WALL_STATUS_COLOR, R.color.mintegral_green);
////
////        properties.put(MIntegralConstans.PROPERTIES_WALL_NAVIGATION_COLOR, R.color.mintegral_green);
////
////        // set the wall tab color of selected and unselected text by hex color
////        // codes
////        properties.put(MIntegralConstans.PROPERTIES_WALL_TAB_SELECTED_TEXT_COLOR, "#ff7900");
////        properties.put(MIntegralConstans.PROPERTIES_WALL_TAB_UNSELECTED_TEXT_COLOR, "#ffaa00");
//    }
//
//    @Override
//    protected void showInterstitial() {
//        if (mOfferWallHandler != null) {
////            mOfferWallHandler.show();
//        }else{
//            Log.d("TradPlus","Mitegral OfferWall ShowFail");
//            if (mCallbackRouter.getListener(mPlacementId)!=null)
//                mCallbackRouter.getListener(mPlacementId).onInterstitialFailed(TradPlusErrorCode.SHOW_FAILED);
//        }
//    }
//
//    @Override
//    protected void onInvalidate() {
//
//    }
//
//    @Override
//    protected LifecycleListener getLifecycleListener() {
//        return null;
//    }
//
//    @Override
//    protected boolean isReadyInterstitial() {
//        return true;
//    }
//
//    private void suportGDPR(MIntegralSDK mIntegralSDK,Context context ,Map<String, Object> localExtras) {
//        if (localExtras != null && localExtras.size() > 0) {
//            if (localExtras.containsKey(AppKeyManager.GDPR_CONSENT) && localExtras.containsKey(AppKeyManager.IS_UE)) {
//                boolean need_set_gdpr=false;
//                int consent = (int)localExtras.get(AppKeyManager.GDPR_CONSENT);
//                if(consent == TradPlus.PERSONALIZED){
//                    need_set_gdpr = true;
//                }
//
//                boolean isEu = (boolean) localExtras.get(AppKeyManager.IS_UE);
//                Log.i("gdpr", "suportGDPR: "+need_set_gdpr+":isUe:"+isEu);
//                int open = need_set_gdpr ? MIntegralConstans.IS_SWITCH_ON : MIntegralConstans.IS_SWITCH_OFF;
//                String level = MIntegralConstans.AUTHORITY_ALL_INFO;
//                mIntegralSDK.setUserPrivateInfoType(context, level, open);
//            }
//            if(localExtras.containsKey(AppKeyManager.KEY_CCPA)){
//                boolean cppa = (boolean) localExtras.get(AppKeyManager.KEY_CCPA);
//                mIntegralSDK.setDoNotTrackStatus(!cppa);
//            }
//        }
//    }
//
//
//    @Override
//    public void onOfferWallLoadSuccess() {
//        Log.d("TradPlus","Mitegral OfferWall onOfferWallLoadSuccess");
//        if (mCallbackRouter.getListener(mPlacementId)!=null)
//        mCallbackRouter.getListener(mPlacementId).onInterstitialLoaded();
//    }
//
//
//    @Override
//    public void onOfferWallLoadFail(String s) {
//        Log.d("TradPlus","Mitegral OfferWall onOfferWallLoadFail , esg == " +s);
//        if (mCallbackRouter.getListener(mPlacementId)!=null)
//        mCallbackRouter.getListener(mPlacementId).onInterstitialFailed(TradPlusErrorCode.NETWORK_NO_FILL);
//    }
//
//    @Override
//    public void onOfferWallOpen() {
//        Log.d("TradPlus","Mitegral OfferWall onOfferWallOpen");
//        if (mCallbackRouter.getListener(mPlacementId)!=null)
//        mCallbackRouter.getListener(mPlacementId).onInterstitialShown();
//    }
//
//    @Override
//    public void onOfferWallShowFail(String s) {
//        Log.d("TradPlus","Mitegral OfferWall onOfferWallShowFail , esg == " +s);
//        if (mCallbackRouter.getListener(mPlacementId)!=null)
//        mCallbackRouter.getListener(mPlacementId).onInterstitialFailed(TradPlusErrorCode.SHOW_FAILED);
//    }
//
//    @Override
//    public void onOfferWallClose() {
//        Log.d("TradPlus","Mitegral OfferWall onOfferWallClose");
//        if (mCallbackRouter.getListener(mPlacementId)!=null)
//        mCallbackRouter.getListener(mPlacementId).onInterstitialDismissed();
//    }
//
//    @Override
//    public void onOfferWallCreditsEarned(String s, int i) {
//        Log.d("TradPlus","Mitegral OfferWall onOfferWallCreditsEarned, currency name : "+ s + " , amount : "+ i);
//        if (mCallbackRouter.getListener(mPlacementId)!=null)
//        mCallbackRouter.getListener(mPlacementId).onInterstitialRewarded(s,i);
//    }
//
//    @Override
//    public void onOfferWallAdClick() {
//        Log.d("TradPlus","Mitegral OfferWall onOfferWallAdClick");
//        if (mCallbackRouter.getListener(mPlacementId)!=null)
//        mCallbackRouter.getListener(mPlacementId).onInterstitialClicked();
//    }
//}
