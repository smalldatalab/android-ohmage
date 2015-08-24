/*
 * Copyright (C) 2014 ohmage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ohmage.log;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.ohmage.provider.OhmageDbHelper;
import org.ohmage.provider.OhmageDbHelper.Tables;
import org.ohmage.provider.ResponseContract.Responses;
import org.ohmage.sync.ResponseSyncAdapter;

public class AppLogContentProvider extends ContentProvider {

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Delete not allowed. Use AppLogManager.");
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.ohmage.applog";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Insert not allowed. Use AppLogManager.");
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        throw new UnsupportedOperationException("Query not allowed. Use AppLogManager.");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Update not allowed. Use AppLogManager.");
    }

    private boolean isSyncAdapter(Uri uri) {
        return uri.getQueryParameter(ResponseSyncAdapter.IS_SYNCADAPTER) != null;
    }
}
