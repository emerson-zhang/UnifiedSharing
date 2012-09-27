
package com.zy.unifiedsharing.utils.thirdplatform;

import java.io.IOException;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.b5mandroid.utils.Constants;
import com.weibo.net.AccessToken;
import com.weibo.net.AsyncWeiboRunner;
import com.weibo.net.AsyncWeiboRunner.RequestListener;
import com.weibo.net.DialogError;
import com.weibo.net.Utility;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboDialogListener;
import com.weibo.net.WeiboException;
import com.weibo.net.WeiboParameters;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class SinaWeiboHelper implements ThirdPlatformHelper
{
    private Activity mActivity;
    private Weibo mWeibo;
    private SharedPreferences mPreferences;

    public SinaWeiboHelper(Activity activity)
    {
        super();
        this.mActivity = activity;
        mWeibo = Weibo.getInstance();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    public void sinaWeiboOAuth(WeiboDialogListener dialogListener)
    {
        mWeibo.setupConsumerConfig(Constants.OAUTH_SINA_WEIBO_APPKEY, Constants.OAUTH_SINA_WEIBO_APPSECRET);
        mWeibo.setRedirectUrl(Constants.OAUTH_SINA_WEIBO_CALLBACK);
        mWeibo.authorize(mActivity, dialogListener);
    }

    @Override
    public void shareContent(final AsyncListener shareListener , String title , final String content , final String imageUrl)
    {
        final String weiboToken = mPreferences.getString(Constants.PREFERENCE_KEY_SINA_WEIBO_ACCESS_TOKEN, "");

        if (!hasAuthed())
        {
            // 认证用
            ThirdPlatformAuthListener authListener = new ThirdPlatformAuthListener()
            {

                @Override
                public void onAuthException(Exception e)
                {
                    shareListener.onRequestException(e);
                }

                @Override
                public void onAuthComplete(Bundle bundle)
                {
                    publish(content, bundle.getString("access_token"), imageUrl, shareListener);
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
            AccessToken accessToken = new AccessToken(weiboToken, Constants.OAUTH_SINA_WEIBO_APPSECRET);
            mWeibo.setAccessToken(accessToken);
            publish(content, weiboToken, imageUrl, shareListener);
        }
    }

    /**
     * [简要描述]:发表微博 [详细描述]:
     * 
     * @author [lscm]
     * @email [emerson.zhang@b5m.com]
     * @date [Sep 6, 2012]
     * @method [publish]
     * @param content
     * @param accesstoken
     * @param picurl
     */
    private void publish(String content , String accesstoken , String picurl , final AsyncListener shareListener)
    {

        RequestListener requestListener = new RequestListener()
        {
            @Override
            public void onIOException(IOException e)
            {
                shareListener.onRequestException(e);
            }

            @Override
            public void onError(WeiboException e)
            {
                e.printStackTrace();
                shareListener.onRequestError(e);
            }

            @Override
            public void onComplete(String response)
            {
                shareListener.onRequestComplete(response, null);
            }
        };

        WeiboParameters bundle = new WeiboParameters();
        bundle.add("status", content);
        bundle.add("access_token", accesstoken);
        bundle.add("annotations", "[]");
        String url = null;

        if ((picurl == null) || (picurl.length() == 0))
        {
            // 仅文字
            url = Weibo.SERVER + "statuses/update.json";
        }
        else
        {
            // 图文
            url = Weibo.SERVER + "statuses/upload_url_text.json";
            bundle.add("url", picurl);
        }
        AsyncWeiboRunner weiboRunner = new AsyncWeiboRunner(mWeibo);
        weiboRunner.request(mActivity, url, bundle, Utility.HTTPMETHOD_POST, requestListener);
    }

    @Override
    public void auth(final ThirdPlatformAuthListener authListener)
    {
        // 认证用
        WeiboDialogListener oauthListener = new WeiboDialogListener()
        {
            @Override
            public void onWeiboException(WeiboException e)
            {
                authListener.onAuthException(e);
            }

            @Override
            public void onError(DialogError e)
            {
                authListener.onAuthException(new Exception(e));
            }

            @Override
            public void onComplete(Bundle values)
            {
                String token = values.getString("access_token");
                String expires_in = values.getString("expires_in");
                String uid = values.getString("uid");

                Date date = new Date(System.currentTimeMillis() + Long.valueOf(expires_in) * 1000);

                Editor edit = mPreferences.edit();
                edit.putString(Constants.PREFERENCE_KEY_SINA_WEIBO_ACCESS_TOKEN, token);
                edit.putLong(Constants.PREFERENCE_KEY_SINA_WEIBO_EXPIRES_IN, date.getTime());
                edit.putString(Constants.PREFERENCE_KEY_SINA_WEIBO_UID, uid);
                edit.commit();

                // 认证成功
                authListener.onAuthComplete(values);
            }

            @Override
            public void onCancel()
            {
                authListener.onAuthCancel();
            }
        };
        this.sinaWeiboOAuth(oauthListener);
    }

    @Override
    public boolean hasAuthed()
    {
        String weiboToken = mPreferences.getString(Constants.PREFERENCE_KEY_SINA_WEIBO_ACCESS_TOKEN, "");
        Long weiboExpires_in = mPreferences.getLong(Constants.PREFERENCE_KEY_SINA_WEIBO_EXPIRES_IN, 0);

        Date date = new Date(weiboExpires_in);

        if (weiboToken.equals("") || date.before(new Date()))
            return false;
        else
            return true;
    }

    @Override
    public void clearAuthInformation()
    {
        // 执行清理
        Editor e = mPreferences.edit();
        e.remove(Constants.PREFERENCE_KEY_SINA_WEIBO_ACCESS_TOKEN);
        e.remove(Constants.PREFERENCE_KEY_SINA_WEIBO_EXPIRES_IN);
        e.remove(Constants.PREFERENCE_KEY_SINA_WEIBO_UID);
        e.commit();
    }

    @Override
    public String getPlatformName()
    {
        return "新浪微博";
    }

    @Override
    public void getUserInfo(final AsyncListener asyncListener)
    {
        if (!hasAuthed())
        {
            // 认证用
            ThirdPlatformAuthListener authListener = new ThirdPlatformAuthListener()
            {

                @Override
                public void onAuthException(Exception e)
                {
                    asyncListener.onRequestException(e);
                }

                @Override
                public void onAuthComplete(Bundle bundle)
                {
                    realGetUserInfo(asyncListener, bundle);
                }

                @Override
                public void onAuthCancel()
                {
                    asyncListener.onRequestCancel();
                }
            };
            this.auth(authListener);
        }
        else
        {
            String weiboToken = mPreferences.getString(Constants.PREFERENCE_KEY_SINA_WEIBO_ACCESS_TOKEN, "");
            Bundle b = new Bundle();
            b.putString("access_token", weiboToken);
            b.putString("uid", mPreferences.getString(Constants.PREFERENCE_KEY_SINA_WEIBO_UID, ""));
            realGetUserInfo(asyncListener, b);
        }
    }

    private void realGetUserInfo(final AsyncListener asyncListener , Bundle bundle)
    {
        RequestListener requestListener = new RequestListener()
        {
            @Override
            public void onIOException(IOException e)
            {
                asyncListener.onRequestException(e);
            }

            @Override
            public void onError(WeiboException e)
            {
                asyncListener.onRequestError(e);
            }

            @Override
            public void onComplete(String response)
            {
                Bundle bundle = new Bundle();
                try
                {
                    JSONObject info = new JSONObject(response);
                    bundle.putString("id", info.getString("id"));
                    bundle.putString("screen_name", info.getString("screen_name"));//用户昵称
                    bundle.putString("name", info.getString("name"));//友好显示名称
                    bundle.putInt("province", info.getInt("province"));//用户所在地区ID
                    bundle.putInt("city", info.getInt("city"));//用户所在城市ID
                    bundle.putString("location", info.getString("location"));//用户所在地
                    bundle.putString("description", info.getString("description"));//用户描述
                    bundle.putString("gender", info.getString("gender"));//性别，m：男、f：女、n：未知
                    
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }

                asyncListener.onRequestComplete(response, bundle);
            }
        };

        WeiboParameters params = new WeiboParameters();
        params.add("access_token", bundle.getString("access_token"));
        params.add("uid", bundle.getString("uid"));
        String url = Weibo.SERVER + "users/show.json";

        asyncListener.onRequestStart();
        
        AsyncWeiboRunner weiboRunner = new AsyncWeiboRunner(mWeibo);
        weiboRunner.request(mActivity, url, params, Utility.HTTPMETHOD_GET, requestListener);
    }

}
