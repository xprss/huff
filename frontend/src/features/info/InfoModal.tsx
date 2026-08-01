import { ExternalLink, Github, X } from "lucide-react";
import { REPOSITORY_URL } from "../../app/constants";
import { APP_VERSION } from "../../version";

export function InfoModal({ onClose }: { onClose: () => void }) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal info-modal" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-head">
          <h2>Info</h2>
          <button className="close-button" type="button" onClick={onClose} aria-label="Chiudi">
            <X size={19} />
          </button>
        </header>

        <p className="app-version">Versione {APP_VERSION}</p>

        <div className="repo-panel">
          <Github size={26} aria-hidden="true" />
          <div>
            <h3>Repository GitHub</h3>
            <a className="repo-link" href={REPOSITORY_URL} target="_blank" rel="noreferrer">
              <span>github.com/xprss/huff</span>
              <ExternalLink size={16} aria-hidden="true" />
            </a>
          </div>
        </div>
      </section>
    </div>
  );
}
