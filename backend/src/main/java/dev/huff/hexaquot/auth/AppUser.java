package dev.huff.hexaquot.auth;

public record AppUser(
    String id,
    String email,
    String displayName,
    String nickname,
    String profileEmoji,
    String bio,
    String inputHandPreference,
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
        this(id, email, displayName, nickname, profileEmoji, bio, "RIGHT", authenticated, null);
    }

    public AppUser(
        String id,
        String email,
        String displayName,
        String nickname,
        String profileEmoji,
        String bio,
        boolean authenticated,
        AdminPrivileges admin
    ) {
        this(id, email, displayName, nickname, profileEmoji, bio, "RIGHT", authenticated, admin);
    }
}
