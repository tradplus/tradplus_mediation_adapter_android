package com.tradplus.crosspro.manager;

import android.content.Context;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.common.util.FileUtil;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.base.network.util.ResourceDiskCacheManager;
import com.tradplus.ads.base.network.util.ResourceEntry;
import com.tradplus.crosspro.manager.resource.CPLoader;
import com.tradplus.crosspro.manager.resource.CPResourceStatus;

import java.io.FileInputStream;
import java.io.InputStream;

import static com.tradplus.ads.common.FSConstants.CP_SECONDS_MILLIS;

public class CPResourceManager {

    private static CPResourceManager sInstance;

    private CPResourceManager() {
    }

    public synchronized static CPResourceManager getInstance() {
        if (sInstance == null) {
            sInstance = new CPResourceManager();
        }
        return sInstance;
    }


    public FileInputStream getInputStream(String url) {
        String resFileName = FileUtil.hashKeyForDisk(url);
        if(GlobalTradPlus.getInstance().getContext() == null) return null;
        return ResourceDiskCacheManager.getInstance(GlobalTradPlus.getInstance().getContext())
                .getFileInputStream(ResourceEntry.INTERNAL_CACHE_TYPE, resFileName);
    }

    public boolean writeToDiskLruCache(String url, InputStream inputStream) {
        if (url == null || inputStream == null) {
            return false;
        }

        String resFileName = FileUtil.hashKeyForDisk(url);
        return ResourceDiskCacheManager.getInstance(GlobalTradPlus.getInstance().getContext()).saveNetworkInputStreamToFile(ResourceEntry.INTERNAL_CACHE_TYPE, resFileName, inputStream);


    }

    public void load(Context context,String placementId,CPAdResponse cpAdResponse, final CPLoader.CPLoaderListener listener,String adSourceId) {
        CPLoader cpLoader = new CPLoader(placementId, CP_SECONDS_MILLIS,adSourceId);
        cpLoader.load(context,cpAdResponse,listener);
    }

    public boolean isExist(CPAdResponse cpAdResponse) {
        return CPResourceStatus.isExist(cpAdResponse);
    }
}
