import React from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Eye, EyeOff } from "lucide-react";
import { api, ApiError } from "../../api";
import { queryKeys } from "../../app/queries";
import type { HexatrapActionDto, HexatrapClueRequestDto, HexatrapGameDto, HexatrapQuestionRequestDto, HexatrapTodayDto, HexatrapVerdictRequestDto } from "../../types";
import { DailyGameIntro } from "../game/components/DailyGameIntro";
import { CaseClock, CaseMeta } from "./components/CaseParts";
import { CasePanel } from "./components/CasePanel";
import { CaseReveal } from "./components/CaseReveal";
import { EvidencePanel } from "./components/EvidencePanel";
import { HexatrapTutorial } from "./components/HexatrapTutorial";
import { InterrogationPanel } from "./components/InterrogationPanel";
import { NotebookPanel } from "./components/NotebookPanel";
import { VerdictPanel } from "./components/VerdictPanel";
import { cycleMark, cycleSuspicion, EMPTY_NOTEBOOK, EMPTY_VERDICT, isClosed, newerGame, type Notebook, type VerdictDraft } from "./hexatrapModel";
import "./hexatrap.css";

type Tab = "case" | "questions" | "evidence" | "notebook" | "verdict";
const TABS: { id: Tab; label: string }[] = [
  { id: "case", label: "Caso" },
  { id: "questions", label: "Domande" },
  { id: "evidence", label: "Reperti" },
  { id: "notebook", label: "Taccuino" },
  { id: "verdict", label: "Verdetto" }
];

type Pending =
  | { kind: "question"; request: HexatrapQuestionRequestDto }
  | { kind: "clue"; request: HexatrapClueRequestDto }
  | { kind: "verdict"; request: HexatrapVerdictRequestDto };

function readStored<T>(key: string, fallback: T): T {
  try {
    const value = localStorage.getItem(key);
    return value ? JSON.parse(value) as T : fallback;
  } catch {
    return fallback;
  }
}

function store(key: string, value: unknown) {
  try {
    if (value == null) localStorage.removeItem(key);
    else localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Play still works when device storage is unavailable.
  }
}

function send(operation: Pending): Promise<HexatrapActionDto> {
  if (operation.kind === "question") return api.hexatrapQuestion(operation.request);
  if (operation.kind === "clue") return api.hexatrapClue(operation.request);
  return api.hexatrapVerdict(operation.request);
}

