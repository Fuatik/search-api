package com.neviswealth.searchapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.embedding")
public class AppEmbeddingProperties {

    private String provider = "hashing";

    private Hashing hashing = new Hashing();

    @Getter
    @Setter
    public static class Hashing {

        private int dimensions = 384;
    }
}
