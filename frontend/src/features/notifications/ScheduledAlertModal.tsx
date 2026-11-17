import { X } from "lucide-react";
import type { ScheduledAlertDto } from "../../types";
import "./notifications.css";

export function ScheduledAlertModal({ alert, onClose }: { alert: ScheduledAlertDto; onClose: () => void }) {
  return <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
    <section className="modal launch-modal scheduled-alert-modal" role="dialog" aria-modal="true" aria-labelledby="scheduled-alert-title" onMouseDown={(event) => event.stopPropagation()}>
      <header className="modal-head"><h2 id="scheduled-alert-title">{alert.title}</h2><button className="close-button" type="button" onClick={onClose} aria-label="Chiudi"><X size={19} /></button></header>
      <p className="scheduled-alert-body">{alert.body}</p>
      <div className="admin-actions scheduled-alert-actions">{alert.url ? <a className="profile-action-button primary" href={alert.url} onClick={onClose}>Apri</a> : null}<button className="profile-action-button" type="button" onClick={onClose}>Chiudi</button></div>
    </section>
  </div>;
}
