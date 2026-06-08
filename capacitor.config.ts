import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.fleetpay.app',
  appName: 'FleetPay',
  webDir: 'www',
  server: {
    url: 'https://fleetpay.fleetcarservice.com.br/app/splash',
    cleartext: false,
    // Permite navegação dentro do mesmo domínio na WebView
    allowNavigation: ['fleetpay.fleetcarservice.com.br', '*.fleetcarservice.com.br']
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 1500,
      launchAutoHide: true,
      backgroundColor: '#003d82',
      showSpinner: true,
      spinnerColor: '#ffffff',
      androidScaleType: 'CENTER_CROP',
      splashFullScreen: true,
      splashImmersive: true
    },
    StatusBar: {
      style: 'DARK',
      backgroundColor: '#003d82',
      overlaysWebView: false
    },
    PushNotifications: {
      presentationOptions: ['badge', 'sound', 'alert']
    }
  },
  android: {
    allowMixedContent: true,
    captureInput: true,
    webContentsDebuggingEnabled: true,
    appendUserAgent: 'FleetPayApp',
    backgroundColor: '#003d82'
  },
  ios: {
    contentInset: 'always',
    allowsLinkPreview: false,
    scrollEnabled: true
  }
};

export default config;
