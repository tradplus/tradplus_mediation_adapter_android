package com.tradplus.crosspro.network.base;

public class CPError {

    /***
     * Error Code
     */
    protected String code;
    /***
     * Error Message
     */
    protected String desc;

    public CPError(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public String printStackTrace() {
        return "code[ " + code + " ],desc[ " + desc + " ]";
    }
}