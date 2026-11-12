package dev.huff.hexaquot.leaderboard;

public record LeaderboardEntryDto(
    int rank,
    String displayName,
    String nickname,
    String avatarUrl,
    int wins,
    Long points
) {
    public LeaderboardEntryDto(int rank, String displayName, String nickname, String avatarUrl, int wins) {
        this(rank, displayName, nickname, avatarUrl, wins, null);
    }
}
