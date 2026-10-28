import React from "react";
import { Bot, CalendarDays, ChevronDown, CornerDownLeft, History, LoaderCircle, MessageSquare, Plus, Send, Trash2, UserRound, X } from "lucide-react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { ApiError, api } from "../../api";
import type { AdminGameStatsFiltersDto, AdminGameStatsGame, AdminGameStatsUserDto } from "../../types";

type ChatMessage = { role: "user" | "assistant"; content: string; users?: readonly string[]; filters?: AdminGameStatsFiltersDto };
type Mention = { type: "user" | "game"; start: number; end: number; query: string };
const MAX_USERS = 10;
const GAME_OPTIONS: readonly { id: AdminGameStatsGame; label: string }[] = [
  { id: "hexaword", label: "HexaWord" }, { id: "hexahack", label: "HexaHack" },
  { id: "hexasky", label: "HexaSky" }, { id: "hexaflow", label: "HexaFlow" },
  { id: "hexastar", label: "HexaStar" }, { id: "hexaeco", label: "HexaEco" },
  { id: "hexatrap", label: "HexaTrap" },
];
const PROMPT_SUGGESTIONS = [
  "Quante partite sono state giocate in questo periodo?",
  "Qual è il tasso di vittoria per ciascun gioco?",
  "Quali giochi hanno registrato più partite questa settimana?",
  "Mostrami la classifica dei dieci giocatori con più vittorie.",
  "Come sono cambiate le partite giocate giorno per giorno?",
  "Quale gioco ha il maggior numero di partite completate?",
  "Confronta il numero di vittorie tra i giochi.",
  "Quali sono gli andamenti delle partite nell’ultimo mese?",
  "Quante partite sono state iniziate ma non completate?",
  "Mostrami la percentuale di completamento per gioco.",
  "Qual è il giorno della settimana con più partite?",
  "Quali giocatori hanno migliorato di più il proprio tasso di vittoria?",
  "Confronta le statistiche degli utenti selezionati.",
  "Come si distribuiscono le vittorie tra i giocatori?",
  "Quali giochi hanno avuto più attività nel fine settimana?",
  "Mostrami la classifica per numero di partite completate.",
  "In quali periodi si nota un aumento delle partite giocate?",
  "Quante vittorie e sconfitte ci sono state per ogni gioco?",
  "Quale gioco ha il tasso di completamento più alto?",
  "Confronta l’attività di questa settimana con quella precedente.",
  "Quali utenti hanno giocato più spesso nel periodo selezionato?",
  "Mostrami l’andamento settimanale delle vittorie.",
  "Ci sono differenze nel tasso di vittoria tra i giochi?",
  "Riassumi i risultati principali del periodo selezionato.",
];

