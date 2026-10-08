package com.neviswealth.searchapi.summary;

/**
 * Provides document summaries with a deterministic, offline algorithm and no external dependencies.
 */
public interface SummaryProvider {

    String summarize(String content);
}
