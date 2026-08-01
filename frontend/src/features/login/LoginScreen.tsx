import React from "react";
import { APP_NAME } from "../../app/constants";
import { useAppTheme } from "../../theme";

const LOGIN_DECOR_STATES = ["correct", "present", "absent"] as const;
type LoginDecorState = (typeof LOGIN_DECOR_STATES)[number];
type LoginDecorTile = {
  state: LoginDecorState;
  style: React.CSSProperties;
};

const LOGIN_DECOR_TILES = buildLoginDecorTiles();

export function LoginScreen({ loginUrl }: { loginUrl: string }) {
  return (
    <section className="login-screen" aria-labelledby="login-title">
      <div className="login-decor" aria-hidden="true">
        {LOGIN_DECOR_TILES.map((tile, index) => (
          <span className={`login-decor-tile ${tile.state}`} key={index} style={tile.style} />
        ))}
      </div>
      <div className="login-copy">
        <h2 id="login-title">{APP_NAME}</h2>
        <p>Accedi per giocare la partita del giorno.</p>
      </div>
      <a className="login-button" href={loginUrl}>
        <GoogleLogo />
        <span>Accedi con Google</span>
      </a>
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

function GoogleLogo() {
  const theme = useAppTheme();

  return (
    <svg className="google-logo" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        fill={theme.colors.googleBlue}
        d="M23.49 12.27c0-.79-.07-1.54-.2-2.27H12v4.29h6.47a5.53 5.53 0 0 1-2.4 3.63v2.96h3.89c2.27-2.09 3.53-5.17 3.53-8.61Z"
      />
      <path
        fill={theme.colors.googleGreen}
        d="M12 24c3.24 0 5.96-1.07 7.95-2.91l-3.89-2.96c-1.08.72-2.45 1.15-4.06 1.15-3.12 0-5.77-2.11-6.71-4.95H1.28v3.05A12 12 0 0 0 12 24Z"
      />
      <path
        fill={theme.colors.googleYellow}
        d="M5.29 14.33a7.21 7.21 0 0 1 0-4.66V6.62H1.28a12.01 12.01 0 0 0 0 10.76l4.01-3.05Z"
      />
      <path
        fill={theme.colors.googleRed}
        d="M12 4.72c1.76 0 3.34.6 4.59 1.79l3.44-3.44C17.95 1.13 15.23 0 12 0A12 12 0 0 0 1.28 6.62l4.01 3.05C6.23 6.83 8.88 4.72 12 4.72Z"
      />
    </svg>
  );
}
