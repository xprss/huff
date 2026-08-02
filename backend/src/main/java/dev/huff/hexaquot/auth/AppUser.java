package dev.huff.hexaquot.auth;

public record AppUser(
    String id,
    String email,
    String displayName,
    String nickname,
    String profileEmoji,
    boolean authenticated,
    AdminPrivileges admin
) {
    public AppUser(
        String id,
        String email,
        String displayName,
        String nickname,
        String profileEmoji,
        boolean authenticated
    ) {
        this(id, email, displayName, nickname, profileEmoji, authenticated, null);
    }
}
