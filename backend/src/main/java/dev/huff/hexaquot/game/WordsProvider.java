package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class WordsProvider {
    public static final int WORD_LENGTH = 6;

    @ConfigProperty(name = "app.words.path")
    String wordsPath;

    @Inject
    ObjectMapper objectMapper;

    private List<String> words;
    private Set<String> wordSet;

    @PostConstruct
    void load() {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(wordsPath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Word list not found: " + wordsPath);
            }
            List<String> rawWords = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            LinkedHashSet<String> normalizedWords = new LinkedHashSet<>();
            for (String word : rawWords) {
                String normalized = normalize(word);
                if (normalized.length() != WORD_LENGTH) {
                    throw new IllegalStateException("Invalid word length in " + wordsPath + ": " + word);
                }
                normalizedWords.add(normalized);
            }
            if (normalizedWords.isEmpty()) {
                throw new IllegalStateException("Word list is empty: " + wordsPath);
            }
            words = List.copyOf(normalizedWords);
            wordSet = Set.copyOf(normalizedWords);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load word list", error);
        }
    }

    public List<String> words() {
        return words;
    }

    public boolean contains(String word) {
        return wordSet.contains(normalize(word));
    }

    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value.trim().toLowerCase(Locale.ITALIAN), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "");
    }
}
