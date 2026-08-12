package dev.huff.hexaquot.leaderboard;

public record LeaderboardEntryDto(
    int rank,
    String displayName,
    String nickname,
    String profileEmoji,
    int wins,
    int hexawordWins,
    int hexadigitWins
) {
    public LeaderboardEntryDto(int rank, String displayName, String nickname, String profileEmoji, int wins) {
        this(rank, displayName, nickname, profileEmoji, wins, wins, 0);
    }
}
