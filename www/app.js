// FleetPay Mobile - Capacitor Bridge
// A URL do PWA é carregada automaticamente via capacitor.config.ts (server.url)

document.addEventListener('DOMContentLoaded', async () => {
    try {
        const { SplashScreen } = Capacitor.Plugins;
        const { StatusBar } = Capacitor.Plugins;
        const { PushNotifications } = Capacitor.Plugins;
        const { App } = Capacitor.Plugins;

        // Status bar
        try {
            await StatusBar.setStyle({ style: 'DARK' });
            await StatusBar.setBackgroundColor({ color: '#003d82' });
        } catch (e) {}

        // Push Notifications
        try {
            const permission = await PushNotifications.requestPermissions();
            if (permission.receive === 'granted') {
                await PushNotifications.register();
            }

            PushNotifications.addListener('registration', (token) => {
                console.log('[Push] Token:', token.value);
                fetch('https://fleetpay.fleetcarservice.com.br/api/push/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ token: token.value, platform: 'android' })
                }).catch(() => {});
            });

            PushNotifications.addListener('pushNotificationActionPerformed', (action) => {
                const data = action.notification.data;
                if (data && data.url) {
                    window.location.href = data.url;
                }
            });
        } catch (e) {}

        // Botão voltar (Android)
        App.addListener('backButton', ({ canGoBack }) => {
            if (canGoBack) {
                window.history.back();
            } else {
                App.exitApp();
            }
        });

    } catch (e) {
        console.error('[FleetPay] Erro na inicialização:', e);
    }
});
