import React from "react";
import { ChevronRight } from "lucide-react";
import type { ProfileUpdateDto, StatsSetDto, UserDto } from "../../types";
import { PuzzleArtwork, type PuzzleKind } from "../game/components/PuzzleArtwork";
import type { StatsGame } from "../stats/StatsView";
import { ProfileEditSheet, type ProfileEditFocus } from "./ProfileEditSheet";
import { ProfileIdentityCard, ProfileSummaryCard } from "./ProfileParts";
import "./profile.css";

export function ProfileView({
  user,
  stats,
  editing,
  onEditingChange,
  onOpenStats,
  onSave,
  onSaveAvatar,
  onSuccess,
  onError
}: {
  user: UserDto;
  stats: StatsSetDto;
  editing: boolean;
  onEditingChange: (editing: boolean) => void;
  onOpenStats: (game: StatsGame) => void;
  onSave: (profile: ProfileUpdateDto) => Promise<UserDto>;
  onSaveAvatar: (photo: Blob | null) => Promise<UserDto>;
  onSuccess: (message: string) => void;
  onError: (message: string) => void;
}) {
  const [editFocus, setEditFocus] = React.useState<ProfileEditFocus>("name");
  const openEditor = (focus: ProfileEditFocus = "name") => {
    setEditFocus(focus);
    onEditingChange(true);
  };

  const games: { id: Exclude<StatsGame, "overall">; kind: PuzzleKind; name: string; value: number; label: string }[] = [
    { id: "hexaword", kind: "word", name: "Hexaword", value: stats.hexaword.won, label: "vinte" },
    { id: "hexahack", kind: "hack", name: "Hexahack", value: stats.hexahack.completedAccesses, label: "accessi" },
    { id: "hexasky", kind: "sky", name: "Hexasky", value: stats.hexasky.won, label: "vinte" },
    { id: "hexaflow", kind: "flow", name: "Hexaflow", value: stats.hexaflow.completed, label: "completate" },
    { id: "hexastar", kind: "star", name: "Hexastar", value: stats.hexastar.won, label: "vinte" },
    { id: "hexaeco", kind: "eco", name: "Hexaeco", value: stats.hexaeco.completed, label: "completate" },
    { id: "hexatrap", kind: "trap", name: "Hexatrap", value: stats.hexatrap.won, label: "vinte" }
  ];

  return (
    <section className="profile-view my-profile" aria-label="Il tuo profilo">
      <ProfileIdentityCard
        avatarUrl={user.avatarUrl}
        name={user.displayName || user.nickname}
        nickname={user.nickname}
        bio={user.bio}
        onEdit={() => openEditor()}
      />
      <ProfileSummaryCard stats={stats.overall} medals={user.medals} onOpenStats={() => onOpenStats("overall")} />

      <section className="my-profile-section" aria-labelledby="profile-games-title">
        <h3 id="profile-games-title">I tuoi giochi</h3>
        <div className="my-profile-games">
          {games.map((game) => (
            <button
              key={game.id}
              className={`my-profile-game my-profile-game--${game.kind}`}
              type="button"
              onClick={() => onOpenStats(game.id)}
              aria-label={`${game.name}: ${game.value} ${game.label}. Apri statistiche`}
            >
              <PuzzleArtwork kind={game.kind} />
              <strong>{game.name}</strong>
              <span><b>{game.value}</b> {game.label}</span>
            </button>
          ))}
        </div>
      </section>

      <section className="my-profile-section" aria-labelledby="profile-settings-title">
        <h3 id="profile-settings-title">Impostazioni</h3>
        <div className="my-profile-card my-profile-settings">
          <button type="button" onClick={() => openEditor("hand")}>
            <span>Joystick</span>
            <span className="my-profile-setting-value">{user.inputHandPreference === "LEFT" ? "Sinistra" : "Destra"}</span>
            <ChevronRight size={17} aria-hidden="true" />
          </button>
          {user.email ? (
            <div>
              <span>Account</span>
              <span className="my-profile-setting-value">{user.email}</span>
            </div>
          ) : null}
        </div>
      </section>

      {editing ? (
        <ProfileEditSheet
          user={user}
          initialFocus={editFocus}
          onClose={() => onEditingChange(false)}
          onSave={onSave}
          onSaveAvatar={onSaveAvatar}
          onSuccess={onSuccess}
          onError={onError}
        />
      ) : null}
    </section>
  );
}
