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

package org.ohmage.operators;

import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import org.ohmage.app.Ohmage;
import org.ohmage.operators.ContentProviderSaver.Savable;
import org.ohmage.sync.OhmageSyncAdapter;

import javax.inject.Inject;

import rx.Subscriber;
import rx.functions.Action1;

/**
 * Saves an item to the ohmage content provider
 */
public class ContentProviderSaver implements Action1<Savable> {
    private static final String TAG = ContentProviderSaver.class.getSimpleName();

    private final boolean mIsSyncAdapter;

    @Inject Gson gson;

    public ContentProviderSaver() {
        this(false);
    }

    public ContentProviderSaver(boolean isSyncAdapter) {
        Ohmage.app().getApplicationGraph().inject(this);
        mIsSyncAdapter = isSyncAdapter;
    }

    @Override public void call(Savable savable) {
        try {
            Log.e(ContentProviderSaver.class.getSimpleName(), savable.toString());
            Uri uri = savable.getUrl();
            if (mIsSyncAdapter)
                uri = OhmageSyncAdapter.appendSyncAdapterParam(uri);
            Ohmage.app().getContentResolver().insert(uri, savable.toContentValues(this));
            savable.onSaved();
        }catch (Exception e){
            Log.e(ContentProviderSaver.class.getSimpleName(), "Error ", e);
        }
    }

    public Gson gson() {
        return gson;
    }

    public static interface Savable {
        ContentValues toContentValues(ContentProviderSaver saver);

        Uri getUrl();

        void onSaved();
    }

    public static class ContentProviderSaverSubscriber extends Subscriber<Savable> {

        private final boolean mIsSyncAdapter;

        public ContentProviderSaverSubscriber(boolean isSyncAdapter) {
            mIsSyncAdapter = isSyncAdapter;
        }

        @Override public void onCompleted() {

        }

        @Override public void onError(Throwable e) {
            e.printStackTrace();
        }

        @Override public void onNext(Savable args) {
            new ContentProviderSaver(mIsSyncAdapter).call(args);
        }
    }
}