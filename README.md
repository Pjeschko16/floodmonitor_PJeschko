# Flood Monitor – REST-Schnittstelle für ein Hochwasser-Frühwarnsystem

Flood Monitor ist eine Spring-Boot-Anwendung, die fünf Messstationen simuliert und Stations- sowie Messdaten über eine REST-API bereitstellt. Die API unterstützt JSON und XML. Ein jQuery-Webclient zeigt die Stationsdaten in einer Tabelle.

Diese README beschreibt den aktuellen Code. Die ausführlichere Gesprächsunterlage mit Simulationsregeln und offenen Punkten steht in [Dokumentation.md](Dokumentation.md); eine kurze Verlaufssummary der KI-Unterstützung steht in [AI.md](AI.md).

## Inhalt des Projekts

| Bereich                               | Ort                                                                      |
| ------------------------------------- | ------------------------------------------------------------------------ |
| Spring-Boot-Anwendung                 | `src/main/java/com/example/floodmonitor`                                 |
| Browserclient                         | `src/main/resources/static/index.html`                                   |
| Build-Konfiguration                   | `build.gradle`, `settings.gradle`, `gradlew`, `gradlew.bat`              |
| automatisierte Tests                  | `src/test/java/com/example/floodmonitor/StationApiTests.java`            |
| API-Dokumentation                     | OpenAPI über Springdoc, live unter `/swagger-ui.html` und `/v3/api-docs` |
| Projekt- und Simulationsdokumentation | [Dokumentation.md](Dokumentation.md)                                     |
| ursprüngliche Aufgabenstellung        | [TASK.md](TASK.md)                                                       |

Es gibt derzeit keine `application.example.properties`, keine exportierten OpenAPI-Dateien und keine gespeicherten Client-Screenshots.

## Start

Voraussetzung: Java 17 oder neuer. Der Gradle Wrapper ist im Projekt enthalten.

```powershell
.\gradlew.bat bootRun
```

Die Anwendung startet unter `http://localhost:8080`.

| URL                                          | Inhalt                                           |
| -------------------------------------------- | ------------------------------------------------ |
| `http://localhost:8080/`                     | Dashboard                                        |
| `http://localhost:8080/api/v1/stations`      | Stationsliste, Format per `Accept`               |
| `http://localhost:8080/api/v1/stations/json` | Stationsliste inline als JSON                    |
| `http://localhost:8080/api/v1/stations/xml`  | Stationsliste inline als XML                     |
| `http://localhost:8080/swagger-ui.html`      | Swagger UI                                       |
| `http://localhost:8080/v3/api-docs`          | OpenAPI als JSON; YAML unter `/v3/api-docs.yaml` |

## Konfiguration

In `src/main/resources/application.properties` ist aktuell nur `server.port=8080` gesetzt. Weitere Einstellungen haben Code-Defaults und können als Spring-Properties übergeben werden:

| Property              | Standard | Bedeutung                               |
| --------------------- | --------:| --------------------------------------- |
| `simulation.interval` | `10000`  | Simulationsintervall in Millisekunden   |
| `simulation.enabled`  | `true`   | Simulation aktivieren oder deaktivieren |

Zum Beispiel mit verkürztem Intervall:

```powershell
.\gradlew.bat bootRun --args="--simulation.interval=2000"
```

Mit `--simulation.enabled=false` wird die Simulation deaktiviert. Die Tests verwenden diese Einstellung.

## Architektur

```text
com.example.floodmonitor
├── config/       OpenApiConfig
├── controller/   StationController – HTTP-Endpunkte
├── error/        ApiError, ApiException, GlobalExceptionHandler
├── model/        Station, Measurement, Statistics, Warnlevel
├── repository/   StationRepository, MeasurementRepository (In-Memory)
└── service/      SimulationService – Fachlogik und Simulation

src/main/resources/static/index.html – Webclient
```

