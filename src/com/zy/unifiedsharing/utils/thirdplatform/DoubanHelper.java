package com.zy.unifiedsharing.utils.thirdplatform;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.http.Header;

import com.zy.unifiedsharing.utils.network.HttpUtils;
import com.zy.unifiedsharing.utils.thirdplatform.ui.OAuthDoubanDialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class DoubanHelper implements ThirdPlatformHelper {

	private Context mContext;
	private SharedPreferences mPreferences;
	private DoubanService doubanService;

	public DoubanHelper(Context context) {
		this.mContext = context;
		mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		doubanService = new DoubanService(Constants.OAUTH_DOUBAN_APIKEY,
				Constants.OAUTH_DOUBAN_SECRET_KEY, "http://www.b5m.com");
	}

	@Override
	public void shareContent(final AsyncListener shareListener, String title,
			String content, String imageUrl) {

		final Bundle params = new Bundle();
		params.putString("source", Constants.OAUTH_DOUBAN_APIKEY);
		params.putString("text", content);

		if (imageUrl != null && imageUrl.length() != 0) {
			try {
				JSONArray media = new JSONArray();
				JSONObject record = new JSONObject();
				record.put("type", "image");
				record.put("src", imageUrl);
				record.put("href", imageUrl);
				media.put(record);
				JSONObject object = new JSONObject();
				object.put("media", media);
				params.putString("attachments", object.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		if (!hasAuthed()) {
			// 未进行授权，先授权
			// 授权listener
			ThirdPlatformAuthListener authListener = new ThirdPlatformAuthListener() {
				@Override
				public void onAuthException(Exception e) {
					shareListener.onRequestException(e);
				}

				@Override
				public void onAuthComplete(Bundle bundle) {
					// 授权成功
					params.putString("access_token",
							bundle.getString("access_token"));
					realShareContent(shareListener, params);
				}

				@Override
				public void onAuthCancel() {
					shareListener.onRequestCancel();
				}
			};
			this.auth(authListener);
		} else {
			String token = mPreferences.getString(
					Constants.PREFERENCE_KEY_DOUBAN_ACCESS_TOKEN, "");
			params.putString("access_token", token);
			realShareContent(shareListener, params);
		}
	}

	private void realShareContent(final AsyncListener asyncListener,
			final Bundle bundle) {
		Thread workerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					asyncListener.onRequestStart();

					List<Header> headers = new ArrayList<Header>();
					headers.add(new BasicHeader("Authorization", "Bearer "
							+ bundle.getString("access_token")));
					headers.add(new BasicHeader("Content-Type",
							"application/x-www-form-urlencoded"));

					List<NameValuePair> params = new ArrayList<NameValuePair>();
					params.add(new BasicNameValuePair("source", bundle
							.getString("source")));
					params.add(new BasicNameValuePair("text", "擦"));

					if (bundle.containsKey("attachments"))
						params.add(new BasicNameValuePair("attachments", bundle
								.getString("attachments")));

					// JSONObject jsonParam = new JSONObject();
					// jsonParam.put("source", bundle.getString("source"));
					// jsonParam.put("text", bundle.getString("text"));
					// if (bundle.containsKey("attachments"))
					// jsonParam.put("attachments", new
					// JSONObject(bundle.getString("attachments")));
					// System.out.println(jsonParam.toString());

					HttpResponse response = HttpUtils.getResponse(
							"https://api.douban.com/shuo/v2/statuses/",
							headers, params, mContext, HttpUtils.POST);
					String responseString = EntityUtils.toString(response
							.getEntity());
					JSONObject info = new JSONObject(responseString);
					System.out.println(responseString + " "
							+ response.getStatusLine().getStatusCode());

					if (response.getStatusLine().getStatusCode() == 200) {
						Bundle result = new Bundle();
						result.putString("name", info.getString("name"));
						result.putString("uid", info.getString("uid"));
						result.putString("id", info.getString("id"));

						asyncListener.onRequestComplete(responseString, result);
					} else {
						asyncListener.onRequestError(new Exception(info
								.getString("msg")));
					}
				} catch (Exception e) {
					asyncListener.onRequestException(e);
					e.printStackTrace();
				}
			}
		});

		workerThread.start();
	}

	@Override
	public String getPlatformName() {
		return "豆瓣";
	}

	@Override
	public void auth(final ThirdPlatformAuthListener authListener) {
		String url = doubanService.getAuthorizationUrl();

		ThirdPlatformAuthListener getCodeListener = new ThirdPlatformAuthListener() {

			@Override
			public void onAuthException(Exception e) {
				authListener.onAuthException(e);
			}

			@Override
			public void onAuthComplete(Bundle bundle) {
				final String code = bundle.getString("code");

				Thread workthread = new Thread(new Runnable() {
					@Override
					public void run() {
						// 请求授权信息
						try {
							// 线程阻塞的操作
							Bundle b = doubanService.getAuthResponse(code);

							Editor editor = mPreferences.edit();
							editor.putString(
									Constants.PREFERENCE_KEY_DOUBAN_ACCESS_TOKEN,
									b.getString("access_token"));
							editor.putString(
									Constants.PREFERENCE_KEY_DOUBAN_REFRESH_TOKEN,
									b.getString("refresh_token"));
							editor.putLong(
									Constants.PREFERENCE_KEY_DOUBAN_EXPIRES_IN,
									System.currentTimeMillis()
											+ Long.parseLong(b
													.getString("expires_in")));
							editor.commit();

							authListener.onAuthComplete(b);
						} catch (Exception e) {
							e.printStackTrace();
							authListener.onAuthException(e);
						}
					}
				});
				workthread.start();
			}

			@Override
			public void onAuthCancel() {
				authListener.onAuthCancel();
			}
		};

		OAuthDoubanDialog dialog = new OAuthDoubanDialog(mContext,
				getCodeListener, url);
		dialog.show();
	}

	@Override
	public void clearAuthInformation() {
	}

	@Override
	public boolean hasAuthed() {
		String token = mPreferences.getString(
				Constants.PREFERENCE_KEY_DOUBAN_ACCESS_TOKEN, "");
		Long expiresIn = mPreferences.getLong(
				Constants.PREFERENCE_KEY_DOUBAN_EXPIRES_IN, 0);
		Date date = new Date(expiresIn);
		if (token.equals("") || date.before(new Date()))
			return false;
		else
			return true;
	}

	@Override
	public void getUserInfo(final AsyncListener asyncListener) {
		if (!hasAuthed()) {
			ThirdPlatformAuthListener authListener = new ThirdPlatformAuthListener() {
				@Override
				public void onAuthException(Exception e) {
				}

				@Override
				public void onAuthComplete(Bundle bundle) {
					realGetUserInfo(bundle, asyncListener);
				}

				@Override
				public void onAuthCancel() {
				}
			};
			auth(authListener);
		} else {
			String token = mPreferences.getString(
					Constants.PREFERENCE_KEY_DOUBAN_ACCESS_TOKEN, "");
			Bundle b = new Bundle();
			b.putString("access_token", token);
			realGetUserInfo(b, asyncListener);
		}
	}

	private void realGetUserInfo(final Bundle bundle,
			final AsyncListener asyncListener) {

		Thread workerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				asyncListener.onRequestStart();

				List<Header> headers = new ArrayList<Header>();
				headers.add(new BasicHeader("Authorization", "Bearer "
						+ bundle.getString("access_token")));
				HttpResponse response = HttpUtils.getResponse(
						"https://api.douban.com/v2/user/~me", headers,
						new JSONObject(), mContext, HttpUtils.GET);
				try {
					String responseString = EntityUtils.toString(response
							.getEntity());
					JSONObject info = new JSONObject(responseString);

					if (response.getStatusLine().getStatusCode() == 200) {
						Bundle result = new Bundle();
						result.putString("name", info.getString("name"));
						result.putString("uid", info.getString("uid"));
						result.putString("id", info.getString("id"));

						asyncListener.onRequestComplete(responseString, result);
					} else {
						asyncListener.onRequestError(new Exception(info
								.getString("msg")));
					}

				} catch (Exception e) {
					asyncListener.onRequestException(e);
					e.printStackTrace();
				}
			}
		});
		workerThread.start();

	}

	private class DoubanService {
		private String apiKey;
		private String apiSecret;
		private String redirectUri;

		public DoubanService(String apiKey, String apiSecret, String redirectUri) {
			super();
			this.apiKey = apiKey;
			this.apiSecret = apiSecret;
			this.redirectUri = redirectUri;
		}

		public String getAuthorizationUrl() {
			StringBuffer sb = new StringBuffer();
			sb.append("https://www.douban.com/service/auth2/auth?");

			sb.append("client_id=");
			sb.append(apiKey);

			sb.append("&");
			sb.append("redirect_uri=");
			sb.append(redirectUri);

			sb.append("&");
			sb.append("scope=");
			sb.append("douban_basic_common,shuo_basic_r,shuo_basic_w");

			sb.append("&");
			sb.append("response_type=code");

			return sb.toString();
		}

		private Bundle getAuthResponse(String code) throws Exception {
			Bundle bundle = new Bundle();

			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("client_id", apiKey));
			params.add(new BasicNameValuePair("client_secret", apiSecret));
			params.add(new BasicNameValuePair("redirect_uri", redirectUri));
			params.add(new BasicNameValuePair("client_id", apiKey));
			params.add(new BasicNameValuePair("grant_type",
					"authorization_code"));
			params.add(new BasicNameValuePair("code", code));

			HttpResponse response = HttpUtils.getResponse(
					"https://www.douban.com/service/auth2/token", null, params,
					mContext, HttpUtils.POST);
			String responseString = EntityUtils.toString(response.getEntity());
			JSONObject info = new JSONObject(responseString);

			bundle.putString("access_token", info.getString("access_token"));
			bundle.putString("douban_user_id", info.getString("douban_user_id"));
			bundle.putString("expires_in",
					String.valueOf(info.getInt("expires_in")));
			bundle.putString("refresh_token", info.getString("refresh_token"));

			return bundle;
		}
	}
}
