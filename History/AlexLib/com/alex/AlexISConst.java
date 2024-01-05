package com.alex;

import com.ironsource.mediationsdk.config.VersionInfo;

public class AlexISConst {

    public static final String KEY_IMPRESSION_DATA = "impression_data";

    public static String getNetworkVersion() {
        try {
            return VersionInfo.VERSION;
        } catch (Throwable ignored) {
        }
        return "";
    }
}
