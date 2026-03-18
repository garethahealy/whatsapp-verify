package com.garethahealy.whatsappverify.commands;

import com.garethahealy.whatsappverify.model.Member;
import com.garethahealy.whatsappverify.services.LdapGuessService;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;

@CommandLine.Command(
    name = "verify",
    description = "Verify phone numbers against LDAP",
    mixinStandardHelpOptions = true)
public class VerifyCommand implements Runnable {

    @CommandLine.Option(names = {"-p", "--phone-list"}, description = "List of comma separated phone numbers", required = true)
    String csvPhoneNumbers;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Output to write CSV to", defaultValue = "whatsapp.csv")
    String output;

    @CommandLine.Option(names = {"-l", "--last-output"}, description = "Last output created, if one exists", defaultValue = "whatsapp.csv")
    String lastOutput;

    @CommandLine.Option(names = {"-vpn", "--fail-if-no-vpn"}, description = "Throw an exception if can't connect to LDAP", defaultValue = "true")
    boolean failNoVpn;

    @Inject
    private Logger logger;

    @Inject
    private LdapGuessService ldapGuessService;

    @Inject
    private PhoneNumberUtil phoneNumberUtil;

    @Override
    public void run() {
        if (csvPhoneNumbers == null || csvPhoneNumbers.isBlank()) {
            logger.info("No phone numbers provided, nothing to verify");
            return;
        }

        try {
            Path outputPath = Path.of(output);
            if (!Files.exists(outputPath)) {
                Path parent = outputPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                Files.createFile(outputPath);
            }

            Map<Phonenumber.PhoneNumber, Member> lastOutputMap = parseCsv(Path.of(lastOutput));
            List<Phonenumber.PhoneNumber> phoneNumbers = parsePhoneNumbers(csvPhoneNumbers);
            List<Member> members = collectMembers(phoneNumbers);

            List<Member> mergedMembers = mergeLastWithNow(members, lastOutputMap);

            writeCsv(outputPath, mergedMembers);
        } catch (IOException | NumberParseException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Phonenumber.PhoneNumber> parsePhoneNumbers(String csvPhoneNumbers) throws NumberParseException {
        List<String> trimmed = Arrays.stream(csvPhoneNumbers.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .toList();

        logger.infof("%s phone numbers to verify", trimmed.size());

        List<Phonenumber.PhoneNumber> answer = new ArrayList<>();
        for (String raw : trimmed) {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(raw, null);
            number.setRawInput(raw);

            answer.add(number);
        }

        return answer;
    }

    private List<Member> collectMembers(List<Phonenumber.PhoneNumber> phoneNumbers) {
        return Multi.createFrom()
            .iterable(phoneNumbers)
            .onItem()
            .transformToUniAndMerge(number ->
                Uni.createFrom().item(() -> {
                    try {
                        return ldapGuessService.attempt(number, failNoVpn);
                    } catch (IOException | LdapException e) {
                        throw new RuntimeException(e);
                    }
                }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
            )
            .collect().asList()
            .await().atMost(Duration.ofMinutes(5));
    }

    private Map<Phonenumber.PhoneNumber, Member> parseCsv(Path input) throws IOException, NumberParseException {
        Map<Phonenumber.PhoneNumber, Member> answer = new HashMap<>();

        if (Files.exists(input)) {
            CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader(Member.Headers.class)
                .setSkipHeaderRecord(true)
                .get();

            try (Reader reader = new BufferedReader(new FileReader(input.toFile(), StandardCharsets.UTF_8))) {
                Iterable<CSVRecord> records = csvFormat.parse(reader);
                for (CSVRecord record : records) {
                    String whatsAppNumber = record.get(Member.Headers.WhatsAppNumber);
                    String redHatEmailAddress = record.get(Member.Headers.RedHatEmailAddress).trim();

                    try {
                        Phonenumber.PhoneNumber number = phoneNumberUtil.parse(whatsAppNumber, null);
                        number.setRawInput(whatsAppNumber);

                        Member member = Member.from(number, redHatEmailAddress.isEmpty() ? null : redHatEmailAddress);

                        answer.put(member.whatsAppNumber(), member);
                    } catch (NumberParseException e) {
                        logger.errorf("Skipping %s - %s", whatsAppNumber, e);
                    }
                }
            }

            logger.infof("Parsed %s from %s for last-output", answer.size(), input);
        } else {
            logger.infof("--last-output=%s does not exist", input);
        }

        return answer;
    }

    private List<Member> mergeLastWithNow(List<Member> members, Map<Phonenumber.PhoneNumber, Member> lastOutputMap) {
        List<Member> answer = new ArrayList<>();

        for (Member current : members) {
            if (current.redHatEmailAddress().isPresent()) {
                // Verified via LDAP
                logger.debugf("==> %s Verified via LDAP", current.whatsAppNumber().getRawInput());
                answer.add(current);
            } else {
                Optional<Member> found = lastOutputMap.values().stream().filter(last ->
                    phoneNumberUtil.isNumberMatch(current.whatsAppNumber(), last.whatsAppNumber()) == PhoneNumberUtil.MatchType.EXACT_MATCH).findFirst();

                if (found.isPresent()) {
                    Member foundMember = found.get();
                    if (foundMember.redHatEmailAddress().isPresent()) {
                        answer.add(Member.from(current.whatsAppNumber(), foundMember.redHatEmailAddress().get()));

                        logger.infof("-> %s found via mergeLastWithNow", foundMember.redHatEmailAddress().get());
                    } else {
                        // In Last, but Email empty
                        logger.debugf("==> %s In Last, but Email empty", current.whatsAppNumber().getRawInput());
                        answer.add(current);
                    }
                } else {
                    // Not in LDAP or Last
                    logger.debugf("==> %s Not in LDAP or Last", current.whatsAppNumber().getRawInput());
                    answer.add(current);
                }
            }
        }

        return answer;
    }

    private void writeCsv(Path output, List<Member> members) throws IOException {
        CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setHeader(Member.Headers.class)
            .get();

        members.sort(Comparator.comparing(member -> member.redHatEmailAddress().orElse(""), String.CASE_INSENSITIVE_ORDER));

        try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                for (Member current : members) {
                    csvPrinter.printRecord(current.toArray(phoneNumberUtil));
                }
            }
        }
    }
}
