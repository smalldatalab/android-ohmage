/*
 * Copyright (C) 2013 ohmage
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

package org.ohmage.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.util.Log;

import org.apache.http.auth.AuthenticationException;
import org.ohmage.auth.AuthUtil;

import java.io.IOException;

import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by cketcham on 12/9/13.
 */
public class OhmageErrorHandler implements ErrorHandler {

    private static final String TAG = OhmageErrorHandler.class.getSimpleName();

    @Override public Throwable handleError(RetrofitError cause) {

        Response r = cause.getResponse();
        if (r != null && r.getStatus() == 401) {
            // invalidate the access token
            AccountManager accountManager = AccountManager.get(Ohmage.app());
            Account[] accounts = accountManager.getAccountsByType(AuthUtil.ACCOUNT_TYPE);
            if (accounts.length != 0) {
                String token =    accountManager.peekAuthToken(accounts[0], AuthUtil.AUTHTOKEN_TYPE);
                if(token != null){
                    accountManager.invalidateAuthToken(AuthUtil.ACCOUNT_TYPE, token);
                    Log.e(TAG, "Invalidated "+ token);
                }
            }
            return new AuthenticationException("Error authenticating with ohmage", cause);
        }

        return new IOException(cause.getResponse().getStatus() + "(" + cause.getResponse().getReason() + ")", cause);
    }
}
