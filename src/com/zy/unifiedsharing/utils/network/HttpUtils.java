package com.zy.unifiedsharing.utils.network;

import java.io.IOException;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class HttpUtils {
	public static int GET=0;
	public static int POST=1;
	
	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			Log.i("NetWorkState", "Unavailabel");
			return false;
		} else {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						Log.i("NetWorkState", "Availabel");
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static HttpResponse getResponse(String url,List<Header> headers,JSONObject json,Context context,int type){
		HttpResponse response = null;
		try {
			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
			HttpClient httpClient = new DefaultHttpClient(httpParams);
			ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE); 
			NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

			if (type==0) {
				HttpGet request = new HttpGet(url);
				
				if (headers!=null) {
                    for (int i = 0; i < headers.size(); i++) {
                        request.addHeader(headers.get(i));
                    }
                }
				response=httpClient.execute(request);
			}
			else {
				HttpPost request = new HttpPost(url);
				request.setEntity(new StringEntity(json.toString(),HTTP.UTF_8));
	        	
				if (headers!=null) {
		        	for (int i = 0; i < headers.size(); i++) {
						request.addHeader(headers.get(i));
					}
				}
	        	
				if (activeNetInfo.getExtraInfo()!=null&&activeNetInfo.getExtraInfo().equals("cmwap")) {
					HttpHost proxy = new HttpHost("10.0.0.172", 80, "http");
		        	HttpHost target = new HttpHost(url.split("/")[2], 80, "http");
		        	httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
					
		        	response = httpClient.execute(target, request);
				}
				else {
					response = httpClient.execute(request);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}
	
    public static HttpResponse getResponse(String url , List<Header> headers , List<NameValuePair> params , Context context , int type) throws IOException
    {
        HttpResponse response = null;
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
        HttpClient httpClient = new DefaultHttpClient(httpParams);
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

        if (type == 0)
        {
            HttpGet request = new HttpGet(url);
            if (headers!=null) {
                for (int i = 0; i < headers.size(); i++) {
                    request.addHeader(headers.get(i));
                }
            }
            response = httpClient.execute(request);
        }
        else
        {
            HttpPost request = new HttpPost(url);
            request.setParams(new BasicHttpParams());
            request.setEntity(new UrlEncodedFormEntity(params));

            if (headers != null)
            {
                for (int i = 0; i < headers.size(); i++)
                {
                    request.addHeader(headers.get(i));
                }
            }

            if (activeNetInfo.getExtraInfo() != null && activeNetInfo.getExtraInfo().equals("cmwap"))
            {
                HttpHost proxy = new HttpHost("10.0.0.172", 80, "http");
                HttpHost target = new HttpHost(url.split("/")[2], 80, "http");
                httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

                response = httpClient.execute(target, request);
            }
            else
            {
                response = httpClient.execute(request);
            }
        }
        return response;
    }
	
	public static void sendRequest(String url,List<NameValuePair> params){
		try{
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
		HttpClient httpClient = new DefaultHttpClient(httpParams);
		HttpPost request = new HttpPost(url);
		request.setEntity(new UrlEncodedFormEntity(params,HTTP.UTF_8));
		httpClient.execute(request);
		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}