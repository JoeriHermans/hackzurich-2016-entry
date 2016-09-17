package com.hackzurich.carapp.carapphackzurich;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class FrontEndActivity extends Activity {

    private WebView myWebView;
    private WebSettings webSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front_end);

        myWebView = (WebView) findViewById(R.id.webview);
        myWebView.setWebViewClient(new WebViewClient());
        myWebView.loadUrl("http://przemyslawhardyn.com/hack");
        webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        myWebView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        myWebView.onPause();
    }

    protected void onDestroy(){
        myWebView.destroy();
        super.onDestroy();
    }
}
