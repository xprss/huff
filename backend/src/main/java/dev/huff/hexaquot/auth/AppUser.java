package dev.huff.hexaquot.auth;

public record AppUser(
    String id,
    String email,
    String displayName,
    String nickname,
    String avatarUrl,
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
        String avatarUrl,
        String bio,
        boolean authenticated
    ) {
        this(id, email, displayName, nickname, avatarUrl, bio, "RIGHT", authenticated, null);
    }

    public AppUser(
        String id,
        String email,
        String displayName,
        String nickname,
        String avatarUrl,
        String bio,
        boolean authenticated,
        AdminPrivileges admin
    ) {
        this(id, email, displayName, nickname, avatarUrl, bio, "RIGHT", authenticated, admin);
    }
}
