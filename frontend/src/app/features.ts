/**
 * Vite substitutes this value while building the client. Keep Hexahack hidden
 * until the public rollout is ready by setting VITE_HIDE_HEXAHACK=true.
 */
export const isHexahackHidden = import.meta.env.VITE_HIDE_HEXAHACK?.trim().toLowerCase() === "true";
