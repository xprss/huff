import React from "react";

/**
 * HexaQuot mark: a soft, open "Q" drawn as one tubular stroke with a tail and a
 * central node, in the same style as the game emblems (see PuzzleArtwork).
 * Keep public/icons/huff-icon.svg in sync when changing the paths.
 */
export function BrandLogo({ size = 26, className = "" }: { size?: number; className?: string }) {
  const gradientId = `${React.useId()}-brand`;
  return (
    <svg
      className={`brand-logo-svg ${className}`}
      width={size}
      height={size}
      viewBox="15 15 72 72"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <linearGradient id={gradientId} x1="15%" y1="15%" x2="85%" y2="85%">
          <stop offset="0%" stopColor="#ede9fe" />
          <stop offset="100%" stopColor="#a78bfa" />
        </linearGradient>
      </defs>
      <path
        d="M 50 21 C 33 21, 21 34, 21 50 C 21 66, 34 79, 50 79 C 66 79, 79 66, 79 50 C 79 38, 72 28, 62 23.5"
        stroke={`url(#${gradientId})`}
        strokeWidth="7"
        strokeLinecap="round"
      />
      <path d="M 60 62 C 66 70, 72 76, 81 79" stroke={`url(#${gradientId})`} strokeWidth="7" strokeLinecap="round" />
      <circle cx="50" cy="50" r="6" fill={`url(#${gradientId})`} />
    </svg>
  );
}