Ablauf einer API-Anfrage: **Controller → Service → Repository**. Der Controller verarbeitet HTTP-Parameter und ruft den Service auf. Der Service validiert Werte, filtert Daten und berechnet Warnungen und Statistiken. Die Repositories speichern Stationen und Messhistorien im Arbeitsspeicher. `GlobalExceptionHandler` liefert einheitliche Fehlerantworten.

## Datenmodell

**Station:** `id`, `stationName`, `timestamp`, `waterLevel`, `waterTemperature`, `unit`, `batteryLevel`, `flowRate`, `status`, `warnLevel`, `precipitationRate`, `waterRiseRate`, `river` und Aktivstatus.

Die fünf Startstationen heißen Station Vienna, Station Linz, Station Salzburg, Station Graz und Station Innsbruck. Ihre IDs sind `1` bis `5`. Wasserstände werden in Metern (`m`) gespeichert, Niederschlag in `mm/h`. Das `river`-Feld ist bei den Startstationen derzeit nicht befüllt.

**Measurement:** `timestamp`, `waterLevel`, `flowRate`, `rainfall` und `warningLevel`. Java-seitig heißen die letzten beiden Werte `precipitationRate` und `warnLevel`.

**Statistics:** Minimum und Maximum des Wasserstands, durchschnittlicher Wasserstand und Durchfluss, Niederschlagssumme sowie Anzahl aller Messungen, Warnungen und kritischen Warnungen.

Die Projektmodelle sind bewusst einfacher als das vollständige Zielschema der Aufgabenstellung: Es gibt keine `Location`-Klasse und keine eigenen `StationStatus`- oder `WarningLevel`-Enums. `status` und Warnstufe sind Strings. Zeitstempel werden als Strings gespeichert. IDs aus Beispielen wie `ST-001` existieren nicht; gültig sind aktuell `1` bis `5`.

## Simulationsregeln

Die Regeln sind in `service/SimulationService.java` implementiert:

- Beim Start werden fünf Stationen und jeweils ein erster Messwert erzeugt. Anfangs-Pegel: zufällig zwischen 0,5 und 4,0 m; Wassertemperatur: 4 bis 20 °C; Akku: 100 %.
- Standardmäßig läuft die Simulation alle zehn Sekunden. Sie bezieht den vorherigen Pegel jeder Station ein.
- Mit 20 % Wahrscheinlichkeit wird Niederschlag bis 25 mm/h simuliert; sonst 0 mm/h.
- Der Pegel verändert sich durch eine kleine Gauß-Zufallsschwankung und den Niederschlagseinfluss. Er wird nicht negativ und auf zwei Nachkommastellen gerundet.
- Der Durchfluss orientiert sich am Wasserstand (`Wasserstand × 1,5`) mit kleiner Zufallsabweichung und bleibt mindestens null.
- Der Akku sinkt pro Lauf zufällig um 0,1 bis 0,5 Prozentpunkte. Unter 10 % wird der Status `MAINTENANCE` gesetzt.
- `DEACTIVATED` und `FAILED` werden übersprungen; dabei wird `UNKNOWN` gesetzt und keine neue Messung gespeichert. `MAINTENANCE` stoppt die Simulation derzeit nicht.

### Warnstufen

Die Warnstufe wird ausschließlich serverseitig berechnet:

| Bedingung                                                              | Warnstufe  |
| ---------------------------------------------------------------------- | ---------- |
| Wasserstand mindestens 4,2 m                                           | `CRITICAL` |
| Wasserstand mindestens 3,0 m, aber unter 4,2 m                         | `WARNING`  |
| Wasserstand unter 3,0 m                                                | `NORMAL`   |
| Bei `NORMAL`: Regen über 15 mm/h und Anstieg über 0,08 m pro Intervall | `WARNING`  |
| Status `DEACTIVATED` oder `FAILED`                                     | `UNKNOWN`  |

## REST-Schnittstelle

Basis-URL: `/api/v1`.

