import React from "react";
import { useInfiniteQuery, useMutation, useQueryClient, type InfiniteData } from "@tanstack/react-query";
import { Flame, Leaf, Moon, Star, Sun, UserRound, Waves } from "lucide-react";
import { api, ApiError } from "../../api";
import { queryKeys } from "../../app/queries";
import type { HexatrapActionDto, HexatrapDossierDto, HexatrapEventDto, HexatrapEventsDto, HexatrapQuestionRequestDto, HexatrapSubmissionRequestDto, HexatrapTodayDto } from "../../types";
import { DailyGameIntro } from "../game/components/DailyGameIntro";
import { hypotheses, retain, unlocked } from "./hexatrapRules";
import "./hexatrap.css";

const SEALS = ["Sole", "Luna", "Stella", "Fiamma", "Onda", "Foglia"];
const ICONS = [Sun, Moon, Star, Flame, Waves, Leaf];
const advisorName = (advisor: number) => `Consigliere ${advisor === 0 ? "A" : "B"}`;
type Pending = { kind: "question"; request: HexatrapQuestionRequestDto } | { kind: "submission"; request: HexatrapSubmissionRequestDto };
function readStored<T>(key: string, fallback: T): T {
  try { const value = localStorage.getItem(key); return value ? JSON.parse(value) as T : fallback; } catch { return fallback; }
}
function store(key: string, value: unknown) {
  try { if (value == null) localStorage.removeItem(key); else localStorage.setItem(key, JSON.stringify(value)); } catch { /* Play still works with unavailable device storage. */ }
}
function dossierText(dossier: HexatrapDossierDto) {
  const a = SEALS[dossier.firstSeal], b = SEALS[dossier.secondSeal];
  return dossier.relation === "SAME" ? `${a} e ${b} hanno lo stesso stato.` : dossier.relation === "IMPLIES" ? `Se ${a} è acceso, anche ${b} è acceso.` : `Almeno uno fra ${a} e ${b} è acceso.`;
}
function Event({ event }: { event: HexatrapEventDto }) {
  return <li className="trap-event"><small>#{event.sequence} · {event.type === "SUBMISSION" ? "Il tuo inserimento" : advisorName(event.advisor!)}</small>
    <span>{event.type === "SEAL" ? `Il sigillo ${SEALS[event.seal!]} è acceso?` : event.type === "NUMBER" ? "Qual è il numero segreto?" : `Numero: ${event.number}`}</span>
    <strong>{event.type === "SEAL" ? event.answer ? "Sì." : "No." : event.type === "NUMBER" ? String(event.number) : event.correct ? "Corretto!" : "Non è il numero segreto. Puoi riprovare."}</strong></li>;
}

