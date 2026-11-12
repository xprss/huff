import React from "react";
import { HexahackStatsPanel, HexaskyStatsPanel, HexaflowStatsPanel, HexaecoStatsPanel, HexatrapStatsPanel, StatsPanel, StatsTabs, type StatsGame } from "../stats/StatsView";
import type { PublicPlayerProfileDto } from "../../types";
import { ProfileIdentityCard, ProfileSummaryCard } from "./ProfileParts";

export function PublicProfileView({ profile }: { profile: PublicPlayerProfileDto }) {
  const [activeStats, setActiveStats] = React.useState<StatsGame>("overall");
  const stats = { overall: profile.overallStats, hexaword: profile.hexawordStats, hexahack: profile.hexahackStats, hexasky: profile.hexaskyStats, hexaflow: profile.hexaflowStats, hexastar: profile.hexastarStats, hexaeco: profile.hexaecoStats, hexatrap: profile.hexatrapStats };
  return (
    <section className="profile-view my-profile public-profile-view" aria-label={`Profilo di ${profile.nickname}`}>
      <ProfileIdentityCard
        emoji={profile.profileEmoji}
        name={profile.displayName || profile.nickname}
        nickname={profile.nickname}
        bio={profile.bio}
      />
      <ProfileSummaryCard stats={profile.overallStats} medals={profile.medals} />
      <section className="my-profile-section" aria-labelledby="public-profile-stats-title">
        <h3 id="public-profile-stats-title">Statistiche</h3>
        <StatsTabs active={activeStats} onChange={setActiveStats} />
        <div className="account-panel stats-detail">
          {activeStats === "hexatrap" ? <HexatrapStatsPanel stats={stats.hexatrap} /> : activeStats === "hexahack" ? <HexahackStatsPanel stats={stats.hexahack} /> : activeStats === "hexasky" ? <HexaskyStatsPanel stats={stats.hexasky} /> : activeStats === "hexaflow" ? <HexaflowStatsPanel stats={stats.hexaflow} /> : activeStats === "hexaeco" ? <HexaecoStatsPanel stats={stats.hexaeco} /> : <StatsPanel stats={stats[activeStats]} />}
        </div>
      </section>
    </section>
  );
}
