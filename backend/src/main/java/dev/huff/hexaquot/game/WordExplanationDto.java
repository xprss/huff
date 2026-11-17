package dev.huff.hexaquot.game;

/**
 * Explanation of today's word, available once the player's game is over.
 * {@code PENDING} means the agent is still writing it and the client should ask
 * again shortly; {@code UNAVAILABLE} means it cannot be produced right now.
 */
public record WordExplanationDto(Status status, String explanation) {
    public enum Status { READY, PENDING, UNAVAILABLE }

    static WordExplanationDto ready(String explanation) {
        return new WordExplanationDto(Status.READY, explanation);
    }

    static WordExplanationDto pending() {
        return new WordExplanationDto(Status.PENDING, null);
    }

    static WordExplanationDto unavailable() {
        return new WordExplanationDto(Status.UNAVAILABLE, null);
    }
}
