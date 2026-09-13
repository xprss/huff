import { BarChart3, Grid2X2, Trophy, UserRound } from "lucide-react";
import type { AppRoute } from "./routing";

export function AppNavigation({ activeRoute, onNavigate, onOpenStats }: {
  activeRoute: AppRoute;
  onNavigate: (route: AppRoute) => void;
  onOpenStats: () => void;
}) {
  const gameActive = ["games", "game", "hexahack", "hexasky", "hexaflow", "hexastar"].includes(activeRoute);
  const items = [
    { label: "Giochi", Icon: Grid2X2, active: gameActive, onClick: () => onNavigate("games") },
    { label: "Classifica", Icon: Trophy, active: activeRoute === "leaderboard" || activeRoute === "player", onClick: () => onNavigate("leaderboard") },
    { label: "Statistiche", Icon: BarChart3, active: activeRoute === "stats", onClick: onOpenStats },
    { label: "Profilo", Icon: UserRound, active: activeRoute === "profile", onClick: () => onNavigate("profile") }
  ];
  return <nav className="app-navigation" aria-label="Navigazione principale">{items.map(({ label, Icon, active, onClick }) =>
    <button key={label} type="button" className={`nav-item${active ? " active" : ""}`} aria-current={active ? "page" : undefined} onClick={onClick}><Icon size={21} aria-hidden="true" /><span>{label}</span></button>
  )}</nav>;
}
