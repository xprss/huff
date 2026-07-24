import React from "react";

export type AppThemeMode = "dark" | "light";

export type AppThemeConditions = {
  preferredMode: AppThemeMode;
};

export type AppThemeColors = {
  page: string;
  surface: string;
  surfaceSoft: string;
  surfaceStrong: string;
  surfaceRaised: string;
  surfaceGlass: string;
  surfaceBorder: string;
  tile: string;
  tileBorder: string;
  tileFilled: string;
  text: string;
  muted: string;
  mutedStrong: string;
  primary: string;
  primaryStrong: string;
  accent: string;
  accentStrong: string;
  danger: string;
  button: string;
  buttonBorder: string;
  buttonShadow: string;
  hoverBorder: string;
  focusRing: string;
  modal: string;
  modalBorder: string;
  modalShadow: string;
  track: string;
  key: string;
  keyText: string;
  keyShadow: string;
  keyPressed: string;
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
  boardShadow: string;
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
  metricBackground: string;
  metricBorder: string;
  feedbackEmpty: string;
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
    page: "#0d1020",
    surface: "#15192b",
    surfaceSoft: "#1b2138",
    surfaceStrong: "#242b46",
    surfaceRaised: "#202743",
    surfaceGlass: "rgba(21, 25, 43, 0.78)",
    surfaceBorder: "rgba(144, 160, 196, 0.2)",
    tile: "#171c30",
    tileBorder: "#46506f",
    tileFilled: "#90a0c4",
    text: "#f7f4ea",
    muted: "#aeb8d2",
    mutedStrong: "#d4ddf4",
    primary: "#25d7a1",
    primaryStrong: "#11b985",
    accent: "#46c9ff",
    accentStrong: "#1798da",
    danger: "#ff6b7a",
    button: "#202743",
    buttonBorder: "rgba(144, 160, 196, 0.32)",
    buttonShadow: "rgba(4, 7, 20, 0.42)",
    hoverBorder: "rgba(70, 201, 255, 0.78)",
    focusRing: "rgba(70, 201, 255, 0.36)",
    modal: "#161b2f",
    modalBorder: "rgba(144, 160, 196, 0.22)",
    modalShadow: "rgba(4, 7, 20, 0.68)",
    track: "#242b46",
    key: "#2c3658",
    keyText: "#f7f4ea",
    keyShadow: "rgba(4, 7, 20, 0.36)",
    keyPressed: "#39466c",
    error: "#ff7c89",
    correct: "#25d7a1",
    present: "#ffd166",
    absent: "#566078",
    transparent: "transparent",
    appGlowCorrect: "rgba(37, 215, 161, 0.24)",
    appGlowError: "rgba(255, 107, 122, 0.2)",
    titleText: "#fff7dc",
    titleGlowSoft: "rgba(255, 247, 220, 0.48)",
    titleGlowMedium: "rgba(70, 201, 255, 0.36)",
    titleGlowWide: "rgba(37, 215, 161, 0.22)",
    onSolid: "#07130f",
    onPresent: "#221700",
    loginButtonBorder: "rgba(247, 244, 234, 0.8)",
    loginButtonBackground: "#f7f4ea",
    loginButtonText: "#15192b",
    loginButtonShadow: "rgba(4, 7, 20, 0.28)",
    loginButtonHoverBorder: "#46c9ff",
    loadingSpinnerTrack: "rgba(174, 184, 210, 0.22)",
    shareButtonBorder: "rgba(37, 215, 161, 0.72)",
    shareButtonShadow: "rgba(37, 215, 161, 0.28)",
    boardShadow: "rgba(4, 7, 20, 0.34)",
    terminalBackground: "#070a14",
    terminalText: "#dfffee",
    terminalInsetBorder: "rgba(70, 201, 255, 0.28)",
    terminalOverlay: "rgba(7, 10, 20, 0.96)",
    terminalWonText: "#ddfff1",
    terminalWonBorder: "rgba(37, 215, 161, 0.38)",
    terminalWonRing: "rgba(37, 215, 161, 0.18)",
    terminalLostText: "#ffe1e5",
    terminalLostBorder: "rgba(255, 107, 122, 0.28)",
    terminalLostRing: "rgba(255, 107, 122, 0.16)",
    terminalCellBorder: "rgba(70, 201, 255, 0.36)",
    terminalCursor: "#46c9ff",
    terminalCursorShadow: "rgba(70, 201, 255, 0.48)",
    terminalWonCellBorder: "rgba(37, 215, 161, 0.86)",
    terminalWonFilledText: "#f2fff8",
    terminalWonGlowSoft: "rgba(221, 255, 241, 0.66)",
    terminalWonGlowMedium: "rgba(37, 215, 161, 0.48)",
    terminalWonGlowWide: "rgba(37, 215, 161, 0.24)",
    terminalLostCellBorder: "rgba(255, 107, 122, 0.76)",
    terminalLostFilledText: "#fff0f2",
    terminalLostGlowSoft: "rgba(255, 225, 229, 0.62)",
    terminalLostGlowMedium: "rgba(255, 107, 122, 0.42)",
    terminalLostGlowWide: "rgba(255, 107, 122, 0.22)",
    modalBackdrop: "rgba(4, 7, 20, 0.68)",
    distributionBar: "#25d7a1",
    metricBackground: "rgba(32, 39, 67, 0.82)",
    metricBorder: "rgba(144, 160, 196, 0.2)",
    feedbackEmpty: "rgba(144, 160, 196, 0.12)",
    resultWon: "#25d7a1",
    resultLost: "#ff6b7a",
    googleBlue: "#4285f4",
    googleGreen: "#34a853",
    googleYellow: "#fbbc05",
    googleRed: "#ea4335"
  }
};

