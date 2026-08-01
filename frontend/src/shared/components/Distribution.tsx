import type { GuessAttempt, GuessDistributionDto } from "../../types";

const DISTRIBUTION_ATTEMPTS = ["1", "2", "3", "4", "5", "6"] as const satisfies readonly GuessAttempt[];

export function Distribution({ distribution }: { distribution: GuessDistributionDto }) {
  const maxDistribution = Math.max(1, ...Object.values(distribution));

  return (
    <div className="distribution">
      {DISTRIBUTION_ATTEMPTS.map((attempt) => {
        const value = Number(distribution[attempt] ?? 0);
        return (
          <div className="distribution-row" key={attempt}>
            <span>{attempt}</span>
            <div>
              <b style={{ width: `${Math.max(8, (value / maxDistribution) * 100)}%` }}>{value}</b>
            </div>
          </div>
        );
      })}
    </div>
  );
}
