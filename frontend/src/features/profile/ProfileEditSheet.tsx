import React from "react";
import { createPortal } from "react-dom";
import { PROFILE_EMOJIS } from "../../app/constants";
import type { InputHandPreference, ProfileUpdateDto, UserDto } from "../../types";

const NAME_MAX = 80;
const NICKNAME_MAX = 29;
const BIO_MAX = 200;

export type ProfileEditFocus = "name" | "hand";

type Draft = {
  displayName: string;
  nickname: string;
  profileEmoji: string;
  bio: string;
  inputHandPreference: InputHandPreference;
};

function draftFrom(user: UserDto): Draft {
  return {
    displayName: user.displayName ?? "",
    nickname: toEditableNickname(user.nickname),
    profileEmoji: user.profileEmoji,
    bio: user.bio ?? "",
    inputHandPreference: user.inputHandPreference
  };
}

export function toEditableNickname(nickname: string) {
  return nickname.replace(/@/g, "");
}

/**
 * Full-height edit sheet for the personal profile. Every field, the emoji
 * included, is saved in a single request; closing with unsaved changes asks
 * for confirmation first.
 */
export function ProfileEditSheet({
  user,
  initialFocus = "name",
  onClose,
  onSave,
  onSuccess,
  onError
}: {
  user: UserDto;
  initialFocus?: ProfileEditFocus;
  onClose: () => void;
  onSave: (profile: ProfileUpdateDto) => Promise<UserDto>;
  onSuccess: (message: string) => void;
  onError: (message: string) => void;
}) {
  const initial = React.useMemo(() => draftFrom(user), [user]);
  const [draft, setDraft] = React.useState<Draft>(initial);
  const [saving, setSaving] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [confirmDiscard, setConfirmDiscard] = React.useState(false);
  const sheetRef = React.useRef<HTMLElement | null>(null);
  const nameRef = React.useRef<HTMLInputElement | null>(null);
  const handRef = React.useRef<HTMLDivElement | null>(null);

  const dirty = (Object.keys(initial) as (keyof Draft)[]).some((key) => draft[key] !== initial[key]);
  const nameError = draft.displayName.trim() ? null : "Inserisci un nome.";
  const nicknameError = draft.nickname.trim() ? null : "Inserisci un nickname.";
  const canSave = dirty && !nameError && !nicknameError && !saving;

  const update = <K extends keyof Draft>(key: K, value: Draft[K]) => {
    setDraft((current) => ({ ...current, [key]: value }));
    setConfirmDiscard(false);
    setError(null);
  };

  const requestClose = React.useCallback(() => {
    if (saving) return;
    if (dirty && !confirmDiscard) {
      setConfirmDiscard(true);
      return;
    }
    onClose();
  }, [confirmDiscard, dirty, onClose, saving]);

  // Lock page scroll, focus the requested field and restore focus on close.
  React.useEffect(() => {
    const previouslyFocused = document.activeElement as HTMLElement | null;
    const { overflow } = document.body.style;
    document.body.style.overflow = "hidden";
    const target = initialFocus === "hand"
      ? handRef.current?.querySelector<HTMLButtonElement>("[aria-checked='true']")
      : nameRef.current;
    target?.focus({ preventScroll: true });
    if (initialFocus === "hand") handRef.current?.scrollIntoView({ block: "center" });
    return () => {
      document.body.style.overflow = overflow;
      previouslyFocused?.focus?.({ preventScroll: true });
    };
  }, [initialFocus]);

  // Escape closes; Tab stays inside the sheet.
  React.useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        requestClose();
        return;
      }
      if (event.key !== "Tab" || !sheetRef.current) return;
      const focusable = [...sheetRef.current.querySelectorAll<HTMLElement>(
        "button:not(:disabled), input:not(:disabled), textarea:not(:disabled)"
      )];
      if (!focusable.length) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [requestClose]);

  async function submit(event?: React.FormEvent) {
    event?.preventDefault();
    if (!canSave) return;
    try {
      setSaving(true);
      setError(null);
      await onSave({
        displayName: draft.displayName.trim(),
        nickname: `@${draft.nickname}`,
        profileEmoji: draft.profileEmoji,
        bio: draft.bio.trim(),
        inputHandPreference: draft.inputHandPreference
      });
      onSuccess("Profilo aggiornato.");
      onClose();
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : "Impossibile aggiornare il profilo.";
      setError(message);
      onError(message);
    } finally {
      setSaving(false);
    }
  }

  return createPortal(
    <div className="profile-sheet-backdrop" role="presentation">
      <section
        ref={sheetRef}
        className="profile-sheet"
        role="dialog"
        aria-modal="true"
        aria-labelledby="profile-sheet-title"
      >
        <header className="profile-sheet-bar">
          <button className="profile-sheet-text-button" type="button" onClick={requestClose} disabled={saving}>
            Annulla
          </button>
          <h2 id="profile-sheet-title">Modifica profilo</h2>
          <button
            className="profile-sheet-text-button profile-sheet-save"
            type="submit"
            form="profile-sheet-form"
            disabled={!canSave}
            aria-busy={saving}
          >
            {saving ? <span className="profile-sheet-spinner" aria-hidden="true" /> : null}
            {saving ? "Salvo…" : "Salva"}
          </button>
        </header>

        {confirmDiscard ? (
          <div className="profile-sheet-confirm" role="alert">
            <span>Scartare le modifiche?</span>
            <button type="button" onClick={() => setConfirmDiscard(false)}>Continua</button>
            <button className="danger" type="button" onClick={onClose}>Scarta</button>
          </div>
        ) : null}

        <form id="profile-sheet-form" className="profile-sheet-body" onSubmit={submit} noValidate>
          <fieldset className="profile-sheet-avatar" disabled={saving}>
            <legend className="sr-only">Emoji del profilo</legend>
            <span className="profile-sheet-avatar-preview" aria-hidden="true">{draft.profileEmoji}</span>
            <div className="profile-sheet-emoji-grid" role="radiogroup" aria-label="Emoji del profilo">
              {PROFILE_EMOJIS.map((emoji) => (
                <button
                  key={emoji}
                  type="button"
                  role="radio"
                  aria-checked={draft.profileEmoji === emoji}
                  aria-label={`Emoji ${emoji}`}
                  className={draft.profileEmoji === emoji ? "selected" : ""}
                  onClick={() => update("profileEmoji", emoji)}
                >
                  {emoji}
                </button>
              ))}
            </div>
          </fieldset>

          <label className="profile-sheet-field">
            <span>Nome</span>
            <input
              ref={nameRef}
              value={draft.displayName}
              onChange={(event) => update("displayName", event.target.value)}
              maxLength={NAME_MAX}
              disabled={saving}
              autoComplete="name"
              aria-invalid={Boolean(nameError)}
              aria-describedby={nameError ? "profile-name-error" : undefined}
            />
            {nameError ? <small id="profile-name-error" className="profile-sheet-error">{nameError}</small> : null}
          </label>

          <label className="profile-sheet-field">
            <span>Nickname</span>
            <div className="profile-sheet-nickname">
              <span aria-hidden="true">@</span>
              <input
                value={draft.nickname}
                onChange={(event) => update("nickname", toEditableNickname(event.target.value))}
                maxLength={NICKNAME_MAX}
                disabled={saving}
                autoComplete="off"
                autoCapitalize="none"
                spellCheck={false}
                aria-invalid={Boolean(nicknameError)}
                aria-describedby={nicknameError ? "profile-nickname-error" : undefined}
              />
            </div>
            {nicknameError ? <small id="profile-nickname-error" className="profile-sheet-error">{nicknameError}</small> : null}
          </label>

          <label className="profile-sheet-field">
            <span>
              Bio <small aria-live="polite">{draft.bio.length}/{BIO_MAX}</small>
            </span>
            <textarea
              value={draft.bio}
              onChange={(event) => update("bio", event.target.value)}
              maxLength={BIO_MAX}
              disabled={saving}
              rows={3}
              placeholder="Due parole su di te"
            />
          </label>

          <div className="profile-sheet-field" ref={handRef}>
            <span id="profile-hand-label">Joystick</span>
            <div className="profile-sheet-segmented" role="radiogroup" aria-labelledby="profile-hand-label">
              {(["LEFT", "RIGHT"] as const).map((hand) => (
                <button
                  key={hand}
                  type="button"
                  role="radio"
                  aria-checked={draft.inputHandPreference === hand}
                  className={draft.inputHandPreference === hand ? "selected" : ""}
                  disabled={saving}
                  onClick={() => update("inputHandPreference", hand)}
                >
                  {hand === "LEFT" ? "Sinistra" : "Destra"}
                </button>
              ))}
            </div>
            <small>Lato dello schermo in cui compare il joystick nei giochi.</small>
          </div>

          {error ? <p className="profile-sheet-error profile-sheet-error--block" role="alert">{error}</p> : null}
        </form>
      </section>
    </div>,
    document.body
  );
}
