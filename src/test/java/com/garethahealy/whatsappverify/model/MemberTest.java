package com.garethahealy.whatsappverify.model;

import com.google.i18n.phonenumbers.Phonenumber;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberTest {

    @Test
    void fromBuildsMember() {
        Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber().setRawInput("+44 7818 511214");

        Member member = Member.from(phoneNumber, "gahealy@redhat.com");

        assertEquals(phoneNumber, member.whatsAppNumber());
        assertEquals("gahealy@redhat.com", member.redHatEmailAddress());
    }

    @Test
    void toArraySanitizesWhatsAppNumber() {
        Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber().setRawInput("+44 7818 511214");
        Member member = new Member(phoneNumber, "gahealy@redhat.com");

        List<String> result = member.toArray();

        assertEquals(Arrays.asList("00447818511214", "gahealy@redhat.com"), result);
    }
}
