package com.tradplus.ads.baidu;

import com.tradplus.ads.base.adapter.TPLoadAdapterListener;
import com.tradplus.ads.base.adapter.TPShowAdapterListener;

import java.util.HashMap;
import java.util.Map;

public class BaiduInterstitialCallbackRouter {

    private static BaiduInterstitialCallbackRouter instance;

    private final Map<String, TPLoadAdapterListener> listeners = new HashMap<>();
    private final Map<String, TPShowAdapterListener> showlisteners = new HashMap<>();

    public static BaiduInterstitialCallbackRouter getInstance() {
        if(instance == null){
            instance = new BaiduInterstitialCallbackRouter();
        }
        return instance;
    }

    public void removeListeners(String placementId) {
        listeners.remove(placementId);
        showlisteners.remove(placementId);
    }

    public Map<String, TPLoadAdapterListener> getListeners() {
        return listeners;
    }

    public void addListener(String id, TPLoadAdapterListener listener){
        getListeners().put(id,listener);
    }

    public TPLoadAdapterListener getListener(String id){
        return getListeners().get(id);
    }

    public Map<String, TPShowAdapterListener> getShowListeners() {
        return showlisteners;
    }

    public void addShowListener(String id, TPShowAdapterListener listener){
        getShowListeners().put(id,listener);

    }

    public TPShowAdapterListener getShowListener(String id){
        return getShowListeners().get(id);
    }


}
