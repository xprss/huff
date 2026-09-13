import React from "react";
import { createPortal } from "react-dom";
import { Metric } from "../../shared/components/Metric";
import { ArrowRight, Check, ChevronLeft, Edit3, Flame, Hand, Sparkles, Trophy, X } from "lucide-react";
import { PROFILE_EMOJIS } from "../../app/constants";
import { MedalCounts } from "../../shared/components/MedalCounts";
import type { InputHandPreference, ProfileUpdateDto, StatsSetDto, UserDto } from "../../types";
import type { StatsGame } from "../stats/StatsView";

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
  const [displayName, setDisplayName] = React.useState(user.displayName ?? "");
  const [nicknameHandle, setNicknameHandle] = React.useState(toEditableNickname(user.nickname));
  const [bio, setBio] = React.useState(user.bio ?? "");
  const [inputHandPreference, setInputHandPreference] = React.useState<InputHandPreference>(user.inputHandPreference);
  const [showEmojiPicker, setShowEmojiPicker] = React.useState(false);
  const [saving, setSaving] = React.useState(false);
  const editorRef = React.useRef<HTMLElement | null>(null);

  React.useEffect(() => {
    if (!editing) return;
    editorRef.current?.scrollIntoView({ block: "start" });
    editorRef.current?.querySelector("input")?.focus({ preventScroll: true });
  }, [editing]);


  React.useEffect(() => {
    setDisplayName(user.displayName ?? "");
    setNicknameHandle(toEditableNickname(user.nickname));
    setBio(user.bio ?? "");
    setInputHandPreference(user.inputHandPreference);
  }, [user.bio, user.displayName, user.inputHandPreference, user.nickname]);

  function cancelEdit() {
    setDisplayName(user.displayName ?? "");
    setNicknameHandle(toEditableNickname(user.nickname));
    setBio(user.bio ?? "");
    setInputHandPreference(user.inputHandPreference);
    onEditingChange(false);
  }

  async function saveProfile(profile: ProfileUpdateDto, successMessage: string) {
    try {
      setSaving(true);
      const updated = await onSave(profile);
      setDisplayName(updated.displayName ?? "");
      setNicknameHandle(toEditableNickname(updated.nickname));
      setBio(updated.bio ?? "");
      onEditingChange(false);
      setShowEmojiPicker(false);
      onSuccess(successMessage);
    } catch (error) {
      onError(error instanceof Error ? error.message : "Impossibile aggiornare il profilo.");
    } finally {
      setSaving(false);
    }
  }

  function submitProfile(event: React.FormEvent) {
    event.preventDefault();
    void saveProfile(
      {
        displayName,
        nickname: `@${nicknameHandle}`,
        profileEmoji: user.profileEmoji,
        bio,
        inputHandPreference
      },
      "Profilo aggiornato."
    );
  }

  const games = [
    { id: "hexaword", name: "Hexaword", played: stats.hexaword.played, completed: stats.hexaword.won, label: "vinte" },
    { id: "hexahack", name: "Hexahack", played: null, completed: stats.hexahack.completedAccesses, label: "accessi completati" },
    { id: "hexasky", name: "Hexasky", played: stats.hexasky.played, completed: stats.hexasky.won, label: "vinte" },
    { id: "hexaflow", name: "Hexaflow", played: stats.hexaflow.started, completed: stats.hexaflow.completed, label: "completate" },
    { id: "hexastar", name: "Hexastar", played: stats.hexastar.played, completed: stats.hexastar.won, label: "vinte" }
  ] as const;
  const winRate = stats.overall.played ? Math.round(stats.overall.won / stats.overall.played * 100) : 0;

  return (
    <section className="profile-view personal-profile" aria-labelledby="profile-page-title">
      <header className="account-page-heading">
        <button className="icon-button" type="button" onClick={onBack} aria-label="Torna ai giochi"><ChevronLeft size={21} /></button>
        <div><p className="eyebrow">Il tuo spazio</p><h2 id="profile-page-title">Profilo</h2></div>
      </header>
      <section className="profile-hero" aria-label="La tua identità">
        <div className="profile-cover" aria-hidden="true"><Sparkles size={50} /><span>Ogni giorno, una nuova intuizione.</span></div>
        <div className="profile-hero-body">
          <button className="profile-emoji" type="button" onClick={() => setShowEmojiPicker(true)} disabled={saving || editing} aria-label="Modifica emoji profilo">
            <span className="profile-emoji-glyph">{user.profileEmoji}</span><span className="profile-emoji-edit"><Edit3 size={13} aria-hidden="true" /></span>
          </button>
          <div className="profile-identity"><strong>{user.displayName || user.nickname}</strong><span>{user.nickname}</span></div>
          {!editing ? <button className="profile-action-button" type="button" onClick={() => onEditingChange(true)}><Edit3 size={16} aria-hidden="true" /> Modifica profilo</button> : null}
          <p className={`profile-bio${user.bio ? "" : " empty-bio"}`}>{user.bio || "Ogni giocatore ha una storia. Aggiungi una bio per raccontare la tua."}</p>
        </div>
      </section>
      {editing ? <section ref={editorRef} className="account-panel profile-editor" aria-label="Modifica profilo"><h3>Un profilo che ti somiglia</h3><p>Personalizza la tua identità e scegli come giocare.</p>
              <form className="profile-form" onSubmit={submitProfile}>
                <label>
                  <span>Nome</span>
                  <input
                    value={displayName}
                    onChange={(event) => setDisplayName(event.target.value)}
                    maxLength={80}
                    disabled={saving}
                    autoComplete="name"
                  />
                </label>
                <label>
                  <span>Nickname</span>
                  <div className="profile-nickname-field">
                    <span className="profile-nickname-prefix" aria-hidden="true">
                      @
                    </span>
                    <input
                      value={nicknameHandle}
                      onChange={(event) => setNicknameHandle(toEditableNickname(event.target.value))}
                      maxLength={29}
                      disabled={saving}
                      autoComplete="off"
                      spellCheck={false}
                    />
                  </div>
                </label>
                <label>
                  <span>Bio</span>
                  <textarea
                    value={bio}
                    onChange={(event) => setBio(event.target.value)}
                    maxLength={200}
                    disabled={saving}
                    rows={3}
                    aria-describedby="bio-length"
                  />
                  <small id="bio-length">{bio.length}/200 caratteri</small>
                </label>
                <fieldset className="profile-hand-preference" disabled={saving}>
                  <legend>Joystick di navigazione</legend>
                  <span>Mano di utilizzo</span>
                  <div role="radiogroup" aria-label="Mano per il joystick">
                    {(["LEFT", "RIGHT"] as const).map((hand) => (
                      <button
                        className={inputHandPreference === hand ? "selected" : ""}
                        type="button"
                        role="radio"
                        aria-checked={inputHandPreference === hand}
                        key={hand}
                        onClick={() => setInputHandPreference(hand)}
                      >
                        {hand === "LEFT" ? "Sinistra" : "Destra"}
                      </button>
                    ))}
                  </div>
                </fieldset>
                <div className="profile-form-actions">
                  <button className="profile-action-button primary" type="submit" disabled={saving}>
                    <Check size={17} />
                    <span>Salva</span>
                  </button>
                  <button className="profile-action-button" type="button" onClick={cancelEdit} disabled={saving}>
                    <X size={17} />
                    <span>Annulla</span>
                  </button>
                </div>
              </form>
      </section> : null}
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
          <h3 id="profile-games-title">Cinque modi di metterti alla prova</h3>
          <div className="profile-game-list">{games.map((game) => <button className={`profile-game-row profile-game-row--${game.id}`} key={game.id} type="button" onClick={() => onOpenStats(game.id)}>
            <span className="profile-game-mark" aria-hidden="true">{game.name.slice(4, 5).toUpperCase()}</span><span><strong>{game.name}</strong><small>{game.completed} {game.label}{game.played !== null ? ` · ${game.played} giocate` : ""}</small></span><ArrowRight size={17} aria-hidden="true" />
          </button>)}</div>
        </section>
        <section className="account-panel profile-preferences" aria-label="Preferenze di gioco">
          <h3><Hand size={19} aria-hidden="true" /> A modo tuo</h3>
          <p>Il joystick di navigazione è a {user.inputHandPreference === "LEFT" ? "sinistra" : "destra"}, per seguire la tua mano.</p>
          <button className="profile-text-link" type="button" onClick={() => onEditingChange(true)}>Modifica preferenze <ArrowRight size={17} aria-hidden="true" /></button>
          {user.email ? <div className="profile-account"><span>Account collegato</span><strong>{user.email}</strong></div> : null}
        </section>
      </div>
      {showEmojiPicker ? createPortal(
        <div className="modal-backdrop" role="presentation" onMouseDown={() => setShowEmojiPicker(false)}>
          <section
            className="modal emoji-modal"
            role="dialog"
            aria-modal="true"
            aria-label="Scegli emoji profilo"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <header className="modal-head">
              <h2>Emoji</h2>
              <button className="close-button" type="button" onClick={() => setShowEmojiPicker(false)} aria-label="Chiudi">
                <X size={19} />
              </button>
            </header>
            <div className="emoji-grid">
              {PROFILE_EMOJIS.map((emoji) => (
                <button
                  className={`emoji-choice ${emoji === user.profileEmoji ? "selected" : ""}`}
                  type="button"
                  key={emoji}
                  onClick={() =>
                    void saveProfile(
                      {
                        displayName: user.displayName ?? "",
                        nickname: user.nickname,
                        profileEmoji: emoji,
                        bio: user.bio,
                        inputHandPreference: user.inputHandPreference
                      },
                      "Emoji aggiornata."
                    )
                  }
                  disabled={saving}
                  aria-pressed={emoji === user.profileEmoji}
                >
                  {emoji}
                </button>
              ))}
            </div>
          </section>
        </div>
      , document.body) : null}
    </section>
  );
}
function toEditableNickname(nickname: string) {
  return nickname.replace(/@/g, "");
}
