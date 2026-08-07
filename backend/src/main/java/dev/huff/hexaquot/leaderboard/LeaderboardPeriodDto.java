package dev.huff.hexaquot.leaderboard;

import java.util.List;

public record LeaderboardPeriodDto(String startDate, String endDate, List<LeaderboardEntryDto> entries) {
}
