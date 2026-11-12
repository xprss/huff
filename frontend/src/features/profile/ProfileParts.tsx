import { ChevronRight, Pencil } from "lucide-react";
import type { MedalCountsDto, StatsDto } from "../../types";
import "./profile.css";

/**
 * Identity card: emoji avatar, name, nickname and bio. With `onEdit` the
 * avatar and the pencil open the editor and an empty bio becomes an
 * "add bio" link; without it the card is read-only.
 */
export function ProfileIdentityCard({
  emoji,
  name,
  nickname,
  bio,
  onEdit
}: {
  emoji: string;
  name: string;
  nickname: string;
  bio: string | null;
  onEdit?: () => void;
}) {
  return (
    <section className="my-profile-card my-profile-identity" aria-label={onEdit ? "La tua identità" : `Identità di ${name}`}>
      {onEdit ? (
        <button className="my-profile-avatar" type="button" onClick={onEdit} aria-label="Cambia emoji del profilo">
          <span aria-hidden="true">{emoji}</span>
        </button>
      ) : (
        <span className="my-profile-avatar" aria-hidden="true"><span>{emoji}</span></span>
      )}
      <div className="my-profile-names">
        <strong>{name}</strong>
        <span>{nickname}</span>
      </div>
      {onEdit ? (
        <button className="my-profile-edit" type="button" onClick={onEdit} aria-label="Modifica profilo">
          <Pencil size={17} aria-hidden="true" />
        </button>
      ) : <span aria-hidden="true" />}
      {bio ? (
        <p className="my-profile-bio">{bio}</p>
      ) : onEdit ? (
        <button className="my-profile-add-bio" type="button" onClick={onEdit}>
          + Aggiungi una bio
        </button>
      ) : null}
    </section>
  );
}

/**
 * Summary strip (games played, win rate, best streak) plus medals. With
 * `onOpenStats` the strip is a button leading to the full statistics.
 */
export function ProfileSummaryCard({
  stats,
  medals,
  onOpenStats
}: {
  stats: StatsDto;
  medals: MedalCountsDto;
  onOpenStats?: () => void;
}) {
  const winRate = stats.played ? Math.round((stats.won / stats.played) * 100) : 0;
  const numbers = (
    <>
      <span><strong>{stats.played}</strong><small>Giocate</small></span>
      <span><strong>{winRate}%</strong><small>Vittorie</small></span>
      <span><strong>{stats.maxStreak}</strong><small>Serie record</small></span>
    </>
  );
  return (
    <section className="my-profile-card my-profile-summary" aria-label="Riepilogo">
      {onOpenStats ? (
        <button className="my-profile-numbers" type="button" onClick={onOpenStats} aria-label="Apri tutte le statistiche">
          {numbers}
          <ChevronRight className="my-profile-numbers-chevron" size={18} aria-hidden="true" />
        </button>
      ) : (
        <div className="my-profile-numbers my-profile-numbers--static">{numbers}</div>
      )}
      <div className="my-profile-medals" aria-label={`Medaglie: ${medals.gold} oro, ${medals.silver} argento, ${medals.bronze} bronzo`}>
        <small>Medaglie</small>
        <span aria-hidden="true">🥇 <strong>{medals.gold}</strong></span>
        <span aria-hidden="true">🥈 <strong>{medals.silver}</strong></span>
        <span aria-hidden="true">🥉 <strong>{medals.bronze}</strong></span>
      </div>
    </section>
  );
}
