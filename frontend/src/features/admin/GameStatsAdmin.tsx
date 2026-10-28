import React from "react";
import { Bot, CornerDownLeft, LoaderCircle, Send, UserRound, X } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { ApiError, api } from "../../api";
import type { AdminGameStatsUserDto } from "../../types";

type ChatMessage = { role: "user" | "assistant"; content: string; users?: readonly string[] };
type Mention = { start: number; end: number; query: string };
const MAX_USERS = 10;

function activeMention(value: string, caret: number): Mention | null {
  const beforeCaret = value.slice(0, caret);
  const match = beforeCaret.match(/(^|\s)@([a-zA-Z0-9._-]*)$/);
  if (!match) return null;
  const start = caret - match[0].length + match[1].length;
  return { start, end: caret, query: match[2] };
}

export function GameStatsAdmin({ onAuthRequired, onError }: {
  onAuthRequired: (error: unknown) => boolean;
  onError: (message: string) => void;
}) {
  const [message, setMessage] = React.useState("");
  const [selected, setSelected] = React.useState<AdminGameStatsUserDto[]>([]);
  const [mention, setMention] = React.useState<Mention | null>(null);
  const [activeSuggestion, setActiveSuggestion] = React.useState(0);
  const [messages, setMessages] = React.useState<ChatMessage[]>([]);
  const [streaming, setStreaming] = React.useState(false);
  const [threadId] = React.useState(() => crypto.randomUUID());
  const textarea = React.useRef<HTMLTextAreaElement>(null);
  const abort = React.useRef<AbortController | null>(null);
  const callbacks = React.useRef({ onAuthRequired, onError });
  callbacks.current = { onAuthRequired, onError };
  const suggestions = useQuery({
    queryKey: ["admin", "game-stats", "users", mention?.query ?? ""],
    queryFn: () => api.adminGameStatsUsers(mention?.query ?? "").then((result) => result.users),
    enabled: mention !== null && selected.length < MAX_USERS,
    staleTime: 15_000
  });
  const options = (suggestions.data ?? []).filter((user) => !selected.some((item) => item.nickname === user.nickname));

  React.useEffect(() => {
    if (suggestions.error) {
      if (!callbacks.current.onAuthRequired(suggestions.error)) callbacks.current.onError(suggestions.error instanceof Error ? suggestions.error.message : "Ricerca utenti non riuscita.");
    }
  }, [suggestions.error]);

  React.useEffect(() => () => abort.current?.abort(), []);

  function updateMention(value: string, caret: number) {
    setMention(activeMention(value, caret));
    setActiveSuggestion(0);
  }

  function choose(user: AdminGameStatsUserDto) {
    if (!mention || selected.length >= MAX_USERS) return;
    const next = `${message.slice(0, mention.start)}${message.slice(mention.end)}`;
    const caret = mention.start;
    setMessage(next);
    setSelected((current) => [...current, user]);
    setMention(null);
    requestAnimationFrame(() => {
      textarea.current?.focus();
      textarea.current?.setSelectionRange(caret, caret);
    });
  }

  function onKeyDown(event: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (mention && options.length > 0) {
      if (event.key === "ArrowDown") { event.preventDefault(); setActiveSuggestion((index) => (index + 1) % options.length); return; }
      if (event.key === "ArrowUp") { event.preventDefault(); setActiveSuggestion((index) => (index - 1 + options.length) % options.length); return; }
      if (event.key === "Escape") { setMention(null); return; }
      if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); choose(options[activeSuggestion]); return; }
    }
    if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); void send(); }
  }

  async function send() {
    const prompt = message.trim();
    if (!prompt || streaming) return;
    const taggedUsers = selected.map((user) => user.nickname);
    setMessages((current) => [...current, { role: "user", content: prompt, users: taggedUsers }, { role: "assistant", content: "" }]);
    setMessage("");
    setSelected([]);
    setMention(null);
    setStreaming(true);
    const controller = new AbortController();
    abort.current = controller;
    try {
      await api.streamAdminGameStats({ message: prompt, threadId, users: taggedUsers }, (token) => {
        setMessages((current) => current.map((item, index) => index === current.length - 1 ? { ...item, content: item.content + token } : item));
      }, controller.signal);
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

  return <section className="game-stats-admin" aria-label="Statistiche AI">
    <header className="game-stats-header">
      <div className="game-stats-icon"><Bot size={22} /></div>
      <div><p className="cms-kicker">Analisi dei giochi</p><h2>Statistiche AI</h2><p>Chiedi andamenti, classifiche e confronti. Usa @ per selezionare fino a 10 utenti.</p></div>
    </header>
    <div className="game-stats-transcript" aria-live="polite">
      {messages.length === 0 ? <div className="game-stats-empty"><Bot size={25} /><strong>Da dove vuoi iniziare?</strong><span>Per esempio: “Quante partite sono state vinte questo mese?”</span></div> : null}
      {messages.map((item, index) => <article className={`game-stats-message ${item.role}`} key={`${index}-${item.role}`}>
        <span className="game-stats-avatar">{item.role === "assistant" ? <Bot size={17} /> : <UserRound size={17} />}</span>
        <div className="game-stats-bubble">
          {item.users?.length ? <div className="game-stats-inline-tags">{item.users.map((user) => <span key={user}>{user}</span>)}</div> : null}
          <p>{item.content || (streaming && index === messages.length - 1 ? <span className="game-stats-cursor" /> : null)}</p>
        </div>
      </article>)}
    </div>
    <form className="game-stats-composer" onSubmit={(event) => { event.preventDefault(); void send(); }}>
      {selected.length ? <div className="game-stats-tags" aria-label="Utenti selezionati">{selected.map((user) => <span className="game-stats-tag" key={user.nickname}><span>@{user.nickname.replace(/^@/, "")}</span><button type="button" aria-label={`Rimuovi ${user.nickname}`} onClick={() => setSelected((current) => current.filter((item) => item.nickname !== user.nickname))}><X size={13} /></button></span>)}</div> : null}
      <div className="game-stats-input-row">
        <textarea ref={textarea} value={message} onChange={(event) => { setMessage(event.target.value); updateMention(event.target.value, event.target.selectionStart); }} onClick={(event) => updateMention(event.currentTarget.value, event.currentTarget.selectionStart)} onKeyDown={onKeyDown} placeholder="Chiedi qualcosa sulle statistiche… digita @ per scegliere utenti" rows={2} maxLength={4000} disabled={streaming} />
        <button className="game-stats-send" type="submit" disabled={streaming || !message.trim()} aria-label="Invia domanda">{streaming ? <LoaderCircle className="spin" size={18} /> : <Send size={18} />}</button>
      </div>
      {mention && selected.length < MAX_USERS ? <div className="game-stats-suggestions" role="listbox" aria-label="Utenti">
        {suggestions.isFetching && !suggestions.data ? <div className="game-stats-suggestion-hint">Cerco utenti…</div> : null}
        {!suggestions.isFetching && options.length === 0 ? <div className="game-stats-suggestion-hint">Nessun utente trovato.</div> : null}
        {options.slice(0, 10).map((user, index) => <button type="button" role="option" aria-selected={index === activeSuggestion} className={index === activeSuggestion ? "active" : ""} key={user.nickname} onMouseDown={(event) => event.preventDefault()} onClick={() => choose(user)}>
          <span className="game-stats-suggestion-avatar">{(user.displayName || user.nickname).slice(0, 1).toUpperCase()}</span><span><strong>{user.displayName || "Giocatore"}</strong><small>@{user.nickname.replace(/^@/, "")}</small></span><CornerDownLeft size={15} />
        </button>)}
      </div> : null}
      <div className="game-stats-composer-foot"><span>{selected.length}/{MAX_USERS} utenti</span><span>Invio per inviare · Shift+Invio per andare a capo</span></div>
    </form>
  </section>;
}
