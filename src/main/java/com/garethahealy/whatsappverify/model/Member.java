package com.garethahealy.whatsappverify.model;

import com.google.i18n.phonenumbers.Phonenumber;

import java.util.Arrays;
import java.util.List;

public record Member(Phonenumber.PhoneNumber whatsAppNumber, String redHatEmailAddress) {

    public enum Headers {
        WhatsAppNumber,
        RedHatEmailAddress,
    }

    public static Member from(Phonenumber.PhoneNumber whatsAppNumber, String redHatEmailAddress) {
        return new Member(whatsAppNumber, redHatEmailAddress);
    }

    public List<String> toArray(PhoneNumberUtil phoneNumberUtil) {
        String sanitizedWhatsAppNumber = phoneNumberUtil.format(whatsAppNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        return Arrays.asList(sanitizedWhatsAppNumber, redHatEmailAddress.orElse(""));
    }
}
