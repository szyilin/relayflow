package com.relayflow.module.im.enums;

public final class ImBotEnablePolicy {

    public static final String MANDATORY = "mandatory";
    public static final String DEFAULT_ON = "default_on";
    public static final String OPT_IN = "opt_in";
    public static final String INSTALLABLE = "installable";

    private ImBotEnablePolicy() {
    }

    public static boolean autoEnableOnActive(String policy) {
        return MANDATORY.equals(policy) || DEFAULT_ON.equals(policy);
    }
}
