import React from "react";
import { createPortal } from "react-dom";
import { Activity, ChevronRight, CircleHelp, Radio, Share2, ShieldCheck, X } from "lucide-react";
import type {
  HexahackGameDto,
  HexahackProbeActionDto,
  HexahackProbeRequestDto,
  HexahackProbeType,
  HexahackRank,
  HexahackStatsDto,
  HexahackSubmissionActionDto,
  HexahackTodayDto
} from "../../types";

const RANK_LABELS: Readonly<Record<HexahackRank, string>> = {
  GHOST: "S · GHOST",
  SHADOW: "A · SHADOW",
  BREACH: "B · BREACH",
  TRACED: "C · TRACED"
};

const PROBE_LABELS: Readonly<Record<HexahackProbeType, string>> = {
  PING: "PING",
  BIT_SCAN: "BIT SCAN",
  LINK_TRACE: "LINK TRACE",
  CHECKSUM: "CHECKSUM"
};

const TUTORIAL_STORAGE_KEY = "huff.hexahack.tutorial.v2";

export function HexahackView({
  today,
  stats,
  busy,
  onProbe,
  onSubmit,
  onError,
  onComplete
}: {
  today: HexahackTodayDto;
  stats: HexahackStatsDto;
  busy: boolean;
  onProbe: (probe: HexahackProbeRequestDto) => Promise<HexahackProbeActionDto>;
  onSubmit: (request: { requestId: string; code: string }) => Promise<HexahackSubmissionActionDto>;
  onError: (message: string) => void;
  onComplete: (game: HexahackGameDto) => void;
}) {
  const game = today.game;
  const completed = game?.status === "COMPLETED";
  const [digits, setDigits] = React.useState(() => emptyCode());
  const [probeType, setProbeType] = React.useState<HexahackProbeType>("PING");
  const [position, setPosition] = React.useState(1);
  const [otherPosition, setOtherPosition] = React.useState(2);
  const [threshold, setThreshold] = React.useState(5);
  const [tutorialOpen, setTutorialOpen] = React.useState(() => localStorage.getItem(TUTORIAL_STORAGE_KEY) !== "done");
  const inputRefs = React.useRef<Array<HTMLInputElement | null>>([]);
  const currentStealth = game?.currentStealth ?? 100;
  const projectedRank = game?.projectedRank ?? "GHOST";

  React.useEffect(() => {
    if (completed && game.solution) setDigits(game.solution.split(""));
  }, [completed, game?.solution]);

  function setDigit(index: number, raw: string) {
    if (completed || busy) return;
    const digit = raw.replace(/\D/g, "").slice(-1);
    setDigits((current) => current.map((value, cell) => cell === index ? digit : value));
    if (digit && index < 5) inputRefs.current[index + 1]?.focus();
  }

  function handleCodeKey(event: React.KeyboardEvent<HTMLInputElement>, index: number) {
    if (event.key === "Backspace" && !digits[index] && index > 0) inputRefs.current[index - 1]?.focus();
    if (event.key === "ArrowLeft" && index > 0) inputRefs.current[index - 1]?.focus();
    if (event.key === "ArrowRight" && index < 5) inputRefs.current[index + 1]?.focus();
    if (event.key === "Enter") void submitCode();
  }

  async function runProbe() {
    try {
      await onProbe({
        requestId: requestId(),
        type: probeType,
        position,
        ...(probeType === "PING" ? { threshold } : {}),
        ...(probeType === "LINK_TRACE" || probeType === "CHECKSUM" ? { otherPosition } : {})
      });
    } catch (error) {
      onError(message(error, "Sonda non disponibile."));
    }
  }

  async function submitCode() {
    if (digits.some((digit) => !digit)) {
      onError("Inserisci tutte e sei le cifre del codice.");
      return;
    }
    try {
      const result = await onSubmit({ requestId: requestId(), code: digits.join("") });
      if (!result.result.granted) {
        setDigits(emptyCode());
        inputRefs.current[0]?.focus();
      } else {
        onComplete(result.game);
      }
    } catch (error) {
      onError(message(error, "Codice non valido."));
    }
  }

  async function share() {
    if (!game || game.status !== "COMPLETED") return;
    const probes = game.log.filter((entry) => entry.kind === "PROBE").length;
    const text = [
      `Hexahack · nodo ${game.puzzleDate}`,
      `${RANK_LABELS[game.rank ?? "TRACED"]} · Stealth ${game.finalStealth}`,
      `${"◆".repeat(Math.min(probes, 12))}${probes > 12 ? "+" : ""} ${probes} sonde · ${game.wrongSubmissions} errori`
    ].join("\n");
    try {
      if (navigator.share) await navigator.share({ title: "HexaQuot · Hexahack", text, url: `${window.location.origin}/#/hexahack` });
      else await navigator.clipboard.writeText(`${text}\n${window.location.origin}/#/hexahack`);
    } catch (error) {
      if (!(error instanceof DOMException && error.name === "AbortError")) onError("Condivisione non disponibile.");
    }
  }

  return (
    <section className="hack-shell" aria-label="Hexahack">
      <header className="hack-heading">
        <div><span className="hack-kicker">NODE/{today.puzzleDate}</span><h2>Hexahack</h2></div>
        <button className="tutorial-help" type="button" onClick={() => setTutorialOpen(true)} aria-label="Come si gioca"><CircleHelp size={20} /></button>
      </header>

      <div className="hack-status-grid" aria-label="Telemetria nodo">
        <div><span>Somma</span><strong>{today.freeClues.totalSum}</strong></div>
        <div><span>Cifre distinte</span><strong>{today.freeClues.distinctDigits}</strong></div>
        <div><span>Stealth</span><strong>{currentStealth}</strong></div>
        <div><span>Rango previsto</span><strong>{RANK_LABELS[projectedRank]}</strong></div>
      </div>
      <div className="hack-stealth-track" role="meter" aria-label={`Stealth ${currentStealth}`} aria-valuemin={Math.min(0, currentStealth)} aria-valuenow={currentStealth} aria-valuemax={100}>
        <span style={{ width: `${Math.max(0, Math.min(100, currentStealth))}%` }} />
      </div>

      <div className="hack-code" role="group" aria-label="Codice di sei cifre">
        {digits.map((digit, index) => (
          <label className="hack-code-cell" key={index}>
            <span>{index + 1}</span>
            <input
              ref={(element) => { inputRefs.current[index] = element; }}
              value={digit}
              onChange={(event) => setDigit(index, event.target.value)}
              onKeyDown={(event) => handleCodeKey(event, index)}
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={1}
              disabled={completed || busy}
              aria-label={`Cifra ${index + 1}`}
            />
          </label>
        ))}
      </div>

      {completed && game ? (
        <AccessGranted game={game} stats={stats} onShare={() => void share()} />
      ) : (
        <div className="hack-workspace">
          <section className="hack-console" aria-label="Console delle sonde">
            <header><Radio size={17} /><strong>SONDE</strong><span>Costo {game?.totalCost ?? 0}</span></header>
            <div className="hack-probe-controls">
              <div className="hack-probe-tabs" role="tablist" aria-label="Tipo di sonda">
                {(Object.keys(PROBE_LABELS) as HexahackProbeType[]).map((type) => (
                  <button type="button" role="tab" aria-selected={probeType === type} className={probeType === type ? "selected" : ""} onClick={() => setProbeType(type)} key={type}>
                    {PROBE_LABELS[type]} <small>+{type === "CHECKSUM" ? 2 : 1}</small>
                  </button>
                ))}
              </div>
              <div className="hack-probe-params">
                <NumberSelect label="Posizione" value={position} values={[1, 2, 3, 4, 5, 6]} onChange={setPosition} />
                {probeType === "PING" ? <NumberSelect label="Soglia" value={threshold} values={[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]} onChange={setThreshold} /> : null}
                {probeType === "LINK_TRACE" || probeType === "CHECKSUM" ? (
                  <NumberSelect label="Seconda" value={otherPosition} values={[1, 2, 3, 4, 5, 6]} onChange={setOtherPosition} />
                ) : null}
                <button className="hack-execute" type="button" disabled={busy || otherPosition === position && (probeType === "LINK_TRACE" || probeType === "CHECKSUM")} onClick={() => void runProbe()}>
                  Esegui <ChevronRight size={16} />
                </button>
              </div>
            </div>
          </section>

          <section className="hack-log" aria-label="Log persistente">
            <header><Activity size={16} /><strong>LOG</strong><span>{game?.log.length ?? 0} eventi</span></header>
            <div aria-live="polite">
              {game?.log.map((entry) => <LogLine entry={entry} key={entry.sequence} />)}
              {!game?.log.length ? <p><span>000</span> In attesa di una decisione…</p> : null}
            </div>
          </section>
        </div>
      )}

      {!completed ? <button className="hack-inject" type="button" disabled={busy || digits.some((digit) => !digit)} onClick={() => void submitCode()}>
        <ShieldCheck size={19} /> INJECT CODE
      </button> : null}

      <MasteryStats stats={stats} />
      {tutorialOpen ? <Tutorial onClose={() => { localStorage.setItem(TUTORIAL_STORAGE_KEY, "done"); setTutorialOpen(false); }} /> : null}
    </section>
  );
}

