package com.twitter.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/*
 * Purpose: This class is a helper class user to create a database
 *          It stores the friends status updates from Twitter
 */
public class Database extends SQLiteOpenHelper{
	
	private static final String TAG = "quicksynch";
	static final int version = 2;
	static final String DB_NAME = "timeline.db";
	static final String username = "username";
	static final String text = "status";
	
	public Database(Context context) {
		super(context, DB_NAME, null, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "create table FRIENDS_TIMELINE (_id integer primary key, username text, status text, imgurl text);";
		db.execSQL(sql);
		Log.d(TAG,"Friends status Table created");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("drop table FRIENDS_TIMELINE;");
		this.onCreate(db);
	}
	public static ContentValues convertToContentValues (Long id, String userName, String text, String Img_URL) {
		ContentValues values = new ContentValues();
		values.put("_id", id);
		values.put("username", userName);
		values.put("status", text);
		values.put("imgurl", Img_URL);
		return values;
	}
}
