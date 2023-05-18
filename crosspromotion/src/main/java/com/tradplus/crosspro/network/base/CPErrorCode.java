package com.tradplus.crosspro.network.base;

public class CPErrorCode {

    public final static String unknow = "-9999";

    public static final String exception = "100";
    public final static String httpStatuException = "101";

    public final static String timeOutError = "201";
    public final static String outOfCapError = "203";
    public final static String inPacingError = "204";

    public final static String noADError = "301";
    public final static String noSettingError = "302";

    public final static String rewardedVideoPlayVideoMissing = "401";
    public final static String rewardedVideoPlayError = "402";
    public final static String incompleteResourceError = "303";




    public static final String fail_load_timeout = "Load timeout!";
    public static final String fail_save = "Save fail!";
    public static final String fail_load_cannel = "Load cancel!";
    public static final String fail_connect = "Http connect error!";
    public static final String fail_params = "offerid、placementid can not be null!";
    public static final String fail_no_offer = "No fill, cp = null!";
    public static final String fail_no_setting = "No fill, setting = null!";
    public static final String fail_out_of_cap = "Ad is out of cap!";
    public static final String fail_in_pacing = "Ad is in pacing!";
    public static final String fail_null_context = "context = null!";
    public static final String fail_player = "Video player error!";
    public static final String fail_no_video_url = "Video url no exist!";
    public static final String fail_video_file_error_ = "Video file error!";
    public static final String fail_incomplete_resource = "Incomplete resource allocation!";


    public static CPError get(String code, String msg) {
        return new CPError(code, msg);
    }

}