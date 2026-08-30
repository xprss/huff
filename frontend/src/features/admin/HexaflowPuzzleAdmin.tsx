import React from "react";
import { ChevronLeft, ChevronRight, CircleAlert, CircleCheck, Grid3X3, ListChecks, Plus, Save, Send, Trash2 } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../api";
import { queryKeys } from "../../app/queries";
import type { HexaflowAnswerDto, HexaflowPuzzleAdminDto, HexaflowPuzzleDraftDto } from "../../types";

const EDITOR_TIMEZONE = "Europe/Rome";
const blankGrid = () => Array.from({ length: 48 }, () => "");

function editorToday() {
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: EDITOR_TIMEZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).formatToParts();
  const value = Object.fromEntries(parts.filter((part) => part.type !== "literal").map((part) => [part.type, part.value]));
  return `${value.year}-${value.month}-${value.day}`;
}

const monthNow = () => editorToday().slice(0, 7);

export function HexaflowPuzzleAdmin({ onSuccess, onError }: { onSuccess: (message: string) => void; onError: (message: string) => void }) {
  const client = useQueryClient();
  const [month, setMonth] = React.useState(monthNow);
  const [date, setDate] = React.useState<string | null>(null);
  const [creationDate, setCreationDate] = React.useState(editorToday);
  const [isNew, setIsNew] = React.useState(false);
  const [draft, setDraft] = React.useState<HexaflowPuzzleDraftDto | null>(null);
  const [activeAnswer, setActiveAnswer] = React.useState(0);
  const [confirmAction, setConfirmAction] = React.useState<"publish" | "delete" | null>(null);
  const monthQuery = useQuery({ queryKey: queryKeys.adminHexaflowPuzzles(month), queryFn: () => api.adminHexaflowPuzzles(month) });
  const detailQuery = useQuery({ queryKey: queryKeys.adminHexaflowPuzzle(date ?? ""), queryFn: () => api.adminHexaflowPuzzle(date ?? ""), enabled: Boolean(date) && !isNew });

  React.useEffect(() => {
    if (detailQuery.data) {
      setDraft(toDraft(detailQuery.data));
      setActiveAnswer(0);
      setConfirmAction(null);
    }
  }, [detailQuery.data]);

  function updated(value: HexaflowPuzzleAdminDto) {
    setIsNew(false);
    client.setQueryData(queryKeys.adminHexaflowPuzzle(value.puzzleDate), value);
    setDraft(toDraft(value));
    void client.invalidateQueries({ queryKey: ["admin", "hexaflow"] });
    void client.invalidateQueries({ queryKey: queryKeys.hexaflowToday });
    void client.invalidateQueries({ queryKey: queryKeys.hexaflowStats });
    void client.invalidateQueries({ queryKey: queryKeys.overallStats });
    onSuccess(value.status === "PUBLISHED" ? "Puzzle pubblicato e disponibile per i giocatori." : "Bozza salvata.");
  }

  const save = useMutation({ mutationFn: (value: HexaflowPuzzleDraftDto) => api.saveAdminHexaflowPuzzle(value.puzzleDate, value), onSuccess: updated });
  const publish = useMutation({ mutationFn: (value: string) => api.publishAdminHexaflowPuzzle(value), onSuccess: updated });
  const unpublish = useMutation({ mutationFn: (value: string) => api.draftAdminHexaflowPuzzle(value), onSuccess: updated });
  const remove = useMutation({
    mutationFn: (value: string) => api.deleteAdminHexaflowPuzzle(value),
    onSuccess: () => {
      setDate(null);
      setDraft(null);
      setConfirmAction(null);
      void client.invalidateQueries({ queryKey: ["admin", "hexaflow"] });
      void client.invalidateQueries({ queryKey: queryKeys.hexaflowToday });
      onSuccess("Bozza eliminata.");
    }
  });

  React.useEffect(() => {
    const error = monthQuery.error ?? detailQuery.error ?? save.error ?? publish.error ?? unpublish.error ?? remove.error;
    if (error) onError(error instanceof Error ? error.message : "Operazione CMS non riuscita.");
  }, [monthQuery.error, detailQuery.error, save.error, publish.error, unpublish.error, remove.error, onError]);

  function newPuzzle() {
    const value = creationDate;
    setIsNew(true);
    setDate(value);
    setDraft({ puzzleDate: value as HexaflowPuzzleDraftDto["puzzleDate"], themeClue: "", grid: blankGrid(), answers: [] });
    setActiveAnswer(0);
    setConfirmAction(null);
  }

  function openPuzzle(puzzleDate: string) {
    setIsNew(false);
    setDate(puzzleDate);
    setDraft(null);
    setConfirmAction(null);
  }

  function shiftMonth(delta: number) {
    const value = new Date(`${month}-01T12:00:00Z`);
    value.setUTCMonth(value.getUTCMonth() + delta);
    setMonth(value.toISOString().slice(0, 7));
    setDate(null);
    setDraft(null);
  }

  function setCell(index: number, value: string) {
    if (!draft) return;
    const grid = [...draft.grid];
    grid[index] = value.toUpperCase().replace(/[^A-Z]/g, "").slice(-1);
    setDraft({ ...draft, grid });
  }

  function addAnswer(type: "THEME" | "FLOW" = "THEME") {
    if (!draft) return;
    const answers = [...draft.answers, { id: crypto.randomUUID(), label: "", type, path: [] }];
    setDraft({ ...draft, answers });
    setActiveAnswer(answers.length - 1);
  }

  function updateAnswer(index: number, update: Partial<HexaflowAnswerDto>) {
    if (!draft) return;
    setDraft({ ...draft, answers: draft.answers.map((answer, answerIndex) => answerIndex === index ? { ...answer, ...update } : answer) });
  }

  function removeAnswer(index: number) {
    if (!draft) return;
    const answers = draft.answers.filter((_, answerIndex) => answerIndex !== index);
    setDraft({ ...draft, answers });
    setActiveAnswer((current) => Math.max(0, Math.min(current, answers.length - 1)));
  }

  function drawCell(cell: number) {
    if (!draft?.answers[activeAnswer]) return;
    const answer = draft.answers[activeAnswer];
    const path = [...answer.path];
    if (path[path.length - 2] === cell) path.pop();
    else if (!path.includes(cell) && (path.length === 0 || adjacent(path[path.length - 1], cell))) path.push(cell);
    updateAnswer(activeAnswer, { path });
  }

  const saved = detailQuery.data;
  const immutable = saved?.immutable ?? false;
  const editorLocked = immutable || saved?.status === "PUBLISHED";
  const covered = new Map<number, number>();
  draft?.answers.forEach((answer) => answer.path.forEach((cell) => covered.set(cell, (covered.get(cell) ?? 0) + 1)));
  const errorCount = saved?.validationErrors.length ?? 0;
  const completeGrid = draft?.grid.filter(Boolean).length ?? 0;

  if (!draft) {
    return (
      <section className="hexaflow-cms cms-library" aria-label="Puzzle Hexaflow">
        <header className="cms-library-header">
          <div>
            <p className="cms-kicker">CMS · Hexaflow</p>
            <h2>Calendario puzzle</h2>
            <p>Prepara, verifica e pubblica la sfida del giorno.</p>
          </div>
          <div className="cms-create-card">
            <label>
              <span>Data di gioco</span>
              <input type="date" value={creationDate} onChange={(event) => setCreationDate(event.target.value)} />
            </label>
            <button className="cms-primary-button" type="button" onClick={newPuzzle}><Plus size={17} />Nuovo puzzle</button>
          </div>
        </header>

        <section className="cms-library-panel" aria-label="Puzzle del mese">
          <div className="cms-month-toolbar">
            <button className="cms-icon-button" type="button" onClick={() => shiftMonth(-1)} aria-label="Mese precedente"><ChevronLeft /></button>
            <strong>{formatMonth(month)}</strong>
            <button className="cms-icon-button" type="button" onClick={() => shiftMonth(1)} aria-label="Mese successivo"><ChevronRight /></button>
          </div>
          {monthQuery.isPending ? <p className="cms-empty">Caricamento del calendario…</p> : null}
          {!monthQuery.isPending && monthQuery.data?.puzzles.length === 0 ? <p className="cms-empty">Nessun puzzle programmato per questo mese.</p> : null}
          <div className="cms-puzzle-list">
            {monthQuery.data?.puzzles.map((puzzle) => (
              <button className="cms-puzzle-card" type="button" key={puzzle.puzzleDate} onClick={() => openPuzzle(puzzle.puzzleDate)}>
                <span className={`cms-status ${puzzle.status.toLowerCase()}`}>{puzzle.status === "PUBLISHED" ? "Pubblicato" : "Bozza"}</span>
                <strong>{formatDate(puzzle.puzzleDate)}</strong>
                <span>{puzzle.themeClue || "Indizio da aggiungere"}</span>
                <span className="cms-puzzle-meta">{puzzle.answerCount} percorsi · {puzzle.coveredCells}/48 celle</span>
                <span className={`cms-validity ${puzzle.valid ? "valid" : "invalid"}`}>{puzzle.valid ? <CircleCheck size={16} /> : <CircleAlert size={16} />}{puzzle.valid ? "Pronto" : "Da completare"}</span>
              </button>
            ))}
          </div>
        </section>
      </section>
    );
  }

  const preparedDraft = prepareDraft(draft);
  const hasUnsavedChanges = !saved || JSON.stringify(preparedDraft) !== JSON.stringify(prepareDraft(toDraft(saved)));

  return (
    <section className="hexaflow-cms cms-editor" aria-label="Editor puzzle Hexaflow">
      <header className="cms-editor-header">
        <button className="cms-back-button" type="button" onClick={() => { setIsNew(false); setDraft(null); setDate(null); }}><ChevronLeft size={18} />Calendario</button>
        <div>
          <p className="cms-kicker">Puzzle del {formatDate(draft.puzzleDate)}</p>
          <h2>{saved?.status === "PUBLISHED" ? "Puzzle pubblicato" : "Editor puzzle"}</h2>
        </div>
        <span className={`cms-status ${saved?.status?.toLowerCase() ?? "draft"}`}>{saved?.status === "PUBLISHED" ? "Pubblicato" : "Bozza"}</span>
      </header>

      <section className="cms-progress-card" aria-label="Stato della bozza">
        <div><Grid3X3 size={20} /><span><strong>{completeGrid}/48</strong> lettere inserite</span></div>
        <div><ListChecks size={20} /><span><strong>{draft.answers.length}</strong> percorsi definiti</span></div>
        <div className={errorCount ? "warning" : "ready"}>{errorCount ? <CircleAlert size={20} /> : <CircleCheck size={20} />}<span><strong>{errorCount || "Nessun"}</strong> {errorCount === 1 ? "controllo da risolvere" : "controlli da risolvere"}</span></div>
      </section>

      <label className="cms-theme">
        <span>Indizio del tema</span>
        <input value={draft.themeClue} disabled={editorLocked} placeholder="es. Cose che trovi in un giardino" onChange={(event) => setDraft({ ...draft, themeClue: event.target.value })} />
      </label>

      <div className="cms-editor-layout">
        <section className="cms-grid-panel">
          <div className="cms-panel-heading"><div><h3>Griglia</h3><p>Inserisci le lettere, poi traccia il percorso selezionato.</p></div><div className="cms-grid-legend"><span className="selected">Selezionato</span><span className="covered">Usato</span><span className="overlap">Sovrapposto</span></div></div>
          <div className="hexaflow-grid cms-grid" role="grid" aria-label="Griglia Hexaflow 8 per 6">
            {draft.grid.map((letter, index) => (
              <div key={index} role="gridcell" className={`${covered.get(index) ? "covered" : ""} ${(covered.get(index) ?? 0) > 1 ? "overlap" : ""} ${draft.answers[activeAnswer]?.path.includes(index) ? "selected" : ""}`}>
                <input value={letter} disabled={editorLocked} onChange={(event) => setCell(index, event.target.value)} aria-label={`Lettera riga ${Math.floor(index / 6) + 1}, colonna ${(index % 6) + 1}`} />
                <button type="button" disabled={editorLocked || !draft.answers[activeAnswer]} onClick={() => drawCell(index)} aria-label={`Aggiungi la cella ${index + 1} al percorso`}>{draft.answers[activeAnswer]?.path.includes(index) ? "✓" : "+"}</button>
                <small>{index + 1}</small>
              </div>
            ))}
          </div>
          {!draft.answers[activeAnswer] ? <p className="cms-grid-help">Crea o seleziona una risposta per iniziare a disegnare un percorso.</p> : <p className="cms-grid-help">Stai disegnando: <strong>{solutionForPath(draft.grid, draft.answers[activeAnswer].path) || "percorso senza lettere"}</strong> · {draft.answers[activeAnswer].path.length} celle. Tocca la penultima cella per tornare indietro.</p>}
        </section>

        <section className="cms-answers-panel">
          <div className="cms-panel-heading"><div><h3>Percorsi</h3><p>Un Flusso, una o più parole tema.</p></div></div>
          <div className="cms-answer-actions">
            <button type="button" disabled={editorLocked} onClick={() => addAnswer()}><Plus size={16} />Parola tema</button>
            <button type="button" disabled={editorLocked || draft.answers.some((answer) => answer.type === "FLOW")} onClick={() => addAnswer("FLOW")}><Plus size={16} />Flusso</button>
          </div>
          <div className="cms-answer-list">
            {draft.answers.length === 0 ? <p className="cms-empty">Inizia aggiungendo una parola tema o il Flusso.</p> : null}
            {draft.answers.map((answer, index) => {
              const solution = solutionForPath(draft.grid, answer.path);
              return <article key={answer.id} className={`cms-answer ${activeAnswer === index ? "active" : ""}`}>
                <button className="cms-answer-select" type="button" onClick={() => setActiveAnswer(index)} aria-pressed={activeAnswer === index}>
                  <span className={`cms-answer-type ${answer.type.toLowerCase()}`}>{answer.type === "FLOW" ? "Flusso" : "Tema"}</span>
                  <strong>{solution || "Traccia il percorso"}</strong>
                  <span>{answer.path.length} celle</span>
                </button>
                <div className="cms-answer-fields">
                  <label><span>Tipo</span><select value={answer.type} disabled={editorLocked} onFocus={() => setActiveAnswer(index)} onChange={(event) => updateAnswer(index, { type: event.target.value as HexaflowAnswerDto["type"] })}><option value="THEME">Parola tema</option><option value="FLOW">Flusso</option></select></label>
                  <label><span>Parola composta</span><output className="cms-answer-word">{solution || "Seleziona le celle"}</output></label>
                  <button className="cms-delete-answer" type="button" disabled={editorLocked} onClick={() => removeAnswer(index)} aria-label={`Elimina ${solution || "percorso"}`}><Trash2 size={17} /></button>
                </div>
              </article>;
            })}
          </div>
        </section>
      </div>

      {errorCount > 0 ? <section className="cms-errors" aria-live="polite"><div><CircleAlert size={20} /><strong>Controlla questi punti prima di pubblicare</strong></div>{saved?.validationErrors.map((error, index) => <p key={`${error.code}-${index}`}>{error.message}{error.answerIndex !== null ? ` · percorso ${error.answerIndex + 1}` : ""}</p>)}</section> : null}

      <footer className="cms-actions" aria-label="Azioni puzzle">
        {confirmAction ? (
          <div className="cms-confirmation"><strong>{confirmAction === "publish" ? "Pubblicare questo puzzle?" : "Eliminare definitivamente questa bozza?"}</strong><span>{confirmAction === "publish" ? "Dopo la pubblicazione di oggi non potrà più essere modificato." : "L’operazione non può essere annullata."}</span><button className={confirmAction === "delete" ? "danger" : "cms-primary-button"} type="button" disabled={publish.isPending || remove.isPending} onClick={() => confirmAction === "publish" ? publish.mutate(draft.puzzleDate) : remove.mutate(draft.puzzleDate)}>{confirmAction === "publish" ? <><Send size={16} />Conferma pubblicazione</> : <><Trash2 size={16} />Elimina bozza</>}</button><button type="button" onClick={() => setConfirmAction(null)}>Annulla</button></div>
        ) : <>
          <button type="button" disabled={editorLocked || save.isPending} onClick={() => save.mutate(preparedDraft)}><Save size={16} />{save.isPending ? "Salvataggio…" : "Salva bozza"}</button>
          {saved?.status === "PUBLISHED" ? <button type="button" disabled={immutable || unpublish.isPending} onClick={() => unpublish.mutate(draft.puzzleDate)}>Torna in bozza</button> : <button className="cms-primary-button" type="button" title={hasUnsavedChanges ? "Salva la bozza prima di pubblicare." : undefined} disabled={!saved || publish.isPending || errorCount > 0 || hasUnsavedChanges} onClick={() => setConfirmAction("publish")}><Send size={16} />Pubblica</button>}
          <button className="danger" type="button" disabled={!saved || saved.status !== "DRAFT" || draft.puzzleDate <= editorToday()} onClick={() => setConfirmAction("delete")}><Trash2 size={16} />Elimina</button>
        </>}
      </footer>
    </section>
  );
}

