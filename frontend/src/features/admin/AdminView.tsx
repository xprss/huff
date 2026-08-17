import React from "react";
import { createPortal } from "react-dom";
import { Check, ChevronLeft, ChevronRight, Eye, EyeOff, Search, Save, Trash2, X } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../api";
import { PROFILE_EMOJIS } from "../../app/constants";
import { adminPlayerQueryOptions, adminPlayersQueryOptions } from "../../app/queries";
import { Distribution } from "../../shared/components/Distribution";
import { LoadingSpinner } from "../../shared/components/LoadingSpinner";
import { Metric } from "../../shared/components/Metric";
import type {
  AdminGameDto,
  AdminHexahackGameDto,
  AdminPlayerDetailDto,
  AdminPlayerSortDto,
  AdminPlayerSummaryDto,
  AdminPlayerUpdateDto
} from "../../types";

type ConfirmAction = { readonly kind: "save" } | { readonly kind: "delete"; readonly typedValue: string };

const adminPlayerSortOptions: readonly { readonly value: AdminPlayerSortDto; readonly label: string }[] = [
  { value: "alphabetical", label: "Alfabetico" },
  { value: "recent-game", label: "Partita più recente" },
  { value: "games-played", label: "Più partite giocate" }
];
const ADMIN_SEARCH_DEBOUNCE_MS = 350;

