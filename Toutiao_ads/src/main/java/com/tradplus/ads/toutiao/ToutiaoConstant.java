package com.tradplus.ads.toutiao;

public class ToutiaoConstant {

    public static final int NATIVE_PATCH_VIDEO = 3; // 贴片广告

    public static final int DOWNLOAD_TYPE_NO_POPUP = 0;// 对于应用的下载不做特殊处理；
    public static final int DOWNLOAD_TYPE_POPUP = 1;
    public static final String ADSIZE_RATIO = "ad_size_ratio"; // 插屏广告尺寸
    public static final String AD_REWARD_AGAIN = "reward_again"; // 激励视频 再看一次
    public static final String ZOOM_OUT = "zoom_out";
    public static final String REWARD_TYPE = "rewardType";
    public static final String EXTRA = "extra";

    public static int EXPRESS_VIEW_ACCEPTED_SIZE = 500; // 模版激励视频 默认size
    public static int NATIVE_IMAGE_ACCEPTED_SIZE_X = 600; //自渲染
    public static int NATIVE_IMAGE_ACCEPTED_SIZE_Y = 257; //自渲染
    public static int IMAGE_ACCEPTED_SIZE_X = 1080;
    public static int IMAGE_ACCEPTED_SIZE_Y = 1920;
    public static int NATIVE_DEFAULT_WIDTH = 320;
    public static int NATIVE_DEFAULT_HEIGHT = 340;

    //穿山甲国内 插屏广告 尺寸
    //1:1
    public static float EXPRESSVIEW_WIDTH1 = 300;
    public static float EXPRESSVIEW_HEIGHT1 = 300;
    //2:3
    public static float EXPRESSVIEW_WIDTH2 = 300;
    public static float EXPRESSVIEW_HEIGHT2 = 450;
    //3:2
    public static float EXPRESSVIEW_WIDTH3 = 450;
    public static float EXPRESSVIEW_HEIGHT3 = 300;
}
