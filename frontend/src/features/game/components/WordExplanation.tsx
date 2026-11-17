import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { isExplanationPending, wordExplanationQueryOptions, type ExplainedGame } from "../../../app/queries";

/**
 * The daily word's explanation, shown once the game is over. While the agent is
 * still writing it the card holds its place with a quiet shimmer; when it cannot
 * be produced the card stays out of the way.
 */
export function WordExplanation({ game, puzzleDate, onReady }: {
  game: ExplainedGame;
  puzzleDate: string;
  onReady?: () => void;
}) {
  const options = wordExplanationQueryOptions(game, puzzleDate);
  // Re-render on every poll, even when an unchanged PENDING answer is
  // structurally shared, so the placeholder disappears once polling gives up.
  const query = useQuery({ ...options, notifyOnChangeProps: "all" });
  const state = useQueryClient().getQueryState(options.queryKey);
  const explanation = query.data?.status === "READY" ? query.data.explanation : null;
  const pending = query.isPending || (state !== undefined && isExplanationPending(state));

  useEffect(() => {
    if (explanation) onReady?.();
  }, [explanation]);

  if (!explanation && !pending) return null;
  return <section className={`word-explanation${explanation ? "" : " is-loading"}`} aria-label="Spiegazione" aria-busy={!explanation}>
    <p className="eyebrow">Spiegazione</p>
    {explanation
      ? <p className="word-explanation-text">{explanation}</p>
      : <span className="word-explanation-placeholder" aria-hidden="true"><i/><i/></span>}
  </section>;
}
