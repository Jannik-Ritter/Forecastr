# Forecastr

Forecastr ist ein Java-21-Prototyp für kurzlebige binäre Prognosemärkte. Das Projekt besteht aus einem Spring-Boot-Server mit REST, WebSocket und H2, einem Java-Konsolenclient und einem mobilen Webclient im vertikalen Feed-Stil.

## Bauen

Voraussetzungen: JDK 21 und Maven 3.9+. Für den Webclient werden zusätzlich Node.js 22 und npm 10 benötigt.

```bash
cd forecastr
mvn clean package
cd web
npm ci
npm run build
```

## Starten

Server und Client in getrennten Terminals starten:

```bash
java -jar server/target/forecastr-server-1.0.0-SNAPSHOT.jar
java -jar client/target/client.jar
```

Der Server läuft standardmäßig unter `http://localhost:8080` und lädt beim Start Beispieldaten aus CSV-Dateien. Die H2-Datenbank wird bei jedem Neustart zurückgesetzt.

Der Client verwendet eine übersichtliche deutsche Textoberfläche mit nummerierten Menüs und
formatierten Marktkarten.

```bash
java -jar client/target/client.jar --plain
```

Falls der Server auf einem anderen Port läuft, kann die Adresse weiterhin explizit gesetzt werden:

```bash
java -jar client/target/client.jar --server http://localhost:18080
```

Für die internen Last- und Nebenläufigkeitstests:

```bash
java -jar server/target/forecastr-server-1.0.0-SNAPSHOT.jar --spring.profiles.active=test
java -jar client/target/client.jar --stress --script stress.txt
```

Alternativ startet ein Script Server und Konsolenclient gemeinsam.

```bash
./scripts/run.sh
```

Der Webclient verwendet eine auf Smartphones optimierte, vertikal scrollbare Marktansicht. Server und Webclient starten gemeinsam mit:

```bash
./scripts/run-web.sh
```

Eigene Tierbilder unter `forecastr/web/src/assets/pets/` werden zufällig als Feed-Hintergründe verwendet.

Die vollständige automatisierte Suite kann alternativ mit `--simulate` oder aus dem
Admin-Panel gestartet werden; dafür muss der Server mit dem Profil `test` laufen.

Gewinner teilen den gesamten Einsatzpool anteilig. Auf den Nettogewinn werden 5 % Gebühr erhoben; nicht auflösbare Ereignisse werden vollständig erstattet.
