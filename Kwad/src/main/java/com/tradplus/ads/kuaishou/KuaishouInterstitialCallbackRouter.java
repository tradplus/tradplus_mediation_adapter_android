package com.tradplus.ads.kuaishou;

import com.tradplus.ads.base.adapter.TPLoadAdapterListener;
import com.tradplus.ads.base.adapter.TPShowAdapterListener;

import java.util.HashMap;
import java.util.Map;

public class KuaishouInterstitialCallbackRouter {
    private static KuaishouInterstitialCallbackRouter instance;
    private KuaishouPidReward mKuaishouPidReward;

    private final Map<String, TPLoadAdapterListener> listeners = new HashMap<>();
    private final Map<String, TPShowAdapterListener> showlisteners = new HashMap<>();
    private final Map<String, KuaishouPidReward> pidlisteners = new HashMap<>();

    public static KuaishouInterstitialCallbackRouter getInstance() {
        if(instance == null){
            instance = new KuaishouInterstitialCallbackRouter();
        }
        return instance;
    }

    public Map<String, TPLoadAdapterListener> getListeners() {
        return listeners;
    }

    public Map<String, TPShowAdapterListener> getShowListeners() {
        return showlisteners;
    }

    public void removeListeners(String placementId) {
        listeners.remove(placementId);
        showlisteners.remove(placementId);
    }

    public Map<String, KuaishouPidReward> getPidlisteners() {
        return pidlisteners;
    }

    public void addListener(String id, TPLoadAdapterListener listener){
        getListeners().put(id,listener);
    }

    public void addShowListener(String id, TPShowAdapterListener listener){
        getShowListeners().put(id,listener);
    }

    public void addPidListener(String id, KuaishouPidReward mIronSourcePidReward) {
        getPidlisteners().put(id, mIronSourcePidReward);
    }

    public KuaishouPidReward getKuaishouPidReward(String id) {
        return getPidlisteners().get(id);
    }

    public TPLoadAdapterListener getListener(String id){
        return getListeners().get(id);
    }

    public TPShowAdapterListener getShowListener(String id){
        return getShowListeners().get(id);
    }
}
