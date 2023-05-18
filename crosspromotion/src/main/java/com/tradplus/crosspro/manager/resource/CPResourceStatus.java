package com.tradplus.crosspro.manager.resource;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.common.util.FileUtil;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.base.network.util.ResourceDiskCacheManager;
import com.tradplus.ads.base.network.util.ResourceEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CPResourceStatus {

    private static Map<String, Integer> sStateMap = new HashMap<>();
    public static final int NORMAL = 0;
    public static final int LOADING = 1;

    public static boolean isLoading(String url) {

        Integer state = sStateMap.get(url);
        return CPResourceStatus.LOADING == (state != null ? state : CPResourceStatus.NORMAL);
    }

    public static void setState(String url, int state) {
        sStateMap.put(url, state);
    }


    /**
     * Check if the resource exists
     */
    public static boolean isExist(String url) {
        String resourceName = FileUtil.hashKeyForDisk(url);
        if(GlobalTradPlus.getInstance().getContext() == null) return false;
        return ResourceDiskCacheManager.getInstance(GlobalTradPlus.getInstance().getContext()).isExistFile(ResourceEntry.INTERNAL_CACHE_TYPE, resourceName);
    }



    /**
     * Check if the resource exists
     */
    public static boolean isExist(CPAdResponse cpAdResponse) {
        if (cpAdResponse == null) {
            return false;
        }
        List<String> urlList = cpAdResponse.getUrlList();
        int size = urlList.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                if (!isExist(urlList.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }
    /**
     * Check if the resource exists
     */
    public static boolean isEndCardExist(CPAdResponse cpAdResponse) {
        if (cpAdResponse == null) {
            return false;
        }
        List<String> urlList = cpAdResponse.getUrlList();
        int size = urlList.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                if(cpAdResponse.isEndCardUrl(urlList.get(i))) {
                    if (!isExist(urlList.get(i))) {
                        return false;
                    }
                }
            }
            return true;
        }
        return true;
    }
    /**
     * Check if the resource exists
     */
    public static boolean isVideoExist(CPAdResponse cpAdResponse) {
        if (cpAdResponse == null) {
            return false;
        }
        List<String> urlList = cpAdResponse.getUrlList();
        int size = urlList.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                if(cpAdResponse.isVideoUrl(urlList.get(i))) {
                    if (!isExist(urlList.get(i))) {
                        return false;
                    }
                }
            }
            return true;
        }
        return true;
    }

}
