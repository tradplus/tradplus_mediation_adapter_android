package com.tradplus.ads.youdao;

import com.tradplus.ads.base.common.TPError;
import com.youdao.sdk.nativeads.NativeErrorCode;

import static com.tradplus.ads.base.common.TPError.CONNECTION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;


public class YoudaoErrorUtil {

    public static TPError getTradPlusErrorCode(NativeErrorCode errorCode) {
        TPError tradPlusErrorCode = new TPError();
        switch (errorCode) {
            case CONNECTION_ERROR:
                tradPlusErrorCode.setTpErrorCode(CONNECTION_ERROR);
                break;
            case NETWORK_NO_FILL:
                tradPlusErrorCode.setTpErrorCode(NETWORK_NO_FILL);
                break;
            default:
                tradPlusErrorCode.setTpErrorCode(UNSPECIFIED);
        }

        tradPlusErrorCode.setErrorCode(errorCode.getCode() + "");
        tradPlusErrorCode.setErrorMessage(errorCode.name());

        return tradPlusErrorCode;
    }
}