function activeMention(value: string, caret: number): Mention | null {
  const beforeCaret = value.slice(0, caret);
  const match = beforeCaret.match(/(^|\s)([@#])([a-zA-Z0-9._-]*)$/);
  if (!match) return null;
  const start = caret - match[0].length + match[1].length;
  return { type: match[2] === "@" ? "user" : "game", start, end: caret, query: match[3] };
}

export function GameStatsAdmin({ adminId, onAuthRequired, onError }: {
  adminId: string;
  onAuthRequired: (error: unknown) => boolean;
  onError: (message: string) => void;
}) {
  const [message, setMessage] = React.useState("");
  const [selected, setSelected] = React.useState<AdminGameStatsUserDto[]>([]);
  const [mention, setMention] = React.useState<Mention | null>(null);
  const [activeSuggestion, setActiveSuggestion] = React.useState(0);
  const [messages, setMessages] = React.useState<ChatMessage[]>([]);
  const [promptSuggestionIndex, setPromptSuggestionIndex] = React.useState(0);
  const [promptSuggestionPhase, setPromptSuggestionPhase] = React.useState<"enter" | "exit">("enter");
  const [historyExpanded, setHistoryExpanded] = React.useState(false);
  const [selectedGames, setSelectedGames] = React.useState<AdminGameStatsGame[]>([]);
  const [from, setFrom] = React.useState("");
  const [to, setTo] = React.useState("");
  const [streaming, setStreaming] = React.useState(false);
  const [threadId, setThreadId] = React.useState<string>(() => crypto.randomUUID());
  const [loadingThread, setLoadingThread] = React.useState(false);
  const textarea = React.useRef<HTMLTextAreaElement>(null);
  const abort = React.useRef<AbortController | null>(null);
  const callbacks = React.useRef({ onAuthRequired, onError });
  callbacks.current = { onAuthRequired, onError };
  const queryClient = useQueryClient();
  const threadQueryKey = ["admin", "game-stats", "threads", adminId];
  const suggestions = useQuery({
    queryKey: ["admin", "game-stats", "users", mention?.type === "user" ? mention.query : ""],
    queryFn: () => api.adminGameStatsUsers(mention?.type === "user" ? mention.query : "").then((result) => result.users),
    enabled: mention?.type === "user" && selected.length < MAX_USERS,
    staleTime: 15_000
  });
  const threads = useQuery({
    queryKey: threadQueryKey,
    queryFn: () => api.adminGameStatsThreads().then((result) => result.threads)
  });
  const options = (suggestions.data ?? []).filter((user) => !selected.some((item) => item.nickname === user.nickname));
  const gameOptions = GAME_OPTIONS.filter((game) => !selectedGames.includes(game.id)
    && `${game.id} ${game.label}`.toLowerCase().includes(mention?.query.toLowerCase() ?? ""));

  React.useEffect(() => {
    if (suggestions.error) {
      if (!callbacks.current.onAuthRequired(suggestions.error)) callbacks.current.onError(suggestions.error instanceof Error ? suggestions.error.message : "Ricerca utenti non riuscita.");
    }
  }, [suggestions.error]);

  React.useEffect(() => {
    if (threads.error) {
      if (!callbacks.current.onAuthRequired(threads.error)) callbacks.current.onError(threads.error instanceof Error ? threads.error.message : "Storico conversazioni non disponibile.");
    }
  }, [threads.error]);

  React.useEffect(() => () => abort.current?.abort(), []);

  React.useEffect(() => {
    if (messages.length > 0) return;
    let holdTimeout = 0;
    let transitionTimeout = 0;
    const scheduleNext = () => {
      holdTimeout = window.setTimeout(() => {
        setPromptSuggestionPhase("exit");
        transitionTimeout = window.setTimeout(() => {
          setPromptSuggestionIndex((index) => (index + 1) % PROMPT_SUGGESTIONS.length);
          setPromptSuggestionPhase("enter");
          scheduleNext();
        }, 220);
      }, 5_260);
    };
    scheduleNext();
    return () => {
      window.clearTimeout(holdTimeout);
      window.clearTimeout(transitionTimeout);
    };
  }, [messages.length]);

  function updateMention(value: string, caret: number) {
    setMention(activeMention(value, caret));
    setActiveSuggestion(0);
  }

  function choose(option: AdminGameStatsUserDto | (typeof GAME_OPTIONS)[number]) {
    if (!mention) return;
    const next = `${message.slice(0, mention.start)}${message.slice(mention.end)}`;
    const caret = mention.start;
    setMessage(next);
    if (mention.type === "user" && "nickname" in option && selected.length < MAX_USERS) {
      setSelected((current) => [...current, option]);
    } else if (mention.type === "game" && "id" in option) {
      setSelectedGames((current) => current.includes(option.id) ? current : [...current, option.id]);
    }
    setMention(null);
    requestAnimationFrame(() => {
      textarea.current?.focus();
      textarea.current?.setSelectionRange(caret, caret);
    });
  }

  function onKeyDown(event: React.KeyboardEvent<HTMLTextAreaElement>) {
    const choicesCount = mention?.type === "user" ? options.length : gameOptions.length;
    if (mention && choicesCount > 0) {
      if (event.key === "ArrowDown") { event.preventDefault(); setActiveSuggestion((index) => (index + 1) % choicesCount); return; }
      if (event.key === "ArrowUp") { event.preventDefault(); setActiveSuggestion((index) => (index - 1 + choicesCount) % choicesCount); return; }
      if (event.key === "Escape") { setMention(null); return; }
      if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        if (mention.type === "user") choose(options[activeSuggestion]);
        else choose(gameOptions[activeSuggestion]);
        return;
      }
    }
    if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); void send(); }
  }

  async function openThread(id: string) {
    if (streaming || loadingThread || id === threadId) return;
    setLoadingThread(true);
    try {
      const conversation = await api.adminGameStatsThread(id);
      setThreadId(id);
      setMessages(conversation.messages.map((item) => ({ role: item.role, content: item.content, filters: item.filters })));
      setMessage("");
      setSelected([]);
      setSelectedGames([]);
      setFrom("");
      setTo("");
      setMention(null);
    } catch (error) {
      if (!onAuthRequired(error)) onError(error instanceof ApiError ? error.message : "Conversazione non disponibile.");
      void queryClient.invalidateQueries({ queryKey: threadQueryKey });
    } finally {
      setLoadingThread(false);
    }
  }

  function newThread() {
    if (streaming || loadingThread) return;
    setThreadId(crypto.randomUUID());
    setMessages([]);
    setMessage("");
    setSelected([]);
    setSelectedGames([]);
    setFrom("");
    setTo("");
    setMention(null);
  }

  async function deleteThread(id: string) {
    if (streaming || loadingThread) return;
    try {
      await api.deleteAdminGameStatsThread(id);
      if (id === threadId) newThread();
      await queryClient.invalidateQueries({ queryKey: threadQueryKey });
    } catch (error) {
      if (!onAuthRequired(error)) onError(error instanceof ApiError ? error.message : "Eliminazione conversazione non riuscita.");
    }
  }

  async function send() {
    const prompt = message.trim();
    if (!prompt || streaming || loadingThread) return;
    const taggedUsers = selected.map((user) => user.nickname);
    const filters: AdminGameStatsFiltersDto = { users: taggedUsers, games: selectedGames, from: from || null, to: to || null };
    setMessages((current) => [...current, { role: "user", content: prompt, users: taggedUsers, filters }, { role: "assistant", content: "" }]);
    setMessage("");
    setSelected([]);
    setMention(null);
    setStreaming(true);
    const controller = new AbortController();
    abort.current = controller;
    try {
      await api.streamAdminGameStats({ message: prompt, threadId, users: taggedUsers, games: selectedGames, from: from || undefined, to: to || undefined }, (token) => {
        setMessages((current) => current.map((item, index) => index === current.length - 1 ? { ...item, content: item.content + token } : item));
      }, controller.signal);
      void queryClient.invalidateQueries({ queryKey: threadQueryKey });
    } catch (error) {
      if (!(error instanceof DOMException && error.name === "AbortError")) {
        if (!onAuthRequired(error)) onError(error instanceof ApiError ? error.message : "Risposta delle statistiche non disponibile.");
        setMessages((current) => current.map((item, index) => index === current.length - 1 && !item.content ? { ...item, content: "Non sono riuscito a completare la richiesta." } : item));
      }
    } finally {
      abort.current = null;
      setStreaming(false);
    }
  }

  return <section className="game-stats-layout" aria-label="Statistiche AI">
    <aside className="game-stats-history" aria-label="Storico conversazioni">
      <div className="game-stats-history-heading">
        <button type="button" className="game-stats-history-toggle" onClick={() => setHistoryExpanded((expanded) => !expanded)} aria-expanded={historyExpanded}>
          <span><History size={17} /> Conversazioni</span><ChevronDown className={historyExpanded ? "expanded" : ""} size={16} />
        </button>
        <button className="game-stats-history-new" type="button" onClick={newThread} disabled={streaming || loadingThread} aria-label="Nuova conversazione" title="Nuova conversazione"><Plus size={17} /></button>
      </div>
      {historyExpanded ? <div className="game-stats-history-panel">
        {threads.isFetching && !threads.data ? <div className="game-stats-history-empty">Carico lo storico…</div> : null}
        {!threads.isFetching && (threads.data?.length ?? 0) === 0 ? <div className="game-stats-history-empty">Le chat salvate appariranno qui.</div> : null}
        <div className="game-stats-history-list">
          {threads.data?.map((thread) => <div className={`game-stats-history-item ${thread.id === threadId ? "selected" : ""}`} key={thread.id}>
            <button type="button" className="game-stats-history-open" onClick={() => void openThread(thread.id)} disabled={streaming || loadingThread}>
              <MessageSquare size={15} /><span><strong>{thread.title || "Conversazione"}</strong><small>{thread.preview || "Nessun messaggio"}</small></span>
            </button>
            <button type="button" className="game-stats-history-delete" onClick={() => void deleteThread(thread.id)} disabled={streaming || loadingThread} aria-label={`Elimina conversazione ${thread.title}`} title="Elimina conversazione"><Trash2 size={14} /></button>
          </div>)}
        </div>
      </div> : null}
    </aside>
    <div className="game-stats-main">
      <header className="game-stats-header">
        <div className="game-stats-icon"><Bot size={22} /></div>
        <div><p className="cms-kicker">Analisi dei giochi</p><h2>Statistiche AI</h2><p>Filtra per giocatore con @, per gioco con # e scegli il periodo.</p></div>
      </header>
      <div className="game-stats-transcript" aria-live="polite" aria-busy={loadingThread}>
        {loadingThread ? <div className="game-stats-empty"><LoaderCircle className="spin" size={22} /><span>Ripristino conversazione…</span></div> : null}
        {!loadingThread && messages.length === 0 ? <div className="game-stats-empty"><Bot size={25} /><strong>Da dove vuoi iniziare?</strong>
          <div className="game-stats-prompt-carousel" aria-label="Esempi di domande">
            <button key={promptSuggestionIndex} className={`game-stats-prompt-suggestion ${promptSuggestionPhase}`} type="button" aria-label={`Inserisci: ${PROMPT_SUGGESTIONS[promptSuggestionIndex]}`} onClick={() => {
              setMessage(PROMPT_SUGGESTIONS[promptSuggestionIndex]);
              setMention(null);
              requestAnimationFrame(() => textarea.current?.focus());
            }}>
              <span>{PROMPT_SUGGESTIONS[promptSuggestionIndex]}</span>
            </button>
          </div>
        </div> : null}
        {!loadingThread && messages.map((item, index) => <article className={`game-stats-message ${item.role}`} key={`${index}-${item.role}`}>
          <span className="game-stats-avatar">{item.role === "assistant" ? <Bot size={17} /> : <UserRound size={17} />}</span>
          <div className="game-stats-bubble">
            {(item.users?.length ?? item.filters?.users.length ?? 0) > 0 ? <div className="game-stats-inline-tags">{(item.users ?? item.filters?.users ?? []).map((user) => <span key={user}>{user}</span>)}</div> : null}
            {item.filters && (item.filters.games.length > 0 || item.filters.from || item.filters.to) ? <div className="game-stats-inline-tags">
              {item.filters.games.map((game) => <span key={game}>#{game}</span>)}
              {item.filters.from || item.filters.to ? <span>{item.filters.from || "…"} → {item.filters.to || "…"}</span> : null}
            </div> : null}
            {item.content ? item.role === "assistant"
              ? <div className="game-stats-markdown"><ReactMarkdown remarkPlugins={[remarkGfm]}>{item.content}</ReactMarkdown></div>
              : <p>{item.content}</p>
              : streaming && index === messages.length - 1 ? <span className="game-stats-cursor" /> : null}
          </div>
        </article>)}
      </div>
      <form className="game-stats-composer" onSubmit={(event) => { event.preventDefault(); void send(); }}>
        {selected.length || selectedGames.length ? <div className="game-stats-tags" aria-label="Filtri selezionati">
          {selected.map((user) => <span className="game-stats-tag" key={user.nickname}><span>@{user.nickname.replace(/^@/, "")}</span><button type="button" aria-label={`Rimuovi ${user.nickname}`} onClick={() => setSelected((current) => current.filter((item) => item.nickname !== user.nickname))}><X size={13} /></button></span>)}
          {selectedGames.map((game) => <span className="game-stats-tag" key={game}><span>#{game}</span><button type="button" aria-label={`Rimuovi ${game}`} onClick={() => setSelectedGames((current) => current.filter((item) => item !== game))}><X size={13} /></button></span>)}
        </div> : null}
        <div className="game-stats-period" aria-label="Periodo della ricerca">
          <span><CalendarDays size={15} /> Periodo</span>
          <label>Da <input type="date" value={from} max={to || undefined} onChange={(event) => { const value = event.target.value; setFrom(value); if (to && value > to) setTo(value); }} /></label>
          <label>A <input type="date" value={to} min={from || undefined} onChange={(event) => { const value = event.target.value; setTo(value); if (from && value < from) setFrom(value); }} /></label>
          <small>Stessa data in entrambi i campi per un solo giorno</small>
        </div>
        <div className="game-stats-input-row">
        <textarea ref={textarea} value={message} onChange={(event) => { setMessage(event.target.value); updateMention(event.target.value, event.target.selectionStart); }} onClick={(event) => updateMention(event.currentTarget.value, event.currentTarget.selectionStart)} onKeyDown={onKeyDown} placeholder="Chiedi qualcosa… digita @ per gli utenti o # per i giochi" rows={2} maxLength={4000} disabled={streaming || loadingThread} />
          <button className="game-stats-send" type="submit" disabled={streaming || loadingThread || !message.trim()} aria-label="Invia domanda">{streaming ? <LoaderCircle className="spin" size={18} /> : <Send size={18} />}</button>
        </div>
        {mention && ((mention.type === "user" && selected.length < MAX_USERS) || (mention.type === "game" && selectedGames.length < GAME_OPTIONS.length)) ? <div className="game-stats-suggestions" role="listbox" aria-label={mention.type === "user" ? "Utenti" : "Giochi"}>
          {mention.type === "user" && suggestions.isFetching && !suggestions.data ? <div className="game-stats-suggestion-hint">Cerco utenti…</div> : null}
          {mention.type === "user" && !suggestions.isFetching && options.length === 0 ? <div className="game-stats-suggestion-hint">Nessun utente trovato.</div> : null}
          {mention.type === "game" && gameOptions.length === 0 ? <div className="game-stats-suggestion-hint">Nessun gioco trovato.</div> : null}
          {mention.type === "user" ? options.slice(0, 10).map((user, index) => <button type="button" role="option" aria-selected={index === activeSuggestion} className={index === activeSuggestion ? "active" : ""} key={user.nickname} onMouseDown={(event) => event.preventDefault()} onClick={() => choose(user)}>
            <span className="game-stats-suggestion-avatar">{(user.displayName || user.nickname).slice(0, 1).toUpperCase()}</span><span><strong>{user.displayName || "Giocatore"}</strong><small>@{user.nickname.replace(/^@/, "")}</small></span><CornerDownLeft size={15} />
          </button>) : gameOptions.map((game, index) => <button type="button" role="option" aria-selected={index === activeSuggestion} className={index === activeSuggestion ? "active" : ""} key={game.id} onMouseDown={(event) => event.preventDefault()} onClick={() => choose(game)}>
            <span className="game-stats-suggestion-avatar">#</span><span><strong>{game.label}</strong><small>#{game.id}</small></span><CornerDownLeft size={15} />
          </button>)}
        </div> : null}
        <div className="game-stats-composer-foot"><span>{selected.length}/{MAX_USERS} utenti · {selectedGames.length}/{GAME_OPTIONS.length} giochi</span><span>Invio per inviare · Shift+Invio per andare a capo</span></div>
      </form>
    </div>
  </section>;
}
