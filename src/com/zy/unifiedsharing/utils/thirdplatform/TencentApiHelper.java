
package com.zy.unifiedsharing.utils.thirdplatform;

import java.util.Date;

import com.b5mandroid.utils.Constants;
import com.tencent.weibo.api.TAPI;
import com.tencent.weibo.constants.OAuthConstants;
import com.tencent.weibo.oauthv2.OAuthV2;
import com.tencent.weibo.oauthv2.OAuthV2Client;
import com.zy.unifiedsharing.utils.thirdplatform.ui.OAuthTencentDialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class TencentApiHelper implements ThirdPlatformHelper
{
    private Context mContext;
    private OAuthV2 oAuthV2;
    private TAPI tAPI;
    private SharedPreferences mPreferences;

    public TencentApiHelper(Context context)
    {
        this.mContext = context;
        oAuthV2 = new OAuthV2(Constants.OAUTH_TENCENT_APP_KEY, Constants.OAUTH_TENCENT_APP_SECRET, Constants.OAUTH_CONNECT_QQ_CALLBACK);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void shareContent(final AsyncListener shareListener , String title , final String content , final String imageUrl)
    {

        System.out.println(hasAuthed());
        if (!hasAuthed())
        {
            ThirdPlatformHelper.ThirdPlatformAuthListener authListener = new ThirdPlatformHelper.ThirdPlatformAuthListener()
            {
                @Override
                public void onAuthException(Exception e)
                {
                    shareListener.onRequestException(e);
                }

                @Override
                public void onAuthComplete(Bundle bundle)
                {
                    oAuthV2 = (OAuthV2) bundle.getSerializable("oauth");
                    publish(oAuthV2, content, imageUrl, shareListener);
                }

                @Override
                public void onAuthCancel()
                {
                    shareListener.onRequestCancel();
                }
            };
            auth(authListener);
        }
        else
        {
            String oathString = mPreferences.getString(Constants.PREFERENCE_KEY_TENCENT_WEIBO_AUTH_SUCCESS_STRING, "");
            OAuthV2Client.parseAccessTokenAndOpenId(oathString, oAuthV2);
            publish(oAuthV2, content, imageUrl, shareListener);

        }
    }

    private void publish(final OAuthV2 authV2 , final String content , final String picUrl , final AsyncListener shareListener)
    {
        tAPI = new TAPI(OAuthConstants.OAUTH_VERSION_2_A);
        
        Thread workingThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    String response = null;
                    if ((picUrl == null) || (picUrl.length() == 0))
                    {
                        // 仅文字微博
                        response = tAPI.add(authV2, "json", content, "127.0.0.1");
                    }
                    else
                    {
                        // 图文
                        response = tAPI.addPic(authV2, "json", content, "127.0.0.1", picUrl);
                    }
                    Log.i("TencentApiHelper", response);
                    shareListener.onRequestComplete(response,null);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    shareListener.onRequestException(e);
                }
                finally
                {
                    tAPI.shutdownConnection();
                }
            }
        });
        
        workingThread.start();
        
    }

    @Override
    public void auth(final ThirdPlatformHelper.ThirdPlatformAuthListener authListener)
    {
        ThirdPlatformHelper.ThirdPlatformAuthListener listener = new ThirdPlatformHelper.ThirdPlatformAuthListener()
        {

            @Override
            public void onAuthException(Exception e)
            {
                authListener.onAuthException(e);
            }

            @Override
            public void onAuthComplete(Bundle bundle)
            {
                long expiresIn = bundle.getLong("expires_in");
                String responseData = bundle.getString("responseData");

                // 准备存储
                Editor editor = mPreferences.edit();
                editor.putLong(Constants.PREFERENCE_KEY_TENCENT_WEIBO_EXPIRES_IN, System.currentTimeMillis() + expiresIn);
                editor.putString(Constants.PREFERENCE_KEY_TENCENT_WEIBO_AUTH_SUCCESS_STRING, responseData);
                editor.commit();

                // 执行逻辑
                authListener.onAuthComplete(bundle);
            }

            @Override
            public void onAuthCancel()
            {
                authListener.onAuthCancel();
            }
        };

        // 关闭OAuthV2Client中的默认开启的QHttpClient。
        OAuthV2Client.getQHttpClient().shutdownConnection();
        String url = OAuthV2Client.generateImplicitGrantUrl(oAuthV2);
        OAuthTencentDialog dialog = new OAuthTencentDialog(mContext, listener, url, oAuthV2);
        dialog.show();
    }

    @Override
    public void clearAuthInformation()
    {
        // 清除
        Editor editor = mPreferences.edit();
        editor.remove(Constants.PREFERENCE_KEY_TENCENT_WEIBO_EXPIRES_IN);
        editor.remove(Constants.PREFERENCE_KEY_TENCENT_WEIBO_AUTH_SUCCESS_STRING);
        editor.commit();
    }

    @Override
    public boolean hasAuthed()
    {
        Long expiresIn = mPreferences.getLong(Constants.PREFERENCE_KEY_TENCENT_WEIBO_EXPIRES_IN, 0);
        Date date = new Date(expiresIn);
        if (date.before(new Date()))
            return false;
        else
            return true;

    }

    @Override
    public String getPlatformName()
    {
        return "腾讯微博";
    }

    @Override
    public void getUserInfo(AsyncListener asyncListener)
    {
    }
}
