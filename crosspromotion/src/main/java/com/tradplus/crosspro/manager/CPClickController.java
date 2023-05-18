package com.tradplus.crosspro.manager;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.tradplus.ads.base.common.TPDataManager;
import com.tradplus.ads.common.task.TPTaskManager;
import com.tradplus.ads.common.util.LogUtil;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.pushcenter.event.EventSendMessageUtil;

import java.net.HttpURLConnection;
import java.net.URL;

import static com.tradplus.ads.base.common.TPError.EC_CLICK_OPENAPP_FAILED;
import static com.tradplus.ads.base.common.TPError.EC_CLICK_REQUEST_FAILED;
import static com.tradplus.ads.base.common.TPError.EC_OEPN_APP_SUCCESS;
import static com.tradplus.ads.base.common.TPError.EC_SUCCESS;


/**
 * MyOffer Click Controller
 */

public class CPClickController {
    private final String TAG = getClass().getSimpleName();

    public static final String HOST_GOOGLE_PLAY = "play.google.com";
    public static final String HOST_ANDROID_MARKET = "market.android.com";
    public static final String PATH_DETAILS = "details?";
    public static final String SCHEME_MARKET = "market";
    public static final String PROTOCOL_MARKET = SCHEME_MARKET + "://";

    private final int MAX_JUMP_COUNT = 10;
    private final int MARKET_TYPE = 1;
    private final int BROWSER_TYPE = 2;
    private final int APK_TYPE = 3;
    private final int SYNC_MODE = 0;
    private final int ASYNC_MODE = 1;
    CPAdResponse cpAdResponse;
    private String mCampaignId;

    boolean mIsClicking;
    boolean mIsCancel;

    private String adSourceId;
    Context mContext;

    public CPClickController(Context context, CPAdResponse cpAdResponse, String adSourceId) {
        this.cpAdResponse = cpAdResponse;
        mCampaignId = cpAdResponse.getCampaign_id();
        mContext = context.getApplicationContext();
        this.adSourceId = adSourceId;
    }

    /**
     * start click
     *
     * @param clickStatusCallback
     */
    public void startClick(final String requestId, final ClickStatusCallback clickStatusCallback) {
        if (mIsClicking) {
            return;
        }
        if (clickStatusCallback != null) {
            clickStatusCallback.clickStart();
        }

        mIsClicking = true;
        mIsCancel = false;

        TPTaskManager.getInstance().run_proxy(new Runnable() {
            @Override
            public void run() {
                Log.i("CPClick", "run: deepLinkUrl ： " + cpAdResponse.getDeeplink_url());
                Log.i("CPClick", "run: deepLinkUrl ： " + isApkInstalled(mContext, cpAdResponse.getAd_pkg_name()));
                Log.i("CPClick", "run: deepLinkUrl ： " + cpAdResponse.getAd_pkg_name());
                if (!TextUtils.isEmpty(cpAdResponse.getDeeplink_url()) && isApkInstalled(mContext, cpAdResponse.getAd_pkg_name())) {
                    /**If open deeplink success, it would continue to open click url.**/
                    String deepLinkUrl = cpAdResponse.getDeeplink_url().replaceAll("\\{req_id\\}", requestId == null ? "" : requestId);
                    Log.i("CPClick", "run: deepLinkUrl ： " + deepLinkUrl);
                    if (openDeepLink(mContext, deepLinkUrl)) {
                        Log.i("CPClick", "run: openDeepLink ：");
                        //TODO
//                        AgentEventManager.sendDeepLinkAgent(mCampaignId, mMyOfferAd.getOfferId(), "1", deepLinkUrl, "1");
                        mIsClicking = false;
                        EventSendMessageUtil.getInstance().sendDeepLinkAd(mContext,mCampaignId,cpAdResponse.getAd_id(),EC_OEPN_APP_SUCCESS,adSourceId);
                        EventSendMessageUtil.getInstance().sendClickAdEnd(mContext, mCampaignId, cpAdResponse.getAd_id(), EC_SUCCESS, adSourceId);
                        if (clickStatusCallback != null) {
                            clickStatusCallback.clickEnd();
                        }
                        return;
                    } else {
                        EventSendMessageUtil.getInstance().sendDeepLinkAd(mContext, mCampaignId, cpAdResponse.getAd_id(), EC_CLICK_OPENAPP_FAILED, adSourceId);
                        //TODO
//                        AgentEventManager.sendDeepLinkAgent(mCampaignId, mMyOfferAd.getOfferId(), "1", deepLinkUrl, "0");
                        Log.i("CPClick", "run: openDeepLink else ：");
                    }
                } else {
                }

                /**Open click url**/
                openClickUrl(requestId, clickStatusCallback);

            }
        });
    }