| Methode | Endpunkt                                    | Beschreibung                                                    |
| ------- | ------------------------------------------- | --------------------------------------------------------------- |
| GET     | `/stations`                                 | Stationen; optionale Filter `river`, `isActive`, `warningLevel` |
| GET     | `/stations/json`                            | Stationen als JSON inline anzeigen                              |
| GET     | `/stations/xml`                             | Stationen als XML inline anzeigen                               |
| GET     | `/stations/{id}`                            | Eine Station abrufen                                            |
| GET     | `/stations/{stationId}/measurements/latest` | Aktuellsten Messwert abrufen                                    |
| GET     | `/stations/{stationId}/measurements`        | Messhistorie und Statistiken abrufen                            |
| GET     | `/alerts`                                   | Warnungen; optionale Filter `warningLevel` und `river`          |

Historische Messwerte unterstützen `from`, `to`, `limit` und `warningLevel`. Die Zeitwerte müssen ISO-8601 sein; `from` darf nicht nach `to` liegen. `limit` muss zwischen 1 und 1000 liegen. Warnstufen: `NORMAL`, `WARNING`, `CRITICAL`, `UNKNOWN`.

Beispiele:

```text
GET /api/v1/stations?isActive=true
GET /api/v1/stations?warningLevel=CRITICAL
GET /api/v1/stations/1/measurements?from=2026-05-01T08:00:00Z&to=2026-05-01T12:00:00Z&limit=100
GET /api/v1/alerts?warningLevel=WARNING&river=Donau
```

### JSON und XML

Die üblichen API-Endpunkte unterstützen Content Negotiation über `Accept`:

```http
Accept: application/json
```

```http
Accept: application/xml
```

Ohne `Accept`-Header wird JSON geliefert. Ein nicht unterstütztes Format ergibt `406 Not Acceptable`. Die zusätzlichen `/stations/json`- und `/stations/xml`-Pfade stellen die Formate direkt im Browser dar und lösen keinen Dateidownload aus.

```bash
curl -H "Accept: application/json" http://localhost:8080/api/v1/stations
curl -H "Accept: application/xml" http://localhost:8080/api/v1/stations
curl http://localhost:8080/api/v1/stations/json
curl http://localhost:8080/api/v1/stations/xml
```

Eine Stationsantwort enthält Felder des `Station`-Modells, etwa `id`, `stationName`, `waterLevel`, `waterTemperature`, `flowRate`, `batteryLevel`, `status` und `warnLevel`. Eine Messwertantwort enthält `timestamp`, `waterLevel`, `flowRate`, `rainfall` und `warningLevel`. Die Messwertantwort enthält derzeit nicht alle Felder des Beispiels aus der Aufgabenstellung, etwa `stationId`, `temperature` oder `batteryLevel`.

### Fehler und Statuscodes

Die zentrale Fehlerbehandlung steht in `error/GlobalExceptionHandler.java`. Fehlerantworten enthalten `timestamp`, `status`, `error`, `message` und `path`; Stacktraces werden nicht ausgegeben.

| Status | Anwendung im aktuellen Projekt                      |
| ------ | --------------------------------------------------- |
| 200    | Anfrage erfolgreich                                 |
| 400    | Ungültiges Datum, Zeitraum, Warnstufe oder Limit    |
| 404    | Station oder Messwert nicht gefunden                |
| 406    | Antwortformat nicht unterstützt                     |
| 415    | Handler für nicht unterstützten Medientyp vorhanden |
| 500    | Unerwarteter interner Fehler                        |

Authentifizierung und schreibende Endpunkte (POST, PUT, PATCH, DELETE) sind nicht implementiert; daher gibt es aktuell keine fachlichen Antworten mit 201, 204, 401, 403 oder 409.

## Webclient

Das Dashboard unter `http://localhost:8080/` lädt Stationsdaten mit jQuery und zeigt sie in einer Tabelle. Über das Auswahlfeld wird JSON oder XML angefordert. Die Tabelle aktualisiert sich alle zehn Sekunden und zeigt Station, Wasserstand, Durchfluss, Temperatur, Akku, Status, Warnstufe und Zeitpunkt. Warnstufen sind farblich gekennzeichnet.

