package com.tradplus.ads.facebook;

import com.facebook.ads.AdError;
import com.tradplus.ads.base.common.TPError;

import static com.facebook.ads.AdError.CACHE_ERROR_CODE;
import static com.facebook.ads.AdError.LOAD_TOO_FREQUENTLY_ERROR_CODE;
import static com.facebook.ads.AdError.NETWORK_ERROR_CODE;
import static com.facebook.ads.AdError.NO_FILL_ERROR_CODE;
import static com.facebook.ads.AdError.SERVER_ERROR_CODE;
import static com.tradplus.ads.base.common.TPError.CONNECTION_ERROR;
import static com.tradplus.ads.base.common.TPError.IMAGE_DOWNLOAD_FAILURE;
import static com.tradplus.ads.base.common.TPError.INVALID_RESPONSE;
import static com.tradplus.ads.base.common.TPError.LOAD_TOO_FREQUENTLY;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

/**
 * Created by sainase on 2020-06-10.
 */
public class FacebookErrorUtil {

    public static TPError getTradPlusErrorCode(AdError errorCode){
        TPError tradPlusErrorCode = new TPError();
        switch (errorCode.getErrorCode()) {
            case NO_FILL_ERROR_CODE:
                tradPlusErrorCode.setTpErrorCode(NETWORK_NO_FILL);
                break;
            case NETWORK_ERROR_CODE:
                tradPlusErrorCode.setTpErrorCode(CONNECTION_ERROR);
                break;
            case LOAD_TOO_FREQUENTLY_ERROR_CODE:
                tradPlusErrorCode.setTpErrorCode( LOAD_TOO_FREQUENTLY);
                break;
            case SERVER_ERROR_CODE:
                tradPlusErrorCode .setTpErrorCode( INVALID_RESPONSE);
                break;
            case CACHE_ERROR_CODE:
                tradPlusErrorCode .setTpErrorCode( IMAGE_DOWNLOAD_FAILURE);
                break;
            default:
                tradPlusErrorCode .setTpErrorCode( UNSPECIFIED);
        }

        tradPlusErrorCode.setErrorCode(errorCode.getErrorCode()+"");
        tradPlusErrorCode.setErrorMessage(errorCode.getErrorMessage());

        return tradPlusErrorCode;
    }
}
