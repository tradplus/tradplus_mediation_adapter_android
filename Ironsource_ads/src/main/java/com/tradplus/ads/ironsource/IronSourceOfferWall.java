package com.tradplus.ads.ironsource;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.ironsource.adapters.supersonicads.SupersonicConfig;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.OfferwallListener;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.lang.ref.WeakReference;
import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class IronSourceOfferWall extends TPRewardAdapter implements OfferwallListener {
    public static final String PASS_SCAN_KEY = "passScan";

    private WeakReference<Activity> mWeakRefActivity;

    private IronSourceInterstitialCallbackRouter mCallbackRouter;
    private String appKey, placementId;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if(!(context instanceof Activity)) {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
            return;
        }
        mWeakRefActivity = new WeakReference<>((Activity) context);

        mCallbackRouter = IronSourceInterstitialCallbackRouter.getInstance();

        if (extrasAreValid(tpParams)) {
            appKey = tpParams.get(AppKeyManager.APP_ID);
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (TextUtils.isEmpty(placementId)) {
            placementId = "0";
        }

        mCallbackRouter.addListener(placementId, mLoadAdapterListener);

        if (!AppKeyManager.getInstance().isInited(appKey,AppKeyManager.AdType.OFFERWALL)) {
            suportGDPR(context,userParams);
            SupersonicConfig.getConfigObj().setClientSideCallbacks(true);
            IronSource.init((Activity) context, appKey,IronSource.AD_UNIT.OFFERWALL);
            AppKeyManager.getInstance().addAppKey(appKey,AppKeyManager.AdType.OFFERWALL);

        }

        IronSource.setOfferwallListener(this);

        if (IronSource.isOfferwallAvailable()) {
            Log.d("TradPlus", "IronSource OfferWall isOfferwallAvailable");
            if (mCallbackRouter.getListener(placementId) != null) {
                mCallbackRouter.getListener(placementId).loadAdapterLoaded(null);
            }
        }
    }

    private void suportGDPR(Context context ,Map<String, Object> localExtras) {
        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(AppKeyManager.GDPR_CONSENT) && localExtras.containsKey(AppKeyManager.IS_UE)) {
                boolean need_set_gdpr=false;
                int consent = (int)localExtras.get(AppKeyManager.GDPR_CONSENT);
                if(consent == TradPlus.PERSONALIZED){
                    need_set_gdpr = true;
                }

                boolean isEu = (boolean) localExtras.get(AppKeyManager.IS_UE);
                Log.i("gdpr", "suportGDPR: "+need_set_gdpr+":isUe:"+isEu);
                IronSource.setConsent(need_set_gdpr);
            }
            Log.i("IronSourceOfferWall", "suportGDPR ccpa: "+localExtras.get(AppKeyManager.KEY_CCPA));
            if(localExtras.containsKey(AppKeyManager.KEY_CCPA)){
                boolean cppa = (boolean) localExtras.get(AppKeyManager.KEY_CCPA);
                if(!cppa){
                    IronSource.setMetaData("do_not_sell","true");
                }else {
                    IronSource.setMetaData("do_not_sell","false");
                }
            }
        }
    }


    @Override
    public void showAd() {
        String msg;
        if(mShowListener != null) {
            mCallbackRouter.addShowListener(placementId, mShowListener);
        }
        if (IronSource.isOfferwallAvailable()) {
            if (placementId != null && placementId.length() > 0) {
                IronSource.showOfferwall(placementId);
            } else {
                IronSource.showOfferwall();
            }
            msg = "isAdLoaded";
        } else {
            Log.d("TradPlus", "Tried to show a IronSource OfferWall ad before it finished loading. Please try again.");
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
                msg = "noAdLoaded";
            }
        }

    }

    @Override
    public void clean() {
        super.clean();
        if(mWeakRefActivity.get() != null){
            IronSource.onPause(mWeakRefActivity.get());
            return;
        }
    }

    @Override
    public boolean isReady() {
        return IronSource.isOfferwallAvailable();
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_IRONSOURCE);
    }

    @Override
    public String getNetworkVersion() {
        return "7.1.7";
    }

    /**
     * Invoked when the method 'getOfferWallCredits' fails to retrieve
     * the user's credit balance info.
     * @param ironSourceError - A IronSourceError object which represents the reason of 'getOfferwallCredits' failure.
     * If using client-side callbacks to reward users, it is mandatory to return true on this event
     */
    @Override
    public void onGetOfferwallCreditsFailed(IronSourceError ironSourceError) {
        Log.d("TradPlus", "IronSource OfferWall onGetOfferwallCreditsFailed , ErrorCode == " + ironSourceError.getErrorCode() +", ErrorMessage == " + ironSourceError.getErrorMessage());
        if (mCallbackRouter.getListener(placementId) != null)
        mCallbackRouter.getListener(placementId).loadAdapterLoadFailed(IronSourceErrorUtil.getTradPlusErrorCode(ironSourceError));
    }

    /**
     * Invoked each time the user completes an Offer.
     * Award the user with the credit amount corresponding to the value of the *‘credits’ parameter.
     * @param credits - The number of credits the user has earned.
     * @param totalCredits - The total number of credits ever earned by the user.
     * @param totalCreditsFlag - In some cases, we won’t be able to provide the exact
     * amount of credits since the last event (specifically if the user clears
     * the app’s data). In this case the ‘credits’ will be equal to the ‘totalCredits’, and this flag will be ‘true’.
     * @return boolean - true if you received the callback and rewarded the user, otherwise false.
     */
    @Override
    public boolean onOfferwallAdCredited(final int credits, final int totalCredits, boolean totalCreditsFlag) {
        Log.d("TradPlus", "IronSource OfferWall ad onOfferwallAdCredited : " + " credits:" + credits + " totalCredits:" + totalCredits + " totalCreditsFlag:" + totalCreditsFlag);
        if (mCallbackRouter.getShowListener(placementId) != null) {
            mCallbackRouter.getShowListener(placementId).onReward(Integer.toString(credits), totalCredits);
//            mHandler.removeCallbacks(interstitialDismissedRunnable);

        }
        return true;
    }

    /**
     * Invoked when there is a change in the Offerwall availability status.
     * @param - available - value will change to YES when Offerwall are available.
     * You can then show the offerwall by calling showOfferwall(). Value will *change to NO when Offerwall isn't available.
     */

    @Override
    public void onOfferwallAvailable(boolean available) {
        Log.d("TradPlus", "IronSource OfferWall ad onOfferwallAvailable : " + available);
        if (available) {
            if (mCallbackRouter.getListener(placementId) != null)
            mCallbackRouter.getListener(placementId).loadAdapterLoaded(null);
        } else {
            if (mCallbackRouter.getListener(placementId) != null)
            mCallbackRouter.getListener(placementId).loadAdapterLoadFailed(new TPError(NETWORK_NO_FILL));
        }

    }

