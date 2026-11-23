import React from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Eye, EyeOff, RefreshCw } from "lucide-react";
import { api } from "../../api";
import { queryKeys } from "../../app/queries";
import type { HexatrapAdminItemDto, HexatrapCaseDetailDto, HexatrapCaseStatus, HexatrapCaseSummaryDto } from "../../types";
import "./hexatrapAdmin.css";

const STATUS_LABEL: Record<HexatrapCaseStatus, string> = { READY: "Pronto", GENERATING: "In generazione", FAILED: "Non riuscito" };
const PHASE_LABEL = { DRAFT: "Bozza", VALIDATE: "Validazione", AUDIT: "Revisione", ACCEPTED: "Accettato" } as const;

function issueText(issue: unknown): string {
  if (typeof issue === "string") return issue;
  if (issue && typeof issue === "object" && "message" in issue) {
    const value = issue as { message?: unknown; soft?: unknown };
    return `${value.soft ? "(soft) " : ""}${String(value.message)}`;
  }
  return JSON.stringify(issue);
}

function Metrics({ summary }: { summary: HexatrapCaseSummaryDto }) {
  const m = summary.metrics;
  if (!m) return null;
  return (
    <dl className="trap-admin-metrics">
      <div><dt>Percorso ottimale</dt><dd>{m.certificateCost} min · {m.minimumActions} azioni min.</dd></div>
      <div><dt>Par esperto</dt><dd>{m.expertPar} min</dd></div>
      <div><dt>Mediana a caso</dt><dd>{m.naiveMedian} min</dd></div>
      <div><dt>Ipotesi iniziali</dt><dd>{m.h0Size} su {m.worlds * 2} · A mente {Math.round(m.roleBalance * 100)}%</dd></div>
      <div><dt>Costo totale</dt><dd>{m.totalCost} min</dd></div>
      <div><dt>Trappole / oracoli</dt><dd>{m.traps} / {m.oracles}</dd></div>
      <div><dt>Indovinarlo</dt><dd>1 su {m.guessOdds}</dd></div>
    </dl>
  );
}

function Items({ title, items, records, speakerName }: {
  title: string; items: readonly HexatrapAdminItemDto[]; records: Readonly<Record<string, string>>; speakerName: (id?: string) => string;
}) {
  if (!items.length) return null;
  return (
    <>
      <h4>{title}</h4>
      <ul className="trap-admin-items">
        {items.map((item) => (
          <li key={item.id}>
            <p>
              <code>{item.id}</code>
              {item.speaker ? ` · ${speakerName(item.speaker)}` : item.to ? ` · a ${speakerName(item.to)}` : ""}
              {item.cost ? ` · ${item.cost} min` : ""}
              {item.title ? ` · ${item.title}` : ""}
            </p>
            <p>{item.text}</p>
            <p className="trap-admin-record">{records[item.id]}</p>
            <p><code>{item.formula}</code></p>
          </li>
        ))}
      </ul>
    </>
  );
}

function CaseDetail({ detail }: { detail: HexatrapCaseDetailDto }) {
  const [spoilers, setSpoilers] = React.useState(false);
  const file = detail.caseFile;
  const speakerName = (id?: string) => file?.characters.find((character) => character.id === id)?.name ?? id ?? "";
  return (
    <div className="trap-admin-detail">
      {detail.summary.warnings?.length ? (
        <div className="trap-admin-warnings"><strong>Avvisi di difficoltà</strong><ul>{detail.summary.warnings.map((warning) => <li key={warning}>{warning}</li>)}</ul></div>
      ) : null}
      {detail.summary.lastError ? <p className="trap-admin-error">Ultimo errore: {detail.summary.lastError}</p> : null}
      {file ? (
        <>
          <p>{file.intro}</p>
          <p className="trap-admin-cast">{file.characters.map((character) => `${character.id} · ${character.name}, ${character.role}`).join(" — ")}</p>
          <button type="button" className="profile-action-button" onClick={() => setSpoilers(!spoilers)} aria-pressed={spoilers}>
            {spoilers ? <EyeOff size={16} aria-hidden="true" /> : <Eye size={16} aria-hidden="true" />} {spoilers ? "Nascondi soluzione e formule" : "Mostra soluzione e formule"}
          </button>
          {spoilers ? (
            <>
              <h4>Soluzione</h4>
              <p>Mente <strong>{speakerName(file.solution.liar)}</strong>. {file.facts.map((fact) => {
                const option = fact.options.find((candidate) => candidate.id === file.solution.facts[fact.id]);
                return `${fact.label} ${option?.label ?? "?"}${file.verdictFacts.includes(fact.id) ? " (verdetto)" : ""}`;
              }).join(" · ")}</p>
              <Items title="Certezze" items={file.certainties} records={detail.records} speakerName={speakerName} />
              <Items title="Deposizioni" items={file.statements} records={detail.records} speakerName={speakerName} />
              <Items title="Domande" items={file.questions} records={detail.records} speakerName={speakerName} />
              <Items title="Reperti" items={file.clues} records={detail.records} speakerName={speakerName} />
              <h4>Epilogo</h4>
              <p>{file.epilogue}</p>
            </>
          ) : null}
        </>
      ) : null}
      <h4>Tentativi di generazione</h4>
      {detail.attempts.length ? (
        <ol className="trap-admin-attempts">
          {detail.attempts.map((attempt, index) => (
            <li key={`${attempt.runId}-${attempt.phase}-${index}`} className={attempt.ok ? "ok" : "ko"}>
              <span>{new Date(attempt.createdAt).toLocaleString("it-IT")} · tentativo {attempt.attempt} · {PHASE_LABEL[attempt.phase]} · {attempt.ok ? "ok" : "respinto"} · {Math.round(attempt.durationMs / 100) / 10}s</span>
              {attempt.issues?.length ? <ul>{attempt.issues.map((issue, i) => <li key={i}>{issueText(issue)}</li>)}</ul> : null}
            </li>
          ))}
        </ol>
      ) : <p className="admin-empty">Nessun tentativo registrato.</p>}
    </div>
  );
}

