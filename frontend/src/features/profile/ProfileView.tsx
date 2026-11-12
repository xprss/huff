import React from "react";
import { Metric } from "../../shared/components/Metric";
import { ArrowRight, ChevronLeft, Edit3, Flame, Hand, Trophy } from "lucide-react";
import { MedalCounts } from "../../shared/components/MedalCounts";
import type { ProfileUpdateDto, StatsSetDto, UserDto } from "../../types";
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

  const games = [
    { id: "hexaword", name: "Hexaword", played: stats.hexaword.played, completed: stats.hexaword.won, label: "vinte" },
    { id: "hexahack", name: "Hexahack", played: null, completed: stats.hexahack.completedAccesses, label: "accessi completati" },
    { id: "hexasky", name: "Hexasky", played: stats.hexasky.played, completed: stats.hexasky.won, label: "vinte" },
    { id: "hexaflow", name: "Hexaflow", played: stats.hexaflow.started, completed: stats.hexaflow.completed, label: "completate" },
    { id: "hexastar", name: "Hexastar", played: stats.hexastar.played, completed: stats.hexastar.won, label: "vinte" },
    { id: "hexaeco", name: "Hexaeco", played: stats.hexaeco.completed, completed: stats.hexaeco.completed, label: "completate" },
    { id: "hexatrap", name: "Hexatrap", played: stats.hexatrap.started, completed: stats.hexatrap.won, label: "vinte" }
  ] as const;
  const winRate = stats.overall.played ? Math.round(stats.overall.won / stats.overall.played * 100) : 0;

  return (
    <section className="profile-view personal-profile" aria-labelledby="profile-page-title">
      <header className="account-page-heading">
        <button className="icon-button" type="button" onClick={onBack} aria-label="Torna ai giochi"><ChevronLeft size={21} /></button>
        <div><p className="eyebrow">Il tuo spazio</p><h2 id="profile-page-title">Profilo</h2></div>
      </header>
      <section className="profile-hero" aria-label="La tua identità">
        <div className="profile-cover" aria-hidden="true"><span className="profile-cover-mesh" /></div>
        <div className="profile-hero-body">
          <button className="profile-emoji" type="button" onClick={() => openEditor()} aria-label="Modifica emoji profilo">
            <span className="profile-emoji-glyph">{user.profileEmoji}</span><span className="profile-emoji-edit"><Edit3 size={13} aria-hidden="true" /></span>
          </button>
          <div className="profile-identity"><strong>{user.displayName || user.nickname}</strong><span>{user.nickname}</span></div>
          <button className="profile-action-button" type="button" onClick={() => openEditor()}><Edit3 size={16} aria-hidden="true" /> Modifica profilo</button>
          <p className={`profile-bio${user.bio ? "" : " empty-bio"}`}>{user.bio || "Ogni giocatore ha una storia. Aggiungi una bio per raccontare la tua."}</p>
        </div>
      </section>
      <div className="profile-sections">
        <section className="account-panel profile-overview" aria-labelledby="profile-progress-title">
          <div className="account-panel-heading"><h3 id="profile-progress-title"><Flame size={19} aria-hidden="true" /> Il tuo percorso</h3></div>
          <div className="profile-metrics"><Metric label="Partite giocate" value={stats.overall.played} /><Metric label="Vittorie" value={`${winRate}%`} /><Metric label="Serie attuale" value={stats.overall.currentStreak} /><Metric label="Serie record" value={stats.overall.maxStreak} /></div>
          <button className="profile-text-link" type="button" onClick={() => onOpenStats("overall")}>Esplora tutte le statistiche <ArrowRight size={17} aria-hidden="true" /></button>
        </section>
        <section className="account-panel profile-medals" aria-label="I tuoi traguardi">
          <h3><Trophy size={19} aria-hidden="true" /> I tuoi traguardi</h3>
          <p>Le medaglie conquistate nella classifica generale.</p>
          <MedalCounts medals={user.medals} />
          {user.medals.gold + user.medals.silver + user.medals.bronze === 0 ? <p className="account-note">La prima medaglia ti aspetta. Continua a metterti in gioco!</p> : null}
        </section>
        <section className="account-panel profile-games" aria-labelledby="profile-games-title">
          <h3 id="profile-games-title">I tuoi giochi, un passo alla volta</h3>
          <div className="profile-game-list">{games.map((game) => <button className={`profile-game-row profile-game-row--${game.id}`} key={game.id} type="button" onClick={() => onOpenStats(game.id)}>
            <span className="profile-game-mark" aria-hidden="true">{game.name.slice(4, 5).toUpperCase()}</span><span><strong>{game.name}</strong><small>{game.completed} {game.label}{game.played !== null ? ` · ${game.played} giocate` : ""}</small></span><ArrowRight size={17} aria-hidden="true" />
          </button>)}</div>
        </section>
        <section className="account-panel profile-preferences" aria-label="Preferenze di gioco">
          <h3><Hand size={19} aria-hidden="true" /> A modo tuo</h3>
          <p>Il joystick di navigazione è a {user.inputHandPreference === "LEFT" ? "sinistra" : "destra"}, per seguire la tua mano.</p>
          <button className="profile-text-link" type="button" onClick={() => openEditor("hand")}>Modifica preferenze <ArrowRight size={17} aria-hidden="true" /></button>
          {user.email ? <div className="profile-account"><span>Account collegato</span><strong>{user.email}</strong></div> : null}
        </section>
      </div>
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
