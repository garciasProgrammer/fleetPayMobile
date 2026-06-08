import { App } from '@capacitor/app';
import { PushNotifications } from '@capacitor/push-notifications';
import { Geolocation } from '@capacitor/geolocation';
import { SplashScreen } from '@capacitor/splash-screen';
import { StatusBar, Style } from '@capacitor/status-bar';
import { Browser } from '@capacitor/browser';

// URL de produção do FleetPay
const FLEETPAY_URL = 'https://fleetpay.fleetcarservice.com.br/app/splash';

// Inicialização do app
async function initApp() {
    try {
        // Configura status bar
        await StatusBar.setStyle({ style: Style.Dark });
        await StatusBar.setBackgroundColor({ color: '#003d82' });
    } catch (e) {
        // Web fallback
    }

    // Redireciona para o FleetPay
    window.location.href = FLEETPAY_URL;
}

// Push Notifications
async function initPushNotifications() {
    try {
        const permission = await PushNotifications.requestPermissions();

        if (permission.receive === 'granted') {
            await PushNotifications.register();
        }

        // Token do dispositivo (enviar pro backend)
        PushNotifications.addListener('registration', (token) => {
            console.log('[Push] Token:', token.value);
            // Envia o token para o backend do FleetPay
            sendTokenToBackend(token.value);
        });

        // Erro no registro
        PushNotifications.addListener('registrationError', (error) => {
            console.error('[Push] Erro no registro:', error);
        });

        // Notificação recebida com app aberto
        PushNotifications.addListener('pushNotificationReceived', (notification) => {
            console.log('[Push] Recebida:', notification);
        });

        // Usuário clicou na notificação
        PushNotifications.addListener('pushNotificationActionPerformed', (action) => {
            console.log('[Push] Ação:', action);
            const data = action.notification.data;
            if (data && data.url) {
                window.location.href = data.url;
            }
        });
    } catch (e) {
        console.warn('[Push] Não suportado:', e);
    }
}

// Envia token FCM para o backend
async function sendTokenToBackend(token) {
    try {
        await fetch('https://fleetpay.fleetcarservice.com.br/api/push/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ token, platform: getPlatform() })
        });
    } catch (e) {
        console.warn('[Push] Erro ao enviar token:', e);
    }
}

function getPlatform() {
    if (/android/i.test(navigator.userAgent)) return 'android';
    if (/iphone|ipad/i.test(navigator.userAgent)) return 'ios';
    return 'web';
}

// Gerencia botão voltar (Android)
App.addListener('backButton', ({ canGoBack }) => {
    if (canGoBack) {
        window.history.back();
    } else {
        App.exitApp();
    }
});

// Gerencia retorno ao app (resume)
App.addListener('appStateChange', ({ isActive }) => {
    if (isActive) {
        console.log('[App] Retornou ao primeiro plano');
    }
});

// Deep links
App.addListener('appUrlOpen', (event) => {
    const url = event.url;
    console.log('[DeepLink]', url);
    if (url.includes('fleetpay.fleetcarservice.com.br')) {
        window.location.href = url;
    }
});

// Geolocation
async function initGeolocation() {
    try {
        const permission = await Geolocation.requestPermissions();
        console.log('[Geo] Permissão:', permission.location);
    } catch (e) {
        console.warn('[Geo] Erro ao solicitar permissão:', e);
    }
}

// Inicia o app
document.addEventListener('DOMContentLoaded', () => {
    initApp();
    initPushNotifications();
    initGeolocation();
});
