import React from "react";
import { Check, ChevronLeft, Edit3, X } from "lucide-react";
import { PROFILE_EMOJIS } from "../../app/constants";
import { Distribution } from "../../shared/components/Distribution";
import { Metric } from "../../shared/components/Metric";
import { MedalCounts } from "../../shared/components/MedalCounts";
import type { ProfileUpdateDto, StatsDto, UserDto } from "../../types";

export function ProfileView({
  user,
  stats,
  editing,
  onEditingChange,
  onBack,
  onSave,
  onSuccess,
  onError
}: {
  user: UserDto;
  stats: StatsDto | null;
  editing: boolean;
  onEditingChange: (editing: boolean) => void;
  onBack: () => void;
  onSave: (profile: ProfileUpdateDto) => Promise<UserDto>;
  onSuccess: (message: string) => void;
  onError: (message: string) => void;
}) {
  const [displayName, setDisplayName] = React.useState(user.displayName ?? "");
  const [nicknameHandle, setNicknameHandle] = React.useState(toEditableNickname(user.nickname));
  const [bio, setBio] = React.useState(user.bio ?? "");
  const [showEmojiPicker, setShowEmojiPicker] = React.useState(false);
  const [saving, setSaving] = React.useState(false);
  const winRate = stats && stats.played > 0 ? Math.round((stats.won / stats.played) * 100) : 0;

  React.useEffect(() => {
    setDisplayName(user.displayName ?? "");
    setNicknameHandle(toEditableNickname(user.nickname));
    setBio(user.bio ?? "");
  }, [user.bio, user.displayName, user.nickname]);

  function cancelEdit() {
    setDisplayName(user.displayName ?? "");
    setNicknameHandle(toEditableNickname(user.nickname));
    setBio(user.bio ?? "");
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
        bio
      },
      "Profilo aggiornato."
    );
  }

  return (
    <section className="profile-view" aria-label="Profilo">
      <div className="profile-summary">
        <div className="profile-head">
          <button className="icon-button profile-back" type="button" onClick={onBack} aria-label="Torna al gioco" title="Torna">
            <ChevronLeft size={23} />
          </button>
          <button
            className="profile-emoji"
            type="button"
            onClick={() => setShowEmojiPicker(true)}
            aria-label="Modifica emoji profilo"
            title="Modifica emoji"
          >
            <span className="profile-emoji-glyph">{user.profileEmoji}</span>
            <span className="profile-emoji-edit" aria-hidden="true">
              <Edit3 size={10} />
            </span>
          </button>
          <div className="profile-identity">
            {editing ? (
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
                  />
                </label>
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
            ) : (
              <>
                <strong>{user.displayName}</strong>
                <span>{user.nickname}</span>
                {user.bio ? <p className="profile-bio">{user.bio}</p> : null}
              </>
            )}
          </div>
        </div>
      </div>

      <div className="profile-stats" aria-label="Statistiche personali">
        <MedalCounts medals={user.medals} />
        <div className="stat-grid">
          <Metric label="Giocate" value={stats?.played ?? 0} />
          <Metric label="Vinte" value={stats?.won ?? 0} />
          <Metric label="Perse" value={stats?.lost ?? 0} />
          <Metric label="Vittorie" value={`${winRate}%`} />
        </div>
        <div className="stat-grid compact">
          <Metric label="Serie" value={stats?.currentStreak ?? 0} />
          <Metric label="Record" value={stats?.maxStreak ?? 0} />
        </div>
        <Distribution distribution={stats?.guessDistribution ?? {}} />
      </div>

      {showEmojiPicker ? (
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
                        bio: user.bio
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
      ) : null}
    </section>
  );
}

function toEditableNickname(nickname: string) {
  return nickname.replace(/@/g, "");
}
