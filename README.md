<div align="center">
  <img src="forecastr/web/public/favicon.svg" alt="Forecastr Logo" width="88" height="88">

  <h1>Forecastr</h1>

  <p><strong>Kurze Aufmerksamkeitsspanne. Langfristige Konsequenzen.</strong></p>

  <p>
    <img src="https://img.shields.io/badge/Java-21-0d0d0d?style=flat-square&amp;logo=openjdk&amp;logoColor=white" alt="Java 21">
    <img src="https://img.shields.io/badge/Spring_Boot-3.3.5-6366f1?style=flat-square&amp;logo=springboot&amp;logoColor=white" alt="Spring Boot 3.3.5">
    <img src="https://img.shields.io/badge/React-19-0d0d0d?style=flat-square&amp;logo=react&amp;logoColor=white" alt="React 19">
  </p>
</div>

---

Forecastr ist ein Java-21-Prototyp für kurzlebige binäre Prognosemärkte. Das Projekt besteht aus einem Spring-Boot-Server mit REST, WebSocket und H2, einem Java-Konsolenclient und einem mobilen Webclient im TikTok-Stil.

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

Die vollständige automatisierte Suite kann alternativ mit `--simulate` oder aus dem
Admin-Panel gestartet werden; dafür muss der Server mit dem Profil `test` laufen.

Gewinner teilen den gesamten Einsatzpool anteilig. Auf den Nettogewinn werden 5 % Gebühr erhoben; nicht auflösbare Ereignisse werden vollständig erstattet.
