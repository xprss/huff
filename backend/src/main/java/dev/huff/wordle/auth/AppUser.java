package dev.huff.wordle.auth;

public record AppUser(
    String id,
    String email,
    String displayName,
    boolean authenticated
) {
}
