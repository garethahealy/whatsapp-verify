package com.garethahealy.whatsappverify.config;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
public class PhoneNumberUtilConfig {

    @Singleton
    @Produces
    public PhoneNumberUtil get() {
        return PhoneNumberUtil.getInstance();
    }
}
