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

package org.ohmage.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.model.people.Person;

import org.apache.http.auth.AuthenticationException;
import org.ohmage.app.MainActivity;
import org.ohmage.app.Ohmage;
import org.ohmage.app.OhmageService;
import org.ohmage.app.OhmletActivity.OhmletFragment;
import org.ohmage.app.R;
import org.ohmage.dagger.PlusClientFragmentModule;
import org.ohmage.log.AppLogSyncAdapter;
import org.ohmage.models.AccessToken;
import org.ohmage.models.Ohmlet;
import org.ohmage.models.Ohmlet.Member;
import org.ohmage.models.Ohmlet.Role;
import org.ohmage.models.User;
import org.ohmage.operators.ContentProviderSaver;
import org.ohmage.provider.OhmageContract;
import org.ohmage.provider.ResponseContract;
import org.ohmage.streams.StreamContract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.util.functions.Action1;

public class AuthenticatorActivity extends AuthenticatorFragmentActivity implements
        PlusClientFragment.OnSignInListener, CreateAccountFragment.Callbacks,
        AuthenticateFragment.Callbacks, SignInFragment.Callbacks {

    @Inject AuthHelper auth;
    @Inject OhmageService ohmageService;
    @Inject AccountManager am;
    @Inject PlusClientFragment mPlusClientFragment;

    public static final int REQUEST_CODE_PLUS_CLIENT_FRAGMENT = 0;
    public static final int GOOGLE_CODE_RESULT = 1;

    private static final String TAG_ERROR_DIALOG = "error_dialog";
    private static final String TAG_OHMAGE_SIGN_IN = "sign_in_email";
    private static final String TAG_CREATE_ACCOUNT = "create_account";
    private static final String TAG_INFO_WINDOW = "info_window";
    public static final String EXTRA_HANDLE_USER_RECOVERABLE_ERROR = "extra_handle_error";

    public static final String EXTRA_CLEAR_DEFAULT_ACCOUNT = "clear_default_account";
    public static final String EXTRA_JOIN_OHMLET_ID = "extra_join_ohmlet_id";
    public static final String EXTRA_USER_INVITATION_CODE = "extra_user_invitation_code";
    public static final String EXTRA_EMAIL = "extra_email";

    private boolean mClearDefaultAccount;
    private String omhUsername;

    private AuthenticateFragment mAuthenticateFragment;

    private ArrayList<OhmageService.CancelableCallback> mNetworkCallbacks =
            new ArrayList<OhmageService.CancelableCallback>();

    private String mJoinOhmletId;

    /**
     * We need to listen to the back stack so we know if we should cancel network requests,
     * and if we should stop showing the spinner
     */
    private FragmentManager.OnBackStackChangedListener cancelRequests =
            new FragmentManager.OnBackStackChangedListener() {

                @Override
                public void onBackStackChanged() {
                    FragmentManager fm = getSupportFragmentManager();

                    // When we go back to the top the progress spinner should not be shown
                    if (fm.getBackStackEntryCount() == 0) {
                        showProgress(false);
                    }

                    if (fm.getBackStackEntryCount() - 1 >= 0) {
                        FragmentManager.BackStackEntry entry =
                                fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1);

                        // When we pop back to the info entry, we should cancel all network operations
                        // TODO: only cancel the network operations we started using a tag
                        if (TAG_INFO_WINDOW.equals(entry.getName())) {
                            for (OhmageService.CancelableCallback callback : mNetworkCallbacks) {
                                callback.cancel();
                            }
                        }
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_authenticate);

        FragmentManager fm = getSupportFragmentManager();

        mAuthenticateFragment = (AuthenticateFragment) fm.findFragmentByTag("authenticate");
        // We should add the authenticate fragment the first time this activity is shown
        if (mAuthenticateFragment == null) {
            mAuthenticateFragment = new AuthenticateFragment();
            fm.beginTransaction().add(R.id.authenticate_frame, mAuthenticateFragment,
                    "authenticate").commit();
        }

        // We handle user recoverable errors on behalf of the account authenticator
        UserRecoverableAuthException authException =
                (UserRecoverableAuthException) getIntent()
                        .getSerializableExtra(EXTRA_HANDLE_USER_RECOVERABLE_ERROR);
        if (authException != null) {
            try {
                throw authException;
            } catch (GooglePlayServicesAvailabilityException playEx) {
                GooglePlayServicesErrorDialogFragment fragment =
                        new GooglePlayServicesErrorDialogFragment();
                fragment.setArguments(GooglePlayServicesErrorDialogFragment.createArguments(
                        playEx.getConnectionStatusCode(), REQUEST_CODE_PLUS_CLIENT_FRAGMENT));
                showErrorDialog(fm, fragment);
            } catch (UserRecoverableAuthException userAuthEx) {
                startActivityForResult(userAuthEx.getIntent(), REQUEST_CODE_PLUS_CLIENT_FRAGMENT);
            }
            return;
        }

        // Check to see if an account already exists. If an account exists, it should only allow the
        // user to authenticate with that account since the system account doesn't change
        Account[] accounts = am.getAccountsByType(AuthUtil.ACCOUNT_TYPE);
        if (accounts.length != 0) {
            // If an account exists we should authenticate using that account
            SignInFragment signInEmailFragment =
                    (SignInFragment) fm.findFragmentByTag(TAG_OHMAGE_SIGN_IN);

            // Create the fragment if it doesn't exist
            if (signInEmailFragment == null) {
                signInEmailFragment = new SignInFragment();
            }
            signInEmailFragment.setEmail(accounts[0].name);

            if (!signInEmailFragment.isAdded()) {
                FragmentTransaction ft = fm.beginTransaction().detach(mAuthenticateFragment);
                ft.add(R.id.sign_in_ohmage_frame, signInEmailFragment, TAG_OHMAGE_SIGN_IN).commit();
            } else {
                FragmentTransaction ft = fm.beginTransaction().detach(mAuthenticateFragment);
                ft.attach(signInEmailFragment).commit();
            }
        }

        mClearDefaultAccount = getIntent().getBooleanExtra(EXTRA_CLEAR_DEFAULT_ACCOUNT, false);

        // Check for auto join ohmlet id
        mJoinOhmletId = getIntent().getStringExtra(EXTRA_JOIN_OHMLET_ID);
    }

    @Override
    protected void onResume() {
        super.onResume();

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(cancelRequests);
    }

    @Override
    protected void onPause() {
        super.onPause();

        FragmentManager fm = getSupportFragmentManager();
        fm.removeOnBackStackChangedListener(cancelRequests);
    }

    @Override
    public void onGoogleSignInClick() {
        showProgress(true);
        mPlusClientFragment.signIn(REQUEST_CODE_PLUS_CLIENT_FRAGMENT);
    }

    @Override
    public void onOmhSignInClick(String username, String password) {
        showProgress(true);
        omhUsername = username;

        OhmageService.CancelableCallback<AccessToken> callback =
                new OhmageService.CancelableCallback<AccessToken>() {
                    @Override
                    public void success(AccessToken accessToken, Response response) {
                        if (!isCancelled()) {
                            try {
                                createAccount(omhUsername, accessToken);
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override public void failure(RetrofitError error) {
                        if (isCancelled()) {
                            return;
                        }

                        if (error.getResponse().getStatus() == 400) {
                            runOnUiThread(new Runnable() {
                                @Override public void run() {
                                    if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                                        showProgress(false);
                                    }
                                    Toast.makeText(getBaseContext(), R.string.error_invalid_credentials, Toast.LENGTH_SHORT)
                                                .show();
                                }
                            });
                        } else if (error.getResponse().getStatus() == 404) {
                            runOnUiThread(new Runnable() {
                                @Override public void run() {
                                    if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                                        showProgress(false);
                                    }
                                    Toast.makeText(getBaseContext(), R.string.error_server_not_found, Toast.LENGTH_SHORT)
                                            .show();
                                }
                            });
                        } else {
                            onRetrofitError(error);
                        }
                    }
                };
        ohmageService.getAccessTokenWithOmhUserPassword("password", username,
                password, callback);
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        switch (requestCode) {
            case GOOGLE_CODE_RESULT:
                if (responseCode == RESULT_OK) {
                    String token = intent.getStringExtra("authtoken");
                    if (token != null) {
                        useGoogleToken(token, false);
                        return;
                    }
                }
            case REQUEST_CODE_PLUS_CLIENT_FRAGMENT:
                // Only show progress if the result failed indicating there was an error
                showProgress(responseCode == RESULT_OK);
                mPlusClientFragment.handleOnActivityResult(requestCode, responseCode, intent);
                break;
        }
    }

    @Override
    public void onSignedIn(PlusClient plusClient) {
        // If the user just logged out, their google auth credentials will still be logged in
        // so we need to always clear the first account by default
        if (mClearDefaultAccount) {
            mClearDefaultAccount = false;
            plusClient.clearDefaultAccount();
        } else {
            startLogin(plusClient);
        }
    }

    @Override
    public void onSignInFailed() {
        showProgress(false);
        // There was no default account
        mClearDefaultAccount = false;
    }

    /**
     * Called when the user clicks the create account button if we are performing an action
     * which requires the parent activity to fetch the token. The parent activity must
     * call {@link org.ohmage.auth.CreateAccountFragment.UseToken#useToken(String)} with the token.
     */
    @Override
    public void fetchToken(final CreateAccountFragment.UseToken callback) {
        new GoogleAccessTokenTask(mPlusClientFragment.getClient().getAccountName(),
                new GoogleAccessTokenCallback() {
                    @Override
                    public void onGoogleAccessTokenReceived(String token) {
                        callback.useToken(token);
                    }
                }).execute();
    }

    /**
     * Called when the account is actually being created
     */
    @Override
    public void onCreateAccount() {
        hideKeyboard();
        // Hide the create account fragment and show the progress spinner
        hideFragment(TAG_CREATE_ACCOUNT);
    }

    /**
     * Called when the account is being signed in from the E-mail fragment
     */
    @Override
    public void onAccountSignInOhmage() {
        hideKeyboard();
        // Hide the sign in fragment
        hideFragment(TAG_OHMAGE_SIGN_IN);
    }

    /**
     * Show the sign in fragment
     */
    private void showSignInFragment() {
        FragmentManager fm = getSupportFragmentManager();
        SignInFragment signInOhmageFragment =
                (SignInFragment) fm.findFragmentByTag(TAG_OHMAGE_SIGN_IN);

        // Create the fragment if it doesn't exist
        if (signInOhmageFragment == null) {
            signInOhmageFragment = new SignInFragment();
        }

        // Add the fragment
        addFragment(R.id.sign_in_ohmage_frame, signInOhmageFragment, TAG_OHMAGE_SIGN_IN);
    }

    /**
     * Show the create account fragment
     *
     * @param grantType
     * @param fullName
     */
    private void showCreateAccountFragment(AuthUtil.GrantType grantType, String fullName) {
        FragmentManager fm = getSupportFragmentManager();
        CreateAccountFragment createAccountFragment =
                (CreateAccountFragment) fm.findFragmentByTag(TAG_CREATE_ACCOUNT);

        // Create the fragment if it doesn't exist
        if (createAccountFragment == null) {
            createAccountFragment = new CreateAccountFragment();
        }

        // Set the parameters for this fragment
        createAccountFragment.setGrantType(grantType);
        createAccountFragment.setFullName(fullName);

        // If an email exists, send that
        if (getIntent().hasExtra(EXTRA_EMAIL))
            createAccountFragment.setEmail(getIntent().getStringExtra(EXTRA_EMAIL));

        // Add the fragment
        addFragment(R.id.create_account_frame, createAccountFragment, TAG_CREATE_ACCOUNT);
    }

    /**
     * Shows the fragment with the correct transition and back stack
     *
     * @param fragment
     */
    private void showFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction().detach(mAuthenticateFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(TAG_INFO_WINDOW).attach(fragment).commit();
    }

    /**
     * Adds the fragmetn if it doesn't exist and shows it with the correct transition and
     * back stack
     *
     * @param id
     * @param fragment
     * @param tag
     */
    private void addFragment(int id, Fragment fragment, String tag) {
        if (!fragment.isAdded()) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction().detach(mAuthenticateFragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(TAG_INFO_WINDOW).add(id, fragment, tag).commit();
        } else {
            showFragment(fragment);
        }
    }

    /**
     * Hides a fragment by detaching it from the hierarchy. This allows it to keep its state,
     * so if it is attached later it won't lose anything.
     *
     * @param tag
     */
    private void hideFragment(String tag) {
        showProgress(true);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(tag);
        fm.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .detach(fragment).attach(mAuthenticateFragment).addToBackStack(null).commit();
    }

    /**
     * Shows the progress UI and hides the authentication buttons.
     */
    private void showProgress(final boolean show) {
        mAuthenticateFragment.showProgress(show);
    }

    private void useGoogleToken(final String token, final boolean shouldRetry) {
        OhmageService.CancelableCallback<AccessToken> callback =
                new OhmageService.CancelableCallback<AccessToken>() {
                    @Override
                    public void success(AccessToken accessToken, Response response) {
                        if (!isCancelled()) {
                            createAccount(mPlusClientFragment.getClient().getAccountName(),
                                    accessToken);
                        }
                    }

                    @Override public void failure(RetrofitError error) {
                        if (isCancelled()) {
                            return;
                        }

                        if (!Ohmage.USE_DSU_DATAPOINTS_API && error.getResponse().getStatus() == 409) {
                            // won't happen for OMH DSU
                            Person person =
                                    mPlusClientFragment.getClient().getCurrentPerson();
                            String fullName = null;
                            if (person != null) {
                                fullName = person.getDisplayName();
                            }
                            showCreateAccountFragment(AuthUtil.GrantType.GOOGLE_OAUTH2,
                                    fullName);
                        } else if (error.getResponse().getStatus() == 400 && shouldRetry) {
                            GoogleAuthUtil
                                    .invalidateToken(getApplicationContext(), token.substring(8));
                            startLogin(mPlusClientFragment.getClient());
                        } else {
                            onRetrofitError(error);
                        }
                    }
                };
        ohmageService.getAccessTokenWithGoogleAccessToken(AuthUtil.OMH_CLIENT_ID,
                AuthUtil.OMH_CLIENT_SECRET, token, callback);
    }

    /**
     * Starts the process of getting an auth_token from the server
     *
     * @param plusClient
     */
    private void startLogin(final PlusClient plusClient) {
        final String email = plusClient.getAccountName();
        new GoogleAccessTokenTask(email, new GoogleAccessTokenCallback() {
            @Override
            public void onGoogleAccessTokenReceived(String token) {
                // Now that we have a google accessToken, we can make a request to get one from ohmage
                useGoogleToken(token, true);
            }
        }).execute();
    }

    public void createAccount(String email, AccessToken token) {
        // Join ohmlet if applicable
        if (mJoinOhmletId != null)
            autoJoinOhmlet(token.getUserId());

        // Add the account or find an existing account
        Account account = addOrFindAccount(email, token.getRefreshToken());

        if (mPlusClientFragment.getClient() != null &&
            mPlusClientFragment.getClient().isConnected()) {
            String googleAccountName = mPlusClientFragment.getClient().getAccountName();
            am.setUserData(account, Authenticator.USER_DATA_GOOGLE_ACCOUNT, googleAccountName);
        }

        am.setUserData(account, Authenticator.USE_PASSWORD, String.valueOf(false));
        am.setUserData(account, Authenticator.USER_ID, token.getUserId());
        am.setAuthToken(account, AuthUtil.AUTHTOKEN_TYPE, token.getAccessToken());

        finishAccountAdd(email, token.getAccessToken(), token.getRefreshToken());
    }

    public void createAccount(User user, String password) {
        // Join ohmlet if applicable
        if (mJoinOhmletId != null) {
            if (user.registration != null) {
                autoJoinOhmlet(user.registration.userId);
            } else {
                autoJoinOhmlet("me");
            }
        }

        // Add the account or find an existing account
        Account account = addOrFindAccount(user.email, password);

        // Since we are adding the user with the password this account has not been activated
        am.setUserData(account, Authenticator.USE_PASSWORD, String.valueOf(true));

        // Determine the userId for this user if we can
        if (user.registration != null) {
            am.setUserData(account, Authenticator.USER_ID, user.registration.userId);

            // Join ohmlet if applicable
            if (mJoinOhmletId != null)
                autoJoinOhmlet(user.registration.userId);
        }

        finishAccountAdd(user.email, null, password);
    }

    private Account addOrFindAccount(String email, String password) {
        Account[] accounts = am.getAccountsByType(AuthUtil.ACCOUNT_TYPE);
        Account account = accounts.length != 0 ? accounts[0] :
                new Account(email, AuthUtil.ACCOUNT_TYPE);

        if (accounts.length == 0) {
            am.addAccountExplicitly(account, password, null);

            // Turn on automatic syncing for this account
            ContentResolver.setSyncAutomatically(account, OhmageContract.CONTENT_AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, StreamContract.CONTENT_AUTHORITY, new Bundle(),
                    AuthUtil.SYNC_INTERVAL);

            ContentResolver.setSyncAutomatically(account, StreamContract.CONTENT_AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, StreamContract.CONTENT_AUTHORITY, new Bundle(),
                    AuthUtil.SYNC_INTERVAL);

            ContentResolver.setSyncAutomatically(account, ResponseContract.CONTENT_AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, ResponseContract.CONTENT_AUTHORITY,
                    new Bundle(), AuthUtil.SYNC_INTERVAL);

            ContentResolver.setSyncAutomatically(account, AppLogSyncAdapter.CONTENT_AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, AppLogSyncAdapter.CONTENT_AUTHORITY,
                    new Bundle(), AuthUtil.SYNC_INTERVAL);
        } else {
            am.setPassword(accounts[0], password);
        }
        return account;
    }

    private void finishAccountAdd(String accountName, String authToken, String password) {
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AuthUtil.ACCOUNT_TYPE);
        if (authToken != null)
            intent.putExtra(AccountManager.KEY_AUTHTOKEN, authToken);
        intent.putExtra(AccountManager.KEY_PASSWORD, password);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();

        if (!calledByAuthenticator())
            startActivity(new Intent(getBaseContext(), MainActivity.class));
    }

    private void autoJoinOhmlet(String userId) {
        Ohmlet o = new Ohmlet();
        o.name = "ohmlet";
        o.ohmletId = mJoinOhmletId;
        o.people = new Member.List();
        Member m = new Member();
        m.memberId = TextUtils.isEmpty(userId) ? "me" : userId;
        m.role = Role.MEMBER;
        m.code = getIntent().getStringExtra(OhmletFragment.EXTRA_OHMLET_INVITATION_ID);
        o.people.add(m);
        o.dirty = true;

        Observable.from(o).subscribeOn(Schedulers.io()).doOnNext(
                new ContentProviderSaver()).doOnError(new Action1<Throwable>() {
            @Override public void call(Throwable throwable) {
                throwable.printStackTrace();
            }
        }).subscribe();
    }

    /**
     * This function should be called on a retrofit error
     *
     * @param error
     */
    public void onRetrofitError(final RetrofitError error) {

        // If there is a network error, we should pop back to the last info window if there was one
        getSupportFragmentManager().popBackStack(TAG_INFO_WINDOW, 0);


        // If there is nothing on the back stack to pop we should immediately stop showing progress
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                    showProgress(false);
                }

                Response r = error.getResponse();

                if (error.isNetworkError()) {
                    Toast.makeText(getBaseContext(), R.string.network_error, Toast.LENGTH_SHORT)
                            .show();
                } else if (error.getCause() instanceof AuthenticationException) {
                    Toast.makeText(getBaseContext(), R.string.error_invalid_credentials,
                            Toast.LENGTH_SHORT).show();
                } else if (r != null && r.getBody() instanceof TypedByteArray) {
                    String body = new String(((TypedByteArray) r.getBody()).getBytes());
                    Toast.makeText(getBaseContext(), body, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getBaseContext(), R.string.unknown_error, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    private void hideKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    public static interface GoogleAccessTokenCallback {
        void onGoogleAccessTokenReceived(String token);
    }

    /**
     * This AsyncTask gets an access token from google
     */
    private class GoogleAccessTokenTask extends AsyncTask<Void, Void, String> {

        private final GoogleAccessTokenCallback mCallback;

        private final String mAccountName;

        public GoogleAccessTokenTask(String accountName, GoogleAccessTokenCallback callback) {
            mAccountName = accountName;
            mCallback = callback;
        }

        public void onPreExecute() {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    showProgress(true);
                }
            });
        }

        @Override
        protected String doInBackground(Void... ignore) {
            return getGoogleAccessTokenBlocking(mAccountName);
        }

        @Override
        protected void onPostExecute(String token) {
            super.onPostExecute(token);
            if (!TextUtils.isEmpty(token) && mCallback != null) {
                mCallback.onGoogleAccessTokenReceived(token);
            }
        }

        private String getGoogleAccessTokenBlocking(String accountName) {
            try {
                return auth.googleAuthGetAccessToken(accountName);
            } catch (GooglePlayServicesAvailabilityException playEx) {
                GooglePlayServicesErrorDialogFragment fragment =
                        new GooglePlayServicesErrorDialogFragment();
                fragment.setArguments(GooglePlayServicesErrorDialogFragment.createArguments(
                        playEx.getConnectionStatusCode(), GOOGLE_CODE_RESULT));
                showErrorDialog(getSupportFragmentManager(), fragment);
            } catch (UserRecoverableAuthException userAuthEx) {
                startActivityForResult(userAuthEx.getIntent(), GOOGLE_CODE_RESULT);
            } catch (IOException transientEx) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(getBaseContext(), getString(R.string.network_error),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (GoogleAuthException authEx) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(getBaseContext(), getString(R.string.account_error),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return null;
        }
    }

    /**
     * Shows the error dialog if there is one from {@link GoogleAuthUtil}
     *
     * @param errorDialog
     */
    public static void showErrorDialog(FragmentManager fragmentManager,
            DialogFragment errorDialog) {
        DialogFragment oldErrorDialog =
                (DialogFragment) fragmentManager.findFragmentByTag(TAG_ERROR_DIALOG);
        if (oldErrorDialog != null) {
            oldErrorDialog.dismiss();
        }

        errorDialog.show(fragmentManager, TAG_ERROR_DIALOG);
    }

    @Override
    protected List<Object> getModules() {
        return Arrays.<Object>asList(new PlusClientFragmentModule(this));
    }

}