export function HexatrapView({ today, userId, preview, inputBlocked, onError, onComplete }: {
  today: HexatrapTodayDto; userId: string; preview: boolean; inputBlocked: boolean;
  onError: (error: unknown) => void; onComplete: (game: HexatrapGameDto) => void;
}) {
  const client = useQueryClient();
  const key = `huff.hexatrap.v2.${userId}.${today.puzzleDate}`;
  const caseFile = today.caseFile;
  const game = today.game;
  const closed = isClosed(game);
  const [tab, setTab] = React.useState<Tab>(() => closed ? "verdict" : readStored<Tab>(`${key}.tab`, "case"));
  const [lens, setLens] = React.useState<boolean>(() => readStored(`${key}.lens`, false));
  const [notebook, setNotebook] = React.useState<Notebook>(() => readStored(`${key}.notebook`, EMPTY_NOTEBOOK));
  const [draft, setDraft] = React.useState<VerdictDraft>(() => readStored(`${key}.verdict`, EMPTY_VERDICT));
  const [pending, setPending] = React.useState<Pending | null>(() => readStored(`${key}.pending`, null));
  const [message, setMessage] = React.useState("");
  const [announcement, setAnnouncement] = React.useState("");
  const revealRef = React.useRef<HTMLDivElement | null>(null);

  const mutation = useMutation({
    mutationFn: send,
    onSuccess: (action) => {
      setPending(null);
      store(`${key}.pending`, null);
      setMessage("");
      client.setQueryData<HexatrapTodayDto>(queryKeys.hexatrapToday, (current) =>
        current && current.puzzleDate === action.game.puzzleDate && newerGame(current.game, action.game) ? { ...current, game: action.game } : current);
      void client.invalidateQueries({ queryKey: queryKeys.hexatrapToday });
      void client.invalidateQueries({ queryKey: queryKeys.hexatrapStats });
      const latestAnswer = action.game.asked[action.game.asked.length - 1];
      if (action.game.status !== "IN_PROGRESS") {
        void client.invalidateQueries({ queryKey: queryKeys.overallStats });
        void client.invalidateQueries({ queryKey: queryKeys.leaderboards() });
        void client.invalidateQueries({ queryKey: ["player"] });
        setTab("verdict");
        if (!action.replayed) onComplete(action.game);
        window.setTimeout(() => revealRef.current?.querySelector<HTMLElement>(".trap-reveal")?.focus(), 50);
      } else if (latestAnswer && latestAnswer.sequence === action.game.sequence) {
        const character = caseFile?.characters.find((candidate) => candidate.id === latestAnswer.to);
        setAnnouncement(`${character?.name ?? ""} risponde: ${latestAnswer.answer ? "sì" : "no"}.`);
      } else {
        setAnnouncement("Reperto esaminato.");
      }
    },
    onError: (error) => {
      onError(error);
      const definitive = error instanceof ApiError && error.status >= 400 && error.status < 500;
      if (definitive) {
        setPending(null);
        store(`${key}.pending`, null);
      }
      setMessage(error instanceof Error ? error.message : "Connessione interrotta.");
      void client.invalidateQueries({ queryKey: queryKeys.hexatrapToday });
    }
  });

  if (!today.available || !caseFile) {
    return (
      <div className="trap-view" aria-label="Hexatrap">
        <DailyGameIntro game="Hexatrap" title="Il caso è in preparazione">Ogni notte un nuovo caso viene scritto e verificato. Riprova tra poco.</DailyGameIntro>
      </div>
    );
  }

  const busy = inputBlocked || mutation.isPending || Boolean(pending);
  const envelope = () => ({ requestId: crypto.randomUUID(), puzzleDate: today.puzzleDate, rulesVersion: today.rulesVersion });
  function run(operation: Pending) {
    if (mutation.isPending || inputBlocked) return;
    setPending(operation);
    store(`${key}.pending`, operation);
    mutation.mutate(operation);
  }
  function changeTab(next: Tab) {
    setTab(next);
    store(`${key}.tab`, next);
  }
  function updateNotebook(next: Notebook) {
    setNotebook(next);
    store(`${key}.notebook`, next);
  }
  function updateDraft(next: VerdictDraft) {
    setDraft(next);
    store(`${key}.verdict`, next);
  }
  function submitVerdict() {
    if (!draft.liar) return;
    run({ kind: "verdict", request: { ...envelope(), liar: draft.liar, facts: draft.facts } });
  }

  return (
    <div className="trap-view" aria-label="Hexatrap">
      <DailyGameIntro game={preview ? "Hexatrap · anteprima admin" : "Hexatrap · il caso del giorno"} title={caseFile.title}>
        Chi dice il vero e chi mente? Indaga prima che scada il tempo.
      </DailyGameIntro>
      <CaseMeta caseFile={caseFile} />
      <div className="trap-sticky">
        <CaseClock caseFile={caseFile} game={game} />
        <div className="trap-tabs" role="tablist" aria-label="Sezioni del caso">
          {TABS.map((entry) => (
            <button key={entry.id} id={`trap-tab-${entry.id}`} type="button" role="tab" aria-selected={tab === entry.id}
              aria-controls={`trap-panel-${entry.id}`} onClick={() => changeTab(entry.id)}>
              {entry.label}
              {entry.id === "questions" && game?.asked.length ? <span className="trap-count">{game.asked.length}</span> : null}
              {entry.id === "evidence" && game?.revealed.length ? <span className="trap-count">{game.revealed.length}</span> : null}
            </button>
          ))}
        </div>
      </div>
      {tab === "case" || tab === "questions" ? (
        <button type="button" className={`trap-lens${lens ? " is-on" : ""}`} aria-pressed={lens} onClick={() => { setLens(!lens); store(`${key}.lens`, !lens); }}>
          {lens ? <Eye size={16} aria-hidden="true" /> : <EyeOff size={16} aria-hidden="true" />} Lente delle ipotesi {lens ? "attiva" : "spenta"}
        </button>
      ) : null}
      <div id={`trap-panel-${tab}`} role="tabpanel" aria-labelledby={`trap-tab-${tab}`} ref={revealRef}>
        {tab === "case" ? <CasePanel caseFile={caseFile} notebook={notebook} lens={lens} /> : null}
        {tab === "questions" ? (
          <InterrogationPanel caseFile={caseFile} game={game} lens={lens} busy={busy}
            onAsk={(questionId) => run({ kind: "question", request: { ...envelope(), questionId } })} />
        ) : null}
        {tab === "evidence" ? (
          <EvidencePanel caseFile={caseFile} game={game} busy={busy} onReveal={(clueId) => run({ kind: "clue", request: { ...envelope(), clueId } })} />
        ) : null}
        {tab === "notebook" ? (
          <NotebookPanel caseFile={caseFile} notebook={notebook}
            onMark={(factId, optionId) => updateNotebook(cycleMark(notebook, caseFile, factId, optionId))}
            onSuspect={(id) => updateNotebook(cycleSuspicion(notebook, id))}
            onReset={() => updateNotebook(EMPTY_NOTEBOOK)} />
        ) : null}
        {tab === "verdict" ? (
          closed && game ? <CaseReveal caseFile={caseFile} game={game} /> : (
            <VerdictPanel caseFile={caseFile} game={game} draft={draft} busy={busy} onChange={updateDraft} onSubmit={submitVerdict} />
          )
        ) : null}
      </div>
      <p className="trap-visually-hidden" role="status" aria-live="polite" aria-atomic="true">{announcement}</p>
      {message ? <p role="alert" className="trap-error">{message}</p> : null}
      {pending && !mutation.isPending ? (
        <div className="trap-panel" role="status">
          <p>Un'operazione è in attesa di conferma. Puoi ritrasmetterla senza pagarla due volte.</p>
          <button type="button" disabled={inputBlocked} onClick={() => run(pending)}>Recupera operazione</button>
        </div>
      ) : null}
      <HexatrapTutorial />
    </div>
  );
}
