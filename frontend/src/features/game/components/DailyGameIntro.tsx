import type { ReactNode } from "react";
import { CircleHelp } from "lucide-react";

export function DailyGameIntro({
  game,
  title,
  onOpenTutorial,
  children
}: {
  game: string;
  title: string;
  onOpenTutorial?: () => void;
  children: ReactNode;
}) {
  return (
    <header className={`daily-game-intro ${onOpenTutorial ? "with-help" : ""}`}>
      <div>
        <p className="eyebrow">{game}</p>
        <h2>{title}</h2>
        <p>{children}</p>
      </div>
      {onOpenTutorial ? <button className="tutorial-help" type="button" onClick={onOpenTutorial} aria-label="Come si gioca"><CircleHelp size={20} /></button> : null}
    </header>
  );
}