    /**
     * Open Ad click url
     *
     * @param requestId
     * @param clickStatusCallback
     */
    private void openClickUrl(String requestId, final ClickStatusCallback clickStatusCallback) {
        final String clickUrl = (cpAdResponse.getClick_url() != null ? cpAdResponse.getClick_url() : "").replaceAll("\\{req_id\\}", requestId == null ? "" : requestId);
        Log.i(TAG, "openClickUrl: " + cpAdResponse.getClick_type());
        Log.i(TAG, "openClickUrl: " + cpAdResponse.getClick_url());
        boolean isNeedJump = true; //Default need to open app page
        if (cpAdResponse.getClick_type() != MARKET_TYPE && cpAdResponse.getClick_type() != APK_TYPE) {
            handleClickResult(clickUrl, clickStatusCallback);
            return;
        }

        if (clickUrl.endsWith(".apk")) {
            handleClickResult(clickUrl, clickStatusCallback);
            return;
        }

        if (!clickUrl.startsWith("http")) {
            handleClickResult(cpAdResponse.getClick_url(), clickStatusCallback);
            return;
        }
        if (cpAdResponse.getClick_mode() == ASYNC_MODE) {//异步跳转
            handleClickResult(cpAdResponse.getPreview_url(), clickStatusCallback);
            isNeedJump = false;
        }

        String resultUrl = handleUrl302Result(clickUrl);
        if (isNeedJump) {
            if(!TextUtils.isEmpty(resultUrl)) {
                handleClickResult(resultUrl, clickStatusCallback);
            }
        }
    }

