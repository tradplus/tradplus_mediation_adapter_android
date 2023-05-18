package com.tradplus.ads.klevin;


import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.LOAD_TOO_FREQUENTLY;
import static com.tradplus.ads.base.common.TPError.NETWORK_INVALID_STATE;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.NETWORK_TIMEOUT;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import com.tradplus.ads.base.common.TPError;

public class KlevinErrorUtil {

    public static TPError getTradPlusErrorCode(String errorMessager, int errorCode, String errorMsg) {
        TPError tradPlusErrorCode = new TPError();

        if (errorCode == 1207 || errorCode == 1250 || errorCode == 1251) {
            tradPlusErrorCode.setTpErrorCode(NETWORK_NO_FILL);
            tradPlusErrorCode.setErrorMessage("请求设备异常无广告返回，建议更换设备或使用Demo调试ID");
        } else if (errorCode == 5300 || errorCode == 5301 || errorCode == 5302) {
            tradPlusErrorCode.setTpErrorCode(NETWORK_NO_FILL);
            tradPlusErrorCode.setErrorMessage("广告请求成功，但无广告匹配");
        } else if (errorCode == 5400 || errorCode == 5402) {
            tradPlusErrorCode.setTpErrorCode(LOAD_TOO_FREQUENTLY);
            tradPlusErrorCode.setErrorMessage("短时间重复请求过多");
        } else if (errorCode == 13001) {
            tradPlusErrorCode.setTpErrorCode(NETWORK_INVALID_STATE);
            tradPlusErrorCode.setErrorMessage("网络异常");
        } else if (errorCode == 13002) {
            tradPlusErrorCode.setTpErrorCode(NETWORK_TIMEOUT);
            tradPlusErrorCode.setErrorMessage("网络超时");
        } else if (errorCode == 12003) {
            tradPlusErrorCode.setTpErrorCode(INIT_FAILED);
            tradPlusErrorCode.setErrorMessage("SDK未初始化");
        } else {
            tradPlusErrorCode.setTpErrorCode(UNSPECIFIED);
        }

        return tradPlusErrorCode;
    }
}
