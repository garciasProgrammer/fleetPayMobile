const { GoogleAuth } = require('google-auth-library');

const FCM_TOKEN = 'fqQlj9G3TGGZHpXg3Ikmll:APA91bG4gaAn_qGewr1dU1JPlxFQZmMZr734mvusUL90eW4f8BCYUVuMYkphTbCGrq-kpdeAH6BY6biz2HqYF--GiJExa_v47OSaS9_NOZojhz1wyHngq9A';

async function sendPush() {
    const auth = new GoogleAuth({
        keyFile: './firebase-service-account.json',
        scopes: ['https://www.googleapis.com/auth/firebase.messaging']
    });

    const client = await auth.getClient();
    const accessToken = await client.getAccessToken();

    const res = await fetch('https://fcm.googleapis.com/v1/projects/fleetpay-mobile/messages:send', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + accessToken.token,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            message: {
                token: FCM_TOKEN,
                notification: {
                    title: 'FleetPay',
                    body: 'Push notification funcionando!'
                },
                data: {
                    url: '/app/contratos'
                },
                android: {
                    priority: 'high'
                }
            }
        })
    });

    const result = await res.json();
    console.log('Status:', res.status);
    console.log('Resposta:', JSON.stringify(result, null, 2));
}

sendPush().catch(console.error);