export function HexatrapView({ today, userId, inputBlocked, onError, onComplete }: {
  today: HexatrapTodayDto; userId: string; inputBlocked: boolean; onError: (error: unknown) => void; onComplete: () => void;
}) {
  const client = useQueryClient();
  const storageKey = `huff.hexatrap.v1.${userId}.${today.puzzleDate}`;
  const [advisor, setAdvisor] = React.useState(() => readStored(storageKey + ".advisor", 0));
  const [seal, setSeal] = React.useState(0);
  const [questionType, setQuestionType] = React.useState<"SEAL" | "NUMBER">("SEAL");
  const [number, setNumber] = React.useState("");
  const [notes, setNotes] = React.useState<string[]>(() => readStored(storageKey + ".notes", Array(6).fill("?")));
  const [pending, setPending] = React.useState<Pending | null>(() => readStored(storageKey + ".pending", null));
  const [message, setMessage] = React.useState("");
  const game = today.game;
  const won = game?.status === "WON";
  const latestEvent = game?.history.events[0];
  const history = useInfiniteQuery({
    queryKey: queryKeys.hexatrapEvents(today.puzzleDate),
    queryFn: ({ pageParam }) => api.hexatrapEvents(today.puzzleDate, pageParam),
    initialPageParam: undefined as number | undefined,
    getNextPageParam: page => page.nextBefore ?? undefined,
    enabled: Boolean(game),
    initialData: game ? { pages: [game.history], pageParams: [undefined] } : undefined,
    staleTime: Infinity,
    refetchOnMount: false,
    refetchOnReconnect: false,
    refetchOnWindowFocus: false
  });
  const latestSequence = game?.history.events[0]?.sequence ?? 0;
  React.useEffect(() => {
    // Resume from the latest bounded page after an action or another device's update.
    // Previously loaded pages are immutable, but refetching all of them on each question is costly.
    if (game) client.setQueryData<InfiniteData<HexatrapEventsDto>>(queryKeys.hexatrapEvents(today.puzzleDate), {
      pages: [game.history], pageParams: [undefined]
    });
  }, [latestSequence, client, today.puzzleDate]);
  const mutation = useMutation({
    mutationFn: (operation: Pending) => operation.kind === "question" ? api.hexatrapQuestion(operation.request) : api.hexatrapSubmit(operation.request),
    onSuccess: (action: HexatrapActionDto) => {
      setPending(null); store(storageKey + ".pending", null); setMessage("");
      client.setQueryData<HexatrapTodayDto>(queryKeys.hexatrapToday, current => {
        const sequence = current?.game?.history.events[0]?.sequence ?? 0;
        return current?.puzzleDate === action.game.puzzleDate && sequence <= action.event.sequence ? { ...current, game: action.game } : current;
      });
      void client.invalidateQueries({ queryKey: queryKeys.hexatrapToday });
      void client.invalidateQueries({ queryKey: queryKeys.hexatrapStats });
      if (action.game.status === "WON") {
        void client.invalidateQueries({ queryKey: queryKeys.overallStats });
        void client.invalidateQueries({ queryKey: queryKeys.leaderboards() });
        void client.invalidateQueries({ queryKey: ["player"] });
        if (!action.replayed) onComplete();
      }
    },
    onError: error => {
      onError(error);
      const definitive = error instanceof ApiError && error.status >= 400 && error.status < 500;
      if (definitive) { setPending(null); store(storageKey + ".pending", null); }
      setMessage(error instanceof Error ? error.message : "Connessione interrotta.");
      void client.invalidateQueries({ queryKey: queryKeys.hexatrapToday });
    }
  });
  function send(operation: Pending) {
    if (mutation.isPending || inputBlocked) return;
    setPending(operation); store(storageKey + ".pending", operation); mutation.mutate(operation);
  }
  function ask() {
    send({ kind: "question", request: { requestId: crypto.randomUUID(), puzzleDate: today.puzzleDate, rulesVersion: today.rulesVersion, type: questionType, advisor, seal: questionType === "SEAL" ? seal : null } });
  }
  function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!/^\d+$/.test(number) || +number < 1 || +number > 9864) return;
    send({ kind: "submission", request: { requestId: crypto.randomUUID(), puzzleDate: today.puzzleDate, rulesVersion: today.rulesVersion, number: +number } });
  }
  const busy = inputBlocked || mutation.isPending || Boolean(pending);
  const events = [...new Map([...(history.data?.pages.flatMap(p => p.events) ?? []), ...(game?.history.events ?? [])].map(e => [e.sequence, e])).values()].sort((a,b) => b.sequence - a.sequence);
  return <div className="trap-view" aria-label="Hexatrap">
    <DailyGameIntro game="Hexatrap" title="Di chi ti fidi?">Due consiglieri, sei sigilli e un numero segreto da 1 a 9864.</DailyGameIntro>
    <details className="trap-panel trap-help"><summary>Come si gioca · esempio senza punteggio</summary><p>Un consigliere dice sempre la verità, l’altro mente sempre. I ruoli restano fissi. Il dossier è sicuramente vero: usa le risposte sì/no per capire chi è leale.</p><p>Quando le prove bastano, puoi chiedere il numero a entrambi. Scegli tu a chi credere e inserisci il numero. Nessun limite a domande o inserimenti.</p><p>Parti da 1000 punti. Ogni domanda costa 100 punti, anche se ripetuta o sul numero. Ogni inserimento errato costa 200 punti. I punti possono essere negativi.</p><Tutorial /></details>
    <div className="trap-score" aria-label="Punteggio"><strong>{game?.points ?? 1000} <small>punti</small></strong><span>{game?.questions ?? 0} domande · {game?.wrongSubmissions ?? 0} errori</span></div>
    <details className="trap-panel trap-dossier" open><summary>Dossier · indizi certi</summary><p>Esattamente <strong>{today.dossier.litCount} sigilli</strong> sono accesi.</p><p>{dossierText(today.dossier)}</p></details>
    {won && game.reveal ? <section className="trap-panel trap-victory" aria-label="Sfida risolta"><h3>Hai sciolto l’inganno!</h3><p>Il numero segreto è <strong>{game.reveal.number}</strong>.</p><p>{advisorName(game.reveal.loyalAdvisor)} è leale. {advisorName(1 - game.reveal.loyalAdvisor)} mente.</p><p>{game.reveal.explanation}</p><ul>{SEALS.map((name,i) => <li key={name}>{name}: <strong>{game.reveal!.configuration & (1 << i) ? "acceso" : "spento"}</strong></li>)}</ul></section> : <>
      <fieldset className="trap-advisors" disabled={busy}><legend>A chi vuoi rivolgerti?</legend>{[0,1].map(a => <button key={a} type="button" aria-pressed={advisor === a} onClick={() => { setAdvisor(a); store(storageKey + ".advisor", a); }}><UserRound size={38} aria-hidden="true"/><strong>{advisorName(a)}</strong><span>{advisor === a ? "Selezionato" : "Seleziona"}</span></button>)}</fieldset>
      <fieldset className="trap-seals" disabled={busy}><legend>Scegli un sigillo</legend>{SEALS.map((name,i) => { const Icon = ICONS[i]; return <button key={name} type="button" aria-pressed={questionType === "SEAL" && seal === i} onClick={() => { setSeal(i); setQuestionType("SEAL"); }}><Icon size={22} aria-hidden="true"/>{name}</button>; })}</fieldset>
      {game?.numberUnlocked ? <div className="trap-unlocked"><p>Le risposte raccolte ora permettono una deduzione certa</p><button type="button" aria-pressed={questionType === "NUMBER"} disabled={busy} onClick={() => setQuestionType("NUMBER")}>Chiedi il numero</button></div> : <p className="trap-hint">La domanda sul numero si sblocca quando le risposte permettono di dedurre con certezza chi è leale.</p>}
      <section className="trap-panel trap-preview" aria-label="Anteprima domanda"><small>A {advisorName(advisor)}</small><p>{questionType === "SEAL" ? `Il sigillo ${SEALS[seal]} è acceso?` : "Qual è il numero segreto?"}</p><button type="button" className="trap-primary" disabled={busy || questionType === "NUMBER" && !game?.numberUnlocked} onClick={ask}>{mutation.isPending ? "Invio…" : "Invia domanda · −100 punti"}</button><div className="trap-reply" aria-live="polite" aria-atomic="true">{latestEvent && latestEvent.type !== "SUBMISSION" ? <><small>{advisorName(latestEvent.advisor!)} risponde alla domanda #{latestEvent.sequence}</small><strong>{latestEvent.type === "SEAL" ? latestEvent.answer ? "Sì." : "No." : latestEvent.number}</strong></> : null}</div></section>
      <form className="trap-panel trap-submit" onSubmit={submit}><label htmlFor="trap-number">Il numero segreto</label><div><input id="trap-number" type="number" inputMode="numeric" min="1" max="9864" step="1" required value={number} onChange={e => setNumber(e.target.value)} disabled={busy || !game?.numberRequested} placeholder="1–9864"/><button type="submit" disabled={busy || !game?.numberRequested}>Conferma</button></div>{!game?.numberRequested ? <small>Prima sblocca e chiedi il numero a un consigliere.</small> : <small>Puoi riprovare: ogni errore costa 200 punti.</small>}</form>
    </>}
    {message ? <p role="alert" className="trap-error">{message}</p> : null}
    {pending && !mutation.isPending ? <div className="trap-panel" role="status"><p>Un’operazione è in attesa di conferma. Puoi ritrasmetterla senza pagarla due volte.</p><button type="button" disabled={inputBlocked} onClick={() => send(pending)}>Recupera operazione</button></div> : null}
    <details className="trap-panel"><summary>I tuoi appunti sui sigilli</summary><p>Annotazioni personali: non cambiano le domande né le risposte.</p><div className="trap-notes">{SEALS.map((name,i) => <label key={name}>{name}<select aria-label={`Appunto ${name}`} value={notes[i]} onChange={e => { const next = [...notes]; next[i] = e.target.value; setNotes(next); store(storageKey + ".notes", next); }}><option value="?">?</option><option value="on">Acceso</option><option value="off">Spento</option></select></label>)}</div><small>Gli appunti restano su questo dispositivo.</small></details>
    <section className="trap-panel trap-history" aria-label="Dialogo e cronologia"><h3>Il dialogo</h3><div>{events[0] ? <p>{events[0].type === "SUBMISSION" ? "Ultimo inserimento" : advisorName(events[0].advisor!)}: <strong>{events[0].type === "SEAL" ? events[0].answer ? "Sì." : "No." : events[0].type === "NUMBER" ? events[0].number : events[0].correct ? "Corretto!" : "Numero errato."}</strong></p> : <p>Il dialogo comincia con la tua prima domanda.</p>}</div><ol>{events.map(event => <Event key={event.sequence} event={event}/>)}</ol>{history.isError ? <button type="button" onClick={() => void history.refetch()}>Riprova a caricare la cronologia</button> : null}{history.hasNextPage ? <button type="button" disabled={history.isFetchingNextPage} onClick={() => void history.fetchNextPage()}>{history.isFetchingNextPage ? "Caricamento…" : "Carica scambi precedenti"}</button> : null}</section>
  </div>;
}

