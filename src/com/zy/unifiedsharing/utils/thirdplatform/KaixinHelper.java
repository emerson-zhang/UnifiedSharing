
package com.zy.unifiedsharing.utils.thirdplatform;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.kaixin.connect.AsyncKaixin;
import com.kaixin.connect.Kaixin;
import com.kaixin.connect.exception.KaixinAuthError;
import com.kaixin.connect.exception.KaixinError;
import com.kaixin.connect.listener.AsyncKaixinListener;
import com.kaixin.connect.listener.KaixinAuthListener;

public class KaixinHelper implements ThirdPlatformHelper
{
    // private static final String TAG = "KaixinHelper";

    private Kaixin mKaixin;
    private Context mContext;
    private SharedPreferences mPreferences;

    private static final String[] permissions = { "basic", "create_records" };

    public KaixinHelper(Context mContext)
    {
        mKaixin = Kaixin.getInstance();
        this.mContext = mContext;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public void auth(KaixinAuthListener authListener)
    {
        mKaixin.authorize(mContext, permissions, authListener);
    }

    @Override
    public void shareContent(final AsyncListener shareListener , String title , String content , String imageUrl)
    {
        final String token = mPreferences.getString(Constants.PREFERENCE_KEY_OAUTH_KAIXIN_ACCESS_TOKEN, "");

        final AsyncKaixin asyncKaixin = new AsyncKaixin(mKaixin);
        final String kaixinREST = "/records/add.json";
        final Bundle params = new Bundle();
        params.putByteArray("content", content.getBytes());
        params.putByteArray("picurl", imageUrl.getBytes());

        // 异步提交信息
        final AsyncKaixinListener asynclistener = new AsyncKaixinListener()
        {
            @Override
            public void onRequestComplete(String response , Object obj)
            {
                shareListener.onRequestComplete(response, obj);
            }

            @Override
            public void onRequestError(KaixinError kaixinError , Object obj)
            {
                shareListener.onRequestError(new Exception(kaixinError));
            }

            @Override
            public void onRequestNetError(Throwable fault , Object obj)
            {
                shareListener.onRequestException(new Exception(fault));
            }

        };

        if (!hasAuthed())
        {

            // 先进行认证
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
                    // 认证成功后调用
                    asyncKaixin.request(mContext, kaixinREST, params, "POST", asynclistener, (new Object()));
                }

                @Override
                public void onAuthCancel()
                {
                    shareListener.onRequestCancel();
                }
            };

            this.auth(authListener);
        }
        else
        {
            mKaixin.setAccessToken(token);
            // 可直接分享
            asyncKaixin.request(mContext, kaixinREST, params, "POST", asynclistener, (new Object()));
        }
    }

    @Override
    public void auth(final ThirdPlatformHelper.ThirdPlatformAuthListener authListener)
    {
        // 先进行认证
        KaixinAuthListener kaixinAuthListener = new KaixinAuthListener()
        {
            @Override
            public void onAuthComplete(Bundle values)
            {
                long expIn = Long.parseLong(values.getString("expires_in"));
                String accessToken = values.getString("access_token");

                // 写token
                Editor editor = mPreferences.edit();
                editor.putString(Constants.PREFERENCE_KEY_OAUTH_KAIXIN_ACCESS_TOKEN, accessToken);
                editor.putString(Constants.PREFERENCE_KEY_OAUTH_KAIXIN_REFRESH_TOKEN, values.getString("refresh_token"));
                editor.putLong(Constants.PREFERENCE_KEY_OAUTH_KAIXIN_EXPIRES_IN, System.currentTimeMillis() + expIn * 1000);
                editor.commit();

                // 认证成功后调用
                authListener.onAuthComplete(values);
            }

            @Override
            public void onAuthError(KaixinAuthError kaixinAuthError)
            {
                authListener.onAuthException(new Exception(kaixinAuthError));
            }

            @Override
            public void onAuthCancelLogin()
            {
                authListener.onAuthCancel();
            }

            @Override
            public void onAuthCancel(Bundle values)
            {
                authListener.onAuthCancel();
            }
        };

        this.auth(kaixinAuthListener);
    }

    @Override
    public boolean hasAuthed()
    {
        String token = mPreferences.getString(Constants.PREFERENCE_KEY_OAUTH_KAIXIN_ACCESS_TOKEN, "");
        Long expiresIn = mPreferences.getLong(Constants.PREFERENCE_KEY_OAUTH_KAIXIN_EXPIRES_IN, 0);
        Date date = new Date(expiresIn);
        if (token.equals("") || date.before(new Date()))
            return false;
        else
            return true;
    }

    @Override
    public void clearAuthInformation()
    {
        Editor e = mPreferences.edit();
        e.remove(Constants.PREFERENCE_KEY_OAUTH_KAIXIN_ACCESS_TOKEN);
        e.remove(Constants.PREFERENCE_KEY_OAUTH_KAIXIN_EXPIRES_IN);
        e.remove(Constants.PREFERENCE_KEY_OAUTH_KAIXIN_REFRESH_TOKEN);
        e.commit();
    }

    @Override
    public String getPlatformName()
    {
        return "开心网";
    }

    @Override
    public void getUserInfo(final AsyncListener asyncListener)
    {
        if (!hasAuthed())
        {
            // 未被授权，先进行授权
            ThirdPlatformHelper.ThirdPlatformAuthListener authListener = new ThirdPlatformHelper.ThirdPlatformAuthListener()
            {
                @Override
                public void onAuthException(Exception e)
                {
                    asyncListener.onRequestException(e);
                }

                @Override
                public void onAuthComplete(Bundle bundle)
                {
                    // 认证成功后调用
                    realGetUserInfo(asyncListener, bundle);
                }

                @Override
                public void onAuthCancel()
                {
                    asyncListener.onRequestCancel();
                }
            };

            this.auth(authListener);
        }else{
           String token = mPreferences.getString(Constants.PREFERENCE_KEY_OAUTH_KAIXIN_ACCESS_TOKEN, "");
           mKaixin.setAccessToken(token);
        }
    }

    private void realGetUserInfo(final AsyncListener asyncListener , Bundle params)
    {
        AsyncKaixinListener kaixinListener = new AsyncKaixinListener()
        {
            @Override
            public void onRequestNetError(Throwable fault , Object obj)
            {
                asyncListener.onRequestException(new Exception(fault));
            }
            
            @Override
            public void onRequestError(KaixinError kaixinError , Object obj)
            {
                asyncListener.onRequestError(new Exception(kaixinError));
            }
            
            @Override
            public void onRequestComplete(String response , Object obj)
            {
                System.out.println("response: "+response);
                
                Bundle bundle = new Bundle();
                
                try
                {
                    JSONObject info = new JSONObject(response);
                    
                    bundle.putString("uid", info.getString("uid"));
                    bundle.putString("name", info.getString("name"));
                    bundle.putString("gender", info.getString("gender"));
                    bundle.putString("logo50", info.getString("logo50"));
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
                
                asyncListener.onRequestComplete(response, bundle);
            }
        };
        
        AsyncKaixin mAsyncKaixin = new AsyncKaixin(mKaixin);
        String kaixinREST = "/users/me.json";

        asyncListener.onRequestStart();
        
        mAsyncKaixin.request(mContext, kaixinREST, null, "GET", kaixinListener, (new Object()));
    }
}
