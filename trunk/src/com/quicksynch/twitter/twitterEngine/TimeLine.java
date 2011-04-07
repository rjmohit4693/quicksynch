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

package com.quicksynch.twitter.twitterEngine;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.quicksynch.twitter.R;
import com.quicksynch.twitter.data.QuicksynchProvider.TimelineHelper;
import com.quicksynch.twitter.oauthConnect.Oauth_Keys;

public class TimeLine extends Activity{
	
	private final static String consumerKey = Oauth_Keys.twitter_consumer_key;
	private final static String consumerSecret = Oauth_Keys.twitter_consumer_secret;
	private final static String TAG = "quicksynch";
	private SharedPreferences prefs;
	private String prefs_file = "twitter_prefs";
	private String userKey, userSecret;
	private OAuthConsumer mConsumer = null;
	private HttpClient client;
	private Button tweet;
	private EditText tweetText;
	private TextView charsLeft;
	private int maxChars = 140;
	private ListView timeLineList;
	private Cursor c;
	private TimlineAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.twitter_list);
		init();
	}
	
	private void init() {
		c = this.managedQuery(TimelineHelper.CONTENT_URI, new String[] {TimelineHelper.ID, TimelineHelper.USERNAME, 
				TimelineHelper.STATUS, TimelineHelper.IMG_URL}, null, null, null);
		timeLineList = (ListView) findViewById(R.twitterList.friendsTimeLine);
		adapter = new TimlineAdapter(this, c);
		timeLineList.setAdapter(adapter);
		
		prefs = getSharedPreferences(prefs_file, 0);
		userKey = prefs.getString("userKey", null);
		userSecret =  prefs.getString("userSecret", null);
		mConsumer = new CommonsHttpOAuthConsumer( consumerKey, consumerSecret);
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
		charsLeft = (TextView)findViewById(R.twitterList.charsLeft);
		tweetText = (EditText) findViewById(R.twitterList.tweet_text);
		
		tweetText.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}
			
			public void afterTextChanged(Editable s) {
				if(Integer.parseInt(charsLeft.getText().toString()) != 0) {
					charsLeft.setText(String.valueOf(maxChars - s.length()));
				}
				else {
					Toast.makeText(getApplicationContext(), "The maximum Limit is 140 chars", Toast.LENGTH_LONG).show();
				}
			}
		});
		
		tweet = (Button) findViewById(R.twitterList.tweet_button);
		
		tweet.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				String temp = tweetText.getText().toString();
				new PostStatus().execute(temp);
				
			}
		});
		
	}
	
	private HttpParams getparams() {
		HttpParams params =  new BasicHttpParams();
		HttpProtocolParams.setUseExpectContinue(params, false);
		return params;
	}
	
	/*
	 * Purpose: This class represents the thread used to post a tweet 
	 */
	private class PostStatus extends AsyncTask<String, Void , JSONObject> {
		private static final String statusPostUrl = "http://api.twitter.com/1/statuses/update.json";
		
		@Override
		protected JSONObject doInBackground(String... params) {
			JSONObject obj = null;
			try {
				HttpPost post = new HttpPost(statusPostUrl);
				LinkedList<BasicNameValuePair> list= new LinkedList<BasicNameValuePair>();
				list.add(new BasicNameValuePair("status", params[0]));
				post.setEntity(new UrlEncodedFormEntity(list, HTTP.UTF_8));
				post.setParams(getparams());
				mConsumer.sign(post);
				String responce = client.execute(post, new BasicResponseHandler());
				obj = new JSONObject(responce);
				Log.d(TAG,"" + responce);
				
				tweetText.post(new Runnable() {
					public void run() {
						tweetText.setText("");
					}
				});
				
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
			return obj;
		}
	}
	
	private class TimlineAdapter extends CursorAdapter{
		private Context mContext;
		private Cursor mCursor;
		private LayoutInflater mInflater;
		
		public TimlineAdapter(Context context, Cursor c) {
			super(context, c, true);
			this.mContext = context;
			this.mCursor = c;
			mInflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			((TextView)view.findViewById(R.id.username)).setText(cursor.getString(cursor.getColumnIndex(TimelineHelper.USERNAME)));
			((TextView)view.findViewById(R.id.status)).setText(cursor.getString(cursor.getColumnIndex(TimelineHelper.STATUS)));
			final String url = cursor.getString(cursor.getColumnIndex(TimelineHelper.IMG_URL));
			final ImageView img = (ImageView) view.findViewById(R.id.profile_pic);
			
			//fetching the image of the user in a separate thread using a softreference to the ImageView
			new Thread() {

				@Override
				public void run() {
					try {
						URL imgUrl = new URL(url);
						URLConnection conn = imgUrl.openConnection();
						InputStream ip = conn.getInputStream();

						BitmapDrawable bm = new BitmapDrawable(ip);
						final Bitmap profilePic = bm.getBitmap();
						
						img.post(new Runnable() {

							public void run() {
								img.setImageBitmap(profilePic);
							}
						});
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
			}.start();
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return mInflater.inflate(R.layout.timeline_row, parent, false);
		}
		
	}
	
}
