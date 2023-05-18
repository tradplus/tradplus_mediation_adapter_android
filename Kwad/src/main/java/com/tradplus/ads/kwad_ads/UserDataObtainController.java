package com.tradplus.ads.kwad_ads;

import android.location.Location;

import com.kwad.sdk.api.KsCustomController;

import java.util.List;

/**
 * 信息获取控制工具
 */
public class UserDataObtainController extends KsCustomController {

    private boolean userAgree;

    public UserDataObtainController(boolean userAgree) {
        this.userAgree = userAgree;
    }

//  private static class Holder {
//    private static UserDataObtainController sInstance = new UserDataObtainController();
//  }
//
//  public static UserDataObtainController getInstance() {
//    return Holder.sInstance;
//  }
//
//  public UserDataObtainController setUserAgree(boolean userAgree) {
//    this.userAgree = userAgree;
//    return this;
//  }

    @Override
    public boolean canReadLocation() {
        // 为提高广告转化率，取得更好收益，建议媒体在用户同意隐私政策及权限信息后，允许SDK获取地理位置信息。
        if (!userAgree) {
            return false;
        }
        return super.canReadLocation();
    }

    @Override
    public boolean canUsePhoneState() {
        // 为提高广告转化率，取得更好收益，建议媒体在用户同意隐私政策及权限信息后，允许SDK使用手机硬件信息。
        if (!userAgree) {
            return false;
        }
        return super.canUsePhoneState();
    }

    @Override
    public boolean canUseOaid() {
        // 为提高广告转化率，取得更好收益，建议媒体在用户同意隐私政策及权限信息后，允许SDK使用设备oaid。
        if (!userAgree) {
            return false;
        }
        return super.canUseOaid();
    }

    @Override
    public boolean canUseMacAddress() {
        // 为提高广告转化率，取得更好收益，建议媒体在用户同意隐私政策及权限信息后，允许SDK使用设备Mac地址。
        if (!userAgree) {
            return false;
        }
        return super.canUseMacAddress();
    }

    @Override
    public boolean canReadInstalledPackages() {
        // 为提高广告转化率，取得更好收益，建议媒体在用户同意隐私政策及权限信息后，允许SDK读取app安装列表。
        if (!userAgree) {
            return false;
        }
        return super.canReadInstalledPackages();
    }

    @Override
    public boolean canUseStoragePermission() {
        // 为提升SDK的接入体验，广告展示更流畅，建议媒体在用户同意隐私政策及权限信息后，允许SDK使用存储权限。
        if (!userAgree) {
            return false;
        }
        return super.canUseStoragePermission();
    }

    @Override
    public boolean canUseNetworkState() {
        // 为提升SDK的接入体验，广告展示更流畅，建议媒体在用户同意隐私政策及权限信息后，允许SDK读取网络状态信息。
        if (!userAgree) {
            return false;
        }
        return super.canUseNetworkState();
    }

    @Override
    public Location getLocation() {
        return super.getLocation();
    }

    @Override
    public String getImei() {
        return super.getImei();
    }

    @Override
    public String[] getImeis() {
        return super.getImeis();
    }

    @Override
    public String getAndroidId() {
        return super.getAndroidId();
    }

    @Override
    public String getOaid() {
        return super.getOaid();
    }

    @Override
    public String getMacAddress() {
        return super.getMacAddress();
    }

    @Override
    public List<String> getInstalledPackages() {
        return super.getInstalledPackages();
    }
}
