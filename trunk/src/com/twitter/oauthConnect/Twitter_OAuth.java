package com.twitter.oauthConnect;

import com.twitter.R;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import com.twitter.Service.StatusUpdates;
import com.twitter.twitterEngine.TimeLine;

/*
 * Purpose: This is the main Activity which gets the request token and access token from the Twitter
 * 			Web server 
 * TODO:// Need to extract the OAUTH part into a separate engine so that it can be used by all services
 *         Need to create a threaded architecture for that 
 */

public class Twitter_OAuth extends Activity implements OnClickListener{

	private final static String consumerKey = Oauth_Keys.twitter_consumer_key;
	private final static String consumerSecret = Oauth_Keys.twitter_consumer_secret;
	private final static String requestUrl = "http://api.twitter.com/oauth/request_token";
	private final static String accessUrl = "http://api.twitter.com/oauth/access_token";
	private final static String authorizeUrl = "http://api.twitter.com/oauth/authorize";
	private static final String REQUEST_TOKEN = "request_token";
	private static final String REQUEST_SECRET = "request_secret";
	private static final String TAG = "quicksynch";
	private Button login;
	private SharedPreferences prefs;
	private String prefFile = "twitter_prefs";
	private final String CALLBACKURL = "myapp://mainactivity";
	private CommonsHttpOAuthConsumer httpOauthConsumer;
	private OAuthProvider httpOauthprovider;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		initViews();
	}
	
	private void initViews()
	{
		httpOauthConsumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
		httpOauthprovider = new DefaultOAuthProvider(requestUrl, accessUrl, authorizeUrl);
		httpOauthprovider.setOAuth10a(true);
		login=(Button)findViewById(R.login.LoginButton);
		login.setOnClickListener(this);
	}

	public void onClick(View arg0) {
		try {
			prefs = getSharedPreferences(prefFile, 0);
			Editor e = prefs.edit();
			String authUrl = httpOauthprovider.retrieveRequestToken(httpOauthConsumer, CALLBACKURL);
			e.putString(REQUEST_TOKEN, httpOauthConsumer.getToken() );
			e.putString(REQUEST_SECRET, httpOauthConsumer.getTokenSecret() );
			e.commit();
			this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
		} catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onResume () {
		super.onResume();
		Intent intent = getIntent();
		Uri uri = intent.getData();
		if (uri != null && uri.toString().startsWith(CALLBACKURL)) {
			String token = uri.getQueryParameter( OAuth.OAUTH_TOKEN );
			String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
			prefs = getSharedPreferences(prefFile, 0);
			try {
				httpOauthConsumer.setTokenWithSecret( token, prefs.getString( REQUEST_SECRET, "" ) );
				httpOauthprovider.retrieveAccessToken(httpOauthConsumer, verifier);
				String userKey = httpOauthConsumer.getToken();
				String userSecret = httpOauthConsumer.getTokenSecret();
				Editor e = prefs.edit();
				e.putString("userKey", userKey);
				e.putString("userSecret", userSecret);
				e.commit();
				Log.d(TAG,"StartingtheService");
				this.startService(new Intent(this, StatusUpdates.class));
				Log.d(TAG,"Startingthenewactivity");
				Intent i = new Intent(this,TimeLine.class);
				startActivity(i);
			}
			catch(Exception e){
				Log.d(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
	}
}

