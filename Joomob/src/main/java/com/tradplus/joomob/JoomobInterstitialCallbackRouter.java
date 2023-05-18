package com.tradplus.joomob;

import com.tradplus.ads.base.adapter.TPLoadAdapterListener;
import com.tradplus.ads.base.adapter.TPShowAdapterListener;

import java.util.HashMap;
import java.util.Map;

public class JoomobInterstitialCallbackRouter {

    private Map<String, TPLoadAdapterListener> mInterstitialListenerMap = new HashMap<>();
    private Map<String, TPShowAdapterListener> mShowListeners = new HashMap<>();

    private JoomobInterstitialCallbackRouter() {
    }

    public static JoomobInterstitialCallbackRouter getInstance() {
        return Inner.joomobInterstitialCallbackRouter;
    }

    private static class Inner {
        static private JoomobInterstitialCallbackRouter joomobInterstitialCallbackRouter = new JoomobInterstitialCallbackRouter();
    }

    public void addJoomobRewardListener(String pid, TPLoadAdapterListener customEventInterstitialListener) {
        mInterstitialListenerMap.put(pid, customEventInterstitialListener);
    }

    public void addShowListener(String pid, TPShowAdapterListener showAdapterListener) {
        mShowListeners.put(pid, showAdapterListener);
    }

    public TPShowAdapterListener getShowListener(String pid) {
        return mShowListeners.get(pid);
    }


    public TPLoadAdapterListener getListener(String pid) {
        return mInterstitialListenerMap.get(pid);
    }
}
