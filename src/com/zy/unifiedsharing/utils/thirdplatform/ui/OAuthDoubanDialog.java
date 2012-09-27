package com.zy.unifiedsharing.utils.thirdplatform.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zy.unifiedsharing.utils.thirdplatform.ThirdPlatformHelper;


import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class OAuthDoubanDialog extends Dialog
{
    private ThirdPlatformHelper.ThirdPlatformAuthListener mAuthListener;
    private WebView web;
    private String authUrl;

    public OAuthDoubanDialog(Context context, ThirdPlatformHelper.ThirdPlatformAuthListener authListener, String authUrl)
    {
        super(context);
        this.authUrl = authUrl;
        this.mAuthListener = authListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oauth_douban);
        web = (WebView) findViewById(R.id.douban_webview);
        (web.getSettings()).setJavaScriptEnabled(true);
        web.setWebViewClient(new doubanWebviewClient());
        web.loadUrl(authUrl);
    }

    private class doubanWebviewClient extends WebViewClient
    {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view , String url)
        {
            System.out.println(url);
            Pattern pattern = Pattern.compile("^" + "http://www.b5m.com" + ".*");
            Matcher matcher = pattern.matcher(url);
            if (matcher.matches())
            {
                Uri uri = Uri.parse(url);
                String code = uri.getQueryParameter("code");
                System.out.println("catch you!!!!!!!!!!!!!!!! code " + code);
                if (code.length() != 0)
                {
                    Bundle b = new Bundle();
                    b.putString("code", code);
                    OAuthDoubanDialog.this.mAuthListener.onAuthComplete(b);
                    OAuthDoubanDialog.this.dismiss();
                    
                    return true;
                }
            }
            return super.shouldOverrideUrlLoading(view, url);
        }
    }
    
    @Override
    public void cancel()
    {
        super.cancel();
        mAuthListener.onAuthCancel();
    }
}
