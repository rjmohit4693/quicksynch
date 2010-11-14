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
