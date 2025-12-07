package com.garethahealy.whatsappverify.config;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.inject.Singleton;

@Singleton
public class PhoneNumberUtilConfig {

    public PhoneNumberUtil get() {
        return PhoneNumberUtil.getInstance();
    }
}
