
package com.zy.unifiedsharing.utils.thirdplatform;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.b5mandroid.utils.Constants;
import com.renren.api.connect.android.AsyncRenren;
import com.renren.api.connect.android.Renren;
import com.renren.api.connect.android.RequestListener;
import com.renren.api.connect.android.exception.RenrenAuthError;
import com.renren.api.connect.android.exception.RenrenError;
import com.renren.api.connect.android.view.RenrenAuthListener;

public class RenrenHelper implements ThirdPlatformHelper
{
    private static final String TAG = "RenrenHelper";

    private Renren mRenren;
    private Activity mActivity;
    private SharedPreferences mPreferences;

    public RenrenHelper(Activity activity)
    {
        mActivity = activity;
        mRenren = new Renren(Constants.OAUTH_RENREN_APP_KEY, Constants.OAUTH_RENREN_SECRET_KEY, Constants.OAUTH_RENREN_APPID, mActivity);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
    }

    public void renrenAuth(RenrenAuthListener authListener)
    {
        mRenren.authorize(mActivity, authListener);
    }

    @Override
    public void shareContent(final AsyncListener shareListener , String title , String content , String imageUrl)
    {
        final String token = mPreferences.getString(Constants.PREFERENCE_KEY_OAUTH_RENREN_ACCESS_TOKEN, "");
        final String renrenREST = "status.set";
        final Bundle params = new Bundle();
        params.putString("status", content);
        params.putString("url", imageUrl);
        params.putString("method", renrenREST);
        params.putString("type", String.valueOf(6));

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
                    params.remove("sig");
                    publish(mRenren, params, shareListener);
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
            params.putString("access_token", token);
            // 进行分享
            publish(mRenren, params, shareListener);
        }
    }

    /**
     * [简要描述]: [详细描述]:
     * 
     * @author [lscm]
     * @email [emerson.zhang@b5m.com]
     * @date [Sep 6, 2012]
     * @param renren
     * @param params
     * @param shareListener
     */
    private void publish(Renren renren , Bundle params , final AsyncListener shareListener)
    {
        // 异步调用
        final RequestListener requestListener = new RequestListener()
        {

            @Override
            public void onRenrenError(RenrenError renrenError)
            {
                shareListener.onRequestError(new Exception(renrenError));
            }

            @Override
            public void onFault(Throwable fault)
            {
                shareListener.onRequestException(new Exception(fault));
            }

            @Override
            public void onComplete(String response)
            {
                shareListener.onRequestComplete(response, null);
            }
        };

        // 进行分享
        AsyncRenren asyncRenren = new AsyncRenren(mRenren);
        asyncRenren.requestJSON(params, requestListener);
    }

    @Override
    public void auth(final ThirdPlatformAuthListener authListener)
    {
        // 未被授权，先认证
        RenrenAuthListener renrenAuthListener = new RenrenAuthListener()
        {

            @Override
            public void onRenrenAuthError(RenrenAuthError renrenAuthError)
            {
                authListener.onAuthException(new Exception(renrenAuthError));
            }

            @Override
            public void onComplete(Bundle values)
            {
                Log.e(TAG, values.toString());
                // 写token
                if (!values.isEmpty())
                {
                    Editor editor = mPreferences.edit();
                    editor.putString(Constants.PREFERENCE_KEY_OAUTH_RENREN_ACCESS_TOKEN, values.getString("access_token"));
                    editor.putLong(Constants.PREFERENCE_KEY_OAUTH_RENREN_EXPIRES_IN, System.currentTimeMillis() + Long.parseLong(values.getString("expires_in")) * 1000);
                    editor.commit();
                }

                // 认证成功
                authListener.onAuthComplete(values);
            }

            @Override
            public void onCancelLogin()
            {
                authListener.onAuthCancel();
            }

            @Override
            public void onCancelAuth(Bundle values)
            {
                authListener.onAuthCancel();
            }
        };

        renrenAuth(renrenAuthListener);
    }

    @Override
    public boolean hasAuthed()
    {
        String token = mPreferences.getString(Constants.PREFERENCE_KEY_OAUTH_RENREN_ACCESS_TOKEN, "");
        Long expiresIn = mPreferences.getLong(Constants.PREFERENCE_KEY_OAUTH_RENREN_EXPIRES_IN, 0);
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
        e.remove(Constants.PREFERENCE_KEY_OAUTH_RENREN_ACCESS_TOKEN);
        e.remove(Constants.PREFERENCE_KEY_OAUTH_RENREN_EXPIRES_IN);
        e.commit();
        mRenren.logout(mActivity);
    }

    @Override
    public String getPlatformName()
    {
        return "人人网";
    }

    @Override
    public void getUserInfo(final AsyncListener asyncListener)
    {
        final String token = mPreferences.getString(Constants.PREFERENCE_KEY_OAUTH_RENREN_ACCESS_TOKEN, "");
        final String renrenREST = "users.getInfo";
        final Bundle params = new Bundle();
        params.putString("format", "JSON");
        params.putString("method", renrenREST);

        if (!hasAuthed())
        {
            //未被授权，准备授权
            ThirdPlatformAuthListener authListener = new ThirdPlatformAuthListener()
            {
                @Override
                public void onAuthComplete(Bundle bundle)
                {
                    realGetUserInfo(asyncListener, params);
                }

                @Override
                public void onAuthCancel()
                {
                    asyncListener.onRequestCancel();
                }

                @Override
                public void onAuthException(Exception e)
                {
                    asyncListener.onRequestException(e);
                }
            };

            this.auth(authListener);
        }
        else
        {
            params.putString("access_token", token);
            realGetUserInfo(asyncListener, params);
        }
    }

    private void realGetUserInfo(final AsyncListener asyncListener , Bundle params)
    {
        // 异步调用
        final RequestListener requestListener = new RequestListener()
        {

            @Override
            public void onRenrenError(RenrenError renrenError)
            {
                asyncListener.onRequestError(new Exception(renrenError));
            }

            @Override
            public void onFault(Throwable fault)
            {
                asyncListener.onRequestException(new Exception(fault));
            }

            @Override
            public void onComplete(String response)
            {
                Bundle bundle = new Bundle();
                try
                {
                    JSONArray users = new JSONArray(response);
                    JSONObject result = (JSONObject) users.get(0);
                    
                    bundle.putString("uid", result.getString("uid"));
                    bundle.putString("tinyurl", result.getString("tinyurl"));
                    bundle.putString("vip", result.getString("vip"));
                    bundle.putString("sex", result.getString("sex"));
                    bundle.putString("name", result.getString("name"));
                    bundle.putString("star", result.getString("star"));
                    bundle.putString("headurl", result.getString("headurl"));
                    bundle.putString("zidou", result.getString("zidou"));
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
                asyncListener.onRequestComplete(response, bundle);
            }
        };

        // 进行分享
        AsyncRenren asyncRenren = new AsyncRenren(mRenren);
        // 通知监听请求开始
        asyncListener.onRequestStart();
        // 请求数据
        asyncRenren.requestJSON(params, requestListener);
    }
}
