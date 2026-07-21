package dev.huff.wordle.auth;

public record ResolvedUser(
    AppUser user,
    String setCookieHeader,
    String loginUrl
) {
}
