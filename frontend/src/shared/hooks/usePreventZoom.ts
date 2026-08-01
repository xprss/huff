import React from "react";
import { ZOOM_KEYS } from "../../app/constants";

export function usePreventZoom() {
  React.useEffect(() => {
    let lastTouchEnd = 0;
    const nonPassive = { passive: false };

    function preventDefault(event: Event) {
      event.preventDefault();
    }

    function preventWheelZoom(event: WheelEvent) {
      if (event.ctrlKey || event.metaKey) {
        event.preventDefault();
      }
    }

    function preventKeyboardZoom(event: KeyboardEvent) {
      if ((event.ctrlKey || event.metaKey) && ZOOM_KEYS.has(event.key)) {
        event.preventDefault();
      }
    }

    function preventDoubleTapZoom(event: TouchEvent) {
      const now = Date.now();
      if (now - lastTouchEnd <= 300) {
        event.preventDefault();
      }
      lastTouchEnd = now;
    }

    window.addEventListener("wheel", preventWheelZoom, nonPassive);
    window.addEventListener("keydown", preventKeyboardZoom);
    document.addEventListener("gesturestart", preventDefault, nonPassive);
    document.addEventListener("gesturechange", preventDefault, nonPassive);
    document.addEventListener("touchend", preventDoubleTapZoom, nonPassive);

    return () => {
      window.removeEventListener("wheel", preventWheelZoom);
      window.removeEventListener("keydown", preventKeyboardZoom);
      document.removeEventListener("gesturestart", preventDefault);
      document.removeEventListener("gesturechange", preventDefault);
      document.removeEventListener("touchend", preventDoubleTapZoom);
    };
  }, []);
}
