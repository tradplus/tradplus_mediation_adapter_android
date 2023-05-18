package com.tradplus.ads.mintegral.zoomoututils;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

public class ViewUtils {
    public static void removeFromParent(View view){
        if(view != null){
            ViewParent viewParent = view.getParent();
            if(viewParent instanceof ViewGroup){
                ((ViewGroup)viewParent).removeView(view);
            }
        }
    }
}
