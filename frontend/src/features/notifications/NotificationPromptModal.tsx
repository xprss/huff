import { Bell, X } from "lucide-react";

export function NotificationPromptModal({
  isEnabling,
  onEnable,
  onNeverShow,
  onClose
}: {
  isEnabling: boolean;
  onEnable: () => void;
  onNeverShow: () => void;
  onClose: () => void;
}) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="modal notification-prompt-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="notification-prompt-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="modal-head">
          <div className="notification-prompt-title">
            <span className="notification-prompt-icon" aria-hidden="true">
              <Bell size={21} />
            </span>
            <h2 id="notification-prompt-title">Non perdere la prossima sfida</h2>
          </div>
          <button className="close-button" type="button" onClick={onClose} aria-label="Chiudi">
            <X size={19} />
          </button>
        </header>

        <p>
          Attiva le notifiche per ricevere un promemoria della parola quotidiana e gli aggiornamenti sulle tue
          ricompense settimanali.
        </p>

        <div className="notification-prompt-actions">
          <button className="profile-action-button" type="button" onClick={onNeverShow} disabled={isEnabling}>
            Non mostrare più
          </button>
          <button className="profile-action-button primary" type="button" onClick={onEnable} disabled={isEnabling}>
            {isEnabling ? "Attivazione…" : "Attiva notifiche"}
          </button>
        </div>
      </section>
    </div>
  );
}
