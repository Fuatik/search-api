package com.neviswealth.searchapi.summary;

import com.neviswealth.searchapi.config.AppSummaryProperties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.summary.provider", havingValue = "extractive", matchIfMissing = true)
public class ExtractiveSummaryProvider implements SummaryProvider {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
            "by", "is", "are", "was", "were", "be", "been", "has", "have", "had", "this", "that",
            "these", "those", "it", "its", "as", "from", "we", "you", "they", "he", "she"
    );

    private final AppSummaryProperties appSummaryProperties;

    public ExtractiveSummaryProvider(AppSummaryProperties appSummaryProperties) {
        this.appSummaryProperties = appSummaryProperties;
    }

    @Override
    public String summarize(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        List<String> sentences = splitSentences(content);
        int minSentences = appSummaryProperties.getExtractive().getMinSentences();
        if (sentences.size() <= minSentences) {
            return content.trim();
        }

        List<List<String>> tokenizedSentences = new ArrayList<>(sentences.size());
        Map<String, Integer> globalFrequency = new HashMap<>();
        for (String sentence : sentences) {
            List<String> tokens = tokenize(sentence);
            tokenizedSentences.add(tokens);
            for (String token : tokens) {
                globalFrequency.merge(token, 1, Integer::sum);
            }
        }

        List<SentenceScore> scored = new ArrayList<>(sentences.size());
        for (int i = 0; i < sentences.size(); i++) {
            List<String> tokens = tokenizedSentences.get(i);
            Set<String> uniqueTokens = new HashSet<>(tokens);
            int sum = 0;
            for (String token : uniqueTokens) {
                sum += globalFrequency.getOrDefault(token, 0);
            }
            // score = sum(freq(uniqueToken)) / max(1, tokenCount)
            double score = (double) sum / Math.max(1, tokens.size());
            scored.add(new SentenceScore(i, score, sentences.get(i)));
        }

        int maxSentences = appSummaryProperties.getExtractive().getMaxSentences();
        int limit = Math.max(1, Math.min(maxSentences, sentences.size()));
        List<SentenceScore> top = scored.stream()
                .sorted(Comparator.comparingDouble(SentenceScore::score).reversed())
                .limit(limit)
                .sorted(Comparator.comparingInt(SentenceScore::index))
                .toList();

        String summary = String.join(". ", top.stream().map(SentenceScore::sentence).toList()).trim();
        if (summary.isEmpty()) {
            return "";
        }
        char last = summary.charAt(summary.length() - 1);
        if (last == '.' || last == '!' || last == '?') {
            return summary;
        }
        return summary + ".";
    }

    private static List<String> splitSentences(String content) {
        List<String> sentences = new ArrayList<>();
        for (String sentence : content.split("[.!?]+")) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    private static List<String> tokenize(String sentence) {
        String normalized = sentence.toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();
        for (String token : normalized.split("[^\\p{L}\\p{N}]+")) {
            if (token.length() < 2 || STOP_WORDS.contains(token)) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    private record SentenceScore(int index, double score, String sentence) {
    }
}
