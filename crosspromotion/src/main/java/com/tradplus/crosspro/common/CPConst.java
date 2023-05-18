package com.tradplus.crosspro.common;

public class CPConst {

    public static final String ENDCARDCLICKAREA_FULLSCREEN = "0";

    public static final long DEFAULT_CACHE_TIME = 24 * 60 * 60 * 1000;
    public static final long DEFAULT_EXPRETIME = 3 * 60 * 60 * 1000;


    public static final int TYPE_CLICK_TRACK = 1;
    public static final int TYPE_SHOW_IMP = 2;



//1 原生, 2 插屏, 3 开屏, 4 横幅, 5 激励视频, 6 积分墙
    public static class FORMAT {
        public static final String NATIVE_FORMAT = "1";
        public static final String REWARDEDVIDEO_FORMAT = "5";
        public static final String BANNER_FORMAT = "4";
        public static final String INTERSTITIAL_FORMAT = "2";
        public static final String SPLASH_FORMAT = "3";
    }
}