function NumberSelect({ label, value, values, onChange }: { label: string; value: number; values: readonly number[]; onChange: (value: number) => void }) {
  return <label><span>{label}</span><select value={value} onChange={(event) => onChange(Number(event.target.value))}>{values.map((option) => <option value={option} key={option}>{option}</option>)}</select></label>;
}

function LogLine({ entry }: { entry: NonNullable<HexahackGameDto["log"]>[number] }) {
  let line = "";
  if (entry.probe) line = `${PROBE_LABELS[entry.probe.type]} · ${entry.probe.summary}`;
  if (entry.submission) line = entry.submission.granted
    ? "INJECT CODE · ACCESS GRANTED"
    : `INJECT CODE · ${entry.submission.correctPositions}/6 posizioni corrette`;
  return <p><span>{String(entry.sequence).padStart(3, "0")}</span>{line}</p>;
}

function AccessGranted({ game, stats, onShare }: { game: HexahackGameDto; stats: HexahackStatsDto; onShare: () => void }) {
  const statsIncludeCurrent = stats.last30Nodes.some((node) => node.puzzleDate === game.puzzleDate && node.completed);
  const previousGames = Math.max(0, stats.completedAccesses - (statsIncludeCurrent ? 1 : 0));
  const previousAverage = previousGames === 0 || game.finalStealth === null ? null
    : statsIncludeCurrent
      ? (stats.averageStealth * stats.completedAccesses - game.finalStealth) / previousGames
      : stats.averageStealth;
  const comparison = previousAverage === null || game.finalStealth === null ? "Primo accesso registrato"
    : game.finalStealth >= previousAverage ? `+${Math.round(game.finalStealth - previousAverage)} sulla tua media`
      : `${Math.round(game.finalStealth - previousAverage)} sotto la tua media`;
  return <section className="hack-granted" aria-live="polite">
    <span>ACCESS GRANTED</span>
    <h3>{RANK_LABELS[game.rank ?? "TRACED"]}</h3>
    <div><strong>{game.finalStealth}</strong><small>STEALTH</small></div>
    <p>{game.log.length} decisioni · costo {game.totalCost} · {game.wrongSubmissions} errori</p>
    <p>{comparison}</p>
    <button type="button" onClick={onShare}><Share2 size={17} /> Condividi senza spoiler</button>
  </section>;
}

