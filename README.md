# FleetPay Mobile (Capacitor)

App híbrido do FleetPay para Android e iOS usando Capacitor.
Carrega o PWA existente dentro de uma WebView nativa com suporte a push notifications.

## Pré-requisitos

- Node.js 18+
- Android Studio (para build Android)
- Xcode (para build iOS - apenas macOS)
- Conta Firebase (para push notifications)

## Configuração Inicial

```bash
cd fleetpay-mobile
npm install
npx cap sync
```

## Configurar Push Notifications (Firebase)

1. Acesse o [Firebase Console](https://console.firebase.google.com/)
2. Crie um projeto ou use um existente
3. Adicione um app Android com package: `com.fleetpay.app`
4. Baixe o arquivo `google-services.json`
5. Coloque em: `android/app/google-services.json`
6. Para iOS: baixe `GoogleService-Info.plist` e coloque em `ios/App/App/`

## Build Android

```bash
# Sincronizar alterações
npx cap sync android

# Abrir no Android Studio
npx cap open android

# Ou rodar direto no dispositivo/emulador
npx cap run android
```

### Gerar APK de Release

No Android Studio:
1. Build > Generate Signed Bundle/APK
2. Selecione APK ou Android App Bundle (AAB para Play Store)
3. Configure a keystore de assinatura
4. Selecione release
5. O APK será gerado em `android/app/build/outputs/`

## Build iOS

```bash
# Sincronizar alterações
npx cap sync ios

# Abrir no Xcode
npx cap open ios
```

No Xcode:
1. Selecione o target "App"
2. Configure o Team (Apple Developer Account)
3. Archive > Distribute App

## Estrutura do Projeto

```
fleetpay-mobile/
├── www/                    # Assets web (index.html de loading)
│   ├── index.html          # Tela de loading enquanto carrega o PWA
│   └── app.js              # Lógica do Capacitor (push, deeplinks)
├── android/                # Projeto Android nativo
├── ios/                    # Projeto iOS nativo
├── capacitor.config.ts     # Configuração do Capacitor
├── package.json            # Dependências
└── README.md               # Este arquivo
```

## Como Funciona

O app carrega a URL de produção `https://fleetpay.fleetcarservice.com.br/app/splash`
diretamente na WebView nativa. O usuário vê exatamente a mesma interface do PWA,
mas dentro de um container nativo com acesso a:

- Push Notifications (FCM/APNs)
- Status Bar personalizada
- Splash Screen nativa
- Deep Links
- Botão voltar nativo (Android)

## Personalização

### Ícone do App
Substitua os arquivos em:
- Android: `android/app/src/main/res/mipmap-*/`
- iOS: `ios/App/App/Assets.xcassets/AppIcon.appiconset/`

Recomendado: use o [capacitor-assets](https://github.com/ionic-team/capacitor-assets) para gerar automaticamente:
```bash
npm install -D @capacitor/assets
npx capacitor-assets generate --iconBackgroundColor '#003d82' --splashBackgroundColor '#003d82'
```

### Splash Screen
Substitua: `android/app/src/main/res/drawable/splash.png`

### URL de Produção
Altere em `capacitor.config.ts` no campo `server.url`

## Publicação nas Lojas

### Google Play Store
1. Gere um AAB (Android App Bundle) assinado
2. Crie uma conta no [Google Play Console](https://play.google.com/console)
3. Crie o app e faça upload do AAB
4. Preencha as informações da loja (screenshots, descrição)
5. Envie para revisão

### Apple App Store
1. Necessário Mac com Xcode
2. Conta no [Apple Developer Program](https://developer.apple.com/) ($99/ano)
3. Archive no Xcode e distribua via App Store Connect
4. Preencha as informações e envie para revisão
