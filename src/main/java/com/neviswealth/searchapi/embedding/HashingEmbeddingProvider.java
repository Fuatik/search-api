package com.neviswealth.searchapi.embedding;

import com.neviswealth.searchapi.config.AppEmbeddingProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "hashing", matchIfMissing = true)
public class HashingEmbeddingProvider implements EmbeddingProvider {

    private final int dimensions;

    public HashingEmbeddingProvider(AppEmbeddingProperties properties) {
        this.dimensions = properties.getHashing().getDimensions();
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dimensions];
        if (text == null || text.isBlank()) {
            return vector;
        }

        String[] tokens = text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        for (String token : tokens) {
            if (token.length() < 2) {
                continue;
            }

            int hash = stableHash(token);
            int index = Math.floorMod(hash, dimensions);
            vector[index] += 1.0f;
        }

        double sumSquares = 0.0d;
        for (float value : vector) {
            sumSquares += value * value;
        }
        float norm = (float) Math.sqrt(sumSquares);

        if (norm == 0.0f) {
            return vector;
        }

        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
        return vector;
    }

    private int stableHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(bytes, 0, 4).getInt();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
