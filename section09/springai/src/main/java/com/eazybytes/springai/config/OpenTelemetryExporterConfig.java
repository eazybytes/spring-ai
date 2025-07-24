package com.eazybytes.springai.config;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryExporterConfig {

    @Bean
    public OtlpGrpcSpanExporter otlpGrpcSpanExporter(
            @Value("${opentelemetry.exporter.otlp.endpoint}") String url) {
        return OtlpGrpcSpanExporter.builder().setEndpoint(url).build();
    }

}
