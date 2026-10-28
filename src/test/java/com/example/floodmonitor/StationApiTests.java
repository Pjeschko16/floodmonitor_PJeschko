package com.example.floodmonitor;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "simulation.enabled=false")
@AutoConfigureMockMvc
class StationApiTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private StationRepository stationRepository;
    @Autowired private MeasurementRepository measurementRepository;

    @BeforeEach
    void setUpDeterministicData() {
        measurementRepository.deleteAll();
        setWarningLevel("1", "NORMAL");
        setWarningLevel("2", "WARNING");
        setWarningLevel("3", "CRITICAL");
        setWarningLevel("4", "NORMAL");
        setWarningLevel("5", "NORMAL");

        measurementRepository.save("1", measurement("2026-05-01T09:00:00", 2.1));
        measurementRepository.save("1", measurement("2026-05-01T10:30:00", 2.2));
        measurementRepository.save("1", measurement("2026-05-01T12:00:00", 2.3));
    }

    @Test
    void returnsAllStationsAsJson() throws Exception {
        mockMvc.perform(get("/api/v1/stations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void returnsAllStationsAsXml() throws Exception {
        mockMvc.perform(get("/api/v1/stations").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<stationName>Station Vienna</stationName>")));
    }

    @Test
    void displaysStationsAsJsonAtJsonRoute() throws Exception {
        mockMvc.perform(get("/api/v1/stations/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void displaysStationsAsXmlAtXmlRoute() throws Exception {
        mockMvc.perform(get("/api/v1/stations/xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<stationName>Station Vienna</stationName>")));
    }

    @Test
    void returnsExistingStation() throws Exception {
        mockMvc.perform(get("/api/v1/stations/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("1")));
    }

    @Test
    void returnsNotFoundForUnknownStation() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Station ST-999 wurde nicht gefunden")));
    }

    @Test
    void returnsLatestMeasurement() throws Exception {
        mockMvc.perform(get("/api/v1/stations/1/measurements/latest").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp", is("2026-05-01T12:00:00")))
                .andExpect(jsonPath("$.waterLevel", is(2.3)));
    }

    @Test
    void filtersMeasurementsByPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/stations/1/measurements")
                        .param("from", "2026-05-01T10:00:00Z")
                        .param("to", "2026-05-01T11:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements", hasSize(1)))
                .andExpect(jsonPath("$.measurements[0].waterLevel", is(2.2)));
    }

    @Test
    void rejectsInvalidPeriodFormat() throws Exception {
        mockMvc.perform(get("/api/v1/stations/1/measurements").param("from", "yesterday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void rejectsPeriodWhereFromIsAfterTo() throws Exception {
        mockMvc.perform(get("/api/v1/stations/1/measurements")
                        .param("from", "2026-05-01T12:00:00Z")
                        .param("to", "2026-05-01T10:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filtersNormalWarningLevel() throws Exception {
        mockMvc.perform(get("/api/v1/stations").param("warningLevel", "NORMAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void filtersWarningWarningLevel() throws Exception {
        mockMvc.perform(get("/api/v1/stations").param("warningLevel", "WARNING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("2")));
    }

    @Test
    void filtersCriticalWarningLevel() throws Exception {
        mockMvc.perform(get("/api/v1/stations").param("warningLevel", "CRITICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("3")));
    }

    @Test
    void returnsNotAcceptableForUnsupportedFormat() throws Exception {
        mockMvc.perform(get("/api/v1/stations").accept("text/csv"))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void returnsAtMostTheMaximumLimit() throws Exception {
        for (int i = 0; i < 1005; i++) {
            measurementRepository.save("1", measurement("2026-05-01T10:00:00", i));
        }

        mockMvc.perform(get("/api/v1/stations/1/measurements").param("limit", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements", hasSize(1000)));
    }

    @Test
    void publishesOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", is("Floodmonitor API")))
                .andExpect(jsonPath("$.paths['/api/v1/stations'].get.summary", is("Alle Stationen abrufen")))
                .andExpect(jsonPath("$.paths['/api/v1/stations/{stationId}/measurements'].get.parameters", hasSize(5)));
    }

    private void setWarningLevel(String stationId, String warningLevel) {
        Station station = stationRepository.findById(stationId).orElseThrow();
        station.setWarnLevel(warningLevel);
        stationRepository.save(station);
    }

    private Measurement measurement(String timestamp, double waterLevel) {
        return new Measurement(timestamp, waterLevel, 10.0, 0.5, "NORMAL");
    }
}
