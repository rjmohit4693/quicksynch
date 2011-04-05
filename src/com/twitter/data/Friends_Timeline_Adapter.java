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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import com.twitter.R;

/*
 * Purpose: This is an adapter which links the database to the UI
 * TODO:// I dont like the bindview implementation needs some performance enhacements
 */

public class Friends_Timeline_Adapter extends SimpleCursorAdapter{

	static final String from[] = {Database.username,Database.text};
	static final int to[] = {R.friendsTimeLineList.userName,R.friendsTimeLineList.Status};

	public Friends_Timeline_Adapter(Context context, Cursor c) {
		super(context, R.layout.friends_timeline_row, c, from, to);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		super.bindView(view, context, cursor);
		final String url = cursor.getString(3);
		final View v = view;

		new Thread(new Runnable() {
			public void run() {
				try {
					URL imgUrl = new URL(url);
					URLConnection conn = imgUrl.openConnection();
					InputStream ip = conn.getInputStream();

					BitmapDrawable bm = new BitmapDrawable(ip);
					final Bitmap profilePic = bm.getBitmap();
					final ImageView img = (ImageView) v.findViewById(R.friendsTimeLineList.profile_pic);
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
		}).start();
	}
}