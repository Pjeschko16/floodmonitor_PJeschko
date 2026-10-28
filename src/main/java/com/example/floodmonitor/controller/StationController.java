package com.example.floodmonitor.controller;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.MeasurementHistoryResponse;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.service.SimulationService;
import com.example.floodmonitor.error.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Tag(name = "Floodmonitor", description = "Stationen, Messwerte und Hochwasserwarnungen")
public class StationController {

    private final SimulationService simulationService;

    public StationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    // Kombinierter Endpunkt für /stations (inkl. optionaler Filter)
    @GetMapping("/stations")
    @Operation(summary = "Alle Stationen abrufen", description = "Liefert Stationen, optional gefiltert nach Fluss, Aktivstatus oder Warnstufe.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stationen erfolgreich abgerufen", content = {
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Station.class))),
                    @Content(mediaType = "application/xml", array = @ArraySchema(schema = @Schema(implementation = Station.class))) }),
            @ApiResponse(responseCode = "400", description = "Ungültiger Filterwert", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "406", description = "Angefordertes Antwortformat wird nicht unterstützt", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Interner Fehler", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<Station> getStations(
            @Parameter(description = "Flussname")
            @RequestParam(required = false) String river,
            @Parameter(description = "Aktivstatus der Station")
            @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "Warnstufe: NORMAL, WARNING, CRITICAL oder UNKNOWN")
            @RequestParam(required = false) String warningLevel) {
        return simulationService.getFilteredStations(river, isActive, warningLevel);
    }

    @GetMapping(value = "/stations/json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Stationsliste als JSON anzeigen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stationsdaten als JSON"),
            @ApiResponse(responseCode = "400", description = "Ungültiger Filterwert", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<Station>> downloadStationsJson(
            @RequestParam(required = false) String river,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String warningLevel) {
        return stationFile(simulationService.getFilteredStations(river, isActive, warningLevel),
                MediaType.APPLICATION_JSON);
    }

    @GetMapping(value = "/stations/xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "Stationsliste als XML anzeigen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stationsdaten als XML"),
            @ApiResponse(responseCode = "400", description = "Ungültiger Filterwert", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<Station>> downloadStationsXml(
            @RequestParam(required = false) String river,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String warningLevel) {
        return stationFile(simulationService.getFilteredStations(river, isActive, warningLevel),
                MediaType.APPLICATION_XML);
    }

    private ResponseEntity<List<Station>> stationFile(List<Station> stations, MediaType type) {
        return ResponseEntity.ok()
                .contentType(type)
                .body(stations);
    }

    // Einzelne Station abrufen
    @GetMapping("/stations/{id}")
    @Operation(summary = "Station nach ID abrufen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Station gefunden", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Station.class)),
                    @Content(mediaType = "application/xml", schema = @Schema(implementation = Station.class)) }),
            @ApiResponse(responseCode = "404", description = "Station nicht gefunden", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "406", description = "Angefordertes Antwortformat wird nicht unterstützt", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Station> getStationById(@PathVariable String id) {
        return ResponseEntity.ok(simulationService.getStationById(id));
    }

    // Aktuellsten Messwert abrufen
    @GetMapping("/stations/{stationId}/measurements/latest")
    @Operation(summary = "Aktuellsten Messwert abrufen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messwert gefunden", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class)),
                    @Content(mediaType = "application/xml", schema = @Schema(implementation = Measurement.class)) }),
            @ApiResponse(responseCode = "404", description = "Station oder Messwert nicht gefunden", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "406", description = "Angefordertes Antwortformat wird nicht unterstützt", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Measurement> getLatestMeasurement(@PathVariable String stationId) {
        return ResponseEntity.ok(simulationService.getLatestMeasurement(stationId));
    }

    // Historische Messwerte + Statistiken abrufen
    @GetMapping("/stations/{stationId}/measurements")
    @Operation(summary = "Historische Messwerte und Statistiken abrufen",
            description = "Unterstützt Zeitfilter als ISO-8601 und limit von 1 bis 1000. from muss vor oder gleich to liegen.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messwerte und Statistiken", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = MeasurementHistoryResponse.class)),
                    @Content(mediaType = "application/xml", schema = @Schema(implementation = MeasurementHistoryResponse.class)) }),
            @ApiResponse(responseCode = "400", description = "Ungültiges Datum, Warnstufe oder Limit", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Station nicht gefunden", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "406", description = "Angefordertes Antwortformat wird nicht unterstützt", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Interner Fehler", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<MeasurementHistoryResponse> getMeasurements(
            @Parameter(description = "Stations-ID", required = true)
            @PathVariable String stationId,
            @Parameter(description = "Beginn des Zeitraums (ISO-8601, inklusive)")
            @RequestParam(required = false) String from,
            @Parameter(description = "Ende des Zeitraums (ISO-8601, inklusive)")
            @RequestParam(required = false) String to,
            @Parameter(description = "Maximale Anzahl an Messwerten (1–1000)", schema = @Schema(minimum = "1", maximum = "1000"))
            @RequestParam(required = false, defaultValue = "100") int limit,
            @Parameter(description = "Warnstufe: NORMAL, WARNING, CRITICAL oder UNKNOWN")
            @RequestParam(required = false) String warningLevel) {

        List<Measurement> measurements = simulationService.getFilteredMeasurements(stationId, from, to, limit, warningLevel);
        return ResponseEntity.ok(new MeasurementHistoryResponse(
                measurements, simulationService.calculateStatistics(measurements)));
    }

    // Aktuelle Warnungen abrufen (optional gefiltert)
    @GetMapping("/alerts")
    @Operation(summary = "Aktuelle Hochwasserwarnungen abrufen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Warnungen erfolgreich abgerufen", content = {
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Station.class))),
                    @Content(mediaType = "application/xml", array = @ArraySchema(schema = @Schema(implementation = Station.class))) }),
            @ApiResponse(responseCode = "400", description = "Ungültige Warnstufe", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "406", description = "Angefordertes Antwortformat wird nicht unterstützt", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<Station> getAlerts(
            @Parameter(description = "Warnstufe: WARNING oder CRITICAL")
            @RequestParam(required = false) String warningLevel,
            @Parameter(description = "Flussname")
            @RequestParam(required = false) String river) {
        return simulationService.getAlerts(warningLevel, river);
    }
}
