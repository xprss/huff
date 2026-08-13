import React from "react";

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
  id: string;
  name: string;
  colorScheme: "dark" | "light";
  colors: AppThemeColors;
};

export const THEME_STORAGE_KEY = "theme";

export const darkAppTheme: AppTheme = {
  id: "dark",
  name: "Dark",
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
  id: "light",
  name: "Light",
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

export const crazyPinkAppTheme: AppTheme = {
  id: "crazy-pink",
  name: "Crazy pink",
  colorScheme: "dark",
  colors: {
    ...darkAppTheme.colors,
    page: "#3b001f",
    surface: "#620035",
    surfaceSoft: "#7c0045",
    surfaceStrong: "#a6005a",
    surfaceRaised: "#860049",
    surfaceGlass: "rgba(98, 0, 53, 0.82)",
    surfaceBorder: "rgba(255, 164, 220, 0.34)",
    tile: "#56002f",
    tileBorder: "#ec62ae",
    tileFilled: "#ffadd6",
    text: "#fff1f9",
    muted: "#ffafd6",
    mutedStrong: "#ffd1e8",
    primary: "#ff3d9a",
    primaryStrong: "#e60076",
    accent: "#ff82c3",
    accentStrong: "#ff4cac",
    danger: "#ff8abe",
    button: "#820046",
    buttonBorder: "rgba(255, 183, 224, 0.48)",
    buttonShadow: "rgba(48, 0, 26, 0.52)",
    hoverBorder: "#ffb0db",
    focusRing: "rgba(255, 130, 195, 0.5)",
    modal: "#680039",
    modalBorder: "rgba(255, 183, 224, 0.38)",
    modalShadow: "rgba(48, 0, 26, 0.7)",
    track: "#9e0056",
    key: "#9d0055",
    keyText: "#fff1f9",
    keyShadow: "rgba(48, 0, 26, 0.42)",
    keyPressed: "#c9006b",
    error: "#ff91bd",
    correct: "#ff3d9a",
    present: "#ffb3dc",
    absent: "#8f4770",
    appGlowCorrect: "rgba(255, 61, 154, 0.34)",
    appGlowError: "rgba(255, 138, 190, 0.26)",
    titleText: "#fff3fa",
    titleGlowSoft: "rgba(255, 223, 241, 0.68)",
    titleGlowMedium: "rgba(255, 100, 184, 0.58)",
    titleGlowWide: "rgba(255, 61, 154, 0.34)",
    onSolid: "#420022",
    onPresent: "#4a0027",
    loginButtonBorder: "#ffd1e8",
    loginButtonBackground: "#fff1f9",
    loginButtonText: "#620035",
    loginButtonShadow: "rgba(48, 0, 26, 0.38)",
    loginButtonHoverBorder: "#ff82c3",
    loadingSpinnerTrack: "rgba(255, 175, 214, 0.3)",
    shareButtonBorder: "rgba(255, 61, 154, 0.84)",
    shareButtonShadow: "rgba(255, 61, 154, 0.36)",
    boardShadow: "rgba(48, 0, 26, 0.4)",
    terminalBackground: "#300019",
    terminalText: "#fff0f8",
    terminalInsetBorder: "rgba(255, 130, 195, 0.48)",
    terminalOverlay: "rgba(48, 0, 26, 0.96)",
    terminalWonText: "#fff0f8",
    terminalWonBorder: "rgba(255, 61, 154, 0.58)",
    terminalWonRing: "rgba(255, 61, 154, 0.28)",
    terminalLostText: "#fff0f8",
    terminalLostBorder: "rgba(255, 138, 190, 0.5)",
    terminalLostRing: "rgba(255, 138, 190, 0.24)",
    terminalCellBorder: "rgba(255, 130, 195, 0.56)",
    terminalCursor: "#ff82c3",
    terminalCursorShadow: "rgba(255, 130, 195, 0.62)",
    terminalWonCellBorder: "#ff75b7",
    terminalWonFilledText: "#fff5fb",
    terminalWonGlowSoft: "rgba(255, 231, 245, 0.76)",
    terminalWonGlowMedium: "rgba(255, 61, 154, 0.62)",
    terminalWonGlowWide: "rgba(255, 61, 154, 0.34)",
    terminalLostCellBorder: "#ff9ac8",
    terminalLostFilledText: "#fff5fb",
    terminalLostGlowSoft: "rgba(255, 232, 245, 0.72)",
    terminalLostGlowMedium: "rgba(255, 138, 190, 0.56)",
    terminalLostGlowWide: "rgba(255, 138, 190, 0.3)",
    modalBackdrop: "rgba(48, 0, 26, 0.7)",
    distributionBar: "#ff3d9a",
    metricBackground: "rgba(134, 0, 73, 0.84)",
    metricBorder: "rgba(255, 183, 224, 0.3)",
    feedbackEmpty: "rgba(255, 175, 214, 0.18)",
    resultWon: "#ff3d9a",
    resultLost: "#ff91bd"
  }
};

export const matrixAppTheme: AppTheme = {
  id: "matrix",
  name: "Matrix",
  colorScheme: "dark",
  colors: {
    ...darkAppTheme.colors,
    page: "#020805",
    surface: "#07150d",
    surfaceSoft: "#0b2414",
    surfaceStrong: "#103b1d",
    surfaceRaised: "#0c2e17",
    surfaceGlass: "rgba(7, 21, 13, 0.84)",
    surfaceBorder: "rgba(71, 255, 126, 0.26)",
    tile: "#06120b",
    tileBorder: "#247b3b",
    tileFilled: "#62c77a",
    text: "#d9ffe1",
    muted: "#82bd91",
    mutedStrong: "#b8efc4",
    primary: "#00e83a",
    primaryStrong: "#00b92e",
    accent: "#57ff7d",
    accentStrong: "#1fd850",
    danger: "#ff5964",
    button: "#0b2814",
    buttonBorder: "rgba(71, 255, 126, 0.36)",
    buttonShadow: "rgba(0, 0, 0, 0.58)",
    hoverBorder: "#76ff96",
    focusRing: "rgba(87, 255, 125, 0.42)",
    modal: "#071a0d",
    modalBorder: "rgba(71, 255, 126, 0.32)",
    modalShadow: "rgba(0, 0, 0, 0.76)",
    track: "#103b1d",
    key: "#124b22",
    keyText: "#dcffe4",
    keyShadow: "rgba(0, 0, 0, 0.5)",
    keyPressed: "#196a2d",
    error: "#ff6d73",
    correct: "#00e83a",
    present: "#b2ff4b",
    absent: "#31503a",
    appGlowCorrect: "rgba(0, 232, 58, 0.24)",
    appGlowError: "rgba(255, 89, 100, 0.14)",
    titleText: "#d7ffe1",
    titleGlowSoft: "rgba(180, 255, 194, 0.46)",
    titleGlowMedium: "rgba(0, 232, 58, 0.36)",
    titleGlowWide: "rgba(0, 150, 40, 0.3)",
    onSolid: "#001b08",
    onPresent: "#152500",
    loginButtonBorder: "#b8efc4",
    loginButtonBackground: "#d9ffe1",
    loginButtonText: "#06210e",
    loginButtonShadow: "rgba(0, 0, 0, 0.5)",
    loginButtonHoverBorder: "#57ff7d",
    loadingSpinnerTrack: "rgba(130, 189, 145, 0.24)",
    shareButtonBorder: "rgba(0, 232, 58, 0.72)",
    shareButtonShadow: "rgba(0, 232, 58, 0.3)",
    boardShadow: "rgba(0, 0, 0, 0.54)",
    terminalBackground: "#010603",
    terminalText: "#73ff90",
    terminalInsetBorder: "rgba(87, 255, 125, 0.34)",
    terminalOverlay: "rgba(1, 6, 3, 0.97)",
    terminalWonText: "#cbffd5",
    terminalWonBorder: "rgba(0, 232, 58, 0.46)",
    terminalWonRing: "rgba(0, 232, 58, 0.2)",
    terminalLostText: "#ffdce0",
    terminalLostBorder: "rgba(255, 89, 100, 0.34)",
    terminalLostRing: "rgba(255, 89, 100, 0.16)",
    terminalCellBorder: "rgba(87, 255, 125, 0.5)",
    terminalCursor: "#57ff7d",
    terminalCursorShadow: "rgba(87, 255, 125, 0.62)",
    terminalWonCellBorder: "#00e83a",
    terminalWonFilledText: "#e4ffea",
    terminalWonGlowSoft: "rgba(203, 255, 213, 0.64)",
    terminalWonGlowMedium: "rgba(0, 232, 58, 0.5)",
    terminalWonGlowWide: "rgba(0, 232, 58, 0.26)",
    terminalLostCellBorder: "#ff6d73",
    terminalLostFilledText: "#fff1f2",
    terminalLostGlowSoft: "rgba(255, 220, 224, 0.6)",
    terminalLostGlowMedium: "rgba(255, 89, 100, 0.4)",
    terminalLostGlowWide: "rgba(255, 89, 100, 0.2)",
    modalBackdrop: "rgba(0, 6, 2, 0.76)",
    distributionBar: "#00e83a",
    metricBackground: "rgba(9, 37, 18, 0.86)",
    metricBorder: "rgba(71, 255, 126, 0.22)",
    feedbackEmpty: "rgba(71, 255, 126, 0.12)",
    resultWon: "#00e83a",
    resultLost: "#ff6d73"
  }
};

export const pannaAppTheme: AppTheme = {
  id: "panna",
  name: "Panna",
  colorScheme: "light",
  colors: {
    ...lightAppTheme.colors,
    page: "#fff8ec",
    surface: "#fffdf8",
    surfaceSoft: "#f9eddb",
    surfaceStrong: "#f0ddc3",
    surfaceRaised: "#fffaf2",
    surfaceGlass: "rgba(255, 253, 248, 0.86)",
    surfaceBorder: "rgba(146, 111, 75, 0.16)",
    tile: "#fffdf8",
    tileBorder: "#dcbf9f",
    tileFilled: "#9c7d60",
    text: "#45362c",
    muted: "#8b7461",
    mutedStrong: "#665140",
    primary: "#b9806e",
    primaryStrong: "#986251",
    accent: "#9d9bca",
    accentStrong: "#7d7bb0",
    danger: "#c8797b",
    button: "#fffaf2",
    buttonBorder: "rgba(146, 111, 75, 0.2)",
    buttonShadow: "rgba(112, 81, 55, 0.12)",
    hoverBorder: "#b9b5df",
    focusRing: "rgba(157, 155, 202, 0.26)",
    modal: "#fffdf8",
    modalBorder: "rgba(146, 111, 75, 0.16)",
    modalShadow: "rgba(112, 81, 55, 0.2)",
    track: "#f1e5d3",
    key: "#f5e7d6",
    keyText: "#45362c",
    keyShadow: "rgba(112, 81, 55, 0.12)",
    keyPressed: "#ead6c0",
    error: "#c8797b",
    correct: "#a5b985",
    present: "#dfbb78",
    absent: "#aa998a",
    appGlowCorrect: "rgba(165, 185, 133, 0.2)",
    appGlowError: "rgba(200, 121, 123, 0.12)",
    titleText: "#554335",
    titleGlowSoft: "rgba(255, 253, 248, 0.9)",
    titleGlowMedium: "rgba(157, 155, 202, 0.2)",
    titleGlowWide: "rgba(165, 185, 133, 0.18)",
    onSolid: "#fffdf8",
    onPresent: "#4c3517",
    loginButtonBorder: "rgba(146, 111, 75, 0.18)",
    loginButtonBackground: "#fffdf8",
    loginButtonText: "#45362c",
    loginButtonShadow: "rgba(112, 81, 55, 0.14)",
    loginButtonHoverBorder: "#9d9bca",
    loadingSpinnerTrack: "rgba(139, 116, 97, 0.2)",
    shareButtonBorder: "rgba(165, 185, 133, 0.62)",
    shareButtonShadow: "rgba(165, 185, 133, 0.22)",
    boardShadow: "rgba(112, 81, 55, 0.12)",
    terminalBackground: "#393128",
    terminalText: "#fff7e9",
    terminalInsetBorder: "rgba(157, 155, 202, 0.38)",
    terminalOverlay: "rgba(57, 49, 40, 0.96)",
    terminalWonText: "#f4ffec",
    terminalWonBorder: "rgba(165, 185, 133, 0.46)",
    terminalWonRing: "rgba(165, 185, 133, 0.2)",
    terminalLostText: "#fff2f0",
    terminalLostBorder: "rgba(200, 121, 123, 0.36)",
    terminalLostRing: "rgba(200, 121, 123, 0.16)",
    terminalCellBorder: "rgba(157, 155, 202, 0.48)",
    terminalCursor: "#b9b5df",
    terminalCursorShadow: "rgba(157, 155, 202, 0.56)",
    terminalWonCellBorder: "#a5b985",
    terminalWonFilledText: "#f5ffef",
    terminalWonGlowSoft: "rgba(244, 255, 236, 0.7)",
    terminalWonGlowMedium: "rgba(165, 185, 133, 0.52)",
    terminalWonGlowWide: "rgba(165, 185, 133, 0.26)",
    terminalLostCellBorder: "#d7898b",
    terminalLostFilledText: "#fff7f5",
    terminalLostGlowSoft: "rgba(255, 242, 240, 0.68)",
    terminalLostGlowMedium: "rgba(200, 121, 123, 0.42)",
    terminalLostGlowWide: "rgba(200, 121, 123, 0.22)",
    modalBackdrop: "rgba(69, 54, 44, 0.34)",
    distributionBar: "#a5b985",
    metricBackground: "#fbf2e5",
    metricBorder: "rgba(146, 111, 75, 0.12)",
    feedbackEmpty: "rgba(170, 153, 138, 0.18)",
    resultWon: "#849c62",
    resultLost: "#c8797b"
  }
};

export const chromaticAppTheme: AppTheme = {
  id: "chromatic",
  name: "Chromatic",
  colorScheme: "dark",
  colors: {
    ...darkAppTheme.colors,
    page: "#090b12",
    surface: "#161924",
    surfaceSoft: "#242838",
    surfaceStrong: "#353c51",
    surfaceRaised: "#2b3041",
    surfaceGlass: "rgba(28, 32, 46, 0.82)",
    surfaceBorder: "rgba(222, 231, 255, 0.44)",
    tile: "#1b1f2b",
    tileBorder: "#8792ac",
    tileFilled: "#c7d1e7",
    text: "#f5f7ff",
    muted: "#adb7cc",
    mutedStrong: "#d9e0f0",
    primary: "#aab5cc",
    primaryStrong: "#7d899f",
    accent: "#dce5fa",
    accentStrong: "#9ca9c2",
    danger: "#ff8196",
    button: "#262b3b",
    buttonBorder: "rgba(224, 234, 255, 0.48)",
    buttonShadow: "rgba(0, 0, 0, 0.56)",
    hoverBorder: "#ffffff",
    focusRing: "rgba(222, 231, 255, 0.52)",
    modal: "#1b1f2c",
    modalBorder: "rgba(224, 234, 255, 0.42)",
    modalShadow: "rgba(0, 0, 0, 0.72)",
    track: "#3b4358",
    key: "#32394c",
    keyText: "#f5f7ff",
    keyShadow: "rgba(0, 0, 0, 0.46)",
    keyPressed: "#5a647b",
    error: "#ff8196",
    correct: "#6ce4cf",
    present: "#f8cb7b",
    absent: "#677186",
    appGlowCorrect: "rgba(107, 232, 255, 0.22)",
    appGlowError: "rgba(255, 93, 181, 0.18)",
    titleText: "#ffffff",
    titleGlowSoft: "rgba(255, 255, 255, 0.62)",
    titleGlowMedium: "rgba(166, 203, 255, 0.48)",
    titleGlowWide: "rgba(213, 125, 255, 0.28)",
    onSolid: "#ffffff",
    onPresent: "#2c2105",
    loginButtonBorder: "#e4ebff",
    loginButtonBackground: "#f1f4ff",
    loginButtonText: "#1a1e2b",
    loginButtonShadow: "rgba(0, 0, 0, 0.44)",
    loginButtonHoverBorder: "#ffffff",
    loadingSpinnerTrack: "rgba(218, 227, 250, 0.22)",
    shareButtonBorder: "rgba(188, 215, 255, 0.76)",
    shareButtonShadow: "rgba(120, 182, 255, 0.32)",
    boardShadow: "rgba(0, 0, 0, 0.52)",
    terminalBackground: "#080a10",
    terminalText: "#e9efff",
    terminalInsetBorder: "rgba(180, 206, 255, 0.5)",
    terminalOverlay: "rgba(8, 10, 16, 0.97)",
    terminalWonText: "#eafffb",
    terminalWonBorder: "rgba(108, 228, 207, 0.5)",
    terminalWonRing: "rgba(108, 228, 207, 0.2)",
    terminalLostText: "#fff0f3",
    terminalLostBorder: "rgba(255, 129, 150, 0.4)",
    terminalLostRing: "rgba(255, 129, 150, 0.18)",
    terminalCellBorder: "rgba(190, 215, 255, 0.56)",
    terminalCursor: "#e0e9ff",
    terminalCursorShadow: "rgba(180, 206, 255, 0.66)",
    terminalWonCellBorder: "#6ce4cf",
    terminalWonFilledText: "#effffb",
    terminalWonGlowSoft: "rgba(234, 255, 251, 0.7)",
    terminalWonGlowMedium: "rgba(108, 228, 207, 0.56)",
    terminalWonGlowWide: "rgba(108, 228, 207, 0.28)",
    terminalLostCellBorder: "#ff98aa",
    terminalLostFilledText: "#fff5f7",
    terminalLostGlowSoft: "rgba(255, 240, 243, 0.66)",
    terminalLostGlowMedium: "rgba(255, 129, 150, 0.46)",
    terminalLostGlowWide: "rgba(255, 129, 150, 0.24)",
    modalBackdrop: "rgba(4, 5, 10, 0.76)",
    distributionBar: "#8ec5ff",
    metricBackground: "rgba(42, 47, 64, 0.9)",
    metricBorder: "rgba(224, 234, 255, 0.28)",
    feedbackEmpty: "rgba(173, 183, 204, 0.18)",
    resultWon: "#6ce4cf",
    resultLost: "#ff8196"
  }
};

export const noirAppTheme: AppTheme = {
  id: "noir",
  name: "Noir",
  colorScheme: "dark",
  colors: {
    ...darkAppTheme.colors,
    page: "#000000",
    surface: "#111111",
    surfaceSoft: "#1c1c1c",
    surfaceStrong: "#303030",
    surfaceRaised: "#242424",
    surfaceGlass: "rgba(17, 17, 17, 0.84)",
    surfaceBorder: "rgba(255, 255, 255, 0.26)",
    tile: "#101010",
    tileBorder: "#777777",
    tileFilled: "#c9c9c9",
    text: "#ffffff",
    muted: "#b5b5b5",
    mutedStrong: "#dedede",
    primary: "#ffffff",
    primaryStrong: "#d0d0d0",
    accent: "#ffffff",
    accentStrong: "#d0d0d0",
    danger: "#ffffff",
    button: "#222222",
    buttonBorder: "rgba(255, 255, 255, 0.38)",
    buttonShadow: "rgba(0, 0, 0, 0.68)",
    hoverBorder: "#ffffff",
    focusRing: "rgba(255, 255, 255, 0.5)",
    modal: "#151515",
    modalBorder: "rgba(255, 255, 255, 0.3)",
    modalShadow: "rgba(0, 0, 0, 0.8)",
    track: "#333333",
    key: "#303030",
    keyText: "#ffffff",
    keyShadow: "rgba(0, 0, 0, 0.56)",
    keyPressed: "#555555",
    error: "#ffffff",
    correct: "#ffffff",
    present: "#c9c9c9",
    absent: "#5e5e5e",
    appGlowCorrect: "rgba(255, 255, 255, 0.14)",
    appGlowError: "rgba(255, 255, 255, 0.08)",
    titleText: "#ffffff",
    titleGlowSoft: "rgba(255, 255, 255, 0.38)",
    titleGlowMedium: "rgba(255, 255, 255, 0.2)",
    titleGlowWide: "rgba(255, 255, 255, 0.1)",
    onSolid: "#000000",
    onPresent: "#000000",
    loginButtonBorder: "#ffffff",
    loginButtonBackground: "#ffffff",
    loginButtonText: "#000000",
    loginButtonShadow: "rgba(0, 0, 0, 0.5)",
    loginButtonHoverBorder: "#ffffff",
    loadingSpinnerTrack: "rgba(255, 255, 255, 0.2)",
    shareButtonBorder: "rgba(255, 255, 255, 0.72)",
    shareButtonShadow: "rgba(255, 255, 255, 0.22)",
    boardShadow: "rgba(0, 0, 0, 0.66)",
    terminalBackground: "#000000",
    terminalText: "#ffffff",
    terminalInsetBorder: "rgba(255, 255, 255, 0.36)",
    terminalOverlay: "rgba(0, 0, 0, 0.97)",
    terminalWonText: "#ffffff",
    terminalWonBorder: "rgba(255, 255, 255, 0.5)",
    terminalWonRing: "rgba(255, 255, 255, 0.16)",
    terminalLostText: "#ffffff",
    terminalLostBorder: "rgba(255, 255, 255, 0.4)",
    terminalLostRing: "rgba(255, 255, 255, 0.14)",
    terminalCellBorder: "rgba(255, 255, 255, 0.46)",
    terminalCursor: "#ffffff",
    terminalCursorShadow: "rgba(255, 255, 255, 0.56)",
    terminalWonCellBorder: "#ffffff",
    terminalWonFilledText: "#ffffff",
    terminalWonGlowSoft: "rgba(255, 255, 255, 0.64)",
    terminalWonGlowMedium: "rgba(255, 255, 255, 0.4)",
    terminalWonGlowWide: "rgba(255, 255, 255, 0.2)",
    terminalLostCellBorder: "#ffffff",
    terminalLostFilledText: "#ffffff",
    terminalLostGlowSoft: "rgba(255, 255, 255, 0.6)",
    terminalLostGlowMedium: "rgba(255, 255, 255, 0.36)",
    terminalLostGlowWide: "rgba(255, 255, 255, 0.18)",
    modalBackdrop: "rgba(0, 0, 0, 0.78)",
    distributionBar: "#ffffff",
    metricBackground: "rgba(35, 35, 35, 0.9)",
    metricBorder: "rgba(255, 255, 255, 0.2)",
    feedbackEmpty: "rgba(255, 255, 255, 0.12)",
    resultWon: "#ffffff",
    resultLost: "#ffffff"
  }
};

export const childhoodAppTheme: AppTheme = {
  id: "childhood",
  name: "Childhood",
  colorScheme: "light",
  colors: {
    ...lightAppTheme.colors,
    page: "#69dcff",
    surface: "#fffdf2",
    surfaceSoft: "#fff1a8",
    surfaceStrong: "#ffcb4d",
    surfaceRaised: "#fff8cf",
    surfaceGlass: "rgba(255, 253, 242, 0.86)",
    surfaceBorder: "rgba(42, 67, 148, 0.28)",
    tile: "#ffffff",
    tileBorder: "#4365db",
    tileFilled: "#2d47a7",
    text: "#172250",
    muted: "#465a9a",
    mutedStrong: "#263b7c",
    primary: "#ff3c86",
    primaryStrong: "#dd1663",
    accent: "#6546e8",
    accentStrong: "#442ac7",
    danger: "#ed265b",
    button: "#ffffff",
    buttonBorder: "rgba(40, 59, 140, 0.32)",
    buttonShadow: "rgba(44, 70, 158, 0.2)",
    hoverBorder: "#ff3c86",
    focusRing: "rgba(101, 70, 232, 0.32)",
    modal: "#fffdf2",
    modalBorder: "rgba(40, 59, 140, 0.26)",
    modalShadow: "rgba(30, 57, 143, 0.28)",
    track: "#ffe18a",
    key: "#9af26f",
    keyText: "#173f1d",
    keyShadow: "rgba(44, 111, 51, 0.22)",
    keyPressed: "#69dd48",
    error: "#ed265b",
    correct: "#20c962",
    present: "#ffad20",
    absent: "#728ac2",
    appGlowCorrect: "rgba(32, 201, 98, 0.24)",
    appGlowError: "rgba(255, 60, 134, 0.2)",
    titleText: "#ef1665",
    titleGlowSoft: "rgba(255, 255, 255, 0.9)",
    titleGlowMedium: "rgba(255, 50, 141, 0.48)",
    titleGlowWide: "rgba(101, 70, 232, 0.34)",
    onSolid: "#ffffff",
    onPresent: "#4a2800",
    loginButtonBorder: "#4c38c9",
    loginButtonBackground: "#ffffff",
    loginButtonText: "#172250",
    loginButtonShadow: "rgba(44, 70, 158, 0.22)",
    loginButtonHoverBorder: "#ff3c86",
    loadingSpinnerTrack: "rgba(70, 90, 154, 0.22)",
    shareButtonBorder: "rgba(255, 60, 134, 0.68)",
    shareButtonShadow: "rgba(255, 60, 134, 0.26)",
    boardShadow: "rgba(31, 74, 174, 0.2)",
    terminalBackground: "#202772",
    terminalText: "#fffdf2",
    terminalInsetBorder: "rgba(118, 252, 255, 0.58)",
    terminalOverlay: "rgba(32, 39, 114, 0.96)",
    terminalWonText: "#edfff1",
    terminalWonBorder: "rgba(32, 201, 98, 0.58)",
    terminalWonRing: "rgba(32, 201, 98, 0.24)",
    terminalLostText: "#fff1f5",
    terminalLostBorder: "rgba(237, 38, 91, 0.5)",
    terminalLostRing: "rgba(237, 38, 91, 0.22)",
    terminalCellBorder: "rgba(118, 252, 255, 0.56)",
    terminalCursor: "#76fcff",
    terminalCursorShadow: "rgba(118, 252, 255, 0.66)",
    terminalWonCellBorder: "#20c962",
    terminalWonFilledText: "#f2fff5",
    terminalWonGlowSoft: "rgba(237, 255, 241, 0.72)",
    terminalWonGlowMedium: "rgba(32, 201, 98, 0.58)",
    terminalWonGlowWide: "rgba(32, 201, 98, 0.3)",
    terminalLostCellBorder: "#ff5381",
    terminalLostFilledText: "#fff5f7",
    terminalLostGlowSoft: "rgba(255, 241, 245, 0.7)",
    terminalLostGlowMedium: "rgba(237, 38, 91, 0.48)",
    terminalLostGlowWide: "rgba(237, 38, 91, 0.26)",
    modalBackdrop: "rgba(23, 34, 80, 0.42)",
    distributionBar: "#20c962",
    metricBackground: "#fff7c8",
    metricBorder: "rgba(40, 59, 140, 0.18)",
    feedbackEmpty: "rgba(114, 138, 194, 0.2)",
    resultWon: "#20a955",
    resultLost: "#ed265b"
  }
};

export const tricotopiaAppTheme: AppTheme = {
  id: "tricotopia",
  name: "Tricotopia",
  colorScheme: "light",
  colors: {
    ...lightAppTheme.colors,
    page: "#d9d3e8",
    surface: "#fbf8f0",
    surfaceSoft: "#eee7d6",
    surfaceStrong: "#d7c8b3",
    surfaceRaised: "#fffaf0",
    surfaceGlass: "rgba(251, 248, 240, 0.86)",
    surfaceBorder: "rgba(104, 82, 112, 0.2)",
    tile: "#fffaf0",
    tileBorder: "#b79fb4",
    tileFilled: "#816b81",
    text: "#493c4e",
    muted: "#827181",
    mutedStrong: "#625165",
    primary: "#b86d83",
    primaryStrong: "#985267",
    accent: "#778fb2",
    accentStrong: "#596f94",
    danger: "#c96f76",
    button: "#f9f2e4",
    buttonBorder: "rgba(104, 82, 112, 0.22)",
    buttonShadow: "rgba(80, 58, 81, 0.14)",
    hoverBorder: "#b86d83",
    focusRing: "rgba(119, 143, 178, 0.28)",
    modal: "#fbf8f0",
    modalBorder: "rgba(104, 82, 112, 0.18)",
    modalShadow: "rgba(80, 58, 81, 0.24)",
    track: "#e4d9c8",
    key: "#d7e1cb",
    keyText: "#3f5240",
    keyShadow: "rgba(65, 82, 64, 0.16)",
    keyPressed: "#b8caad",
    error: "#c96f76",
    correct: "#86a978",
    present: "#d8ad6b",
    absent: "#9d91a2",
    appGlowCorrect: "rgba(134, 169, 120, 0.2)",
    appGlowError: "rgba(201, 111, 118, 0.13)",
    titleText: "#765574",
    titleGlowSoft: "rgba(255, 252, 245, 0.82)",
    titleGlowMedium: "rgba(184, 109, 131, 0.22)",
    titleGlowWide: "rgba(119, 143, 178, 0.2)",
    onSolid: "#fffdf7",
    onPresent: "#4a3210",
    loginButtonBorder: "rgba(104, 82, 112, 0.24)",
    loginButtonBackground: "#fffaf0",
    loginButtonText: "#493c4e",
    loginButtonShadow: "rgba(80, 58, 81, 0.16)",
    loginButtonHoverBorder: "#b86d83",
    loadingSpinnerTrack: "rgba(130, 113, 129, 0.2)",
    shareButtonBorder: "rgba(134, 169, 120, 0.64)",
    shareButtonShadow: "rgba(134, 169, 120, 0.24)",
    boardShadow: "rgba(80, 58, 81, 0.16)",
    terminalBackground: "#423846",
    terminalText: "#fff8ed",
    terminalInsetBorder: "rgba(172, 191, 225, 0.46)",
    terminalOverlay: "rgba(66, 56, 70, 0.96)",
    terminalWonText: "#f1ffed",
    terminalWonBorder: "rgba(134, 169, 120, 0.48)",
    terminalWonRing: "rgba(134, 169, 120, 0.2)",
    terminalLostText: "#fff0f1",
    terminalLostBorder: "rgba(201, 111, 118, 0.38)",
    terminalLostRing: "rgba(201, 111, 118, 0.16)",
    terminalCellBorder: "rgba(172, 191, 225, 0.48)",
    terminalCursor: "#acbfe1",
    terminalCursorShadow: "rgba(172, 191, 225, 0.58)",
    terminalWonCellBorder: "#86a978",
    terminalWonFilledText: "#f4fff0",
    terminalWonGlowSoft: "rgba(241, 255, 237, 0.68)",
    terminalWonGlowMedium: "rgba(134, 169, 120, 0.5)",
    terminalWonGlowWide: "rgba(134, 169, 120, 0.24)",
    terminalLostCellBorder: "#d6868d",
    terminalLostFilledText: "#fff7f7",
    terminalLostGlowSoft: "rgba(255, 240, 241, 0.66)",
    terminalLostGlowMedium: "rgba(201, 111, 118, 0.42)",
    terminalLostGlowWide: "rgba(201, 111, 118, 0.2)",
    modalBackdrop: "rgba(73, 60, 78, 0.38)",
    distributionBar: "#86a978",
    metricBackground: "#f4ecdf",
    metricBorder: "rgba(104, 82, 112, 0.14)",
    feedbackEmpty: "rgba(157, 145, 162, 0.18)",
    resultWon: "#6d8d61",
    resultLost: "#c96f76"
  }
};

/** Add new selectable themes to this registry. */
export const appThemes = [darkAppTheme, lightAppTheme, crazyPinkAppTheme, matrixAppTheme, pannaAppTheme, chromaticAppTheme, noirAppTheme, childhoodAppTheme, tricotopiaAppTheme] as const satisfies readonly AppTheme[];

export type ThemeCssVariables = {
  [key: `--color-${string}`]: string;
};

const AppThemeContext = React.createContext<AppTheme>(darkAppTheme);
let themeSwitchFrame = 0;

export function getAppTheme(themeId: string | null | undefined): AppTheme {
  return appThemes.find((theme) => theme.id === themeId) ?? darkAppTheme;
}

export function getStoredThemeId(): string {
  const storedThemeId = localStorage.getItem(THEME_STORAGE_KEY);
  if (storedThemeId && appThemes.some((theme) => theme.id === storedThemeId)) return storedThemeId;

  // Preserve the preference saved by versions that only offered light and dark modes.
  return localStorage.getItem("darkMode") === "false" ? lightAppTheme.id : darkAppTheme.id;
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
  root.dataset.theme = theme.id;

  const themeColor = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
  themeColor?.setAttribute("content", theme.colors.page);
}

export function AppThemeProvider({
  children,
  theme
}: {
  children: React.ReactNode;
  theme: AppTheme;
}) {
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
