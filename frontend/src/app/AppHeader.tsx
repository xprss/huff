import React from "react";
import { BarChart3, Bell, BellOff, Check, ChevronLeft, ChevronRight, Edit3, Eye, Grid2X2, Info, LogOut, Menu, Palette, Shield, Star, Trophy, UserRound } from "lucide-react";
import { APP_NAME } from "./constants";
import type { AppRoute } from "./routing";
import type { ToastMessage } from "../shared/toast";
import type { AppTheme } from "../theme";

export function AppHeader({
  puzzleDate,
  toast,
  showStarButton,
  starCanUse,
  showProfileEdit,
  showActionsMenu,
  actionsMenuRef,
  activeRoute,
  canUseGameActions,
  showAdmin,
  notificationsEnabled,
  notificationMenuLabel,
  themes,
  selectedThemeId,
  patternsEnabled,
  showLogout,
  onUseStar,
  onEditProfile,
  onToggleMenu,
  onOpenProfile,
  onOpenGames,
  onOpenStats,
  onOpenLeaderboard,
  onOpenAdmin,
  onOpenInfo,
  onToggleNotifications,
  onSelectTheme,
  onTogglePatterns,
  onLogout,
  onCloseMenu
}: {
  puzzleDate: string;
  toast: ToastMessage | null;
  showStarButton: boolean;
  starCanUse: boolean;
  showProfileEdit: boolean;
  showActionsMenu: boolean;
  actionsMenuRef: React.RefObject<HTMLDivElement>;
  activeRoute: AppRoute;
  canUseGameActions: boolean;
  showAdmin: boolean;
  notificationsEnabled: boolean;
  notificationMenuLabel: string;
  themes: readonly AppTheme[];
  selectedThemeId: string;
  patternsEnabled: boolean;
  showLogout: boolean;
  onUseStar: () => void;
  onEditProfile: () => void;
  onToggleMenu: () => void;
  onOpenProfile: () => void;
  onOpenGames: () => void;
  onOpenStats: () => void;
  onOpenLeaderboard: () => void;
  onOpenAdmin: () => void;
  onOpenInfo: () => void;
  onToggleNotifications: () => void;
  onSelectTheme: (themeId: string) => void;
  onTogglePatterns: () => void;
  onLogout: () => void;
  onCloseMenu: () => void;
}) {
  const [showThemes, setShowThemes] = React.useState(false);

  React.useEffect(() => {
    if (!showActionsMenu) setShowThemes(false);
  }, [showActionsMenu]);

  const isMenuPageActive = (menuPage: "games" | "profile" | "leaderboard" | "admin") => {
    if (menuPage === "games") {
      return activeRoute === "games" || activeRoute === "game" || activeRoute === "hexadigit";
    }
    if (menuPage === "leaderboard") {
      return activeRoute === "leaderboard" || activeRoute === "player";
    }
    return activeRoute === menuPage;
  };

  const menuItemProps = (menuPage: "games" | "profile" | "leaderboard" | "admin") => {
    const isActive = isMenuPageActive(menuPage);
    return {
      className: `menu-item${isActive ? " selected" : ""}`,
      "aria-current": isActive ? ("page" as const) : undefined
    };
  };

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
            <div className="action-menu" role="menu" aria-label={showThemes ? "Temi" : "Azioni"}>
              {showThemes ? (
                <>
                  <button className="menu-item menu-back" type="button" role="menuitem" onClick={() => setShowThemes(false)}>
                    <ChevronLeft size={18} />
                    <span>Temi</span>
                  </button>
                  <div className="menu-divider" role="separator" />
                  {themes.map((theme) => {
                    const selected = theme.id === selectedThemeId;
                    return (
                      <button
                        className={`menu-item theme-menu-item${selected ? " selected" : ""}`}
                        key={theme.id}
                        type="button"
                        role="menuitemradio"
                        aria-checked={selected}
                        onClick={() => onSelectTheme(theme.id)}
                      >
                        <span className="theme-swatch" style={{ background: theme.colors.primary }} aria-hidden="true" />
                        <span>{theme.name}</span>
                        {selected ? <Check className="theme-check" size={17} aria-hidden="true" /> : null}
                      </button>
                    );
                  })}
                  <div className="menu-divider" role="separator" />
                  <button
                    className={`menu-item${patternsEnabled ? " selected" : ""}`}
                    type="button"
                    role="menuitemcheckbox"
                    aria-checked={patternsEnabled}
                    onClick={onTogglePatterns}
                  >
                    <Eye size={18} />
                    <span>{patternsEnabled ? "Nascondi motivo" : "Mostra motivo"}</span>
                    {patternsEnabled ? <Check className="theme-check" size={17} aria-hidden="true" /> : null}
                  </button>
                </>
              ) : (
                <>
              {canUseGameActions ? (
                <>
                  <button {...menuItemProps("games")} type="button" role="menuitem" onClick={onOpenGames}>
                    <Grid2X2 size={18} />
                    <span>Giochi</span>
                  </button>
                  <button {...menuItemProps("profile")} type="button" role="menuitem" onClick={onOpenProfile}>
                    <UserRound size={18} />
                    <span>Profilo</span>
                  </button>
                  <button className="menu-item" type="button" role="menuitem" onClick={onOpenStats}>
                    <BarChart3 size={18} />
                    <span>Statistiche</span>
                  </button>
                  <button {...menuItemProps("leaderboard")} type="button" role="menuitem" onClick={onOpenLeaderboard}>
                    <Trophy size={18} />
                    <span>Leaderboard</span>
                  </button>
                  {showAdmin ? (
                    <button {...menuItemProps("admin")} type="button" role="menuitem" onClick={onOpenAdmin}>
                      <Shield size={18} />
                      <span>Admin</span>
                    </button>
                  ) : null}
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
              <button className="menu-item" type="button" role="menuitem" onClick={() => setShowThemes(true)}>
                <Palette size={18} />
                <span>Temi</span>
                <ChevronRight className="menu-item-trailing-icon" size={18} aria-hidden="true" />
              </button>
              {showLogout ? (
                <>
                  <div className="menu-divider" role="separator" />
                  <button className="menu-item danger" type="button" role="menuitem" onClick={onLogout}>
                    <LogOut size={18} />
                    <span>Esci</span>
                  </button>
                </>
              ) : null}
                </>
              )}
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
