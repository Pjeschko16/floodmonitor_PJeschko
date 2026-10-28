package com.example.floodmonitor.service;

import com.example.floodmonitor.model.*;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import com.example.floodmonitor.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

@Service
public class SimulationService {

    private static final String[] STATION_NAMES = {
            "Station Vienna", "Station Linz", "Station Salzburg",
            "Station Graz", "Station Innsbruck"
    };

    private final Random random = new Random();
    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final double WARN_VALUE = 3.0;      // Ab 3.0m = WARNING (Beispielwerte anpassen)
    private final double CRITICAL_VALUE = 4.2;  // Ab 4.2m = CRITICAL
    @Value("${simulation.enabled:true}")
    private boolean simulationEnabled;

    public SimulationService(StationRepository stationRepository, MeasurementRepository measurementRepository) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
        List<Station> initialStations = generateInitialStations();
        stationRepository.saveAll(initialStations);
        for (Station station : initialStations) {
            measurementRepository.save(station.getId(), new Measurement(station.getTimestamp(), station.getWaterLevel(),
                    station.getFlowRate(), station.getPrecipitationRate(), station.getWarnLevel()));
        }
    }

    private List<Station> generateInitialStations() {
        List<Station> initialList = new ArrayList<>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        for (int i = 0; i < STATION_NAMES.length; i++) {
            Station station = new Station();
            station.setId(String.valueOf(i + 1));
            station.setStationName(STATION_NAMES[i]);
            station.setTimestamp(timestamp);
            station.setWaterLevel(Math.round((0.5 + random.nextDouble() * 3.5) * 100.0) / 100.0);
            station.setWaterTemperature(Math.round((4.0 + random.nextDouble() * 16.0) * 10.0) / 10.0);
            station.setUnit("m");
            station.setBatteryLevel(100.0);
            station.setFlowRate(station.getWaterLevel() * 1.5);
            station.setStatus("ACTIVE");

            // Initiale Werte für Simulation
            station.setPrecipitationRate(0.0);
            station.setWaterRiseRate(0.0);

            // Erste Warnstufe berechnen
            station.setWarnLevel(calculateWarnLevel(station.getWaterLevel(), 0.0, 0.0, station.getStatus()));

            initialList.add(station);
        }
        return initialList;
    }

    @Scheduled(fixedRateString = "${simulation.interval:10000}")
    public synchronized void simulateNewMeasurements() {
        if (!simulationEnabled) return;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        for (Station station : stationRepository.findAll()) {
            if ("DEACTIVATED".equals(station.getStatus()) || "FAILED".equals(station.getStatus())) {
                station.setWarnLevel("UNKNOWN"); // Keine gültige Messung
                continue;
            }

            // 1. Alten Wasserstand merken, um die Anstiegsgeschwindigkeit zu berechnen
            double oldWaterLevel = station.getWaterLevel();

            // 2. Niederschlag simulieren (z.B. zu 20% regnet es, Wert in mm/h)
            double precipitation = (random.nextDouble() < 0.2) ? random.nextDouble() * 25.0 : 0.0;
            station.setPrecipitationRate(Math.round(precipitation * 10.0) / 10.0);

            // 3. Wasserstand realistisch anpassen
            double waterLevelDelta = (random.nextGaussian() * 0.05) + (precipitation * 0.01);
            double newWaterLevel = Math.max(0.0, oldWaterLevel + waterLevelDelta);
            station.setWaterLevel(Math.round(newWaterLevel * 100.0) / 100.0);

            // 4. Anstiegsgeschwindigkeit berechnen (Differenz zum vorherigen Messwert)
            double riseRate = station.getWaterLevel() - oldWaterLevel;
            station.setWaterRiseRate(Math.round(riseRate * 100.0) / 100.0);

            // 5. Weitere Simulation (Durchfluss, Akku, Status)
            double newFlowRate = Math.max(0.0, station.getWaterLevel() * 1.5 + (random.nextGaussian() * 0.1));
            station.setFlowRate(Math.round(newFlowRate * 100.0) / 100.0);

            double newBattery = Math.max(0.0, station.getBatteryLevel() - (0.1 + random.nextDouble() * 0.4));
            station.setBatteryLevel(Math.round(newBattery * 100.0) / 100.0);

            if (station.getBatteryLevel() < 10.0 && !"MAINTENANCE".equals(station.getStatus())) {
                station.setStatus("MAINTENANCE");
            }

            // 6. SERVERSEITIGE WARNSTUFE BERECHNEN UND SETZEN
            String calculatedLevel = calculateWarnLevel(
                    station.getWaterLevel(),
                    station.getPrecipitationRate(),
                    station.getWaterRiseRate(),
                    station.getStatus()
            );
            station.setWarnLevel(calculatedLevel);

            station.setTimestamp(timestamp);
            stationRepository.save(station);
            measurementRepository.save(station.getId(), new Measurement(timestamp, station.getWaterLevel(),
                    station.getFlowRate(), station.getPrecipitationRate(), station.getWarnLevel()));
        }
        System.out.println("Simulierte Messwerte & Warnstufen aktualisiert: " + timestamp);
    }

    /**
     * Die zentrale Warnstufen-Logik (Serverseitig)
     */
    private String calculateWarnLevel(double waterLevel, double precipitation, double riseRate, String status) {
        // Regel 1: Keine gültige Messung bei inaktivem Status
        if ("DEACTIVATED".equals(status) || "FAILED".equals(status)) {
            return "UNKNOWN";
        }

        // Regel 2: Basisberechnung über den Wasserstand
        String level = "NORMAL";
        if (waterLevel >= CRITICAL_VALUE) {
            level = "CRITICAL";
        } else if (waterLevel >= WARN_VALUE) {
            level = "WARNING";
        }

        // Regel 3: Optionale dynamische Faktoren (Frühwarnung bei NORMAL)
        if ("NORMAL".equals(level)) {
            // Wenn es stark regnet (> 15 mm/h) UND der Pegel schnell steigt (> 0.1m pro Intervall)
            if (precipitation > 15.0 && riseRate > 0.08) {
                level = "WARNING";
            }
        }

        return level;
    }

    public List<Station> generateStations() {
        return stationRepository.findAll();
    }

    public Station getStationById(String id) {
        return stationRepository.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "Station " + id + " wurde nicht gefunden"));
    }
    // 1. Stationen nach Filtern durchsuchen
    public List<Station> getFilteredStations(String river, Boolean isActive, String warningLevel) {
        validateWarningLevel(warningLevel);
        return stationRepository.findAll().stream()
                .filter(s -> river == null || river.equalsIgnoreCase(s.getRiver()))
                .filter(s -> isActive == null || isActive == s.isActiv()) // Entsprechend Ihrem Attribut-Namen
                .filter(s -> warningLevel == null || warningLevel.equalsIgnoreCase(s.getWarnLevel()))
                .toList();
    }

    // 2. Aktuellsten Messwert einer Station abrufen
    public Measurement getLatestMeasurement(String stationId) {
        requireStation(stationId);
        return measurementRepository.findLatestByStationId(stationId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "Für Station " + stationId + " existiert noch kein Messwert"));
    }

    // 3. Historische Messwerte mit Parametern (from, to, limit, warningLevel) filtern
    public List<Measurement> getFilteredMeasurements(String stationId, String from, String to, int limit, String warningLevel) {
        requireStation(stationId);
        validateWarningLevel(warningLevel);
        if (limit < 1 || limit > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "limit muss zwischen 1 und 1000 liegen");
        }

        Instant fromInstant = parseInstant(from);
        Instant toInstant = parseInstant(to);
        if (fromInstant != null && toInstant != null && fromInstant.isAfter(toInstant)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "from muss vor oder gleich to liegen");
        }

        // Hier filtern Sie nach den Zeitstempeln (from/to), Warnlevel und begrenzen das Ergebnis auf 'limit'
        return measurementRepository.findByStationId(stationId).stream()
                .filter(m -> fromInstant == null || !parseInstant(m.getTimestamp()).isBefore(fromInstant))
                .filter(m -> toInstant == null || !parseInstant(m.getTimestamp()).isAfter(toInstant))
                .filter(m -> warningLevel == null || warningLevel.equalsIgnoreCase(m.getWarnLevel()))
                .limit(limit)
                .toList();
    }

    private Instant parseInstant(String value) {
        if (value == null) return null;
        try { return OffsetDateTime.parse(value).toInstant(); }
        catch (java.time.format.DateTimeParseException ignored) {
            try { return LocalDateTime.parse(value).toInstant(java.time.ZoneOffset.UTC); }
            catch (java.time.format.DateTimeParseException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Ungültiges Datumsformat: " + value);
            }
        }
    }

    // 4. Alle Warnungen abrufen (optional gefiltert nach warningLevel und river)
    public List<Station> getAlerts(String warningLevel, String river) {
        validateWarningLevel(warningLevel);
        return stationRepository.findAll().stream()
                .filter(s -> !"NORMAL".equalsIgnoreCase(s.getWarnLevel()) && !"UNKNOWN".equalsIgnoreCase(s.getWarnLevel()))
                .filter(s -> warningLevel == null || warningLevel.equalsIgnoreCase(s.getWarnLevel()))
                .filter(s -> river == null || river.equalsIgnoreCase(s.getRiver()))
                .toList();
    }

    // 5. Statistiken für historische Messwerte berechnen
    public Statistics calculateStatistics(List<Measurement> measurements) {
        if (measurements.isEmpty()) {
            return new Statistics(); // Leere Statistik zurückgeben
        }

        double minWater = measurements.stream().mapToDouble(Measurement::getWaterLevel).min().orElse(0.0);
        double maxWater = measurements.stream().mapToDouble(Measurement::getWaterLevel).max().orElse(0.0);
        double avgWater = measurements.stream().mapToDouble(Measurement::getWaterLevel).average().orElse(0.0);
        double avgFlow = measurements.stream().mapToDouble(Measurement::getFlowRate).average().orElse(0.0);
        double sumPrecipitation = measurements.stream().mapToDouble(Measurement::getPrecipitationRate).sum();

        long alertCount = measurements.stream().filter(m -> "WARNING".equalsIgnoreCase(m.getWarnLevel())).count();
        long criticalCount = measurements.stream().filter(m -> "CRITICAL".equalsIgnoreCase(m.getWarnLevel())).count();

        return new Statistics(minWater, maxWater, avgWater, avgFlow, sumPrecipitation, measurements.size(), alertCount, criticalCount);
    }

    private void requireStation(String stationId) {
        if (stationRepository.findById(stationId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Station " + stationId + " wurde nicht gefunden");
        }
    }

    private void validateWarningLevel(String value) {
        if (value == null) return;
        try { Warnlevel.valueOf(value.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ungültige Warnstufe: " + value);
        }
    }
}
