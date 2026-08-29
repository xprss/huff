import React from "react";
import { Check, ChevronLeft, ChevronRight, Plus, Save, Send, Trash2 } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../api";
import { queryKeys } from "../../app/queries";
import type { HexaflowAnswerDto, HexaflowPuzzleAdminDto, HexaflowPuzzleDraftDto } from "../../types";

const today = () => new Date().toISOString().slice(0, 10);
const monthNow = () => today().slice(0, 7);
const blankGrid = () => Array.from({ length: 48 }, () => "");

export function HexaflowPuzzleAdmin({ onSuccess, onError }: { onSuccess: (message: string) => void; onError: (message: string) => void }) {
  const client = useQueryClient();
  const [month, setMonth] = React.useState(monthNow);
  const [date, setDate] = React.useState<string | null>(null);
  const [creationDate, setCreationDate] = React.useState(today);
  const [isNew, setIsNew] = React.useState(false);
  const [draft, setDraft] = React.useState<HexaflowPuzzleDraftDto | null>(null);
  const [activeAnswer, setActiveAnswer] = React.useState(0);
  const monthQuery = useQuery({ queryKey: queryKeys.adminHexaflowPuzzles(month), queryFn: () => api.adminHexaflowPuzzles(month) });
  const detailQuery = useQuery({ queryKey: queryKeys.adminHexaflowPuzzle(date ?? ""), queryFn: () => api.adminHexaflowPuzzle(date ?? ""), enabled: Boolean(date) && !isNew });
  React.useEffect(() => { if (detailQuery.data) setDraft(toDraft(detailQuery.data)); }, [detailQuery.data]);

  const save = useMutation({ mutationFn: (value: HexaflowPuzzleDraftDto) => api.saveAdminHexaflowPuzzle(value.puzzleDate, value), onSuccess: updated });
  const publish = useMutation({ mutationFn: (value: string) => api.publishAdminHexaflowPuzzle(value), onSuccess: updated });
  const unpublish = useMutation({ mutationFn: (value: string) => api.draftAdminHexaflowPuzzle(value), onSuccess: updated });
  const remove = useMutation({ mutationFn: (value: string) => api.deleteAdminHexaflowPuzzle(value), onSuccess: () => { setDate(null); setDraft(null); void client.invalidateQueries({ queryKey: ["admin", "hexaflow"] }); onSuccess("Bozza eliminata."); } });
  function updated(value: HexaflowPuzzleAdminDto) { setIsNew(false); client.setQueryData(queryKeys.adminHexaflowPuzzle(value.puzzleDate), value); setDraft(toDraft(value)); void client.invalidateQueries({ queryKey: ["admin", "hexaflow"] }); onSuccess(value.status === "PUBLISHED" ? "Puzzle pubblicato." : "Bozza salvata."); }
  React.useEffect(() => { const error = monthQuery.error ?? detailQuery.error ?? save.error ?? publish.error ?? unpublish.error ?? remove.error; if (error) onError(error instanceof Error ? error.message : "Operazione CMS non riuscita."); }, [monthQuery.error, detailQuery.error, save.error, publish.error, unpublish.error, remove.error, onError]);

  function newPuzzle() { const value = creationDate; setIsNew(true); setDate(value); setDraft({ puzzleDate: value as HexaflowPuzzleDraftDto["puzzleDate"], themeClue: "", grid: blankGrid(), answers: [] }); }
  function shiftMonth(delta: number) { const d = new Date(`${month}-01T00:00:00Z`); d.setUTCMonth(d.getUTCMonth() + delta); setMonth(d.toISOString().slice(0, 7)); setDate(null); setDraft(null); }
  function setCell(index: number, value: string) { if (!draft) return; const grid = [...draft.grid]; grid[index] = value.toUpperCase().replace(/[^A-Z]/g, "").slice(-1); setDraft({ ...draft, grid }); }
  function addAnswer(type: "THEME" | "FLOW" = "THEME") { if (!draft) return; const answers = [...draft.answers, { id: crypto.randomUUID(), label: "", type, path: [] }]; setDraft({ ...draft, answers }); setActiveAnswer(answers.length - 1); }
  function updateAnswer(index: number, update: Partial<HexaflowAnswerDto>) { if (!draft) return; const answers = draft.answers.map((answer, i) => i === index ? { ...answer, ...update } : answer); setDraft({ ...draft, answers }); }
  function drawCell(cell: number) { if (!draft?.answers[activeAnswer]) return; const answer = draft.answers[activeAnswer]; const path = [...answer.path]; if (path[path.length - 2] === cell) path.pop(); else if (!path.includes(cell) && (path.length === 0 || adjacent(path[path.length - 1], cell))) path.push(cell); updateAnswer(activeAnswer, { path }); }
  const saved = detailQuery.data;
  const covered = new Map<number, number>(); draft?.answers.forEach((answer) => answer.path.forEach((cell) => covered.set(cell, (covered.get(cell) ?? 0) + 1)));

  if (!draft) return <section className="hexaflow-cms"><header><div><h2>Puzzle Hexaflow</h2><p>Bozze e pubblicazioni editoriali</p></div><div className="cms-new"><input type="date" value={creationDate} onChange={(e) => setCreationDate(e.target.value)}/><button type="button" onClick={newPuzzle}><Plus size={17}/> Nuovo</button></div></header><div className="cms-month"><button onClick={() => shiftMonth(-1)}><ChevronLeft/></button><strong>{month}</strong><button onClick={() => shiftMonth(1)}><ChevronRight/></button></div><div className="cms-puzzle-list">{monthQuery.data?.puzzles.map((puzzle) => <button type="button" key={puzzle.puzzleDate} onClick={() => { setIsNew(false); setDate(puzzle.puzzleDate); }}><strong>{puzzle.puzzleDate}</strong><span>{puzzle.status === "PUBLISHED" ? "Pubblicato" : "Bozza"} · {puzzle.coveredCells}/48 celle</span>{puzzle.valid ? <Check size={16}/> : null}</button>)}</div></section>;

  const immutable = saved?.immutable ?? false;
  const editorLocked = immutable || saved?.status === "PUBLISHED";
  return <section className="hexaflow-cms editor"><header><button type="button" onClick={() => { setIsNew(false); setDraft(null); setDate(null); }}><ChevronLeft/> Elenco</button><div><h2>{draft.puzzleDate}</h2><p>{saved?.status ?? "NUOVO"} · {covered.size}/48 celle</p></div></header>
    <label className="cms-theme">Indizio del tema<input value={draft.themeClue} disabled={editorLocked} onChange={(e) => setDraft({ ...draft, themeClue: e.target.value })}/></label>
    <div className="cms-editor-layout"><div><h3>Griglia e anteprima percorsi</h3><div className="hexaflow-grid cms-grid">{draft.grid.map((letter, index) => <div key={index} className={`${covered.get(index) ? "covered" : ""} ${(covered.get(index) ?? 0) > 1 ? "overlap" : ""} ${draft.answers[activeAnswer]?.path.includes(index) ? "selected" : ""}`}><input value={letter} disabled={editorLocked} onChange={(e) => setCell(index, e.target.value)} aria-label={`Lettera ${index + 1}`}/><button type="button" disabled={editorLocked || !draft.answers[activeAnswer]} onClick={() => drawCell(index)} aria-label={`Aggiungi la cella ${index + 1} al percorso`}>+</button><small>{index + 1}</small></div>)}</div></div>
      <div className="cms-answers"><div className="cms-answer-head"><h3>Risposte</h3><button type="button" disabled={editorLocked} onClick={() => addAnswer()}><Plus/> Tema</button><button type="button" disabled={editorLocked || draft.answers.some(a => a.type === "FLOW")} onClick={() => addAnswer("FLOW")}><Plus/> Flusso</button></div>{draft.answers.map((answer, index) => <div key={answer.id} className={`cms-answer ${activeAnswer === index ? "active" : ""}`} onClick={() => setActiveAnswer(index)}><select value={answer.type} disabled={editorLocked} onChange={(e) => updateAnswer(index, { type: e.target.value as HexaflowAnswerDto["type"] })}><option value="THEME">Tema</option><option value="FLOW">Flusso</option></select><input value={answer.label} disabled={editorLocked} placeholder="Risposta" onChange={(e) => updateAnswer(index, { label: e.target.value })}/><span>{answer.path.length} celle</span><button type="button" disabled={editorLocked} onClick={() => setDraft({ ...draft, answers: draft.answers.filter((_, i) => i !== index) })}><Trash2/></button></div>)}</div></div>
    {saved?.validationErrors.length ? <div className="cms-errors"><strong>Da correggere prima della pubblicazione</strong>{saved.validationErrors.map((error, index) => <p key={`${error.code}-${index}`}>{error.message}{error.answerIndex !== null ? ` (risposta ${error.answerIndex + 1})` : ""}</p>)}</div> : null}
    <div className="cms-actions"><button type="button" disabled={editorLocked || save.isPending} onClick={() => save.mutate(draft)}><Save/> Salva bozza</button>{saved?.status === "PUBLISHED" ? <button type="button" disabled={immutable} onClick={() => unpublish.mutate(draft.puzzleDate)}>Torna in bozza</button> : <button type="button" disabled={!saved || publish.isPending} onClick={() => { if (confirm("Pubblicare questo puzzle?")) publish.mutate(draft.puzzleDate); }}><Send/> Pubblica</button>}<button className="danger" type="button" disabled={!saved || saved.status !== "DRAFT" || draft.puzzleDate <= today()} onClick={() => { if (confirm("Eliminare definitivamente la bozza?")) remove.mutate(draft.puzzleDate); }}><Trash2/> Elimina</button></div>
  </section>;
}

function adjacent(a: number, b: number) { return a !== b && Math.abs(Math.floor(a / 6) - Math.floor(b / 6)) <= 1 && Math.abs(a % 6 - b % 6) <= 1; }
function toDraft(puzzle: HexaflowPuzzleAdminDto): HexaflowPuzzleDraftDto { return { puzzleDate: puzzle.puzzleDate, themeClue: puzzle.themeClue, grid: [...puzzle.grid], answers: puzzle.answers.map(a => ({ ...a, path: [...a.path] })) }; }
