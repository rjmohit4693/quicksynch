package com.quicksynch.twitter.data;

import java.util.HashMap;
import java.util.Map;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class QuicksynchProvider extends ContentProvider{
	
	private static String TAG = "QuickSynchProvider";
	private static String TIMELINE_DATABASE_NAME = "quicksynch.db";
	private static int DATABASE_VERSION = 1;
	private DatabaseHelper databaseHelper;
	private static String AUTHORITY = "com.quicksynch.twitter.data.providers.QuickSynchProvider"; 
	private static UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private static final int TIMELINE = 1;
	private static Map timelineProjectionMap;
	
	static {
		//add further uri's here
		mUriMatcher.addURI(AUTHORITY,TimelineHelper.TABLENAME , TIMELINE);
		timelineProjectionMap = new HashMap<String, String>();
		timelineProjectionMap.put(TimelineHelper.ID, TimelineHelper.ID);
		timelineProjectionMap.put(TimelineHelper.USERNAME, TimelineHelper.USERNAME);
		timelineProjectionMap.put(TimelineHelper.STATUS, TimelineHelper.STATUS);
		timelineProjectionMap.put(TimelineHelper.IMG_URL, TimelineHelper.IMG_URL);
	}
	
	private synchronized SQLiteDatabase getWritableDatabase() {
		if(databaseHelper != null) {
			return databaseHelper.getWritableDatabase();
		}
		databaseHelper = new DatabaseHelper(getContext());
		return databaseHelper.getWritableDatabase();
	}
	
	private synchronized SQLiteDatabase getReadableDatabase() {
		if(databaseHelper != null) {
			return databaseHelper.getReadableDatabase();
		}
		
		databaseHelper = new DatabaseHelper(getContext());
		return databaseHelper.getReadableDatabase();
	}
	
	/**
	 * Helper class for easy access for the timeline table
	 * @author ali
	 *
	 */
	public static final class TimelineHelper implements BaseColumns{
		public static final String ID = "_id";
		public static final String USERNAME = "username";
		public static final String STATUS = "status";
		public static final String IMG_URL = "imgurl";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/timeline");
		private static final String TABLENAME = "timeline";
		private static String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.quicksynch.timeline";
	}
	
	private static class DatabaseHelper extends SQLiteOpenHelper{
		
		public DatabaseHelper (Context context) {
			super(context, TIMELINE_DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TimelineHelper.TABLENAME +  "(" + TimelineHelper.ID + " integer primary key," + TimelineHelper.USERNAME +
					" text," + TimelineHelper.STATUS + " text," + TimelineHelper.IMG_URL +" text);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, " Upgrading the database from version:" + oldVersion + " to version:" + newVersion + 
					". This will erase all previous data");
			db.execSQL("DROP TABLE IF EXISTS " + TimelineHelper.TABLENAME);
			onCreate(db);
		}
		
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = getWritableDatabase();
		int count = 0;
		switch(mUriMatcher.match(uri)) {
			case TIMELINE:
				count = db.delete(TimelineHelper.TABLENAME, selection, selectionArgs);
				break;
			default:
				throw new IllegalArgumentException("INCORRECT URI:" + uri.toString());
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch(mUriMatcher.match(uri)) {
			case TIMELINE:
				return TimelineHelper.CONTENT_TYPE;
			default:
				throw new IllegalArgumentException("INCORRECT URI:" + uri.toString());
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		
		switch(mUriMatcher.match(uri)) {
		case TIMELINE:
			if(values == null) {
				throw new IllegalArgumentException("CONTENT VALUES CANNOT BE NULL");
			}
			SQLiteDatabase database = getWritableDatabase();
			long rowId = database.insert(TimelineHelper.TABLENAME, TimelineHelper.STATUS, values);
			
			if(rowId > 0){
				Uri rowUri = ContentUris.withAppendedId(TimelineHelper.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(rowUri, null);
				return rowUri;
			}
			break;
		}
		
		throw new SQLException("Failed to insert row to URI:" + uri.toString() );
	}

	@Override
	public boolean onCreate() {
		databaseHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		switch(mUriMatcher.match(uri)) {
			case TIMELINE:
				builder.setTables(TimelineHelper.TABLENAME);
				builder.setProjectionMap(timelineProjectionMap);
				break;
			default:
				throw new IllegalArgumentException("INCORRECT URI:" + uri.toString()); 
		}
		
		SQLiteDatabase database = getReadableDatabase();
		Cursor c = builder.query(database, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = getWritableDatabase();
		int count = 0;
		switch(mUriMatcher.match(uri)) {
		
			case TIMELINE:
				count = db.update(TimelineHelper.TABLENAME, values, selection, selectionArgs);
				break;
			default:
				throw new IllegalArgumentException("INCORRECT URI: " + uri.toString());
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

}
