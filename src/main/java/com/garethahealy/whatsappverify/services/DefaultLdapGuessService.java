package com.garethahealy.whatsappverify.services;

import com.garethahealy.whatsappverify.model.Member;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.jboss.logging.Logger;

import java.io.IOException;

/**
 * Handles searching LDAP and attempts to 'guess' who someone might be, based on their name or user id
 */
@ApplicationScoped
public class DefaultLdapGuessService {

    @Inject
    Logger logger;

    private final LdapSearchService ldapSearchService;

    public DefaultLdapGuessService(LdapSearchService ldapSearchService) {
        this.ldapSearchService = ldapSearchService;
    }

    /**
     * Attempt to guess the 'phoneToGuess' using several rules
     *
     * @param phoneToGuess
     * @param failNoVpn
     * @return
     * @throws IOException
     * @throws LdapException
     */
    public Member attempt(Phonenumber.PhoneNumber phoneToGuess, boolean failNoVpn) throws IOException, LdapException {
        Member guessed = null;

        if (ldapSearchService.canConnect()) {
            try (LdapConnection connection = ldapSearchService.open()) {
                logger.infof("Attempting to guess %s", phoneToGuess.getRawInput());

                guessed = searchOnMobile(connection, phoneToGuess);
                if (guessed == null) {
                    guessed = searchOnHomePhone(connection, phoneToGuess);
                }
            }
        } else {
            if (failNoVpn) {
                throw new IOException("Unable to connect to LDAP. Are you on the VPN?");
            }
        }

        return guessed;
    }


    private Member searchOnMobile(LdapConnection connection, Phonenumber.PhoneNumber phoneToGuess) throws IOException, LdapException {
        Member answer = searchOnRawMobile(connection, phoneToGuess);
        if (answer == null) {
            answer = searchOnMobileNationalNumberWildcard(connection, phoneToGuess);
        }

        return answer;
    }

    private Member searchOnRawMobile(LdapConnection connection, Phonenumber.PhoneNumber phoneToGuess) throws IOException, LdapException {
        Member answer = null;

        String rhEmail = ldapSearchService.searchOnMobile(connection, phoneToGuess.getRawInput());
        if (!rhEmail.isEmpty()) {
            logger.infof("-> %s found via searchOnRawMobile", rhEmail);

            answer = Member.from(phoneToGuess, rhEmail);
        }

        return answer;
    }

    private Member searchOnMobileNationalNumberWildcard(LdapConnection connection, Phonenumber.PhoneNumber phoneToGuess) throws IOException, LdapException {
        Member answer = null;

        String rhEmail = ldapSearchService.searchOnMobile(connection, "*" + phoneToGuess.getNationalNumber());
        if (!rhEmail.isEmpty()) {
            logger.infof("-> %s found via searchOnMobileNationalNumberWildcard", rhEmail);

            answer = Member.from(phoneToGuess, rhEmail);
        }

        return answer;
    }

    private Member searchOnHomePhone(LdapConnection connection, Phonenumber.PhoneNumber phoneToGuess) throws IOException, LdapException {
        Member answer = searchOnRawHomePhone(connection, phoneToGuess);
        if (answer == null) {
            answer = searchOnHomePhoneNationalNumberWildcard(connection, phoneToGuess);
        }

        return answer;
    }

    private Member searchOnRawHomePhone(LdapConnection connection, Phonenumber.PhoneNumber phoneToGuess) throws IOException, LdapException {
        Member answer = null;

        String rhEmail = ldapSearchService.searchOnHomePhone(connection, phoneToGuess.getRawInput());
        if (!rhEmail.isEmpty()) {
            logger.infof("-> %s found via searchOnRawHomePhone", rhEmail);

            answer = Member.from(phoneToGuess, rhEmail);
        }

        return answer;
    }

    private Member searchOnHomePhoneNationalNumberWildcard(LdapConnection connection, Phonenumber.PhoneNumber phoneToGuess) throws IOException, LdapException {
        Member answer = null;

        String rhEmail = ldapSearchService.searchOnHomePhone(connection, "*" + phoneToGuess.getNationalNumber());
        if (!rhEmail.isEmpty()) {
            logger.infof("-> %s found via searchOnHomePhoneNationalNumberWildcard", rhEmail);

            answer = Member.from(phoneToGuess, rhEmail);
        }

        return answer;
    }
}
