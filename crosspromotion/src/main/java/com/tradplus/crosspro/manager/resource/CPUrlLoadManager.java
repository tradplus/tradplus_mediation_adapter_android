package com.tradplus.crosspro.manager.resource;

import com.tradplus.crosspro.network.base.CPError;
import com.tradplus.crosspro.network.base.CPErrorCode;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CPUrlLoadManager {
    public static final String TAG = CPUrlLoadManager.class.getSimpleName();

    private static CPUrlLoadManager sInstance;
    private CPUrlLoadManager() {

    }

    public synchronized static CPUrlLoadManager getInstance() {
        if(sInstance == null){
            sInstance = new CPUrlLoadManager();
        }
        return sInstance;
    }

    private List<CPResourceLoadResult> mResourceLoadResultList = new CopyOnWriteArrayList<>();

    public interface CPResourceLoadResult {
        /**
         * Success Callback
         */
        void onResourceLoadSuccess(String url);
        /**
         * Fail Callback
         */
        void onResourceLoadFailed(String url, CPError error);
    }

    /**
     * Download Url Register
     */
    public synchronized void register(CPResourceLoadResult result) {
        this.mResourceLoadResultList.add(result);
    }

    /**
     * Download Url Unregister
     */
    public synchronized void unRegister(CPResourceLoadResult result) {
        int size = mResourceLoadResultList.size();
        int removeIndex = -1;
        for (int i = 0; i < size; i++) {
            if(result == mResourceLoadResultList.get(i)) {
                removeIndex = i;
                break;
            }
        }
        if(removeIndex != -1) {
            this.mResourceLoadResultList.remove(removeIndex);
        }
    }

    public void notifyDownloadSuccess(String url) {
        if (mResourceLoadResultList != null) {
            for (CPResourceLoadResult resourceLoadResult : mResourceLoadResultList) {
                resourceLoadResult.onResourceLoadSuccess(url);
            }
        }
    }

    public void notifyDownloadFailed(String url, CPError error) {
        if (mResourceLoadResultList != null) {
            for (CPResourceLoadResult resourceLoadResult : mResourceLoadResultList) {
                resourceLoadResult.onResourceLoadFailed(url, error);
            }
        }
    }
}