function MasteryStats({ stats }: { stats: HexahackStatsDto }) {
  return <section className="hack-mastery" aria-label="Maestria Hexahack">
    <header><strong>MAESTRIA</strong><span>ultimi 30 nodi</span></header>
    <div className="hack-metrics">
      <div><strong>{stats.completedAccesses}</strong><span>Accessi</span></div>
      <div><strong>{stats.averageStealth}</strong><span>Media</span></div>
      <div><strong>{stats.bestStealth}</strong><span>Migliore</span></div>
      <div><strong>{stats.currentStreak}</strong><span>Serie</span></div>
    </div>
    <div className="hack-ranks" aria-label="Distribuzione ranghi">
      {(Object.keys(RANK_LABELS) as HexahackRank[]).map((rank) => <span key={rank}>{RANK_LABELS[rank]} <strong>{stats.rankDistribution[rank] ?? 0}</strong></span>)}
    </div>
    <div className="hack-calendar">
      {stats.last30Nodes.map((node) => <span className={node.completed ? `rank-${node.rank?.toLowerCase()}` : "empty"} title={`${node.puzzleDate}${node.completed ? ` · ${node.rank} · ${node.stealth}` : " · non completato"}`} aria-label={`${node.puzzleDate}${node.completed ? `, rango ${node.rank}, Stealth ${node.stealth}` : ", non completato"}`} key={node.puzzleDate} />)}
    </div>
  </section>;
}

