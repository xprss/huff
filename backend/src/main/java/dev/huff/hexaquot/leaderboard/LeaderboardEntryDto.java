package dev.huff.hexaquot.leaderboard;

public record LeaderboardEntryDto(
    int rank,
    String displayName,
    String nickname,
    String profileEmoji,
    int wins,
    Long points
) {
    public LeaderboardEntryDto(int rank, String displayName, String nickname, String profileEmoji, int wins) {
        this(rank, displayName, nickname, profileEmoji, wins, null);
    }
}