export function HexatrapCaseAdmin({ onSuccess, onError }: { onSuccess: (message: string) => void; onError: (message: string) => void }) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = React.useState<string | null>(null);
  const cases = useQuery({ queryKey: queryKeys.adminHexatrapCases, queryFn: api.adminHexatrapCases, refetchInterval: 20_000 });
  const detail = useQuery({ queryKey: queryKeys.adminHexatrapCase(selected ?? ""), queryFn: () => api.adminHexatrapCase(selected!), enabled: Boolean(selected), refetchInterval: 20_000 });
  const generate = useMutation({
    mutationFn: (date: string) => api.adminGenerateHexatrapCase(date),
    onSuccess: (result) => {
      onSuccess(result.queued ? `Generazione del caso del ${result.puzzleDate} avviata.` : "Generazione già in corso.");
      void queryClient.invalidateQueries({ queryKey: ["admin", "hexatrap"] });
    },
    onError: (error) => onError(error instanceof Error ? error.message : "Generazione non avviata.")
  });
  const tomorrow = React.useMemo(() => {
    const date = new Date();
    date.setDate(date.getDate() + 1);
    return date.toLocaleDateString("sv-SE");
  }, []);
  const rows = cases.data ?? [];
  const hasTomorrow = rows.some((row) => row.puzzleDate === tomorrow);
  return (
    <section className="admin-view trap-admin" aria-label="Casi Hexatrap">
      <div className="admin-toolbar">
        <p className="alert-hint">Ogni notte l'agente scrive il caso del giorno dopo; il backend lo valida e ne verifica ogni frase. Si possono rigenerare solo i casi non ancora giocati.</p>
        {!hasTomorrow && cases.isSuccess ? (
          <button type="button" className="profile-action-button primary" disabled={generate.isPending} onClick={() => generate.mutate(tomorrow)}>
            <RefreshCw size={16} aria-hidden="true" /> Genera il caso di domani
          </button>
        ) : null}
      </div>
      {cases.isPending ? <p className="admin-empty">Caricamento…</p> : null}
      {cases.isSuccess && rows.length === 0 ? <p className="admin-empty">Nessun caso generato finora.</p> : null}
      <ul className="trap-admin-list">
        {rows.map((row) => (
          <li key={row.puzzleDate} className={`trap-admin-row is-${row.status.toLowerCase()}`}>
            <button type="button" className="trap-admin-open" aria-expanded={selected === row.puzzleDate} onClick={() => setSelected(selected === row.puzzleDate ? null : row.puzzleDate)}>
              <span className="trap-admin-date">{row.puzzleDate}</span>
              <strong>{row.title ?? "—"}</strong>
              <span className="trap-admin-tags">
                <span className={`trap-admin-status is-${row.status.toLowerCase()}`}>{STATUS_LABEL[row.status]}</span>
                <span>{row.format} · difficoltà {row.difficulty}</span>
                <span>{row.games} partite · {row.attempts} tentativi</span>
              </span>
            </button>
            <Metrics summary={row} />
            {row.regenerable ? (
              <button type="button" className="profile-action-button" disabled={generate.isPending} onClick={() => generate.mutate(row.puzzleDate)}>
                <RefreshCw size={16} aria-hidden="true" /> {row.status === "READY" ? "Rigenera" : "Genera"}
              </button>
            ) : null}
            {selected === row.puzzleDate ? (detail.data ? <CaseDetail detail={detail.data} /> : <p className="admin-empty">Caricamento…</p>) : null}
          </li>
        ))}
      </ul>
    </section>
  );
}