export const lightAppTheme: AppTheme = {
  id: "base",
  colorScheme: "light",
  colors: {
    page: "#f5f7fb",
    surface: "#ffffff",
    surfaceSoft: "#eef3fb",
    surfaceStrong: "#dde7f5",
    surfaceRaised: "#ffffff",
    surfaceGlass: "rgba(255, 255, 255, 0.82)",
    surfaceBorder: "rgba(55, 69, 103, 0.14)",
    tile: "#ffffff",
    tileBorder: "#b9c4d8",
    tileFilled: "#5f6f92",
    text: "#12182b",
    muted: "#65708b",
    mutedStrong: "#34405e",
    primary: "#00a879",
    primaryStrong: "#008765",
    accent: "#0678d8",
    accentStrong: "#055fac",
    danger: "#d83353",
    button: "#ffffff",
    buttonBorder: "rgba(55, 69, 103, 0.18)",
    buttonShadow: "rgba(44, 56, 86, 0.12)",
    hoverBorder: "rgba(6, 120, 216, 0.58)",
    focusRing: "rgba(6, 120, 216, 0.22)",
    modal: "#ffffff",
    modalBorder: "rgba(55, 69, 103, 0.14)",
    modalShadow: "rgba(44, 56, 86, 0.22)",
    track: "#e5ebf5",
    key: "#edf2fa",
    keyText: "#12182b",
    keyShadow: "rgba(44, 56, 86, 0.12)",
    keyPressed: "#dfe8f5",
    error: "#d83353",
    correct: "#00a879",
    present: "#f0b429",
    absent: "#748098",
    transparent: "transparent",
    appGlowCorrect: "rgba(0, 168, 121, 0.18)",
    appGlowError: "rgba(216, 51, 83, 0.12)",
    titleText: "#111729",
    titleGlowSoft: "rgba(255, 255, 255, 0.82)",
    titleGlowMedium: "rgba(6, 120, 216, 0.16)",
    titleGlowWide: "rgba(0, 168, 121, 0.14)",
    onSolid: "#ffffff",
    onPresent: "#231700",
    loginButtonBorder: "rgba(55, 69, 103, 0.16)",
    loginButtonBackground: "#ffffff",
    loginButtonText: "#12182b",
    loginButtonShadow: "rgba(44, 56, 86, 0.14)",
    loginButtonHoverBorder: "#0678d8",
    loadingSpinnerTrack: "rgba(101, 112, 139, 0.2)",
    shareButtonBorder: "rgba(0, 168, 121, 0.58)",
    shareButtonShadow: "rgba(0, 168, 121, 0.2)",
    boardShadow: "rgba(44, 56, 86, 0.12)",
    terminalBackground: "#101829",
    terminalText: "#ecfff7",
    terminalInsetBorder: "rgba(6, 120, 216, 0.32)",
    terminalOverlay: "rgba(16, 24, 41, 0.96)",
    terminalWonText: "#e9fff6",
    terminalWonBorder: "rgba(0, 168, 121, 0.36)",
    terminalWonRing: "rgba(0, 168, 121, 0.14)",
    terminalLostText: "#fff0f3",
    terminalLostBorder: "rgba(216, 51, 83, 0.28)",
    terminalLostRing: "rgba(216, 51, 83, 0.12)",
    terminalCellBorder: "rgba(70, 201, 255, 0.34)",
    terminalCursor: "#46c9ff",
    terminalCursorShadow: "rgba(70, 201, 255, 0.44)",
    terminalWonCellBorder: "rgba(0, 168, 121, 0.78)",
    terminalWonFilledText: "#f4fff9",
    terminalWonGlowSoft: "rgba(233, 255, 246, 0.6)",
    terminalWonGlowMedium: "rgba(0, 168, 121, 0.44)",
    terminalWonGlowWide: "rgba(0, 168, 121, 0.2)",
    terminalLostCellBorder: "rgba(216, 51, 83, 0.72)",
    terminalLostFilledText: "#fff5f7",
    terminalLostGlowSoft: "rgba(255, 240, 243, 0.6)",
    terminalLostGlowMedium: "rgba(216, 51, 83, 0.34)",
    terminalLostGlowWide: "rgba(216, 51, 83, 0.18)",
    modalBackdrop: "rgba(18, 24, 43, 0.34)",
    distributionBar: "#00a879",
    metricBackground: "#f6f8fc",
    metricBorder: "rgba(55, 69, 103, 0.12)",
    feedbackEmpty: "rgba(116, 128, 152, 0.16)",
    resultWon: "#008765",
    resultLost: "#d83353",
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
let themeSwitchFrame = 0;

export function getAppTheme(conditions: AppThemeConditions): AppTheme {
  return conditions.preferredMode === "light" ? lightAppTheme : baseAppTheme;
}

export function getThemeCssVariables(theme: AppTheme): ThemeCssVariables {
  return Object.fromEntries(
    Object.entries(theme.colors).map(([name, value]) => [`--color-${toKebabCase(name)}`, value])
  ) as ThemeCssVariables;
}

export function applyThemeToDocument(theme: AppTheme, root = document.documentElement) {
  root.classList.add("theme-switching");
  setThemeCssVariables(theme, root);
  window.cancelAnimationFrame(themeSwitchFrame);
  themeSwitchFrame = window.requestAnimationFrame(() => {
    themeSwitchFrame = window.requestAnimationFrame(() => {
      root.classList.remove("theme-switching");
    });
  });
}

function setThemeCssVariables(theme: AppTheme, root: HTMLElement) {
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
