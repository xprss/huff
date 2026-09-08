package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nianna.api.Hyphenator;
import io.github.nianna.api.HyphenatorProperties;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ItalianSyllabifier {
    @ConfigProperty(name = "app.hexastar.patterns.path")
    String patternsPath;

    @ConfigProperty(name = "app.hexastar.overrides.path")
    String overridesPath;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    WordsProvider wordsProvider;

    private Hyphenator hyphenator;
    private Map<String, List<String>> overrides;

    @PostConstruct
    void load() {
        try {
            List<String> patterns;
            try (InputStream input = resource(patternsPath);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                patterns = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.equalsIgnoreCase("UTF-8"))
                    .filter(line -> !line.startsWith("LEFTHYPHENMIN"))
                    .filter(line -> !line.startsWith("RIGHTHYPHENMIN"))
                    .filter(line -> !line.startsWith("%"))
                    .toList();
            }
            hyphenator = new Hyphenator(patterns, new HyphenatorProperties(1, 1));

            Map<String, List<String>> rawOverrides;
            try (InputStream input = resource(overridesPath)) {
                rawOverrides = objectMapper.readValue(input, new TypeReference<>() {});
            }
            Map<String, List<String>> validated = new LinkedHashMap<>();
            rawOverrides.forEach((rawWord, rawSyllables) -> {
                String word = wordsProvider.normalize(rawWord);
                List<String> syllables = rawSyllables == null ? List.of() : rawSyllables.stream()
                    .map(wordsProvider::normalize)
                    .toList();
                if (!wordsProvider.contains(word)) {
                    throw new IllegalStateException("Hexastar override is not in the word list: " + rawWord);
                }
                if (!isEligible(word, syllables)) {
                    throw new IllegalStateException("Invalid Hexastar override for " + rawWord);
                }
                if (validated.put(word, List.copyOf(syllables)) != null) {
                    throw new IllegalStateException("Duplicate Hexastar override: " + rawWord);
                }
            });
            overrides = Map.copyOf(validated);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load Italian syllabification rules", error);
        }
    }

    public List<String> syllabify(String rawWord) {
        String word = wordsProvider.normalize(rawWord);
        List<String> overridden = overrides.get(word);
        if (overridden != null) return overridden;
        if (!word.matches("[a-z]+")) return List.of();
        return List.of(hyphenator.hyphenateToken(word).read("|").split("\\|"));
    }

    public boolean isEligible(String word, List<String> syllables) {
        return word != null
            && word.matches("[a-z]{" + WordsProvider.WORD_LENGTH + "}")
            && syllables != null
            && syllables.size() >= 2
            && syllables.size() <= 4
            && syllables.stream().allMatch(syllable -> syllable != null && syllable.matches("[a-z]{1,4}"))
            && String.join("", syllables).equals(word);
    }

    private InputStream resource(String path) {
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (input == null) throw new IllegalStateException("Resource not found: " + path);
        return input;
    }
}
