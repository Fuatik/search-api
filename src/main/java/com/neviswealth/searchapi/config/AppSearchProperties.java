package com.neviswealth.searchapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.search")
public class AppSearchProperties {

    private int pageSizeDefault = 20;

    private Weights weights = new Weights();

    private String synonymsPath = "classpath:synonyms/search-synonyms.yaml";

    @Getter
    @Setter
    public static class Weights {

        private float lexical = 0.5f;

        private float semantic = 0.3f;

        private float trigram = 0.2f;
    }
}