function Tutorial() {
  const dossier: HexatrapDossierDto = { litCount: 2, relation: "SAME", firstSeal: 0, secondSeal: 1 };
  const [step, setStep] = React.useState(0);
  // Example: only Sole and Luna lit, A loyal; all responses belong to this separate example.
  const answers = [true, true, false, false, false, false];
  let candidates = hypotheses(dossier);
  for (let i = 0; i < step; i++) candidates = retain(candidates, 0, i, answers[i]);
  return <div className="trap-tutorial"><strong>Un esempio separato dalla sfida</strong><p>Due sigilli accesi. Sole e Luna hanno lo stesso stato. Chiediamo ad A i sigilli, in ordine.</p>{answers.slice(0,step).map((value,i) => <p key={i}>{SEALS[i]} è acceso? A: {value ? "Sì" : "No"}.</p>)}<p>{unlocked(candidates) ? "Ora tutte le ipotesi rimaste concordano: nell’esempio A è leale. Nella partita dovrai dedurlo tu." : "Le risposte dell’esempio non bastano ancora a stabilire il ruolo."}</p><button type="button" onClick={() => setStep(step >= 6 ? 0 : step + 1)}>{step >= 6 ? "Ricomincia esempio" : "Prossima domanda di esempio"}</button></div>;
}