function adjacent(a: number, b: number) {
  return a !== b && Math.abs(Math.floor(a / 6) - Math.floor(b / 6)) <= 1 && Math.abs(a % 6 - b % 6) <= 1;
}

function toDraft(puzzle: HexaflowPuzzleAdminDto): HexaflowPuzzleDraftDto {
  return { puzzleDate: puzzle.puzzleDate, themeClue: puzzle.themeClue, grid: [...puzzle.grid], answers: puzzle.answers.map((answer) => ({ ...answer, path: [...answer.path] })) };
}

function formatMonth(month: string) {
  return new Intl.DateTimeFormat("it-IT", { month: "long", year: "numeric", timeZone: EDITOR_TIMEZONE }).format(new Date(`${month}-01T12:00:00Z`));
}

function formatDate(date: string) {
  return new Intl.DateTimeFormat("it-IT", { weekday: "short", day: "numeric", month: "long" }).format(new Date(`${date}T12:00:00`));
}

function solutionForPath(grid: readonly string[], path: readonly number[]) {
  return path.map((cell) => grid[cell] ?? "").join("");
}

function prepareDraft(draft: HexaflowPuzzleDraftDto): HexaflowPuzzleDraftDto {
  return {
    ...draft,
    answers: draft.answers.map((answer) => ({ ...answer, label: solutionForPath(draft.grid, answer.path) }))
  };
}
