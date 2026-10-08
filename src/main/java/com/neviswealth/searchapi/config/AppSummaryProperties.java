package com.neviswealth.searchapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.summary")
public class AppSummaryProperties {

    private String provider = "extractive";

    private Extractive extractive = new Extractive();

    private Ollama ollama = new Ollama();

    @Getter
    @Setter
    public static class Extractive {

        private int minSentences = 2;

        private int maxSentences = 3;
    }

    @Getter
    @Setter
    public static class Ollama {

        private String baseUrl = "http://localhost:11434";

        private String model = "llama3.2:1b";
    }
}
