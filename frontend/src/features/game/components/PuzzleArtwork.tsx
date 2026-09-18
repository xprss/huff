export type PuzzleKind = "word" | "hack" | "sky" | "flow" | "star" | "eco";

/** Decorative previews, independent of the actual daily puzzle. */
export function PuzzleArtwork({ kind }: { kind: PuzzleKind }) {
  return (
    <span className={`puzzle-art puzzle-art--${kind}`} aria-hidden="true">
      {kind === "eco" ? <svg viewBox="0 0 220 100" focusable="false"><path d="M45 73V29H113V73H177" fill="none" stroke="currentColor" strokeWidth="2" strokeDasharray="4 7" opacity=".3"/><circle cx="45" cy="73" r="24" fill="#bc7a16" opacity=".12"/><circle cx="45" cy="73" r="12" fill="#bc7a16"/><path d="m113 12 17 17-17 17-17-17Z" fill="#8060b0"/><path d="m113 1 28 28-28 28-28-28Z" fill="none" stroke="#8060b0" opacity=".2"/><circle cx="177" cy="73" r="12" fill="none" stroke="#bc7a16" strokeWidth="2"/></svg> : null}
      {kind === "word" ? <span className="art-word-grid">{"PAROLAGIOCHI".split("").map((letter, index) => <i key={index} className={index < 6 ? `art-tile-${index % 3}` : "art-tile-outline"}>{letter}</i>)}</span> : null}
      {kind === "hack" ? <span className="art-code"><span className="art-code-dots"><i /><i /><i /></span><span><b>0</b><b>6</b><b>•</b><b>•</b></span><small><i /> ACCESS_</small></span> : null}
      {kind === "sky" ? <span className="art-skyline">{[2, 4, 3, 5].map((height, index) => <i key={index} style={{ height: `${height * 16}%` }}>{Array.from({ length: height }, (_, floor) => <b key={floor} />)}</i>)}</span> : null}
      {kind === "flow" ? <svg viewBox="0 0 220 100" focusable="false"><path d="M35 27H185V74H35" fill="none" stroke="currentColor" strokeWidth="24" strokeLinecap="round" strokeLinejoin="round" opacity=".18" />{[[35, 27, "F"], [110, 27, "L"], [185, 27, "U"], [185, 74, "S"], [110, 74, "S"], [35, 74, "I"]].map(([x, y, letter], index) => <g key={index}><circle cx={x} cy={y} r="18" fill="var(--art-node)" /><text x={x} y={Number(y) + 6} textAnchor="middle" fill="currentColor" fontSize="18" fontWeight="700">{letter}</text></g>)}</svg> : null}
      {kind === "star" ? <span className="art-syllables"><i>PA</i><i>RO</i><i>LA</i><svg viewBox="0 0 40 40" focusable="false"><path d="m20 0 4 14 14 6-14 5-4 15-5-15-15-5 15-6Z" fill="currentColor" /></svg></span> : null}
    </span>
  );
}
