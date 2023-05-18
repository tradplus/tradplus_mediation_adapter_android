package com.tradplus.ads.toutiao;

import com.bytedance.sdk.openadsdk.LocationProvider;
import com.bytedance.sdk.openadsdk.TTCustomController;

public class UserDataCustomController extends TTCustomController {

    private boolean userAgree;

    public UserDataCustomController(boolean userAgree) {
        this.userAgree = userAgree;
    }

//    private static class Holder {
//        private static UserDataCustomController sInstance = new UserDataCustomController();
//    }
//
//    public static UserDataCustomController getInstance() {
//        return Holder.sInstance;
//    }

//    public UserDataCustomController setUserAgree(boolean userAgree) {
//        this.userAgree = userAgree;
//        return this;
//    }

    /**
     * 是否允许SDK主动使用地理位置信息
     *
     * @return true可以获取，false禁止获取。默认为true
     */
    @Override
    public boolean isCanUseLocation() {
        if (!userAgree) {
            return false;
        }
        return super.isCanUseLocation();
    }

    /**
     * 当isCanUseLocation=false时，可传入地理位置信息，穿山甲sdk使用您传入的地理位置信息
     *
     * @return 地理位置参数
     */
    @Override
    public LocationProvider getTTLocation() {
        return super.getTTLocation();
    }

    @Override
    public boolean alist() {
        return super.alist();
    }

    /**
     * 是否允许SDK主动使用手机硬件参数，如：imei
     *
     * @return true可以使用，false禁止使用。默认为true
     */
    @Override
    public boolean isCanUsePhoneState() {
        if (!userAgree) {
            return false;
        }
        return super.isCanUsePhoneState();
    }

    /**
     * 当isCanUsePhoneState=false时，可传入imei信息，穿山甲sdk使用您传入的imei信息
     *
     * @return imei信息
     */
    @Override
    public String getDevImei() {
        return super.getDevImei();
    }

    /**
     * 是否允许SDK主动使用ACCESS_WIFI_STATE权限
     *
     * @return true可以使用，false禁止使用。默认为true
     */
    @Override
    public boolean isCanUseWifiState() {
        if (!userAgree) {
            return false;
        }
        return super.isCanUseWifiState();
    }


    @Override
    public String getMacAddress() {
        return super.getMacAddress();
    }

    /**
     * 是否允许SDK主动使用WRITE_EXTERNAL_STORAGE权限
     *
     * @return true可以使用，false禁止使用。默认为true
     */
    @Override
    public boolean isCanUseWriteExternal() {
        if (!userAgree) {
            return false;
        }
        return super.isCanUseWriteExternal();
    }

    /**
     * 开发者可以传入oaid
     *  信通院OAID的相关采集——如何获取OAID：
     1. 移动安全联盟官网http://www.msa-alliance.cn/
     2. 信通院统一SDK下载http://msa-alliance.cn/col.jsp?id=120
     * @return oaid
     */
    @Override
    public String getDevOaid() {
        return super.getDevOaid();
    }
}
