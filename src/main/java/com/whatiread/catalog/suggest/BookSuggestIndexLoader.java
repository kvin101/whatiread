package com.whatiread.catalog.suggest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatiread.config.MeilisearchProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class BookSuggestIndexLoader {

    private static final Logger log = LoggerFactory.getLogger(BookSuggestIndexLoader.class);
    private static final int BATCH_SIZE = 1000;

    private final MeilisearchProperties properties;
    private final MeilisearchBookIndexClient indexClient;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public BookSuggestIndexLoader(
            MeilisearchProperties properties,
            MeilisearchBookIndexClient indexClient,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.indexClient = indexClient;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadSeedIfNeeded() {
        if (!properties.enabled() || !properties.loadSeedOnStartup()) {
            return;
        }
        if (indexClient.documentCount() > 0) {
            log.info("Meilisearch index '{}' already has documents; skipping seed load", properties.index());
            return;
        }

        List<Map<String, Object>> seed = readSeed();
        if (seed.isEmpty()) {
            log.warn("Book suggest seed is empty; autocomplete will return no matches until indexed");
            return;
        }

        indexClient.ensureIndex();
        for (int start = 0; start < seed.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, seed.size());
            indexClient.addDocuments(seed.subList(start, end));
        }
        log.info("Loaded {} book suggestions into Meilisearch index '{}'", seed.size(), properties.index());
    }

    private List<Map<String, Object>> readSeed() {
        Resource resource = resourceLoader.getResource(properties.seedResource());
        if (!resource.exists()) {
            log.warn("Book suggest seed resource not found: {}", properties.seedResource());
            return List.of();
        }
        try (InputStream input = resource.getInputStream()) {
            List<Map<String, Object>> raw = objectMapper.readValue(input, new TypeReference<>() {
            });
            List<Map<String, Object>> documents = new ArrayList<>(raw.size());
            for (int i = 0; i < raw.size(); i++) {
                Map<String, Object> source = raw.get(i);
                Map<String, Object> document = new HashMap<>();
                document.put("id", String.valueOf(i + 1));
                document.put("title", stringValue(source.get("title")));
                documents.add(document);
            }
            return documents;
        } catch (IOException ex) {
            log.error("Failed to read book suggest seed from {}", properties.seedResource(), ex);
            return List.of();
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
