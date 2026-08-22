import { ChevronLeft } from "lucide-react";
import { MedalCounts } from "../../shared/components/MedalCounts";
import React from "react";
import { HexahackStatsPanel, HexaskyStatsPanel, HexasquareStatsPanel, StatsPanel, StatsTabs, type StatsGame } from "../stats/StatsModal";
import type { PublicPlayerProfileDto } from "../../types";

export function PublicProfileView({ profile, onBack }: { profile: PublicPlayerProfileDto; onBack: () => void }) {
  const [activeStats, setActiveStats] = React.useState<StatsGame>("overall");
  const stats = { overall: profile.overallStats, hexaword: profile.hexawordStats, hexahack: profile.hexahackStats, hexasky: profile.hexaskyStats, hexasquare: profile.hexasquareStats };
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
        <StatsTabs active={activeStats} onChange={setActiveStats} />
        {activeStats === "hexahack" ? <HexahackStatsPanel stats={stats.hexahack} /> : activeStats === "hexasky" ? <HexaskyStatsPanel stats={stats.hexasky} /> : activeStats === "hexasquare" ? <HexasquareStatsPanel stats={stats.hexasquare} /> : <StatsPanel stats={stats[activeStats]} />}
      </div>
    </section>
  );
}
