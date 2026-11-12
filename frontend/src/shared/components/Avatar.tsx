import React from "react";

const ACCENTS = ["word", "hack", "sky", "flow", "star", "eco", "trap"] as const;

/**
 * Initials in the usual style: first letter of the first and last word
 * ("Vincenzo Sagristano" → "VS"); a single word gives one letter. A nickname
 * fallback ignores the leading "@" and splits on . _ -.
 */
export function initialsOf(name: string): string {
  const words = name
    .replace(/^@/, "")
    .split(/[\s._-]+/u)
    .map((word) => word.replace(/[^\p{L}\p{N}]/gu, ""))
    .filter(Boolean);
  if (!words.length) return "?";
  const first = [...words[0]][0] ?? "";
  const last = words.length > 1 ? [...words[words.length - 1]][0] ?? "" : "";
  return (first + last).toLocaleUpperCase("it-IT");
}

function accentFor(name: string) {
  let hash = 0;
  for (const char of name) hash = (hash * 31 + (char.codePointAt(0) ?? 0)) >>> 0;
  return `var(--game-accent-${ACCENTS[hash % ACCENTS.length]})`;
}

/**
 * Profile picture: the uploaded photo when there is one, otherwise the name
 * initials on a color derived from the name. Decorative by default, since
 * the name is always shown next to it.
 */
export function Avatar({
  name,
  avatarUrl,
  size,
  className = ""
}: {
  name: string;
  avatarUrl: string | null | undefined;
  size: number;
  className?: string;
}) {
  const [failedUrl, setFailedUrl] = React.useState<string | null>(null);
  const showPhoto = Boolean(avatarUrl) && failedUrl !== avatarUrl;
  const style = {
    "--avatar-size": `${size}px`,
    "--avatar-accent": accentFor(name)
  } as React.CSSProperties;

  return (
    <span className={`avatar${showPhoto ? " avatar--photo" : ""} ${className}`} style={style} aria-hidden="true">
      {showPhoto ? (
        <img src={avatarUrl!} alt="" loading="lazy" decoding="async" draggable={false} onError={() => setFailedUrl(avatarUrl!)} />
      ) : (
        <span className="avatar-initials">{initialsOf(name)}</span>
      )}
    </span>
  );
}
