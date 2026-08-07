import { ChevronLeft } from "lucide-react";
import { Distribution } from "../../shared/components/Distribution";
import { MedalCounts } from "../../shared/components/MedalCounts";
import { Metric } from "../../shared/components/Metric";
import type { PublicPlayerProfileDto } from "../../types";

export function PublicProfileView({ profile, onBack }: { profile: PublicPlayerProfileDto; onBack: () => void }) {
  const winRate = profile.stats.played > 0 ? Math.round((profile.stats.won / profile.stats.played) * 100) : 0;
  return (
    <section className="profile-view public-profile-view" aria-label={`Profilo di ${profile.nickname}`}>
      <div className="profile-summary">
        <div className="profile-head">
          <button className="icon-button profile-back" type="button" onClick={onBack} aria-label="Torna alla leaderboard" title="Torna">
            <ChevronLeft size={23} />
          </button>
          <span className="profile-emoji readonly" aria-hidden="true">
            <span className="profile-emoji-glyph">{profile.profileEmoji}</span>
          </span>
          <div className="profile-identity">
            <strong>{profile.displayName}</strong>
            <span>{profile.nickname}</span>
            {profile.bio ? <p className="profile-bio">{profile.bio}</p> : null}
          </div>
        </div>
      </div>
      <div className="profile-stats" aria-label="Statistiche pubbliche">
        <MedalCounts medals={profile.medals} />
        <div className="stat-grid">
          <Metric label="Giocate" value={profile.stats.played} />
          <Metric label="Vinte" value={profile.stats.won} />
          <Metric label="Perse" value={profile.stats.lost} />
          <Metric label="Vittorie" value={`${winRate}%`} />
        </div>
        <div className="stat-grid compact">
          <Metric label="Serie" value={profile.stats.currentStreak} />
          <Metric label="Record" value={profile.stats.maxStreak} />
        </div>
        <Distribution distribution={profile.stats.guessDistribution} />
      </div>
    </section>
  );
}