Der Client hat aktuell keine Filter-Steuerelemente und zeigt keine historische Messwerttabelle. Die Filter sind über die REST-API verfügbar.

## OpenAPI

Springdoc stellt die annotierten Endpunkte im OpenAPI-Format bereit:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

Authentifizierung ist derzeit nicht erforderlich und nicht implementiert.

## Automatisierte Tests

Tests befinden sich in `src/test/java/com/example/floodmonitor/StationApiTests.java` und verwenden Spring MockMvc. Sie prüfen JSON/XML, beide Formatpfade, Stationsabrufe, Fehlerfälle, Messwerte, Zeitfilter, Warnstufen, nicht unterstützte Formate, das maximale Limit und die OpenAPI-Ausgabe.

```powershell
.\gradlew.bat test
```

Die Tests setzen `simulation.enabled=false` und legen reproduzierbare Messwerte an. Der letzte bekannte Testlauf endete mit `BUILD SUCCESSFUL`.

## Noch nicht umgesetzt

- Filtersteuerelemente und historische Messwertansicht im Webclient
- befüllte Flussnamen und geografische Positionen
- vollständiges Zieldatenmodell mit `Location`, Status-Enum und Zeittypen aus `java.time`
- persistente Speicherung über einen Neustart hinaus
- Authentifizierung und schreibende API-Endpunkte
- Screenshot-Dateien des Webclients und separate exportierte OpenAPI-Dateien

Git wurde auf Wunsch aus dem Projektumfang ausgeschlossen.

## Fragestellungen aus der Aufgabenstellung

- **JSON und XML:** JSON stellt Daten als Objekte und Arrays dar; XML verwendet verschachtelte Elemente. Die API serialisiert dieselben Stationsdaten in beiden Formaten.
- **Formatwahl:** Spring MVC wertet den `Accept`-Header aus. Die unterstützten Formate sind in `controller/StationController.java` deklariert; Jackson XML ist in `build.gradle` eingebunden.
- **REST:** Ressourcen werden über URLs angesprochen, etwa `/stations/1`; HTTP-Methoden und Statuscodes beschreiben die Aktion und das Ergebnis.
- **GET, POST, PUT, PATCH:** GET liest, POST erstellt, PUT ersetzt und PATCH ändert einzelne Felder. Das Projekt implementiert aktuell nur GET.
- **Neuer Endpunkt:** Controller, bei Bedarf Service/Repository/Model, OpenAPI-Metadaten und Tests anpassen.
- **OpenAPI und Swagger:** OpenAPI ist die API-Beschreibungsspezifikation; Swagger UI zeigt sie interaktiv an. Die Springdoc-Konfiguration liegt in `config/OpenApiConfig.java`.
- **Fehler im Beispiel `GET /stations/add`:** GET sollte keine Station erstellen. Dafür wäre POST mit Request-Body und typischerweise `201 Created` passend.

Ausführlichere Antworten und Quellen stehen in [Dokumentation.md](Dokumentation.md).

## KI-Unterstützung und Reflexion

Der Projektchat wurde zur Unterstützung bei Content Negotiation, Fehlerbehandlung, Schichtenarchitektur, Tests, OpenAPI, jQuery und Dokumentation verwendet. Die wichtigsten Arbeitsschritte sind in [AI.md](AI.md) zusammengefasst.

Ein Browser kann `/api/v1/stations` je nach eigenem `Accept`-Header als XML anfordern und dann den XML-Baum anzeigen. Für eine gezielte Browseransicht gibt es deshalb `/api/v1/stations/json` und `/api/v1/stations/xml`; sie zeigen den Inhalt inline. Messwerte liegen nur im Arbeitsspeicher. Die offenen UI-Filter und das vereinfachte Datenmodell sind oben ausgewiesen.

# 
