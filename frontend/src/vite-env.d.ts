/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_GOOGLE_CLIENT_ID?: string;
  readonly VITE_HIDE_HEXAHACK?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