//    private Runnable interstitialDismissedRunnable = new Runnable() {
//        @Override
//        public void run() {
//            mCallbackRouter.getListener(placementId).onInterstitialDismissed();
//        }
//    };

    /**
     * Invoked when the user is about to return to the application after closing
     * the Offerwall.
     */
    @Override
    public void onOfferwallClosed() {
        Log.d("TradPlus", "IronSource OfferWall ad onOfferwallClosed.");
        if (mCallbackRouter.getShowListener(placementId) != null)
        mCallbackRouter.getShowListener(placementId).onAdClosed();
//        mHandler.postDelayed(interstitialDismissedRunnable, 5000);
    }

    /**
     * Invoked when the Offerwall successfully loads for the user, after calling the 'showOfferwall' method
     */
    @Override
    public void onOfferwallOpened() {
        Log.d("TradPlus", "IronSource OfferWall ad onOfferwallOpened.");
        if (mCallbackRouter.getShowListener(placementId) != null)
        mCallbackRouter.getShowListener(placementId).onAdShown();
    }

    /**
     * Invoked when the method 'showOfferWall' is called and the OfferWall fails to load.
     * @param ironSourceError - A IronSourceError Object which represents the reason of 'showOfferwall' failure.
     */
    @Override
    public void onOfferwallShowFailed(IronSourceError ironSourceError) {
        Log.d("TradPlus", "IronSource OfferWall onOfferwallShowFailed , ErrorCode == " + ironSourceError.getErrorCode() +", ErrorMessage == " + ironSourceError.getErrorMessage());
        if (mCallbackRouter.getShowListener(placementId) != null)
        mCallbackRouter.getShowListener(placementId).onAdVideoError(IronSourceErrorUtil.getTradPlusErrorCode(ironSourceError));
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID);
    }
}
