import React from "react";

export type PuzzleKind = "word" | "hack" | "sky" | "flow" | "star" | "eco" | "trap";

/**
 * Playful, abstract neon vector emblems for each puzzle.
 * Clean tubular strokes without glow, sharing the white-to-primary brand gradient.
 */
export function PuzzleArtwork({ kind }: { kind: PuzzleKind }) {
  const id = React.useId();

  return (
    <span className={`puzzle-art puzzle-art--${kind}`} aria-hidden="true">
      {kind === "word" && (
        <svg viewBox="0 0 100 100" className="art-svg art-svg--word" focusable="false">
          <defs>
            <linearGradient id={`${id}-neon`} x1="15%" y1="15%" x2="85%" y2="85%">
              <stop offset="0%" stopColor="var(--color-title-text, #ffffff)" />
              <stop offset="100%" stopColor="var(--color-primary, #6366f1)" />
            </linearGradient>
          </defs>
          <path
            d="M 32 66 C 18 52, 22 28, 40 24 C 58 20, 66 42, 50 54 C 34 66, 42 88, 60 84 C 78 80, 82 56, 68 42"
            fill="none"
            stroke={`url(#${id}-neon)`}
            strokeWidth="3.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <circle cx="45" cy="35" r="5" fill={`url(#${id}-neon)`} />
          <circle cx="60" cy="68" r="5" fill={`url(#${id}-neon)`} />
        </svg>
      )}

      {kind === "hack" && (
        <svg viewBox="0 0 100 100" className="art-svg art-svg--hack" focusable="false">
          <defs>
            <linearGradient id={`${id}-neon`} x1="15%" y1="15%" x2="85%" y2="85%">
              <stop offset="0%" stopColor="var(--color-title-text, #ffffff)" />
              <stop offset="100%" stopColor="var(--color-primary, #6366f1)" />
            </linearGradient>
          </defs>
          <path
            d="M 24 50 C 24 35, 36 24, 50 24 C 64 24, 76 35, 76 50 C 76 65, 65 76, 50 76 C 36 76, 34 65, 34 56 C 34 46, 42 38, 50 38 C 58 38, 64 44, 64 52 C 64 58, 58 62, 52 62"
            fill="none"
            stroke={`url(#${id}-neon)`}
            strokeWidth="3.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <circle cx="52" cy="50" r="4.5" fill={`url(#${id}-neon)`} />
        </svg>
      )}

      {kind === "sky" && (
        <svg viewBox="0 0 100 100" className="art-svg art-svg--sky" focusable="false">
          <defs>
            <linearGradient id={`${id}-neon`} x1="15%" y1="15%" x2="85%" y2="85%">
              <stop offset="0%" stopColor="var(--color-title-text, #ffffff)" />
              <stop offset="100%" stopColor="var(--color-primary, #6366f1)" />
            </linearGradient>
          </defs>
          <path
            d="M 18 78 C 18 64, 30 64, 34 78 C 34 48, 52 48, 56 78 C 56 34, 76 34, 80 78"
            fill="none"
            stroke={`url(#${id}-neon)`}
            strokeWidth="3.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <circle cx="68" cy="20" r="5" fill={`url(#${id}-neon)`} />
          <path
            d="M 28 34 C 36 26, 48 28, 50 36"
            fill="none"
            stroke={`url(#${id}-neon)`}
            strokeWidth="2.5"
            strokeLinecap="round"
          />
        </svg>
      )}

      {kind === "flow" && (
        <svg viewBox="0 0 100 100" className="art-svg art-svg--flow" focusable="false">
          <defs>
            <linearGradient id={`${id}-neon`} x1="15%" y1="15%" x2="85%" y2="85%">
              <stop offset="0%" stopColor="var(--color-title-text, #ffffff)" />
              <stop offset="100%" stopColor="var(--color-primary, #6366f1)" />
            </linearGradient>
          </defs>
          <path
            d="M 18 64 C 28 64, 32 32, 46 32 C 58 32, 62 52, 52 64 C 44 72, 36 58, 44 44 C 52 30, 70 32, 82 48"
            fill="none"
            stroke={`url(#${id}-neon)`}
            strokeWidth="3.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <circle cx="18" cy="64" r="4.5" fill={`url(#${id}-neon)`} />
          <circle cx="82" cy="48" r="4.5" fill={`url(#${id}-neon)`} />
        </svg>
      )}

      {kind === "star" && (
        <svg viewBox="0 0 100 100" className="art-svg art-svg--star" focusable="false">
          <defs>
            <linearGradient id={`${id}-neon`} x1="15%" y1="15%" x2="85%" y2="85%">
              <stop offset="0%" stopColor="var(--color-title-text, #ffffff)" />
              <stop offset="100%" stopColor="var(--color-primary, #6366f1)" />
            </linearGradient>
          </defs>
          <path
            d="M 50 16 C 50 36, 36 50, 16 50 C 36 50, 50 64, 50 84 C 50 64, 64 50, 84 50 C 64 50, 50 36, 50 16 Z"
            fill="none"
            stroke={`url(#${id}-neon)`}
            strokeWidth="3.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <path
            d="M 50 36 C 50 44, 44 50, 36 50 C 44 50, 50 56, 50 64 C 50 56, 56 50, 64 50 C 56 50, 50 44, 50 36 Z"
            fill={`url(#${id}-neon)`}
            opacity="0.3"
          />
          <circle cx="50" cy="50" r="3" fill={`url(#${id}-neon)`} />
        </svg>
      )}

      {kind === "eco" && (
        <svg viewBox="0 0 100 100" className="art-svg art-svg--eco" focusable="false">
          <defs>
            <linearGradient id={`${id}-neon`} x1="15%" y1="15%" x2="85%" y2="85%">
              <stop offset="0%" stopColor="var(--color-title-text, #ffffff)" />
              <stop offset="100%" stopColor="var(--color-primary, #6366f1)" />
            </linearGradient>
          </defs>
          <circle cx="38" cy="50" r="22" fill="none" stroke={`url(#${id}-neon)`} strokeWidth="3.6" />
          <circle cx="62" cy="50" r="22" fill="none" stroke={`url(#${id}-neon)`} strokeWidth="3.6" />
          <circle cx="38" cy="50" r="5" fill={`url(#${id}-neon)`} />
          <circle cx="62" cy="50" r="5" fill={`url(#${id}-neon)`} />
          {/* <path
            d="M 44 32 C 50 36, 50 64, 56 68"
            fill="none"
            stroke={`url(#${id}-neon)`}
            strokeWidth="2.5"
            strokeLinecap="round"
          /> */}
        </svg>
      )}

      {kind === "trap" && (
        <svg viewBox="0 0 100 100" className="art-svg art-svg--trap" focusable="false">
          <defs>
            <linearGradient id={`${id}-neon`} x1="15%" y1="15%" x2="85%" y2="85%">
              <stop offset="0%" stopColor="var(--color-title-text, #ffffff)" />
              <stop offset="100%" stopColor="var(--color-primary, #6366f1)" />
            </linearGradient>
          </defs>
          <path
            d="M 20 66 C 36 78, 64 78, 80 66"
            fill="none"
            stroke={`url(#${id}-neon)`}
            strokeWidth="3.8"
            strokeLinecap="round"
          />
          <path
            d="M 28 48 C 38 32, 62 26, 74 40"
            fill="none"
            stroke={`url(#${id}-neon)`}
            strokeWidth="3.6"
            strokeLinecap="round"
          />
          <circle cx="28" cy="48" r="6" fill={`url(#${id}-neon)`} />
          <circle cx="74" cy="40" r="6" fill={`url(#${id}-neon)`} />
          <circle cx="50" cy="56" r="3.5" fill={`url(#${id}-neon)`} />
        </svg>
      )}
    </span>
  );
}
