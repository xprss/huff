import type { ReactNode } from "react";

export function DailyGameIntro({
  game,
  title,
  children
}: {
  game: string;
  title: string;
  children: ReactNode;
}) {
  return (
    <header className="daily-game-intro">
      <p className="eyebrow">{game}</p>
      <h2>{title}</h2>
      <p>{children}</p>
    </header>
  );
}
