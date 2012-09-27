
package com.zy.unifiedsharing.utils.thirdplatform.ui;

import com.b5m.bang5mai.R;
import com.b5mandroid.utils.thirdplatform.ThirdPlatformHelper;
import com.tencent.weibo.oauthv2.OAuthV2;
import com.tencent.weibo.oauthv2.OAuthV2Client;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class OAuthTencentDialog extends Dialog
{
    // private static final String TAG = "OAuthTencentDialog";
    private ThirdPlatformHelper.ThirdPlatformAuthListener mAuthListener;
    private WebView web;
    private String authUrl;
    private OAuthV2 oAuth;

    public OAuthTencentDialog(Context context, ThirdPlatformHelper.ThirdPlatformAuthListener authListener, String authUrl, OAuthV2 authV2)
    {
        super(context);
        this.authUrl = authUrl;
        this.mAuthListener = authListener;
        oAuth = authV2;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // LinearLayout linearLayout = new LinearLayout(getContext());
        // web = new WebView(getContext());
        // linearLayout.addView(web, new LayoutParams(LayoutParams.FILL_PARENT,
        // LayoutParams.FILL_PARENT));
        setContentView(R.layout.oauth_douban);
        web = (WebView) findViewById(R.id.douban_webview);

        WebSettings webSettings = web.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        web.setWebViewClient(new TencentWebviewClient());
        web.requestFocus();
        web.loadUrl(authUrl);

    }

    private class TencentWebviewClient extends WebViewClient
    {
        /**
         * 回调方法，当页面开始加载时执行
         */
        @Override
        public void onPageStarted(WebView view , String url , Bitmap favicon)
        {
            // Log.i(TAG, "WebView onPageStarted...");
            // Log.i(TAG, "URL = " + url);
            if (url.indexOf("access_token=") != -1)
            {
                int start = url.indexOf("access_token=");
                String responseData = url.substring(start);

                // 转成正确的URI格式
                String convertURIString = url.replaceFirst("#", "?");
                Uri uri = Uri.parse(convertURIString);
                // 提取过期时间
                long expiresIn = Long.parseLong(uri.getQueryParameter("expires_in")) * 1000;

                // 必要的步骤。
                OAuthV2Client.parseAccessTokenAndOpenId(responseData, oAuth);
                view.destroyDrawingCache();
                view.destroy();
                OAuthTencentDialog.this.dismiss();
                // 通知回调
                Bundle bundle = new Bundle();
                bundle.putSerializable("oauth", oAuth);
                bundle.putString("responseData", responseData);
                bundle.putLong("expires_in", expiresIn);
                // mAuthListener.onAuthSuccess(oAuth);
                mAuthListener.onAuthComplete(bundle);
            }
            super.onPageStarted(view, url, favicon);
        }

        /*
         * TODO Android2.2及以上版本才能使用该方法
         * 目前https://open.t.qq.com中存在http资源会引起sslerror，待网站修正后可去掉该方法
         */
        public void onReceivedSslError(WebView view , SslErrorHandler handler , SslError error)
        {
            if ((null != view.getUrl()) && (view.getUrl().startsWith("https://open.t.qq.com")))
            {
                handler.proceed();// 接受证书
            }
            else
            {
                handler.cancel(); // 默认的处理方式，WebView变成空白页
            }
            // handleMessage(Message msg); 其他处理
        }
    }
}
