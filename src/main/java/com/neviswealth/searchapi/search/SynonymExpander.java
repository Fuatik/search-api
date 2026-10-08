package com.neviswealth.searchapi.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.neviswealth.searchapi.config.AppSearchProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Getter
@Component
public class SynonymExpander {

    private static final TypeReference<LinkedHashMap<String, List<String>>> SYNONYM_MAP_TYPE = new TypeReference<>() {
    };

    private final Map<String, List<String>> synonyms;

    @Autowired
    public SynonymExpander(AppSearchProperties properties, ResourceLoader resourceLoader) {
        this.synonyms = loadSynonyms(properties.getSynonymsPath(), resourceLoader);
    }

    SynonymExpander(Map<String, List<String>> synonyms) {
        this.synonyms = copySynonyms(synonyms);
    }

    public List<String> expand(String normalizedQuery) {
        List<String> values = synonyms.get(normalizedQuery);
        if (values == null) {
            return List.of(normalizedQuery);
        }

        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        expanded.add(normalizedQuery);
        expanded.addAll(values);
        return List.copyOf(expanded);
    }

    private static Map<String, List<String>> loadSynonyms(String path, ResourceLoader resourceLoader) {
        Resource resource = resourceLoader.getResource(path);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, List<String>> loaded = mapper.readValue(inputStream, SYNONYM_MAP_TYPE);
            if (loaded == null) {
                return Map.of();
            }
            return copySynonyms(loaded);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load synonyms from path: " + path, exception);
        }
    }

    private static Map<String, List<String>> copySynonyms(Map<String, List<String>> source) {
        LinkedHashMap<String, List<String>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, value == null ? List.of() : List.copyOf(value)));
        return Collections.unmodifiableMap(copy);
    }
}