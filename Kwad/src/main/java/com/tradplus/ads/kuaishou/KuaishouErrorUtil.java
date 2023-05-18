package com.tradplus.ads.kuaishou;

import com.tradplus.ads.base.common.TPError;

import static com.tradplus.ads.base.common.TPError.CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.CONNECTION_ERROR;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

public class KuaishouErrorUtil {

    public static TPError getTradPlusErrorCode(int errorCode) {
        TPError tradPlusErrorCode = new TPError();
        switch (errorCode) {
            case 40001:
                tradPlusErrorCode.setErrorMessage("没有⽹络");
                tradPlusErrorCode.setTpErrorCode(CONNECTION_ERROR);
                break;
            case 40004:
                tradPlusErrorCode.setErrorMessage("缓存视频资源失");
                break;
            case 310001:
                tradPlusErrorCode.setTpErrorCode(CONFIGURATION_ERROR);
                tradPlusErrorCode.setErrorMessage("appId未注册");
                break;
            case 310004:
                tradPlusErrorCode.setTpErrorCode(CONFIGURATION_ERROR);
                tradPlusErrorCode.setErrorMessage("packageName与注册的packageName不⼀致");
                break;
            case 320003:
                tradPlusErrorCode.setTpErrorCode(CONFIGURATION_ERROR);
                tradPlusErrorCode.setErrorMessage("appId对应账号已封禁");
                break;
            case 330002:
                tradPlusErrorCode.setTpErrorCode(CONFIGURATION_ERROR);
                tradPlusErrorCode.setErrorMessage("posId⽆效");
                break;
            default:
                tradPlusErrorCode.setTpErrorCode(UNSPECIFIED);
        }

        tradPlusErrorCode.setErrorCode(errorCode + "");
        return tradPlusErrorCode;
    }

    public static TPError geTpMsg(String errorMessager, int errorCode, String msg) {
        TPError tradPlusErrorCode = new TPError(errorMessager);
        tradPlusErrorCode.setErrorCode(errorCode + "");
        tradPlusErrorCode.setErrorMessage(msg);
        return tradPlusErrorCode;
    }
}
