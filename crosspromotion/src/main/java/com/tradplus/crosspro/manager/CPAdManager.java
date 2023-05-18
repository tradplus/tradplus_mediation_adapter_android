package com.tradplus.crosspro.manager;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.base.network.util.ImageLoader;
import com.tradplus.ads.base.network.util.ResourceEntry;
import com.tradplus.china.common.ApkDownloadManager;
import com.tradplus.china.common.download.ApkRequest;
import com.tradplus.china.common.resource.ApkResource;
import com.tradplus.crosspro.manager.resource.CPLoader;
import com.tradplus.crosspro.ui.ApkConfirmDialogActivity;

import static com.tradplus.crosspro.common.CPConst.DEFAULT_CACHE_TIME;

public class CPAdManager {

    private static CPAdManager sIntance;
    private Context mContext;


    private CPAdManager(Context context) {
        mContext = context.getApplicationContext();

    }

    public static CPAdManager getInstance(Context context) {
        if (sIntance == null) {
            sIntance = new CPAdManager(context);
        }
        return sIntance;
    }

    public void load(String placementId, final CPLoader.CPLoaderListener listener,String adSourceId) {
        CPResourceManager.getInstance().load(mContext,placementId,getCpAdConfig(placementId),listener,adSourceId);
    }


    public void startDownloadApp(final String requestId, final CPAdResponse cpAdResponse, final String url, final String adSourceId) {
        Log.i("servicedownload", "startDownloadApp: ");
        TradPlus.invoker().runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (1 == cpAdResponse.getClick_confirm()) {
                    ApkConfirmDialogActivity.start(mContext, requestId, cpAdResponse, url,adSourceId);
                } else {
                    realStartDownloadApp(adSourceId, cpAdResponse, url);
                }
            }
        });

    }

    public void realStartDownloadApp(final String requestId, final CPAdResponse cpAdResponse, final String url) {
        Log.i("servicedownload", "realStartDownloadApp: ");
        if (ApkResource.isApkInstalled(GlobalTradPlus.getInstance().getContext(), cpAdResponse.getAd_pkg_name())) {
            //App was installed， open it
            ApkResource.openApp(GlobalTradPlus.getInstance().getContext(), cpAdResponse.getAd_pkg_name());
        } else {
            //App not exist, download it
            ApkRequest apkRequest = new ApkRequest();
            apkRequest.requestId = requestId;
            apkRequest.offerId = cpAdResponse.getCampaign_id();
            apkRequest.url = url;
            apkRequest.pkgName = cpAdResponse.getAd_pkg_name();
            apkRequest.title = cpAdResponse.getAd_name();
            apkRequest.setAdid(cpAdResponse.getAd_id());
            apkRequest.setPid(cpAdResponse.getCampaign_id());
            apkRequest.setAsuid(requestId);
            int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, GlobalTradPlus.getInstance().getContext().getResources().getDisplayMetrics());
            apkRequest.icon = ImageLoader.getInstance(mContext).getBitmapFromDiskCache(new ResourceEntry(ResourceEntry.INTERNAL_CACHE_TYPE, ""), size, size);

            long cache_time = cpAdResponse.getCreative_cache_time();
            ApkDownloadManager.getInstance(GlobalTradPlus.getInstance().getContext()).setCPCacheTime(cache_time > 0 ? cache_time : DEFAULT_CACHE_TIME);
            ApkDownloadManager.getInstance(GlobalTradPlus.getInstance().getContext()).checkAndCleanApk();
            ApkDownloadManager.getInstance(GlobalTradPlus.getInstance().getContext()).handleClick(apkRequest);
        }
    }

    public CPAdResponse getCpAdConfig(String pid) {
//        return new CPAdResponse();
        return CPAdConfigController.getCpAdResponse(pid);
    }

    public boolean isReady(String pid) {
        CPAdResponse _cpAdResponse = getCpAdConfig(pid);
        if (mContext == null || _cpAdResponse == null) {
            return false;
        }

        return CPResourceManager.getInstance().isExist(_cpAdResponse);


    }

}
