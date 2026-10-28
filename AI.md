# Floodmonitor – Zusammenfassung des Chats

## Projektziel

Spring-Boot-REST-Anwendung für ein Hochwasser-Frühwarnsystem. Die API stellt Stationen, Messwerte, Warnungen und Statistiken bereit. Antworten werden über den `Accept`-Header als JSON oder XML ausgegeben.

## Architektur und Implementierung

- **Model:** Datenklassen und Warnstufen-Enum unter `src/main/java/com/example/floodmonitor/model/`.
- **Repository:** `StationRepository` und `MeasurementRepository` halten Stations- und Messdaten im Arbeitsspeicher.
- **Service:** `SimulationService` enthält Simulation, Filterung, Zeitraumprüfung, Warnstufenvalidierung und Statistikberechnung.
- **Controller:** `StationController` stellt die REST-Endpunkte unter `/api/v1` bereit und delegiert die Verarbeitung an den Service.
- **Fehlerbehandlung:** `GlobalExceptionHandler`, `ApiException` und `ApiError` liefern einheitliche Fehlerantworten ohne Stacktrace.
- **Client:** `src/main/resources/static/index.html` zeigt Stationsdaten tabellarisch an. Der Client verwendet HTML, CSS, JavaScript und jQuery, fragt JSON oder XML ab und aktualisiert die Anzeige alle zehn Sekunden.
- **OpenAPI:** Springdoc stellt Swagger UI und `/v3/api-docs` bereit. Die Dokumentation beschreibt Endpunkte, Parameter, Antwortformate, Statuscodes und Fehler. Sie hält fest, dass aktuell keine Authentifizierung erforderlich ist.

## Anzeige von JSON und XML im Browser

Die Stationsliste lässt sich direkt in einer menschenlesbaren Browseransicht aufrufen:

- `/api/v1/stations/json` liefert die Stationsdaten als JSON.
- `/api/v1/stations/xml` liefert die Stationsdaten als XML.

Beide Routen zeigen den Response-Body inline an und setzen keinen `Content-Disposition: attachment`-Header. Der ursprüngliche `/api/v1/stations`-Endpunkt unterstützt weiterhin Content Negotiation über den `Accept`-Header. Der Dashboard-Formatumschalter fordert JSON oder XML per jQuery an und stellt die empfangenen Stationen in der Tabelle dar.

## API und Validierung

- Stationsliste und Warnungen können nach vorhandenen Parametern gefiltert werden.
- Stationsdetails und Messwerte einer unbekannten Stations-ID liefern 404.
- Historische Messwerte unterstützen `from`, `to`, `limit` und `warningLevel`.
- Zeitstempel werden als ISO-8601 geprüft; `from` muss vor oder gleich `to` liegen.
- `limit` ist auf 1 bis 1000 begrenzt.
- Gültige Warnstufen sind `NORMAL`, `WARNING`, `CRITICAL` und `UNKNOWN`.
- JSON ist das Standardformat; JSON und XML werden für API-Antworten unterstützt. Nicht unterstützte `Accept`-Formate liefern 406.
- Messwerte werden beim Start und während der Simulation gespeichert.

## Automatisierte Tests

Die Tests liegen in `src/test/java/com/example/floodmonitor/StationApiTests.java` und verwenden Spring MockMvc. Die Testkonfiguration setzt `simulation.enabled=false`; `@BeforeEach` setzt Stationswarnstufen und Messverläufe kontrolliert zurück.

Die Tests prüfen die geforderten API-Fälle, den zusätzlichen Fall `from` nach `to`, `/v3/api-docs` sowie die JSON- und XML-Anzeige-Routen. Die Simulation wird in Tests mit `simulation.enabled=false` deaktiviert. Ausführung im Projektstamm:

```powershell
.\gradlew.bat test
```

Der letzte dokumentierte Lauf endete mit `BUILD SUCCESSFUL`.

## Technische Mindestanforderungen

Java/Spring Boot, Gradle, REST mit JSON/XML, HTML/CSS/JavaScript/jQuery und automatisierte Tests sind berücksichtigt. Git wurde auf Wunsch aus den Aufgabenanforderungen in `TASK.md` entfernt.

## Bekannte Einschränkung

Das `Measurement`-Modell enthält derzeit Zeitstempel, Wasserstand, Durchfluss, Niederschlag und Warnstufe. Felder wie `stationId`, `temperature`, `batteryLevel` und `status` aus dem Beispielpayload gehören derzeit nicht zum Messwertmodell; diese Stationsinformationen stehen im `Station`-Modell.

## Nützliche Adressen

- Dashboard: `http://localhost:8080/`
- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`
