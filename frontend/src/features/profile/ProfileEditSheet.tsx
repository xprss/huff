import React from "react";
import { createPortal } from "react-dom";
import { Camera, Trash2 } from "lucide-react";
import { Avatar } from "../../shared/components/Avatar";
import type { InputHandPreference, ProfileUpdateDto, UserDto } from "../../types";
import { AvatarCropper } from "./AvatarCropper";

const NAME_MAX = 80;
const NICKNAME_MAX = 29;
const BIO_MAX = 200;
const PHOTO_MAX_BYTES = 2 * 1024 * 1024;
const PHOTO_TYPES = ["image/jpeg", "image/png", "image/webp"];

export type ProfileEditFocus = "name" | "hand";

type Draft = {
  displayName: string;
  nickname: string;
  bio: string;
  inputHandPreference: InputHandPreference;
};

/** Pending photo change, applied only when the sheet is saved. */
type PhotoChange = { kind: "keep" } | { kind: "replace"; blob: Blob; previewUrl: string } | { kind: "remove" };

function draftFrom(user: UserDto): Draft {
  return {
    displayName: user.displayName ?? "",
    nickname: toEditableNickname(user.nickname),
    bio: user.bio ?? "",
    inputHandPreference: user.inputHandPreference
  };
}

export function toEditableNickname(nickname: string) {
  return nickname.replace(/@/g, "");
}

/**
 * Full-height edit sheet for the personal profile. The photo (uploaded,
 * framed in the cropper, or removed) and every field are applied together on
 * "Salva"; closing with unsaved changes asks for confirmation first.
 */
export function ProfileEditSheet({
  user,
  initialFocus = "name",
  onClose,
  onSave,
  onSaveAvatar,
  onSuccess,
  onError
}: {
  user: UserDto;
  initialFocus?: ProfileEditFocus;
  onClose: () => void;
  onSave: (profile: ProfileUpdateDto) => Promise<UserDto>;
  onSaveAvatar: (photo: Blob | null) => Promise<UserDto>;
  onSuccess: (message: string) => void;
  onError: (message: string) => void;
}) {
  const [initial] = React.useState(() => draftFrom(user));
  const [draft, setDraft] = React.useState<Draft>(initial);
  const [photo, setPhoto] = React.useState<PhotoChange>({ kind: "keep" });
  const [cropSource, setCropSource] = React.useState<string | null>(null);
  const [photoError, setPhotoError] = React.useState<string | null>(null);
  const [saving, setSaving] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [confirmDiscard, setConfirmDiscard] = React.useState(false);
  const sheetRef = React.useRef<HTMLElement | null>(null);
  const nameRef = React.useRef<HTMLInputElement | null>(null);
  const handRef = React.useRef<HTMLDivElement | null>(null);
  const fileRef = React.useRef<HTMLInputElement | null>(null);

  const fieldsDirty = (Object.keys(initial) as (keyof Draft)[]).some((key) => draft[key] !== initial[key]);
  const dirty = fieldsDirty || photo.kind !== "keep";
  const nameError = draft.displayName.trim() ? null : "Inserisci un nome.";
  const nicknameError = draft.nickname.trim() ? null : "Inserisci un nickname.";
  const canSave = dirty && !nameError && !nicknameError && !saving;
  const previewUrl = photo.kind === "replace" ? photo.previewUrl : photo.kind === "remove" ? null : user.avatarUrl;
  const previewName = draft.displayName.trim() || draft.nickname || user.nickname;

  // Release object URLs created for previews and the cropper.
  React.useEffect(() => () => {
    if (photo.kind === "replace") URL.revokeObjectURL(photo.previewUrl);
  }, [photo]);
  React.useEffect(() => () => {
    if (cropSource) URL.revokeObjectURL(cropSource);
  }, [cropSource]);

  const update = <K extends keyof Draft>(key: K, value: Draft[K]) => {
    setDraft((current) => ({ ...current, [key]: value }));
    setConfirmDiscard(false);
    setError(null);
  };

  const requestClose = React.useCallback(() => {
    if (saving) return;
    if (cropSource) {
      setCropSource(null);
      return;
    }
    if (dirty && !confirmDiscard) {
      setConfirmDiscard(true);
      return;
    }
    onClose();
  }, [confirmDiscard, cropSource, dirty, onClose, saving]);

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

  // Escape closes (the cropper first); Tab stays inside the sheet.
  React.useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        requestClose();
        return;
      }
      if (event.key !== "Tab" || !sheetRef.current) return;
      const scope = sheetRef.current.querySelector<HTMLElement>(".avatar-cropper") ?? sheetRef.current;
      const focusable = [...scope.querySelectorAll<HTMLElement>(
        "button:not(:disabled), input:not(:disabled):not([type='file']), textarea:not(:disabled), [tabindex='0']"
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

  function choosePhoto(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    if (!PHOTO_TYPES.includes(file.type)) {
      setPhotoError("Usa una foto JPEG, PNG o WebP.");
      return;
    }
    if (file.size > PHOTO_MAX_BYTES) {
      setPhotoError("La foto può pesare al massimo 2 MB.");
      return;
    }
    setPhotoError(null);
    setCropSource(URL.createObjectURL(file));
  }

  function useCroppedPhoto(blob: Blob) {
    setPhoto({ kind: "replace", blob, previewUrl: URL.createObjectURL(blob) });
    setCropSource(null);
    setConfirmDiscard(false);
    setError(null);
  }

  function removePhoto() {
    setPhoto(user.avatarUrl ? { kind: "remove" } : { kind: "keep" });
    setPhotoError(null);
    setConfirmDiscard(false);
  }

  async function submit(event?: React.FormEvent) {
    event?.preventDefault();
    if (!canSave) return;
    try {
      setSaving(true);
      setError(null);
      if (photo.kind === "replace") await onSaveAvatar(photo.blob);
      if (photo.kind === "remove") await onSaveAvatar(null);
      if (fieldsDirty) {
        await onSave({
          displayName: draft.displayName.trim(),
          nickname: `@${draft.nickname}`,
          bio: draft.bio.trim(),
          inputHandPreference: draft.inputHandPreference
        });
      }
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

  const hasPhoto = Boolean(previewUrl);

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
          <div className="profile-sheet-photo">
            <Avatar name={previewName} avatarUrl={previewUrl} size={96} />
            <div className="profile-sheet-photo-actions">
              <button className="profile-sheet-chip" type="button" onClick={() => fileRef.current?.click()} disabled={saving}>
                <Camera size={16} aria-hidden="true" />
                {hasPhoto ? "Cambia foto" : "Carica foto"}
              </button>
              {hasPhoto ? (
                <button className="profile-sheet-chip profile-sheet-chip--danger" type="button" onClick={removePhoto} disabled={saving}>
                  <Trash2 size={16} aria-hidden="true" />
                  Rimuovi
                </button>
              ) : null}
            </div>
            <small className="profile-sheet-photo-hint">JPEG, PNG o WebP, fino a 2 MB. Senza foto mostriamo le tue iniziali.</small>
            {photoError ? <small className="profile-sheet-error" role="alert">{photoError}</small> : null}
            <input
              ref={fileRef}
              className="sr-only"
              type="file"
              accept={PHOTO_TYPES.join(",")}
              onChange={choosePhoto}
              tabIndex={-1}
              aria-hidden="true"
            />
          </div>

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

        {cropSource ? (
          <AvatarCropper src={cropSource} onCancel={() => setCropSource(null)} onConfirm={useCroppedPhoto} />
        ) : null}
      </section>
    </div>,
    document.body
  );
}
