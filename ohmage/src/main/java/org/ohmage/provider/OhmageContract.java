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

package org.ohmage.provider;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import org.ohmage.models.SchemaId;

import java.util.List;

/**
 * Created by cketcham on 1/29/14.
 */
public class OhmageContract {

    public static final String CONTENT_AUTHORITY = "org.ohmage.db";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    interface OhmletColumns {
        /**
         * Unique string identifying the ohmlet
         */
        String OHMLET_ID = "ohmlet_id";

        /**
         * Name of ohmlet
         */
        String OHMLET_NAME = "ohmlet_name";

        /**
         * Description of ohmlet
         */
        String OHMLET_DESCRIPTION = "ohmlet_description";

        /**
         * String array of surveys for this ohmlet
         */
        String OHMLET_SURVEYS = "ohmlet_surveys";

        /**
         * String array of streams for this ohmlet
         */
        String OHMLET_STREAMS = "ohmlet_streams";

        /**
         * String array of members for this ohmlet
         */
        String OHMLET_MEMBERS = "ohmlet_members";

        /**
         * The privacy state for this ohmlet
         */
        String OHMLET_PRIVACY_STATE = "ohmlet_privacy_state";

        /**
         * If this is true the ohmlet should be synchronized to the server
         */
        String OHMLET_DIRTY = "ohmlet_dirty";
    }

    private static final String PATH_OHMLETS = "ohmlets";

    /**
     * Represents an ohmlet.
     */
    public static final class Ohmlets implements BaseColumns, OhmletColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_OHMLETS)
                .build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.ohmage.ohmlets";

        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.ohmage.ohmlets.ohmlet";

        public static Uri getUriForOhmletId(String id) {
            return Uri.withAppendedPath(CONTENT_URI, id);
        }

        public static final String[] DEFAULT_PROJECTION = new String[]{
                OHMLET_ID, OHMLET_NAME, OHMLET_DESCRIPTION, OHMLET_SURVEYS, OHMLET_STREAMS,
                OHMLET_MEMBERS, OHMLET_PRIVACY_STATE, OHMLET_DIRTY
        };
    }

    interface SurveyColumns {
        /**
         * Unique string identifying the survey
         */
        String SURVEY_ID = "survey_id";

        /**
         * Version to identify survey
         */
        String SURVEY_VERSION = "survey_version";

        /**
         * A JSON array of survey items
         */
        String SURVEY_ITEMS = "survey_items";

        /**
         * The name of the survey
         */
        String SURVEY_NAME = "survey_name";

        /**
         * The description of the survey
         */
        String SURVEY_DESCRIPTION = "survey_description";

        /**
         * The pending time of this survey, or -1 if it is not pending
         */
        String SURVEY_PENDING_TIME = "survey_pending_time";

        /**
         * The timezone this survey was pending in for printing the time
         */
        String SURVEY_PENDING_TIMEZONE = "survey_pending_timezone";
    }

    private static final String PATH_SURVEYS = "surveys";

    /**
     * Represents a stream.
     */
    public static final class Surveys implements BaseColumns, SurveyColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SURVEYS)
                .build();

        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.ohmage.surveys.survey";

        public static Uri getUriForSurveyId(SchemaId id) {
            return getUriForSurveyId(id.toString());
        }

        public static Uri getUriForSurveyId(String id) {
            return Uri.withAppendedPath(CONTENT_URI, id.toString());
        }

        public static Uri getUriForSurveyIdVersion(SchemaId id) {
            return Uri.withAppendedPath(getUriForSurveyId(id), id.getVersion());
        }

        public static Uri getUriForSurveyIdVersion(String id, String version) {
            return Uri.withAppendedPath(getUriForSurveyId(id), version);
        }

        public static final String[] DEFAULT_PROJECTION = new String[]{
                SURVEY_ID, SURVEY_VERSION, SURVEY_ITEMS, SURVEY_NAME, SURVEY_DESCRIPTION
        };

        public static String getId(Uri uri) {
            List<String> path = uri.getPathSegments();
            return (path.size() < 2) ? null : path.get(1);
        }

        public static Long getVersion(Uri uri) {
            List<String> path = uri.getPathSegments();
            return (path.size() < 3) ? 1 : Long.parseLong(path.get(2));
        }
    }

    interface StreamColumns {
        /**
         * Unique string identifying the stream
         */
        String STREAM_ID = "stream_id";

        /**
         * Version to identify stream
         */
        String STREAM_VERSION = "stream_version";

        /**
         * The name of this stream
         */
        String STREAM_NAME = "stream_name";

        /**
         * The description of this stream
         */
        String STREAM_DESCRIPTION = "stream_description";

        /**
         * The application for this stream
         */
        String STREAM_APP = "stream_app";
    }

    private static final String PATH_STREAMS = "streams";

    /**
     * Represents a stream.
     */
    public static final class Streams implements BaseColumns, StreamColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_STREAMS)
                .build();

        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.dir/vnd.ohmage.streams.stream";

        public static Uri getUriForStreamId(String id) {
            return Uri.withAppendedPath(CONTENT_URI, id);
        }

        public static Uri getUriForStreamIdVersion(String id, long version) {
            return ContentUris.withAppendedId(getUriForStreamId(id), version);
        }

        public static final String[] DEFAULT_PROJECTION = new String[]{
                STREAM_ID, STREAM_VERSION, STREAM_NAME, STREAM_DESCRIPTION, STREAM_APP
        };

        public static String getId(Uri uri) {
            List<String> path = uri.getPathSegments();
            return (path.size() < 2) ? null : path.get(1);
        }

        public static Long getVersion(Uri uri) {
            List<String> path = uri.getPathSegments();
            return (path.size() < 3) ? null : Long.parseLong(path.get(2));
        }
    }
}
