package com.garethahealy.whatsappverify.model;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class MemberTest {

    @Inject
    PhoneNumberUtil phoneNumberUtil;

    @Test
    void fromBuildsMember() {
        Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber().setRawInput("+44 7818 511214");

        Member member = Member.from(phoneNumber, "gahealy@redhat.com");

        assertEquals(phoneNumber, member.whatsAppNumber());
        assertEquals("gahealy@redhat.com", member.redHatEmailAddress().get());
    }

    @Test
    void toArraySanitizesWhatsAppNumber() {
        Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber().setRawInput("+44 7818 511214");
        Member member = Member.from(phoneNumber, "gahealy@redhat.com");

        List<String> result = member.toArray(phoneNumberUtil);

        assertEquals(Arrays.asList("+44 7818 511214", "gahealy@redhat.com"), result);
    }

    @Test
    void toArrayHandlesNullEmail() {
        Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber().setRawInput("+44 7818 511214");
        Member member = Member.from(phoneNumber, null);

        List<String> result = member.toArray(phoneNumberUtil);

        assertEquals(Arrays.asList("+44 7818 511214", ""), result);
    }
}
