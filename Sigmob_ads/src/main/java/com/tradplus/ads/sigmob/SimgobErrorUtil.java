package com.tradplus.ads.sigmob;

import com.sigmob.windad.WindAdError;
import com.tradplus.ads.base.common.TPError;

import static com.tradplus.ads.base.common.TPError.CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INVALID_PLACEMENTID;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.NETWORK_TIMEOUT;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

/**
 * Created by sainase on 2020-06-16.
 */
public class SimgobErrorUtil {

    public static TPError getTradPlusErrorCode(WindAdError errorCode) {
        TPError tradPlusErrorCode = new TPError();
        switch (errorCode) {
            case ERROR_SIGMOB_PLACEMENTID_EMPTY:
                tradPlusErrorCode.setTpErrorCode(NETWORK_NO_FILL);
                break;
            case ERROR_SIGMOB_AD_TIME_OUT:
                tradPlusErrorCode.setTpErrorCode(NETWORK_TIMEOUT);
            default:
                tradPlusErrorCode.setTpErrorCode(UNSPECIFIED);
        }

        tradPlusErrorCode.setErrorCode(errorCode.getErrorCode() + "");
        tradPlusErrorCode.setErrorMessage(errorCode.getMessage());

        return tradPlusErrorCode;
    }
}
