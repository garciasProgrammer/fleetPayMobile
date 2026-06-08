package com.fleetpay.app;

import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.getcapacitor.BridgeActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends BridgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Criar canal de notificação (obrigatório Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "fleetpay_notifications",
                "FleetPay Notificações",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificações do FleetPay");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        int fleetpayBlue = Color.parseColor("#003d82");
        window.setStatusBarColor(fleetpayBlue);
        window.setNavigationBarColor(fleetpayBlue);

        View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        WebView webView = getBridge().getWebView();

        // Interface JS para downloads
        webView.addJavascriptInterface(new DownloadInterface(this), "AndroidDownload");

        // Injeta override do download após cada página carregar
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Inicializa Push Notifications via Capacitor bridge
                String pushJs = "javascript:(function() {" +
                        "  if (window._pushInitialized) return;" +
                        "  window._pushInitialized = true;" +
                        "  if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.PushNotifications) {" +
                        "    var Push = window.Capacitor.Plugins.PushNotifications;" +
                        "    Push.requestPermissions().then(function(p) {" +
                        "      if (p.receive === 'granted') { Push.register(); }" +
                        "    });" +
                        "    Push.addListener('registration', function(token) {" +
                        "      console.log('[Push] Token: ' + token.value);" +
                        "      fetch('https://fleetpay.fleetcarservice.com.br/api/push/register', {" +
                        "        method: 'POST'," +
                        "        headers: {'Content-Type': 'application/json'}," +
                        "        body: JSON.stringify({token: token.value, platform: 'android'})" +
                        "      }).catch(function(e) { console.warn('[Push] Erro envio token:', e); });" +
                        "    });" +
                        "    Push.addListener('registrationError', function(e) {" +
                        "      console.error('[Push] Erro registro:', JSON.stringify(e));" +
                        "    });" +
                        "    Push.addListener('pushNotificationActionPerformed', function(a) {" +
                        "      console.log('[Push] Notificacao clicada');" +
                        "    });" +
                        "  }" +
                        "})()";
                view.evaluateJavascript(pushJs, null);

                // Intercepta criação de <a download> para funcionar na WebView
                String js = "javascript:(function() {" +
                        "  if (window._downloadPatched) return;" +
                        "  window._downloadPatched = true;" +
                        "  var origCreate = document.createElement.bind(document);" +
                        "  document.createElement = function(tag) {" +
                        "    var el = origCreate(tag);" +
                        "    if (tag.toLowerCase() === 'a') {" +
                        "      var origClick = el.click.bind(el);" +
                        "      el.click = function() {" +
                        "        if (el.hasAttribute('download') && el.href && el.href.startsWith('blob:')) {" +
                        "          var fileName = el.download || 'arquivo.pdf';" +
                        "          fetch(el.href).then(function(r) { return r.blob(); }).then(function(blob) {" +
                        "            var reader = new FileReader();" +
                        "            reader.onloadend = function() {" +
                        "              var base64 = reader.result.split(',')[1];" +
                        "              AndroidDownload.saveFile(base64, fileName, blob.type);" +
                        "            };" +
                        "            reader.readAsDataURL(blob);" +
                        "          });" +
                        "          return;" +
                        "        }" +
                        "        origClick();" +
                        "      };" +
                        "    }" +
                        "    return el;" +
                        "  };" +
                        "})()";
                view.evaluateJavascript(js, null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.contains("fleetcarservice.com.br")) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }
        });

        // DownloadListener para downloads normais (não blob)
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                if (url.startsWith("blob:") || url.startsWith("data:")) {
                    return;
                }

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);

                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) {
                    request.addRequestHeader("Cookie", cookies);
                }

                request.addRequestHeader("User-Agent", userAgent);
                request.setTitle(fileName);
                request.setDescription("Baixando...");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                dm.enqueue(request);

                Toast.makeText(this, "Download: " + fileName, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Erro ao baixar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class DownloadInterface {
        private final Context context;

        public DownloadInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void saveFile(String base64, String fileName, String mimeType) {
            try {
                byte[] data = Base64.decode(base64, Base64.DEFAULT);
                Uri fileUri = null;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, mimeType != null ? mimeType : "application/pdf");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                    fileUri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (fileUri != null) {
                        OutputStream os = context.getContentResolver().openOutputStream(fileUri);
                        os.write(data);
                        os.flush();
                        os.close();
                    }
                } else {
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File file = new File(downloadsDir, fileName);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(data);
                    fos.flush();
                    fos.close();
                    fileUri = androidx.core.content.FileProvider.getUriForFile(context,
                            context.getPackageName() + ".fileprovider", file);
                }

                // Notificação com botão "Abrir"
                final Uri openUri = fileUri;
                final String mime = mimeType != null ? mimeType : "application/pdf";
                ((MainActivity) context).runOnUiThread(() -> {
                    Intent openIntent = new Intent(Intent.ACTION_VIEW);
                    openIntent.setDataAndType(openUri, mime);
                    openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    PendingIntent pendingIntent = PendingIntent.getActivity(
                            context, 0, openIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "fleetpay_notifications")
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                            .setContentTitle("Download concluído")
                            .setContentText(fileName)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .setPriority(NotificationCompat.PRIORITY_HIGH);

                    try {
                        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
                    } catch (SecurityException e) {
                        // Permissão de notificação não concedida
                    }

                    Toast.makeText(context, "Salvo em Downloads: " + fileName, Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                ((MainActivity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }
    }
}
