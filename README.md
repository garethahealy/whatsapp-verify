# whatsapp-verify

CLI to verify phone numbers against LDAP

## Build
Both JVM and Native mode are supported.

```bash
./mvnw clean install
./mvnw clean install -Pnative
```

Which allows you to run via:

```bash
java -jar target/quarkus-app/quarkus-run.jar help
./target/whatsapp-verify-*-runner help

```

## LDAP Lookup

```bash
ldapsearch -x -h ldap.corp.redhat.com -b dc=redhat,dc=com -s sub 'mobile=+447712345678'
ldapsearch -x -h ldap.corp.redhat.com -b dc=redhat,dc=com -s sub 'homePhone=+447712345678'
```

## APIs

Once you've built the code, you can execute the commands, for example:

```bash
./target/whatsapp-verify-*-runner verify --phone-list=+447712345678
```

For a full list of commands, see: [docs](docs)