function Tutorial({ onClose }: { onClose: () => void }) {
  const [step, setStep] = React.useState(0);
  const steps = [
    ["OBIETTIVO", "Ogni giorno devi ricostruire un codice di sei cifre. Somma totale e numero di cifre distinte sono indizi gratuiti: usali per escludere combinazioni."],
    ["PING · costo 1", "Scegli una posizione e una soglia: PING risponde se la cifra è minore, uguale o maggiore. Ad esempio, “posizione 1 = 2” fissa subito quella cifra."],
    ["BIT SCAN · costo 1", "BIT SCAN rivela soltanto la parità della cifra scelta: pari o dispari. È utile per dimezzare le possibilità quando non ti serve un valore esatto."],
    ["LINK TRACE · costo 1", "Confronta due posizioni diverse: scopri se la prima cifra è minore, uguale o maggiore della seconda. Combina questi rapporti con PING e BIT SCAN."],
    ["CHECKSUM · costo 2", "Somma due posizioni diverse. Per esempio, se 1 + 2 = 5 e PING conferma che la posizione 1 è 2, allora la posizione 2 è 3."],
    ["INJECT CODE", "Inserisci le sei cifre quando hai una soluzione. Un tentativo errato indica quante posizioni sono esatte, ma costa 5 Stealth; ogni punto costo delle sonde ne sottrae 2. Meno costo ed errori significano un rango migliore."]
  ] as const;
  const trainingCode = step >= 4 ? ["2", "3", "?", "?", "?", "?"] : step >= 1 ? ["2", "?", "?", "?", "?", "?"] : ["?", "?", "?", "?", "?", "?"];
  return createPortal(<div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
    <section className="modal hack-tutorial" role="dialog" aria-modal="true" aria-labelledby="hack-tutorial-title" onMouseDown={(event) => event.stopPropagation()}>
      <header className="modal-head">
        <h2 id="hack-tutorial-title">Come si gioca a Hexahack</h2>
        <button className="close-button" type="button" onClick={onClose} aria-label="Chiudi"><X size={19} /></button>
      </header>
      <span className="hack-kicker">TRAINING NODE · {step + 1}/{steps.length}</span>
      <div className="hack-tutorial-code" aria-hidden="true">{trainingCode.map((digit, index) => <i key={index}>{digit}</i>)}</div>
      <strong>{steps[step][0]}</strong><p>{steps[step][1]}</p>
      <button className="hack-execute" type="button" onClick={() => step === steps.length - 1 ? onClose() : setStep(step + 1)}>{step === steps.length - 1 ? "Entra nel nodo" : "Continua"} <ChevronRight size={16} /></button>
    </section>
  </div>, document.body);
}

function emptyCode() { return Array.from({ length: 6 }, () => ""); }
function requestId() { return crypto.randomUUID(); }
function message(error: unknown, fallback: string) { return error instanceof Error ? error.message : fallback; }
