package dev.huff.hexaquot.auth;

public record ResolvedUser(
    AppUser user,
    String setCookieHeader,
    String loginUrl
) {
}
