package dev.huff.hexaquot.game;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StatsCalculator {
    private StatsCalculator() {}

    public record CompletedGame(String date, GameStatus status, int attempts) {}

    public static StatsDto calculate(List<CompletedGame> records) {
        int won = 0;
        Map<Integer, Integer> distribution = emptyDistribution();
        int running = 0;
        int maximum = 0;
        LocalDate expected = null;
        for (CompletedGame record : records) {
            LocalDate date = LocalDate.parse(record.date());
            if (record.status() == GameStatus.WON) {
                won++;
                distribution.merge(record.attempts(), 1, Integer::sum);
                running = expected == null || date.equals(expected) ? running + 1 : 1;
            } else {
                running = 0;
            }
            maximum = Math.max(maximum, running);
            expected = date.plusDays(1);
        }
        return new StatsDto(records.size(), won, records.size() - won, running, maximum, distribution);
    }

    public static StatsDto calculateOverall(List<CompletedGame> records) {
        int won = 0;
        Map<Integer, Integer> distribution = emptyDistribution();
        Map<LocalDate, Boolean> wonByDate = new LinkedHashMap<>();
        for (CompletedGame record : records) {
            LocalDate date = LocalDate.parse(record.date());
            wonByDate.putIfAbsent(date, false);
            if (record.status() == GameStatus.WON) {
                won++;
                distribution.merge(record.attempts(), 1, Integer::sum);
                wonByDate.put(date, true);
            }
        }
        int running = 0;
        int maximum = 0;
        LocalDate previous = null;
        for (Map.Entry<LocalDate, Boolean> day : wonByDate.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            if (day.getValue()) {
                running = previous != null && day.getKey().equals(previous.plusDays(1)) ? running + 1 : 1;
            } else {
                running = 0;
            }
            maximum = Math.max(maximum, running);
            previous = day.getKey();
        }
        return new StatsDto(records.size(), won, records.size() - won, running, maximum, distribution);
    }

    private static Map<Integer, Integer> emptyDistribution() {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int attempt = 1; attempt <= 6; attempt++) result.put(attempt, 0);
        return result;
    }
}
