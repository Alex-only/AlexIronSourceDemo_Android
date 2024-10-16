package com.alex;

import com.ironsource.mediationsdk.config.VersionInfo;

public class AlexISConst {

    public static final String KEY_IMPRESSION_DATA = "impression_data";
    public static final String KEY_AD_INFO = "ad_info";

    public static final String KEY_REVENUE = "revenue";
    public static final String KEY_AD_UNIT_ID = "ad_unit_id";
    public static final String KEY_FORMAT = "format";
    public static final String KEY_NETWORK_NAME= "network_name";
    public static final String KEY_NETWORK_PLACEMENT_ID = "network_placement_id";
    public static final String KEY_PLACEMENT = "placement";
    public static final String KEY_COUNTRY_CODE = "country_code";


    private static final String ADAPTER_VERSION = "1.0.3";

    public static String getNetworkVersion() {
        try {
            return VersionInfo.VERSION;
        } catch (Throwable ignored) {
        }
        return "";
    }
}
