import React from "react";

export type AppThemeMode = "dark" | "light";

export type AppThemeConditions = {
  preferredMode: AppThemeMode;
};

export type AppThemeColors = {
  page: string;
  surface: string;
  surfaceStrong: string;
  tile: string;
  tileBorder: string;
  tileFilled: string;
  text: string;
  muted: string;
  button: string;
  buttonBorder: string;
  hoverBorder: string;
  modal: string;
  modalShadow: string;
  track: string;
  key: string;
  error: string;
  correct: string;
  present: string;
  absent: string;
  transparent: string;
  appGlowCorrect: string;
  appGlowError: string;
  titleText: string;
  titleGlowSoft: string;
  titleGlowMedium: string;
  titleGlowWide: string;
  onSolid: string;
  onPresent: string;
  loginButtonBorder: string;
  loginButtonBackground: string;
  loginButtonText: string;
  loginButtonShadow: string;
  loginButtonHoverBorder: string;
  loadingSpinnerTrack: string;
  shareButtonBorder: string;
  shareButtonShadow: string;
  terminalBackground: string;
  terminalText: string;
  terminalInsetBorder: string;
  terminalOverlay: string;
  terminalWonText: string;
  terminalWonBorder: string;
  terminalWonRing: string;
  terminalLostText: string;
  terminalLostBorder: string;
  terminalLostRing: string;
  terminalCellBorder: string;
  terminalCursor: string;
  terminalCursorShadow: string;
  terminalWonCellBorder: string;
  terminalWonFilledText: string;
  terminalWonGlowSoft: string;
  terminalWonGlowMedium: string;
  terminalWonGlowWide: string;
  terminalLostCellBorder: string;
  terminalLostFilledText: string;
  terminalLostGlowSoft: string;
  terminalLostGlowMedium: string;
  terminalLostGlowWide: string;
  modalBackdrop: string;
  distributionBar: string;
  resultWon: string;
  resultLost: string;
  googleBlue: string;
  googleGreen: string;
  googleYellow: string;
  googleRed: string;
};

export type AppTheme = {
  id: "base";
  colorScheme: "dark" | "light";
  colors: AppThemeColors;
};

export const baseAppTheme: AppTheme = {
  id: "base",
  colorScheme: "dark",
  colors: {
    page: "#111816",
    surface: "#17221e",
    surfaceStrong: "#202d28",
    tile: "#17221e",
    tileBorder: "#4d5a54",
    tileFilled: "#89968f",
    text: "#f4f0e8",
    muted: "#aab5ae",
    button: "#25332e",
    buttonBorder: "#58645e",
    hoverBorder: "#c7c1b4",
    modal: "#17221e",
    modalShadow: "rgba(0, 0, 0, 0.5)",
    track: "#25332e",
    key: "#626d66",
    error: "#ff8c82",
    correct: "#27845f",
    present: "#d4aa2d",
    absent: "#626d66",
    transparent: "transparent",
    appGlowCorrect: "rgba(37, 132, 95, 0.18)",
    appGlowError: "rgba(201, 67, 61, 0.16)",
    titleText: "#e3ffe5",
    titleGlowSoft: "rgba(200, 250, 204, 0.78)",
    titleGlowMedium: "rgba(72, 214, 124, 0.52)",
    titleGlowWide: "rgba(72, 214, 124, 0.26)",
    onSolid: "#fff",
    onPresent: "#111816",
    loginButtonBorder: "#d9dce1",
    loginButtonBackground: "#fff",
    loginButtonText: "#1f1f1f",
    loginButtonShadow: "rgba(0, 0, 0, 0.18)",
    loginButtonHoverBorder: "#b9bec8",
    loadingSpinnerTrack: "rgba(170, 181, 174, 0.28)",
    shareButtonBorder: "rgba(39, 132, 95, 0.72)",
    shareButtonShadow: "rgba(39, 132, 95, 0.24)",
    terminalBackground: "#07100d",
    terminalText: "#c8facc",
    terminalInsetBorder: "rgba(170, 181, 174, 0.36)",
    terminalOverlay: "rgba(7, 16, 13, 0.98)",
    terminalWonText: "#d4ffd8",
    terminalWonBorder: "rgba(39, 132, 95, 0.34)",
    terminalWonRing: "rgba(39, 132, 95, 0.18)",
    terminalLostText: "#ffd5d1",
    terminalLostBorder: "rgba(255, 140, 130, 0.26)",
    terminalLostRing: "rgba(255, 140, 130, 0.14)",
    terminalCellBorder: "rgba(200, 250, 204, 0.42)",
    terminalCursor: "#c8facc",
    terminalCursorShadow: "rgba(200, 250, 204, 0.42)",
    terminalWonCellBorder: "rgba(39, 132, 95, 0.86)",
    terminalWonFilledText: "#edffee",
    terminalWonGlowSoft: "rgba(212, 255, 216, 0.82)",
    terminalWonGlowMedium: "rgba(39, 185, 101, 0.58)",
    terminalWonGlowWide: "rgba(39, 185, 101, 0.28)",
    terminalLostCellBorder: "rgba(255, 140, 130, 0.78)",
    terminalLostFilledText: "#fff0ee",
    terminalLostGlowSoft: "rgba(255, 213, 209, 0.8)",
    terminalLostGlowMedium: "rgba(255, 116, 103, 0.5)",
    terminalLostGlowWide: "rgba(255, 116, 103, 0.24)",
    modalBackdrop: "rgba(0, 0, 0, 0.58)",
    distributionBar: "#27845f",
    resultWon: "#23845f",
    resultLost: "#a8322d",
    googleBlue: "#4285f4",
    googleGreen: "#34a853",
    googleYellow: "#fbbc05",
    googleRed: "#ea4335"
  }
};

export type ThemeCssVariables = {
  [key: `--color-${string}`]: string;
};

const AppThemeContext = React.createContext<AppTheme>(baseAppTheme);

export function getAppTheme(conditions: AppThemeConditions): AppTheme {
  void conditions;
  return baseAppTheme;
}

export function getThemeCssVariables(theme: AppTheme): ThemeCssVariables {
  return Object.fromEntries(
    Object.entries(theme.colors).map(([name, value]) => [`--color-${toKebabCase(name)}`, value])
  ) as ThemeCssVariables;
}

export function applyThemeToDocument(theme: AppTheme, root = document.documentElement) {
  const cssVariables = getThemeCssVariables(theme);
  for (const [name, value] of Object.entries(cssVariables)) {
    root.style.setProperty(name, value);
  }
  root.style.colorScheme = theme.colorScheme;

  const themeColor = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
  themeColor?.setAttribute("content", theme.colors.page);
}

export function AppThemeProvider({
  children,
  conditions
}: {
  children: React.ReactNode;
  conditions: AppThemeConditions;
}) {
  const theme = React.useMemo(() => getAppTheme(conditions), [conditions]);

  React.useLayoutEffect(() => {
    applyThemeToDocument(theme);
  }, [theme]);

  return <AppThemeContext.Provider value={theme}>{children}</AppThemeContext.Provider>;
}

export function useAppTheme() {
  return React.useContext(AppThemeContext);
}

function toKebabCase(value: string) {
  return value.replace(/[A-Z]/g, (match) => `-${match.toLowerCase()}`);
}
