import React from "react";
import ReactDOM from "react-dom/client";
import { QueryClientProvider } from "@tanstack/react-query";
import { App } from "./app/App";
import { queryClient } from "./app/queryClient";
import { registerServiceWorker } from "./app/serviceWorker";
import { applyThemeToDocument, getAppTheme, getStoredThemeId } from "./theme";
import "./styles.css";

applyThemeToDocument(getAppTheme(getStoredThemeId()));

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </React.StrictMode>
);

registerServiceWorker();
