import React from "react";
import { ChevronLeft, ChevronRight, Pencil } from "lucide-react";
import type { ProfileUpdateDto, StatsSetDto, UserDto } from "../../types";
import { PuzzleArtwork, type PuzzleKind } from "../game/components/PuzzleArtwork";
import type { StatsGame } from "../stats/StatsView";
import { ProfileEditSheet, type ProfileEditFocus } from "./ProfileEditSheet";
import "./profile.css";

export function ProfileView({
  user,
  stats,
  editing,
  onEditingChange,
  onBack,
  onOpenStats,
  onSave,
  onSuccess,
  onError
}: {
  user: UserDto;
  stats: StatsSetDto;
  editing: boolean;
  onEditingChange: (editing: boolean) => void;
  onBack: () => void;
  onOpenStats: (game: StatsGame) => void;
  onSave: (profile: ProfileUpdateDto) => Promise<UserDto>;
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
  const overall = stats.overall;
  const winRate = overall.played ? Math.round((overall.won / overall.played) * 100) : 0;
  const name = user.displayName || user.nickname;

  return (
    <section className="profile-view my-profile" aria-labelledby="profile-page-title">
      <header className="account-page-heading">
        <button className="my-profile-back" type="button" onClick={onBack} aria-label="Torna ai giochi">
          <ChevronLeft size={20} aria-hidden="true" />
        </button>
        <div><h2 id="profile-page-title">Profilo</h2></div>
      </header>

      <section className="my-profile-card my-profile-identity" aria-label="La tua identità">
        <button className="my-profile-avatar" type="button" onClick={() => openEditor()} aria-label="Cambia emoji del profilo">
          <span aria-hidden="true">{user.profileEmoji}</span>
        </button>
        <div className="my-profile-names">
          <strong>{name}</strong>
          <span>{user.nickname}</span>
        </div>
        <button className="my-profile-edit" type="button" onClick={() => openEditor()} aria-label="Modifica profilo">
          <Pencil size={17} aria-hidden="true" />
        </button>
        {user.bio ? (
          <p className="my-profile-bio">{user.bio}</p>
        ) : (
          <button className="my-profile-add-bio" type="button" onClick={() => openEditor()}>
            + Aggiungi una bio
          </button>
        )}
      </section>

      <section className="my-profile-card my-profile-summary" aria-label="Riepilogo">
        <button className="my-profile-numbers" type="button" onClick={() => onOpenStats("overall")} aria-label="Apri tutte le statistiche">
          <span><strong>{overall.played}</strong><small>Giocate</small></span>
          <span><strong>{winRate}%</strong><small>Vittorie</small></span>
          <span><strong>{overall.maxStreak}</strong><small>Serie record</small></span>
          <ChevronRight className="my-profile-numbers-chevron" size={18} aria-hidden="true" />
        </button>
        <div className="my-profile-medals" aria-label={`Medaglie: ${user.medals.gold} oro, ${user.medals.silver} argento, ${user.medals.bronze} bronzo`}>
          <small>Medaglie</small>
          <span aria-hidden="true">🥇 <strong>{user.medals.gold}</strong></span>
          <span aria-hidden="true">🥈 <strong>{user.medals.silver}</strong></span>
          <span aria-hidden="true">🥉 <strong>{user.medals.bronze}</strong></span>
        </div>
      </section>

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
          onSuccess={onSuccess}
          onError={onError}
        />
      ) : null}
    </section>
  );
}