export function AdminView({
  canManagePlayers,
  onAuthRequired,
  onSuccess,
  onError
}: {
  canManagePlayers: boolean;
  onAuthRequired: (error: unknown) => boolean;
  onSuccess: (message: string) => void;
  onError: (message: string) => void;
}) {
  const queryClient = useQueryClient();
  const [searchInput, setSearchInput] = React.useState("");
  const [search, setSearch] = React.useState("");
  const [playerSort, setPlayerSort] = React.useState<AdminPlayerSortDto>("alphabetical");
  const [page, setPage] = React.useState(0);
  const [selectedUserId, setSelectedUserId] = React.useState<string | null>(null);
  const [confirmAction, setConfirmAction] = React.useState<ConfirmAction | null>(null);
  const playersQuery = useQuery(adminPlayersQueryOptions(search, playerSort, page));
  const detailQuery = useQuery({
    ...adminPlayerQueryOptions(selectedUserId ?? ""),
    enabled: Boolean(selectedUserId)
  });
  const detail = detailQuery.data ?? null;
  const [draft, setDraft] = React.useState<AdminPlayerUpdateDto | null>(null);

  const updateMutation = useMutation({
    mutationFn: ({ userId, update }: { userId: string; update: AdminPlayerUpdateDto }) =>
      api.updateAdminPlayer(userId, update),
    onSuccess: (updated) => {
      queryClient.setQueryData(adminPlayerQueryOptions(updated.player.id).queryKey, updated);
      void queryClient.invalidateQueries({ queryKey: ["admin", "players"] });
      setConfirmAction(null);
      onSuccess("Giocatore aggiornato.");
    }
  });

  const deleteMutation = useMutation({
    mutationFn: api.deleteAdminPlayer,
    onSuccess: (result) => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "players"] });
      setSelectedUserId(null);
      setConfirmAction(null);
      onSuccess(`Giocatore eliminato. Rimossi ${result.games} partite e ${result.pushSubscriptions} subscription.`);
    }
  });

  React.useEffect(() => {
    const error = playersQuery.error ?? detailQuery.error ?? updateMutation.error ?? deleteMutation.error;
    if (!error) return;
    if (onAuthRequired(error)) return;
    onError(error instanceof Error ? error.message : "Operazione admin non riuscita.");
  }, [playersQuery.error, detailQuery.error, updateMutation.error, deleteMutation.error, onAuthRequired, onError]);

  React.useEffect(() => {
    if (!detail) {
      setDraft(null);
      return;
    }
    setDraft({
      displayName: detail.player.displayName ?? "",
      nickname: detail.player.nickname,
      profileEmoji: detail.player.profileEmoji,
      bio: detail.player.bio,
      starAvailable: detail.player.starAvailable,
      starAwardedAt: detail.player.starAwardedAt,
      starUsedAt: detail.player.starUsedAt
    });
    setConfirmAction(null);
  }, [detail]);

  React.useEffect(() => {
    if (!playersQuery.data || playersQuery.data.page === page) return;
    setPage(playersQuery.data.page);
  }, [page, playersQuery.data]);

  React.useEffect(() => {
    const timeout = window.setTimeout(() => {
      setSearch(searchInput.trim());
      setPage(0);
    }, ADMIN_SEARCH_DEBOUNCE_MS);

    return () => window.clearTimeout(timeout);
  }, [searchInput]);

  function openPlayer(player: AdminPlayerSummaryDto) {
    setSelectedUserId(player.id);
    setConfirmAction(null);
  }

  function closeModal() {
    if (updateMutation.isPending || deleteMutation.isPending) return;
    setSelectedUserId(null);
    setConfirmAction(null);
  }

  function requestSave(event: React.FormEvent) {
    event.preventDefault();
    if (!canManagePlayers || !detail || !draft) return;
    setConfirmAction({ kind: "save" });
  }

  async function confirmSave() {
    if (!detail || !draft) return;
    await updateMutation.mutateAsync({ userId: detail.player.id, update: draft });
  }

  async function confirmDelete() {
    if (!detail || confirmAction?.kind !== "delete") return;
    const expected = deleteConfirmationValue(detail.player);
    if (confirmAction.typedValue !== expected) return;
    await deleteMutation.mutateAsync(detail.player.id);
  }

  const playersPage = playersQuery.data ?? null;
  const players = playersPage?.players ?? [];
  const totalPlayers = playersPage?.totalPlayers ?? 0;
  const totalPages = playersPage?.totalPages ?? 1;
  const currentPage = playersPage?.page ?? page;

  return (
    <section className="admin-view" aria-label="Admin">
      <div className="admin-toolbar">
        <div>
          <h2>Admin</h2>
          <p>{totalPlayers} giocatori</p>
        </div>
        <div className="admin-controls">
          <label className="admin-search">
            <Search size={17} />
            <input
              value={searchInput}
              onChange={(event) => {
                setSearchInput(event.target.value);
              }}
              placeholder="Cerca"
              autoComplete="off"
              spellCheck={false}
            />
          </label>
          <label className="admin-sort">
            <span>Ordina</span>
            <select
              value={playerSort}
              onChange={(event) => {
                setPlayerSort(event.target.value as AdminPlayerSortDto);
                setPage(0);
              }}
              aria-label="Ordina giocatori"
            >
              {adminPlayerSortOptions.map((option) => (
                <option value={option.value} key={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      {playersQuery.isPending ? (
        <div className="admin-loading">
          <LoadingSpinner />
        </div>
      ) : (
        <>
          <div className="admin-card-grid">
            {players.map((player) => (
              <button className="admin-player-card" type="button" key={player.id} onClick={() => openPlayer(player)}>
                <span className="admin-player-emoji">{player.profileEmoji}</span>
                <span className="admin-player-main">
                  <strong>{player.displayName ?? "Giocatore"}</strong>
                  <span>{player.nickname}</span>
                </span>
                <span className="admin-player-meta">
                  <span>{player.completed}/{player.gamesStarted} concluse</span>
                  <span>{player.winRate}% vittorie</span>
                </span>
                <span className="admin-player-badges">
                  {player.admin ? <span>Admin</span> : null}
                  {player.starAvailable ? <span>Stella</span> : null}
                  <span>{player.authenticated ? "Google" : "Anon"}</span>
                </span>
              </button>
            ))}
            {players.length === 0 ? <p className="admin-empty">Nessun giocatore trovato.</p> : null}
          </div>
          {totalPages > 1 ? (
            <nav className="admin-pagination" aria-label="Paginazione giocatori">
              <button
                className="profile-action-button"
                type="button"
                onClick={() => setPage((current) => Math.max(0, current - 1))}
                disabled={currentPage <= 0 || playersQuery.isFetching}
              >
                <ChevronLeft size={17} />
                <span>Precedente</span>
              </button>
              <span>Pagina {currentPage + 1} di {totalPages}</span>
              <button
                className="profile-action-button"
                type="button"
                onClick={() => setPage((current) => current + 1)}
                disabled={currentPage + 1 >= totalPages || playersQuery.isFetching}
              >
                <span>Successiva</span>
                <ChevronRight size={17} />
              </button>
            </nav>
          ) : null}
        </>
      )}

      {selectedUserId
        ? createPortal(
          <div className="modal-backdrop" role="presentation" onMouseDown={closeModal}>
          <section
            className="modal admin-modal"
            role="dialog"
            aria-modal="true"
            aria-label="Dettaglio admin giocatore"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <header className="modal-head">
              <h2>Giocatore</h2>
              <button className="close-button" type="button" onClick={closeModal} aria-label="Chiudi">
                <X size={19} />
              </button>
            </header>

            {detailQuery.isPending || !detail || !draft ? (
              <div className="admin-loading">
                <LoadingSpinner />
              </div>
            ) : (
              <div className="admin-modal-content">
                <PlayerIdentity detail={detail} />
                <StatsBlock detail={detail} />

                <form className="admin-edit-panel" onSubmit={requestSave}>
                  <h3>Gestione</h3>
                  <label>
                    <span>Nome</span>
                    <input
                      value={draft.displayName}
                      onChange={(event) => setDraft({ ...draft, displayName: event.target.value })}
                      disabled={!canManagePlayers || updateMutation.isPending}
                    />
                  </label>
                  <label>
                    <span>Nickname</span>
                    <div className="profile-nickname-field">
                      <span className="profile-nickname-prefix" aria-hidden="true">
                        @
                      </span>
                      <input
                        value={toEditableHandle(draft.nickname)}
                        onChange={(event) => setDraft({ ...draft, nickname: `@${toEditableHandle(event.target.value)}` })}
                        maxLength={29}
                        disabled={!canManagePlayers || updateMutation.isPending}
                        spellCheck={false}
                      />
                    </div>
                  </label>
                  <label>
                    <span>Emoji</span>
                    <select
                      value={draft.profileEmoji}
                      onChange={(event) => setDraft({ ...draft, profileEmoji: event.target.value })}
                      disabled={!canManagePlayers || updateMutation.isPending}
                    >
                      {PROFILE_EMOJIS.map((emoji) => (
                        <option value={emoji} key={emoji}>
                          {emoji}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    <span>Bio</span>
                    <textarea
                      value={draft.bio ?? ""}
                      onChange={(event) => setDraft({ ...draft, bio: event.target.value })}
                      maxLength={200}
                      disabled={!canManagePlayers || updateMutation.isPending}
                      rows={3}
                    />
                  </label>
                  <label className="admin-check">
                    <input
                      type="checkbox"
                      checked={draft.starAvailable}
                      onChange={(event) => setDraft({ ...draft, starAvailable: event.target.checked })}
                      disabled={!canManagePlayers || updateMutation.isPending}
                    />
                    <span>Stella disponibile</span>
                  </label>
                  <label>
                    <span>Stella assegnata</span>
                    <input
                      type="datetime-local"
                      value={dateTimeInputValue(draft.starAwardedAt)}
                      onChange={(event) =>
                        setDraft({ ...draft, starAwardedAt: dateTimeInputToIso(event.target.value) })
                      }
                      disabled={!canManagePlayers || updateMutation.isPending}
                      step={60}
                    />
                  </label>
                  <label>
                    <span>Stella usata</span>
                    <input
                      type="datetime-local"
                      value={dateTimeInputValue(draft.starUsedAt)}
                      onChange={(event) => setDraft({ ...draft, starUsedAt: dateTimeInputToIso(event.target.value) })}
                      disabled={!canManagePlayers || updateMutation.isPending}
                      step={60}
                    />
                  </label>
                  <div className="admin-actions">
                    <button className="profile-action-button primary" type="submit" disabled={!canManagePlayers}>
                      <Save size={17} />
                      <span>Salva</span>
                    </button>
                    <button
                      className="profile-action-button danger"
                      type="button"
                      onClick={() => setConfirmAction({ kind: "delete", typedValue: "" })}
                      disabled={!canManagePlayers || deleteMutation.isPending}
                    >
                      <Trash2 size={17} />
                      <span>Elimina</span>
                    </button>
                  </div>
                </form>

                {confirmAction ? (
                  <ConfirmPanel
                    action={confirmAction}
                    detail={detail}
                    pending={updateMutation.isPending || deleteMutation.isPending}
                    onActionChange={setConfirmAction}
                    onCancel={() => setConfirmAction(null)}
                    onConfirmSave={() => void confirmSave()}
                    onConfirmDelete={() => void confirmDelete()}
                  />
                ) : null}

                <GameHistory games={detail.games} />
                <HexahackHistory games={detail.hexahackGames} />
              </div>
            )}
          </section>
          </div>,
          document.body
        )
        : null}
    </section>
  );
}

function PlayerIdentity({ detail }: { detail: AdminPlayerDetailDto }) {
  return (
    <div className="admin-detail-head">
      <span className="admin-detail-emoji">{detail.player.profileEmoji}</span>
      <div>
        <strong>{detail.player.displayName ?? "Giocatore"}</strong>
        <span>{detail.player.nickname}</span>
      </div>
      {detail.player.bio ? <p className="admin-player-bio">{detail.player.bio}</p> : null}
      <div className="admin-facts">
        <span>ID: {detail.player.id}</span>
        <span>Email: {detail.player.email ?? "n/d"}</span>
        <span>Google subject: {detail.googleSubject ?? "n/d"}</span>
        <span>Creato: {formatDateTime(detail.createdAt)}</span>
        <span>Ultima attività: {formatDateTime(detail.player.lastActivityAt)}</span>
        <span>Push subscriptions: {detail.pushSubscriptions}</span>
      </div>
    </div>
  );
}

function StatsBlock({ detail }: { detail: AdminPlayerDetailDto }) {
  return (
    <section className="admin-stats-panel" aria-label="Statistiche giocatore">
      <div className="stat-grid">
        <Metric label="Giocate" value={detail.stats.played} />
        <Metric label="Vinte" value={detail.stats.won} />
        <Metric label="Perse" value={detail.stats.lost} />
        <Metric label="Vittorie" value={`${detail.player.winRate}%`} />
      </div>
      <div className="stat-grid compact">
        <Metric label="Serie" value={detail.stats.currentStreak} />
        <Metric label="Record" value={detail.stats.maxStreak} />
      </div>
      <Distribution distribution={detail.stats.guessDistribution} />
    </section>
  );
}

function ConfirmPanel({
  action,
  detail,
  pending,
  onActionChange,
  onCancel,
  onConfirmSave,
  onConfirmDelete
}: {
  action: ConfirmAction;
  detail: AdminPlayerDetailDto;
  pending: boolean;
  onActionChange: (action: ConfirmAction) => void;
  onCancel: () => void;
  onConfirmSave: () => void;
  onConfirmDelete: () => void;
}) {
  if (action.kind === "save") {
    return (
      <section className="admin-confirm-panel" aria-label="Conferma salvataggio">
        <strong>Conferma aggiornamento</strong>
        <p>Vuoi aggiornare profilo e stato stella di {detail.player.nickname}?</p>
        <div className="admin-actions">
          <button className="profile-action-button primary" type="button" onClick={onConfirmSave} disabled={pending}>
            <Check size={17} />
            <span>Conferma</span>
          </button>
          <button className="profile-action-button" type="button" onClick={onCancel} disabled={pending}>
            <X size={17} />
            <span>Annulla</span>
          </button>
        </div>
      </section>
    );
  }

  const expected = deleteConfirmationValue(detail.player);
  return (
    <section className="admin-confirm-panel danger" aria-label="Conferma eliminazione">
      <strong>Conferma eliminazione</strong>
      <p>Scrivi {expected} per eliminare utente, partite e subscription.</p>
      <input
        value={action.typedValue}
        onChange={(event) => onActionChange({ kind: "delete", typedValue: event.target.value })}
        disabled={pending}
        spellCheck={false}
      />
      <div className="admin-actions">
        <button
          className="profile-action-button danger"
          type="button"
          onClick={onConfirmDelete}
          disabled={pending || action.typedValue !== expected}
        >
          <Trash2 size={17} />
          <span>Elimina</span>
        </button>
        <button className="profile-action-button" type="button" onClick={onCancel} disabled={pending}>
          <X size={17} />
          <span>Annulla</span>
        </button>
      </div>
    </section>
  );
}

function GameHistory({ games }: { games: readonly AdminGameDto[] }) {
  const [revealedSolutions, setRevealedSolutions] = React.useState<ReadonlySet<string>>(() => new Set());

  React.useEffect(() => {
    setRevealedSolutions(new Set());
  }, [games]);

  function toggleSolution(gameId: string) {
    setRevealedSolutions((current) => {
      const next = new Set(current);
      if (next.has(gameId)) {
        next.delete(gameId);
      } else {
        next.add(gameId);
      }
      return next;
    });
  }

  return (
    <section className="admin-games-panel" aria-label="Partite">
      <h3>Partite</h3>
      {games.map((game) => {
        const solutionRevealed = revealedSolutions.has(game.id);
        return (
          <article className="admin-game-row" key={game.id}>
            <header>
              <strong>{game.puzzleDate}</strong>
              <span>{game.status}</span>
              <span>{game.modeLabel}</span>
            </header>
            <div className="admin-solution-row">
              <span>Soluzione: {solutionRevealed ? game.solution.toUpperCase() : maskedSolution(game.solution)}</span>
              <button
                className="admin-eye-button"
                type="button"
                onClick={() => toggleSolution(game.id)}
                aria-label={solutionRevealed ? "Nascondi soluzione" : "Rivela soluzione"}
                title={solutionRevealed ? "Nascondi soluzione" : "Rivela soluzione"}
              >
                {solutionRevealed ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            <p>
              Tentativi: {game.guesses.length} · Creata {formatDateTime(game.createdAt)} · Aggiornata{" "}
              {formatDateTime(game.updatedAt)}
            </p>
            {game.completedAt ? <p>Completata {formatDateTime(game.completedAt)}</p> : null}
            <div className="admin-guesses">
              {game.guesses.map((guess, index) => (
                <div className="admin-guess" key={`${game.id}-${index}`}>
                  <span>{guess.word.toUpperCase()}</span>
                  <span>
                    {guess.tiles.map((tile, tileIndex) => (
                      <i className={`admin-tile ${tile.state.toLowerCase()}`} key={`${guess.word}-${tileIndex}`}>
                        {tile.letter.toUpperCase()}
                      </i>
                    ))}
                  </span>
                </div>
              ))}
            </div>
          </article>
        );
      })}
      {games.length === 0 ? <p className="admin-empty">Nessuna partita.</p> : null}
    </section>
  );
}

function maskedSolution(solution: string) {
  return "*".repeat(solution.length || 6);
}

function HexahackHistory({ games }: { games: readonly AdminHexahackGameDto[] }) {
  return (
    <section className="admin-games-panel" aria-label="Nodi Hexahack">
      <h3>Hexahack</h3>
      {games.map((game) => (
        <article className="admin-game-row" key={game.id}>
          <header><strong>{game.puzzleDate}</strong><span>{game.status}</span><span>{game.rank ?? "—"}</span></header>
          <p>Soluzione: {game.solution} · Costo: {game.totalCost} · Errori: {game.wrongSubmissions}</p>
          <div className="admin-guesses">
            {game.log.map((entry) => (
              <div className="admin-guess" key={`${game.id}-${entry.sequence}`}>
                <span>#{entry.sequence} {entry.kind}</span>
                <span>{entry.probe?.summary ?? entry.submission?.code ?? ""}</span>
              </div>
            ))}
          </div>
        </article>
      ))}
      {games.length === 0 ? <p className="admin-empty">Nessun nodo Hexahack.</p> : null}
    </section>
  );
}

function deleteConfirmationValue(player: AdminPlayerSummaryDto) {
  return player.nickname || player.id;
}

function toEditableHandle(handle: string | null) {
  return (handle ?? "").replace(/@/g, "");
}

function dateTimeInputValue(value: string | null | undefined) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return `${date.getFullYear()}-${padDatePart(date.getMonth() + 1)}-${padDatePart(date.getDate())}T${padDatePart(
    date.getHours()
  )}:${padDatePart(date.getMinutes())}`;
}

function dateTimeInputToIso(value: string) {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date.toISOString();
}

function padDatePart(value: number) {
  return String(value).padStart(2, "0");
}

function formatDateTime(value: string | null | undefined) {
  if (!value) return "n/d";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("it-IT", {
    dateStyle: "short",
    timeStyle: "short"
  });
}
