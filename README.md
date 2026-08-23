# Forecastr

Forecastr ist ein Java-21-Prototyp für kurzlebige binäre Prognosemärkte mit
Spring-Boot-Server und Konsolenclient.

## Voraussetzungen

- JDK 21
- Maven 3.9+

## Bauen

```bash
cd forecastr
mvn package
```

## Starten

Server und Client in getrennten Terminals starten:

```bash
java -jar server/target/forecastr-server-1.0.0-SNAPSHOT.jar
java -jar client/target/client.jar
```
