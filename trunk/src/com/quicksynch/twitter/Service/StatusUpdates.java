/*
 * Copyright (C) 2010 The quicksynch Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.quicksynch.twitter.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import com.quicksynch.twitter.data.QuicksynchProvider;
import com.quicksynch.twitter.data.QuicksynchProvider.TimelineHelper;
import com.quicksynch.twitter.oauthConnect.Oauth_Keys;

public class StatusUpdates extends Service{
	
	private static final String FRIENDS_TIMELINE = "http://api.twitter.com/1/statuses/friends_timeline.json?page=";
	private static final Uri TIMELINE_URI = TimelineHelper.CONTENT_URI;
	private final static String consumerKey = Oauth_Keys.twitter_consumer_key;
	private final static String consumerSecret = Oauth_Keys.twitter_consumer_secret;
	private static final String TAG = "quicksynch";
	private String prefs_file = "twitter_prefs";
	private String userKey, userSecret;
	private SharedPreferences prefs;
	private Handler handler;
	private updater updater;
	private OAuthConsumer mConsumer = null;
	private HttpClient client;
	private int pageNo = 1;
	
	@Override
	public void onCreate() {
		super.onCreate();
		prefs = getSharedPreferences(prefs_file, 0);
		userKey = prefs.getString("userKey", null);
		userSecret =  prefs.getString("userSecret", null);
		handler = new Handler();
		Log.d(TAG, ":Service" + userKey + userSecret);
		mConsumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
		mConsumer.setTokenWithSecret(userKey, userSecret);

		HttpParams parameters = new BasicHttpParams();
		HttpProtocolParams.setVersion(parameters, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(parameters, HTTP.DEFAULT_CONTENT_CHARSET);
		HttpProtocolParams.setUseExpectContinue(parameters, false);
		HttpConnectionParams.setTcpNoDelay(parameters, true);
		HttpConnectionParams.setSocketBufferSize(parameters, 8192);

		SchemeRegistry schReg = new SchemeRegistry();
		schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		ClientConnectionManager manager = new ThreadSafeClientConnManager(parameters, schReg);
		client = new DefaultHttpClient(manager, parameters);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		updater = new updater();
		handler.post(updater);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		handler.removeCallbacks(updater);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	class updater implements Runnable {
		static final long DELAY = 30000L;
		public void run() {
			JSONArray obj = null;
			int i;
			try {
				Log.d(TAG,"thread started");
				HttpGet get = new HttpGet(FRIENDS_TIMELINE + pageNo);
				HttpParams params = new BasicHttpParams();
				HttpProtocolParams.setUseExpectContinue(params, false);
				get.setParams(params);
				//now lets get it :)
				mConsumer.sign(get);
				String responce = client.execute(get, new BasicResponseHandler());
				obj = new JSONArray(responce);
				for(i=0;i<obj.length();i++) {
					JSONObject jsoObj = obj.getJSONObject(i);
					Long id = Long.parseLong(jsoObj.getString("id"));
					JSONObject user = (JSONObject) jsoObj.get("user");
					String name = user.getString("screen_name");
					String text = jsoObj.getString("text");
					String imgUrl = user.getString("profile_image_url");
					ContentValues values = new ContentValues();
					values.put("_id", id);
					values.put("username", name);
					values.put("status", text);
					values.put("imgurl", imgUrl);
					getContentResolver().insert(TimelineHelper.CONTENT_URI, values);
					
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (OAuthMessageSignerException e) {
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				e.printStackTrace();
			}
			pageNo++;
			handler.postDelayed(this,DELAY);
		}
	}
}
