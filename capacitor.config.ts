import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.auratv.app",
  appName: "AuraTV",
  webDir: "dist",
  // Load the live Lovable website directly so the app updates automatically
  server: {
    url: "https://auratvdz.lovable.app/",
  },
  android: {
    // Allow mixed content (some stream URLs may be HTTP)
    allowMixedContent: true,
    // Enable WebView debugging in dev
    webContentsDebuggingEnabled: false,
    // Background color matching AMOLED dark theme
    backgroundColor: "#050505",
  },
  plugins: {
    SplashScreen: {
      launchAutoHide: true,
      launchShowDuration: 2000,
      backgroundColor: "#050505",
      androidScaleType: "CENTER_CROP",
      showSpinner: false,
      splashFullScreen: true,
      splashImmersive: true,
    },
    StatusBar: {
      style: "DARK",
      backgroundColor: "#050505",
    },
  },
};

export default config;
