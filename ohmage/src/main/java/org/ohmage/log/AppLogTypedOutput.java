/*
 * Copyright (C) 2015 ohmage
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

import com.google.gson.JsonObject;

import org.ohmage.app.BuildConfig;
import org.ohmage.sync.ResponseTypedOutput.ResponseFiles;

import java.io.IOException;
import java.io.OutputStream;

import retrofit.mime.MultipartTypedOutput;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedOutput;

/**
 * Uploads an App Log data point
 */
public class AppLogTypedOutput implements TypedOutput {
    private final MultipartTypedOutput mMulitpartType;
    private static final JsonObject acquisitionProvenance;
    static {
        acquisitionProvenance = new JsonObject();
        acquisitionProvenance.addProperty("source_name", "Ohmage-Android-" + BuildConfig.VERSION_NAME);
    }

    public AppLogTypedOutput(JsonObject header, JsonObject data) {

        header.add("acquisition_provenance", acquisitionProvenance);

        JsonObject datapoint = new JsonObject();
        datapoint.add("header", header);
        datapoint.add("body", data);

        mMulitpartType = new MultipartTypedOutput();
        mMulitpartType.addPart("data", new TypedByteArray("application/json",
                datapoint.toString().getBytes()));
    }

    @Override public String fileName() {
        return mMulitpartType.fileName();
    }

    @Override public String mimeType() {
        return mMulitpartType.mimeType();
    }

    @Override public long length() {
        return mMulitpartType.length();
    }

    @Override public void writeTo(OutputStream out) throws IOException {
        mMulitpartType.writeTo(out);
    }
}