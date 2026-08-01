import React from "react";
import { VIEWPORT_SYNC_DELAYS_MS } from "../../app/constants";

export function useAppViewportHeight(refreshKey: unknown) {
  React.useLayoutEffect(() => {
    return installAppViewportHeightSync();
  }, []);

  React.useLayoutEffect(() => {
    scheduleAppViewportHeightSync();
  }, [refreshKey]);
}

function installAppViewportHeightSync() {
  scheduleAppViewportHeightSync();

  function onVisibilityChange() {
    if (document.visibilityState === "visible") {
      scheduleAppViewportHeightSync();
    }
  }

  window.addEventListener("resize", scheduleAppViewportHeightSync);
  window.addEventListener("orientationchange", scheduleAppViewportHeightSync);
  window.addEventListener("focus", scheduleAppViewportHeightSync);
  window.addEventListener("pageshow", scheduleAppViewportHeightSync);
  document.addEventListener("visibilitychange", onVisibilityChange);
  window.visualViewport?.addEventListener("resize", scheduleAppViewportHeightSync);
  window.visualViewport?.addEventListener("scroll", scheduleAppViewportHeightSync);

  return () => {
    window.removeEventListener("resize", scheduleAppViewportHeightSync);
    window.removeEventListener("orientationchange", scheduleAppViewportHeightSync);
    window.removeEventListener("focus", scheduleAppViewportHeightSync);
    window.removeEventListener("pageshow", scheduleAppViewportHeightSync);
    document.removeEventListener("visibilitychange", onVisibilityChange);
    window.visualViewport?.removeEventListener("resize", scheduleAppViewportHeightSync);
    window.visualViewport?.removeEventListener("scroll", scheduleAppViewportHeightSync);
  };
}

function scheduleAppViewportHeightSync() {
  syncAppViewportHeight();
  VIEWPORT_SYNC_DELAYS_MS.forEach((delay) => {
    window.setTimeout(syncAppViewportHeight, delay);
  });
}

function syncAppViewportHeight() {
  const visualViewportHeight = window.visualViewport?.height;
  const viewportHeight = Math.round(
    visualViewportHeight && visualViewportHeight > 0 ? visualViewportHeight : window.innerHeight
  );
  if (!viewportHeight) return;

  document.documentElement.style.setProperty("--app-height", `${viewportHeight}px`);
  document.documentElement.style.setProperty("--app-vh", `${viewportHeight * 0.01}px`);
}
