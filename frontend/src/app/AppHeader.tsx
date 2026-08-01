import React from "react";
import { BarChart3, Bell, BellOff, Edit3, Info, LogOut, Menu, Moon, Star, Sun, UserRound } from "lucide-react";
import { APP_NAME } from "./constants";
import type { ToastMessage } from "../shared/toast";

export function AppHeader({
  puzzleDate,
  toast,
  showStarButton,
  starCanUse,
  showProfileEdit,
  showActionsMenu,
  actionsMenuRef,
  canUseGameActions,
  notificationsEnabled,
  notificationMenuLabel,
  darkMode,
  showLogout,
  logoutUrl,
  onUseStar,
  onEditProfile,
  onToggleMenu,
  onOpenProfile,
  onOpenStats,
  onOpenInfo,
  onToggleNotifications,
  onToggleTheme,
  onCloseMenu
}: {
  puzzleDate: string;
  toast: ToastMessage | null;
  showStarButton: boolean;
  starCanUse: boolean;
  showProfileEdit: boolean;
  showActionsMenu: boolean;
  actionsMenuRef: React.RefObject<HTMLDivElement>;
  canUseGameActions: boolean;
  notificationsEnabled: boolean;
  notificationMenuLabel: string;
  darkMode: boolean;
  showLogout: boolean;
  logoutUrl: string;
  onUseStar: () => void;
  onEditProfile: () => void;
  onToggleMenu: () => void;
  onOpenProfile: () => void;
  onOpenStats: () => void;
  onOpenInfo: () => void;
  onToggleNotifications: () => void;
  onToggleTheme: () => void;
  onCloseMenu: () => void;
}) {
  return (
    <header className="topbar">
      <div className="title-row">
        <div className="title-mark">
          <h1>{APP_NAME}</h1>
          <p className="date">{puzzleDate}</p>
        </div>
        <div className="actions" ref={actionsMenuRef}>
          {showStarButton ? (
            <button
              className={`icon-button star-action ${starCanUse ? "available" : "unavailable"}`}
              type="button"
              onClick={onUseStar}
              aria-disabled={!starCanUse}
              aria-label={starCanUse ? "Usa stella" : "Stella non disponibile"}
              title={starCanUse ? "Usa stella" : "Completa 3 partite di fila per ottenere una stella"}
            >
              <Star size={21} />
            </button>
          ) : null}
          {showProfileEdit ? (
            <button
              className="icon-button profile-nav-edit"
              type="button"
              onClick={onEditProfile}
              aria-label="Modifica nome e nickname"
              title="Modifica"
            >
              <Edit3 size={19} />
            </button>
          ) : null}
          <button
            className="icon-button menu-trigger"
            type="button"
            onClick={onToggleMenu}
            aria-haspopup="menu"
            aria-expanded={showActionsMenu}
            aria-label="Apri menu"
            title="Menu"
          >
            <Menu size={21} />
          </button>
          {showActionsMenu ? (
            <div className="action-menu" role="menu" aria-label="Azioni">
              {canUseGameActions ? (
                <>
                  <button className="menu-item" type="button" role="menuitem" onClick={onOpenProfile}>
                    <UserRound size={18} />
                    <span>Profilo</span>
                  </button>
                  <button className="menu-item" type="button" role="menuitem" onClick={onOpenStats}>
                    <BarChart3 size={18} />
                    <span>Statistiche</span>
                  </button>
                </>
              ) : null}
              <button className="menu-item" type="button" role="menuitem" onClick={onOpenInfo}>
                <Info size={18} />
                <span>Info</span>
              </button>
              <button className="menu-item" type="button" role="menuitem" onClick={onToggleNotifications}>
                {notificationsEnabled ? <BellOff size={18} /> : <Bell size={18} />}
                <span>{notificationMenuLabel}</span>
              </button>
              <button className="menu-item" type="button" role="menuitem" onClick={onToggleTheme}>
                {darkMode ? <Sun size={18} /> : <Moon size={18} />}
                <span>{darkMode ? "Tema chiaro" : "Tema scuro"}</span>
              </button>
              {showLogout ? (
                <>
                  <div className="menu-divider" role="separator" />
                  <a className="menu-item danger" href={logoutUrl} role="menuitem" onClick={onCloseMenu}>
                    <LogOut size={18} />
                    <span>Esci</span>
                  </a>
                </>
              ) : null}
            </div>
          ) : null}
        </div>
      </div>

      {toast ? (
        <div className={`toast ${toast.variant}`} role="status" aria-live="polite" key={toast.id}>
          {toast.text}
        </div>
      ) : null}
    </header>
  );
}
