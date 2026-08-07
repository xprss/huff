import React from "react";
import { APP_NAME } from "../../app/constants";

const LOGIN_DECOR_STATES = ["correct", "present", "absent"] as const;
type LoginDecorState = (typeof LOGIN_DECOR_STATES)[number];
type LoginDecorTile = {
  state: LoginDecorState;
  style: React.CSSProperties;
};

const LOGIN_DECOR_TILES = buildLoginDecorTiles();

type GoogleTokenResponse = {
  access_token?: string;
  error?: string;
  error_description?: string;
};

type GoogleTokenClient = {
  requestAccessToken: (configuration?: { prompt?: string }) => void;
};

type GoogleOAuth = {
  initTokenClient: (configuration: {
    client_id: string;
    scope: string;
    callback: (response: GoogleTokenResponse) => void;
  }) => GoogleTokenClient;
};

declare global {
  interface Window {
    google?: {
      accounts?: {
        oauth2?: GoogleOAuth;
      };
    };
  }
}

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim();

export function GoogleLoginScreen({ onAccessToken }: { onAccessToken: (token: string) => void }) {
  const tokenClient = React.useRef<GoogleTokenClient | null>(null);
  const handlingToken = React.useRef(false);
  const [error, setError] = React.useState<string | null>(null);
  const [ready, setReady] = React.useState(false);

  React.useEffect(() => {
    const clientId = GOOGLE_CLIENT_ID ?? "";
    if (!clientId) {
      setError("Google Client ID non configurato.");
      return;
    }

    let attempts = 0;
    let timer: number | undefined;

    function initialize() {
      const oauth2 = window.google?.accounts?.oauth2;
      if (!oauth2) {
        attempts += 1;
        if (attempts < 20) {
          timer = window.setTimeout(initialize, 100);
        } else {
          setError("Impossibile caricare il login Google.");
        }
        return;
      }

      tokenClient.current = oauth2.initTokenClient({
        client_id: clientId,
        scope: "openid email profile",
        callback: (response) => {
          if (!response.access_token) {
            setError(response.error_description ?? response.error ?? "Google non ha restituito un access token.");
            return;
          }
          if (handlingToken.current) return;
          handlingToken.current = true;
          onAccessToken(response.access_token);
        }
      });
      setReady(true);
    }

    initialize();
    return () => {
      if (timer !== undefined) window.clearTimeout(timer);
    };
  }, [onAccessToken]);

  return (
    <section className="login-screen" aria-labelledby="login-title">
      <div className="login-decor" aria-hidden="true">
        {LOGIN_DECOR_TILES.map((tile, index) => (
          <span className={`login-decor-tile ${tile.state}`} key={index} style={tile.style} />
        ))}
      </div>
      <h1 className="login-game-title">{APP_NAME}</h1>
      <div className="login-box">
        <div className="login-copy">
          <h2 id="login-title">Accedi per giocare</h2>
          <p>Usa il tuo account Google per ottenere automaticamente il token di accesso.</p>
        </div>
        <button
          className="login-button"
          type="button"
          disabled={!ready}
          onClick={() => tokenClient.current?.requestAccessToken({ prompt: "select_account" })}
        >
          {ready ? "Accedi con Google" : "Caricamento login Google…"}
        </button>
        {error ? <p className="google-login-error">{error}</p> : null}
      </div>
    </section>
  );
}

function buildLoginDecorTiles(): LoginDecorTile[] {
  const columns = 6;
  const rows = 5;
  const xJitter = [-3.2, 1.8, -1.1, 3.4, -2.4, 2.6];
  const yJitter = [2.4, -3.1, 1.6, -1.8, 3.2];
  const widths = [54, 72, 44, 86, 62, 78, 50, 68];
  const drift = [
    [-16, -12],
    [14, -18],
    [-10, 16],
    [18, 10],
    [-20, 6],
    [12, 18]
  ];

  return Array.from({ length: columns * rows }, (_, index) => {
    const row = Math.floor(index / columns);
    const column = index % columns;
    const x = 8 + column * 16.8 + xJitter[(row + column) % xJitter.length];
    const y = 11 + row * 20 + yJitter[(column + row * 2) % yJitter.length];
    const [dx, dy] = drift[index % drift.length];
    const duration = 6800 + ((index * 467) % 3600);
    const delay = -1 * ((index * 719) % duration);
    const opacity = 0.11 + ((index * 7) % 7) * 0.012;
    const scale = 0.82 + ((index * 5) % 9) * 0.035;

    return {
      state: LOGIN_DECOR_STATES[index % LOGIN_DECOR_STATES.length],
      style: {
        "--tile-x": `${x.toFixed(1)}%`,
        "--tile-y": `${y.toFixed(1)}%`,
        "--tile-width": `${widths[index % widths.length]}px`,
        "--tile-delay": `${delay}ms`,
        "--tile-duration": `${duration}ms`,
        "--tile-start-x": `${(-dx * 0.65).toFixed(1)}px`,
        "--tile-start-y": `${(-dy * 0.65).toFixed(1)}px`,
        "--tile-dx": `${dx}px`,
        "--tile-dy": `${dy}px`,
        "--tile-opacity": opacity.toFixed(3),
        "--tile-scale": scale.toFixed(2),
        "--tile-scale-start": (scale * 0.56).toFixed(2),
        "--tile-scale-end": (scale * 0.72).toFixed(2)
      } as React.CSSProperties
    };
  });
}