    /**
     * Open DeepLink url
     *
     * @param context
     * @param deepLinkUrl
     * @return
     */
    private boolean openDeepLink(Context context, String deepLinkUrl) {
        boolean openSuccessed = false;
        try {
            if (!TextUtils.isEmpty(deepLinkUrl)) {
                Uri uri = Uri.parse(deepLinkUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setData(uri);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                openSuccessed = true;
            }
        } catch (Throwable t) {
            LogUtil.ownShow(t.getMessage());
        }
        return openSuccessed;
    }


    /**
     * Handle url to redirect
     *
     * @param clickUrl
     * @return
     */
    private String handleUrl302Result(String clickUrl) {
        String startUrl = clickUrl;
        boolean success = false;
        for (int i = 0; i < MAX_JUMP_COUNT; i++) {
            try {
                URL serverUrl = new URL(startUrl);
                HttpURLConnection conn = (HttpURLConnection) serverUrl
                        .openConnection();
                conn.setRequestMethod("GET");
                // Must set to false. If not, it will auto redirect to Location
                conn.setInstanceFollowRedirects(false);

                conn.setConnectTimeout(30 * 1000);
                conn.connect();
                int responseCode = conn.getResponseCode();
                if (responseCode == 302) {
                    startUrl = conn.getHeaderField("Location");

                    if (isGooglePlayUrl(startUrl) || startUrl.endsWith(".apk") || !startUrl.startsWith("http")) {
                        success = true;
                    } else {
                        conn.disconnect();
                        continue;
                    }
                }
                conn.disconnect();

                if (success || responseCode == 200) {
                    return startUrl;
                }
                //TODO
                /**Fail to jump**/
//                AgentEventManager.sendClickFailAgent(mCampaignId, mMyOfferAd.getOfferId(), "1", mMyOfferAd.getClickUrl(), startUrl, responseCode + "", "");
                return "";
            } catch (Exception e) {
                //TODO
//                AgentEventManager.sendClickFailAgent(mCampaignId, mMyOfferAd.getOfferId(), "1", mMyOfferAd.getClickUrl(), startUrl, "", e.getMessage());
                break;
            }
        }
        return "";
    }


    /**
     * Handle the result of clicked
     *
     * @param finalUrl
     */
    private void handleClickResult(String finalUrl, ClickStatusCallback clickStatusCallback) {
        if (mIsCancel) {
            return;
        }
        finalUrl = TextUtils.isEmpty(finalUrl) ? cpAdResponse.getPreview_url() : finalUrl;
        switch (cpAdResponse.getClick_type()) {
            case MARKET_TYPE:
                if (!finalUrl.startsWith("http")) {
                    if (!openMarketApp(finalUrl)) {
                        openBrowserUrl(finalUrl);
                    }
                } else {
                    String googleMarketUrl = convertToMarketUrl(finalUrl);
                    if (!TextUtils.isEmpty(googleMarketUrl)) {
                        if (!openMarketApp(googleMarketUrl)) {
                            openBrowserUrl(finalUrl);
                        }
                    } else {
                        openBrowserUrl(finalUrl);
                    }
                }
                break;
            case BROWSER_TYPE:
                openBrowserUrl(finalUrl);
                break;
            case APK_TYPE:
                downloadApkOrOpenBrowser(finalUrl, clickStatusCallback);
                break;
            default:
                openBrowserUrl(finalUrl);
                break;
        }
        mIsClicking = false;
        if (clickStatusCallback != null) {
            clickStatusCallback.clickEnd();
        }

    }

    /**
     * Apk url open
     *
     * @param finalUrl
     * @param clickStatusCallback
     */
    private void downloadApkOrOpenBrowser(String finalUrl, ClickStatusCallback clickStatusCallback) {
        Log.i(TAG, "downloadApkOrOpenBrowser: " + TradPlus.invoker().getChinaHandler());
        if (TradPlus.invoker().getChinaHandler() != null) {
            if (!TextUtils.isEmpty(finalUrl) && finalUrl.endsWith(".apk")) {
                if (clickStatusCallback != null) {
                    EventSendMessageUtil.getInstance().sendClickAdEnd(mContext, mCampaignId, cpAdResponse.getAd_id(), EC_SUCCESS, adSourceId);
                    clickStatusCallback.downloadApp(finalUrl);
                }
                return;
            }
        }
        openBrowserUrl(finalUrl);
    }

    /**
     * Open Market
     *
     * @param googleMarketUrl
     */
    private boolean openMarketApp(String googleMarketUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleMarketUrl));
            intent.setData(Uri.parse(googleMarketUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        } catch (Throwable e) {
            TradPlus.invoker().runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "Detect that the App Market is not installed and cannot be opened through the App Market.", Toast.LENGTH_LONG).show();
                }
            });
            return false;
        }
        return true;
    }

    /**
     * Open Browser
     *
     * @param finalUrl
     */
    private void openBrowserUrl(String finalUrl) {
        boolean isOpen = false;
        try {
            Intent intent = new Intent("android.intent.action.VIEW",
                    Uri.parse(finalUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            isOpen = true;
        } catch (Exception e2) {
            isOpen = false;
            EventSendMessageUtil.getInstance().sendClickAdEnd(mContext, mCampaignId, cpAdResponse.getAd_id(), EC_CLICK_REQUEST_FAILED, adSourceId);
            e2.printStackTrace();
        } finally {
            if (isOpen) {
                EventSendMessageUtil.getInstance().sendClickAdEnd(mContext, mCampaignId, cpAdResponse.getAd_id(), EC_SUCCESS, adSourceId);
            }
        }
    }

    /**
     * Check if it's GooglePlay's url
     *
     * @param url
     * @return
     */
    private boolean isGooglePlayUrl(String url) {
        try {
            if (TextUtils.isEmpty(url)) {
                return false;
            }
            Uri uri = Uri.parse(url);
            if (uri == null || uri.getHost() == null) {
                return false;
            }
            return uri.getHost().equals(HOST_GOOGLE_PLAY)
                    || uri.getHost().equals(HOST_ANDROID_MARKET);
        } catch (Throwable t) {
        }
        return false;
    }

    /**
     * Convert to Market Url
     *
     * @param url
     * @return
     */
    private String convertToMarketUrl(String url) {
        try {
            if (isGooglePlayUrl(url)) {
                String detailsUrl = url.substring(url.indexOf(PATH_DETAILS));
                return PROTOCOL_MARKET + detailsUrl;

            }
        } catch (Throwable e) {
            if (TPDataManager.getInstance().isDebugMode()) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean isApkInstalled(Context context, String pkgName) {
        if (context == null || TextUtils.isEmpty(pkgName)) {
            return false;
        }
        try {
            ApplicationInfo info = context.getPackageManager()
                    .getApplicationInfo(pkgName, PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Cance click
     */
    public void cancelClick() {
        mIsCancel = true;
    }

    public interface ClickStatusCallback {
        public void clickStart();

        public void clickEnd();

        public void downloadApp(String url);
    }
}
