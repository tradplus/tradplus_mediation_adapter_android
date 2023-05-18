package com.tradplus.ads.ironsource;

import com.ironsource.mediationsdk.logger.IronSourceError;
import com.tradplus.ads.base.common.TPError;

import static com.ironsource.mediationsdk.logger.IronSourceError.ERROR_CODE_NO_ADS_TO_SHOW;
import static com.ironsource.mediationsdk.logger.IronSourceError.ERROR_NO_INTERNET_CONNECTION;
import static com.tradplus.ads.base.common.TPError.CONNECTION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

public class IronSourceErrorUtil {

    public static TPError getTradPlusErrorCode(IronSourceError ironSourceError ){
        TPError tradPlusErrorCode = new TPError();
        switch (ironSourceError.getErrorCode()) {
            case ERROR_NO_INTERNET_CONNECTION:
                tradPlusErrorCode .setTpErrorCode( CONNECTION_ERROR);
                break;
            case ERROR_CODE_NO_ADS_TO_SHOW:
                tradPlusErrorCode .setTpErrorCode( NETWORK_NO_FILL);
                break;
            default:
                tradPlusErrorCode .setTpErrorCode( UNSPECIFIED);
        }
        tradPlusErrorCode.setErrorCode(ironSourceError.getErrorCode()+"");
        tradPlusErrorCode.setErrorMessage(ironSourceError.getErrorMessage());

        return tradPlusErrorCode;
    }

    public static TPError getTradPlusShowFailedErrorCode(IronSourceError ironSourceError ){
        TPError tradPlusErrorCode = new TPError(SHOW_FAILED);

        tradPlusErrorCode.setErrorCode(ironSourceError.getErrorCode()+"");
        tradPlusErrorCode.setErrorMessage(ironSourceError.getErrorMessage());

        return tradPlusErrorCode;
    }

}
