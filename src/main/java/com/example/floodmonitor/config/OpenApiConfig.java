package com.example.floodmonitor.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Floodmonitor API",
        version = "v1",
        description = "REST API für Hochwasserstationen und Messwerte. "
                + "Authentifizierung ist derzeit nicht erforderlich. "
                + "GET-Anfragen benötigen keinen Request-Body. Antworten werden abhängig vom Accept-Header "
                + "als application/json oder application/xml geliefert; ohne Accept wird JSON verwendet. "
                + "Fehlerantworten enthalten timestamp, status, error, message und path."
))
public class OpenApiConfig {
}
