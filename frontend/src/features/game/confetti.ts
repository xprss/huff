import confetti from "canvas-confetti";
import { VICTORY_CONFETTI_COLORS } from "../../app/constants";

export async function launchVictoryConfetti() {
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

  const burst = confetti({
    colors: VICTORY_CONFETTI_COLORS,
    disableForReducedMotion: true,
    gravity: 0.82,
    origin: { x: 0.5, y: 0.45 },
    particleCount: 100,
    scalar: 1.1,
    spread: 95,
    startVelocity: 48,
    ticks: 240
  });

  if (burst) await burst;
}
