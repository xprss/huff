package dev.huff.hexaquot.auth;

public record AdminPrivileges(
    boolean canViewPlayers,
    boolean canViewPlayerDetails,
    boolean canManagePlayers,
    boolean canManageAdmins,
    boolean canManageHexaflowPuzzles
) {
}
