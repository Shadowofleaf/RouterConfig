package ma.mk.routerconfig;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.atzcx.appverupdater.AppVerUpdater;
import com.github.atzcx.appverupdater.Callback;
import com.github.atzcx.appverupdater.enums.UpdateErrors;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private static boolean firstRun = true;
    ProgressBar pbar;
    private WebView mWebView;
    SwipeRefreshLayout mySwipeRefreshLayout;
    AppVerUpdater appVerUpdater = null;
    InterstitialAd mInterstitialAd;
    private static String getUrl;
    private String TAG;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        if(firstRun)
        {
            Toast.makeText(getApplicationContext(), "FIRST RUN", Toast.LENGTH_SHORT).show();
            appVerUpdater = new AppVerUpdater(MainActivity.this)
                    .setUpdateJSONUrl("https://raw.githubusercontent.com/Shadowofleaf/app-update/master/update2.json")
                    .setShowNotUpdated(true)
                    .setViewNotes(true)
                    .setCallback(new Callback() {
                        @Override
                        public void onFailure(UpdateErrors error) {
                            if (error == UpdateErrors.NETWORK_NOT_AVAILABLE) {
                                Toast.makeText(MainActivity.this, R.string.nocnx, Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .build();
        }
        firstRun = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mySwipeRefreshLayout = (SwipeRefreshLayout)this.findViewById(R.id.swipeContainer);
        pbar = (ProgressBar) findViewById(R.id.progress_bar);
        mWebView = (WebView) findViewById(R.id.webview);

        AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("android_studio:ad_template").build();
        adView.loadAd(adRequest);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.interstitial_ad_unit_id));


        requestNewInterstitial();

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        client.get("http://192.168.1.1", params, new TextHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String res) {
                        // called when response HTTP status is "200 OK"
                    }
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                        // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                    }
                }
        );
        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if(progress < 100 && pbar.getVisibility() == ProgressBar.GONE){
                    pbar.setVisibility(ProgressBar.VISIBLE);
                }

                pbar.setProgress(progress);
                if(progress == 100) {
                    pbar.setVisibility(ProgressBar.GONE);
                }
        }
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (!TextUtils.isEmpty(title)) {
                    MainActivity.this.setTitle(title);
                }
            }
            @Override
            public void onConsoleMessage(String message, int lineNumber,
                                         String sourceID) {
                super.onConsoleMessage(message, lineNumber, sourceID);
            }

        });
        mWebView.setWebViewClient(new WebViewClient() {
         @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
        }
    public void onPageFinished(WebView view,String url){
            mySwipeRefreshLayout.setRefreshing(false);
            mInterstitialAd.show();
            getUrl = view.getUrl();
            String cookies = CookieManager.getInstance().getCookie(getUrl);
            Log.d(TAG, "All the cookies in a string:" + cookies);
        }
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
            handler.proceed("username", "password");
        }
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (Uri.parse(url).getHost().equals("192.168.1.1")) {
                // This is your web site, so do not override; let WebView load the page
                return false;
            }
            // Otherwise, the link is not for a page on your site, so launch another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                AlertDialog alertDialog = builder.create();
                String message = "SSL Certificate error.";
                switch (error.getPrimaryError()) {
                    case SslError.SSL_UNTRUSTED:
                        message = "The certificate authority is not trusted.";
                        break;
                    case SslError.SSL_EXPIRED:
                        message = "The certificate has expired.";
                        break;
                    case SslError.SSL_IDMISMATCH:
                        message = "The certificate Hostname mismatch.";
                        break;
                    case SslError.SSL_NOTYETVALID:
                        message = "The certificate is not yet valid.";
                        break;
                }
                message += " Do you want to continue anyway?";
                alertDialog.setTitle("SSL Certificate Error");
                alertDialog.setMessage(message);
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Ignore SSL certificate errors
                        handler.proceed();
                    }
                });

                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        handler.cancel();
                    }
                });
                alertDialog.show();
            }
        }
        );
        mWebView.setInitialScale(1);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
        mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAllowContentAccess(true);
        mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        mWebView.setScrollbarFadingEnabled(false);
        mWebView.loadUrl("http://192.168.1.1/");
        mInterstitialAd.show();
        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        mWebView.reload();
                    }
                }

        );

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_about) {
            Intent intent = new Intent(MainActivity.this, about2.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_1) {
            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            startActivity(intent);
            return true;
        }
        mInterstitialAd.show();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (mWebView.canGoBack()) {
                        mWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    //This function initializes new ads.
    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest);
    }

}

