package dev.huff.hexaquot.leaderboard;

public record LeaderboardEntryDto(
    int rank,
    String displayName,
    String nickname,
    String profileEmoji,
    int wins
) {
}
