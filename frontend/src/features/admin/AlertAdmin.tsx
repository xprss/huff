import React from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../api";
import type { ScheduledAlertDto, ScheduledAlertRequestDto } from "../../types";

const blank = (): ScheduledAlertRequestDto => ({ type: "PUSH", title: "", body: "", url: null, scheduledAt: "" });
const dateTimeInput = (value: string) => {
  const date = new Date(value);
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
};

export function AlertAdmin({ onSuccess, onError }: { onSuccess: (message: string) => void; onError: (message: string) => void }) {
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = React.useState<string | null>(null);
  const [draft, setDraft] = React.useState<ScheduledAlertRequestDto>(blank);
  const [localDate, setLocalDate] = React.useState("");
  const [clock, setClock] = React.useState(Date.now());
  const alerts = useQuery({ queryKey: ["admin", "alerts"], queryFn: api.adminAlerts, refetchInterval: 30_000 });
  const save = useMutation({
    mutationFn: (request: ScheduledAlertRequestDto) => editingId ? api.updateAdminAlert(editingId, request) : api.createAdminAlert(request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "alerts"] });
      setEditingId(null);
      setDraft(blank());
      setLocalDate("");
      onSuccess("Alert programmato.");
    },
    onError: (error) => onError(error instanceof Error ? error.message : "Salvataggio non riuscito.")
  });
  const remove = useMutation({
    mutationFn: api.deleteAdminAlert,
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: ["admin", "alerts"] }); onSuccess("Alert eliminato."); },
    onError: (error) => onError(error instanceof Error ? error.message : "Eliminazione non riuscita.")
  });

  React.useEffect(() => {
    const interval = window.setInterval(() => setClock(Date.now()), 30_000);
    return () => window.clearInterval(interval);
  }, []);
  function edit(alert: ScheduledAlertDto) {
    setEditingId(alert.id);
    setDraft({ type: alert.type, title: alert.title, body: alert.body, url: alert.url, scheduledAt: alert.scheduledAt });
    setLocalDate(dateTimeInput(alert.scheduledAt));
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function submit(event: React.FormEvent) {
    event.preventDefault();
    const date = new Date(localDate);
    if (Number.isNaN(date.getTime()) || date.getTime() <= Date.now()) {
      onError("Seleziona una data e un'ora future.");
      return;
    }
    save.mutate({ ...draft, url: draft.url?.trim() || null, scheduledAt: date.toISOString() });
  }

  const upcoming = alerts.data?.filter((alert) => Date.parse(alert.scheduledAt) > clock) ?? [];
  const past = alerts.data?.filter((alert) => Date.parse(alert.scheduledAt) <= clock) ?? [];
  return <section className="admin-view alert-admin" aria-label="Gestione alert">
    <div className="admin-toolbar"><div><h2>Alert</h2><p>Programma messaggi per tutti gli utenti.</p></div></div>
    {alerts.error ? <p role="alert">{alerts.error instanceof Error ? alerts.error.message : "Impossibile caricare gli alert."}</p> : null}
    <form className="admin-edit-panel alert-form" onSubmit={submit}>
      <h3>{editingId ? "Modifica alert" : "Nuovo alert"}</h3>
      <label><span>Tipo</span><select value={draft.type} onChange={(event) => setDraft({ ...draft, type: event.target.value as ScheduledAlertRequestDto["type"] })}>
        <option value="PUSH">Notifica push</option><option value="MODAL">Modal nell'app</option>
      </select></label>
      <label><span>Titolo</span><input required maxLength={120} value={draft.title} onChange={(event) => setDraft({ ...draft, title: event.target.value })} /></label>
      <label><span>Messaggio</span><textarea required maxLength={1000} rows={4} value={draft.body} onChange={(event) => setDraft({ ...draft, body: event.target.value })} /></label>
      <label><span>Data e ora</span><input required type="datetime-local" min={dateTimeInput(new Date(clock).toISOString())} value={localDate} onChange={(event) => setLocalDate(event.target.value)} /></label>
      <label><span>Link interno (facoltativo)</span><input placeholder="/#/games" value={draft.url ?? ""} onChange={(event) => setDraft({ ...draft, url: event.target.value })} pattern="/#/.*" /></label>
      <p className="alert-hint">L'orario usa il fuso del tuo dispositivo. Le push arrivano agli utenti che le hanno attivate; i modal appaiono alla prossima visita dopo l'orario impostato.</p>
      <div className="admin-actions"><button className="profile-action-button primary" type="submit" disabled={save.isPending}>{editingId ? "Salva modifiche" : "Programma alert"}</button>
        {editingId ? <button className="profile-action-button" type="button" onClick={() => { setEditingId(null); setDraft(blank()); setLocalDate(""); }}>Annulla</button> : null}</div>
    </form>
    <section className="admin-games-panel"><h3>Programmati</h3>
      {upcoming.length ? upcoming.map((alert) => <AlertRow key={alert.id} alert={alert} editable onEdit={() => edit(alert)} onDelete={() => { if (window.confirm(`Eliminare l'alert “${alert.title}”?`)) remove.mutate(alert.id); }} />) : <p className="admin-empty">Nessun alert programmato.</p>}
    </section>
    <section className="admin-games-panel"><h3>Messaggi passati</h3>
      {past.length ? past.map((alert) => <AlertRow key={alert.id} alert={alert} />) : <p className="admin-empty">Nessun messaggio passato.</p>}
    </section>
  </section>;
}

function AlertRow({ alert, editable, onEdit, onDelete }: { alert: ScheduledAlertDto; editable?: boolean; onEdit?: () => void; onDelete?: () => void }) {
  return <article className="admin-game-row alert-row"><header><strong>{alert.title}</strong><span>{alert.type === "PUSH" ? "Push" : "Modal"}</span><span>{new Date(alert.scheduledAt).toLocaleString("it-IT", { dateStyle: "short", timeStyle: "short" })}</span></header>
    <p>{alert.body}</p>{alert.url ? <p>Link: {alert.url}</p> : null}
    {editable ? <div className="admin-actions"><button type="button" className="profile-action-button" onClick={onEdit}>Modifica</button><button type="button" className="profile-action-button danger" onClick={onDelete}>Elimina</button></div> : null}
  </article>;
}
