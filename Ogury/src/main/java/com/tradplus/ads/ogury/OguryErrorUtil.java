package com.tradplus.ads.ogury;

import com.ogury.core.OguryError;
import com.tradplus.ads.base.common.TPError;

import static com.ogury.cm.OguryChoiceManagerErrorCode.NO_INTERNET_CONNECTION;
import static com.ogury.ed.OguryAdFormatErrorCode.AD_NOT_AVAILABLE;
import static com.tradplus.ads.base.common.TPError.CONNECTION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;
import static io.presage.interstitial.PresageErrors.SDK_INIT_FAILED;
import static io.presage.interstitial.PresageErrors.SDK_INIT_NOT_CALLED;

public class OguryErrorUtil {

    public static TPError getTradPlusErrorCode(OguryError errorCode){
        TPError tradPlusErrorCode = new TPError();
        switch (errorCode.getErrorCode()) {
            case NO_INTERNET_CONNECTION:
                tradPlusErrorCode .setTpErrorCode( CONNECTION_ERROR);
                break;
            case SDK_INIT_NOT_CALLED:
                tradPlusErrorCode .setTpErrorCode(  INIT_FAILED);
                break;
            case SDK_INIT_FAILED:
                tradPlusErrorCode .setTpErrorCode(  CONNECTION_ERROR);
                break;
            case AD_NOT_AVAILABLE:
                tradPlusErrorCode .setTpErrorCode(  NETWORK_NO_FILL);
                break;
            default:
                tradPlusErrorCode .setTpErrorCode(  UNSPECIFIED);
        }

        tradPlusErrorCode.setErrorCode(errorCode.getErrorCode()+"");
        tradPlusErrorCode.setErrorMessage(errorCode.getMessage());

        return tradPlusErrorCode;
    }
}
