package dev.huff.hexaquot.auth;

public record AppUser(
    String id,
    String email,
    String displayName,
    String nickname,
    String profileEmoji,
    String bio,
    boolean authenticated,
    AdminPrivileges admin
) {
    public AppUser(
        String id,
        String email,
        String displayName,
        String nickname,
        String profileEmoji,
        String bio,
        boolean authenticated
    ) {
        this(id, email, displayName, nickname, profileEmoji, bio, authenticated, null);
    }
}
