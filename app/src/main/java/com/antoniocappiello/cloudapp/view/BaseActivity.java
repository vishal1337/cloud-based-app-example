/*
 * Created by Antonio Cappiello on 2/8/16 8:19 PM
 * Copyright (c) 2016. All rights reserved.
 *
 * Last modified 2/8/16 8:19 PM
 */

package com.antoniocappiello.cloudapp.view;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.antoniocappiello.cloudapp.App;
import com.antoniocappiello.cloudapp.R;
import com.antoniocappiello.cloudapp.presenter.backend.BackendAdapter;
import com.antoniocappiello.cloudapp.presenter.command.Command;
import com.antoniocappiello.cloudapp.presenter.command.OnSignInFailed;
import com.antoniocappiello.cloudapp.presenter.command.OnSignInSucceeded;
import com.antoniocappiello.cloudapp.presenter.command.OnLoggedOut;
import com.antoniocappiello.cloudapp.view.login.CreateAccountActivity;
import com.antoniocappiello.cloudapp.view.login.LoginActivity;
import com.orhanobut.logger.Logger;

import javax.inject.Inject;

public class BaseActivity extends AppCompatActivity {

    @Inject
    BackendAdapter mBackendAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((App) getApplication()).appComponent().inject(this);

        if (!((this instanceof LoginActivity) || (this instanceof CreateAccountActivity))) {
            mBackendAdapter.addAuthStateListener(null, new OnLoggedOut(this));
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!((this instanceof LoginActivity) || (this instanceof CreateAccountActivity))) {
            mBackendAdapter.removeAuthStateListener();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_settings:
                return true;
            case R.id.action_log_out:
                logOut();
        }
        return super.onOptionsItemSelected(item);
    }

    public void logOut() {
        Logger.d("Logging out");
        mBackendAdapter.logOut();
    }

}