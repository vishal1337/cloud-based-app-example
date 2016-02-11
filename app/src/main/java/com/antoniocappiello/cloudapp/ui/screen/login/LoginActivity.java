package com.antoniocappiello.cloudapp.ui.screen.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.antoniocappiello.cloudapp.App;
import com.antoniocappiello.cloudapp.BuildConfig;
import com.antoniocappiello.cloudapp.R;
import com.antoniocappiello.cloudapp.service.action.Action;
import com.antoniocappiello.cloudapp.service.action.OnSignInFailed;
import com.antoniocappiello.cloudapp.service.action.OnSignInSucceeded;
import com.antoniocappiello.cloudapp.service.backend.BackendAdapter;
import com.antoniocappiello.cloudapp.ui.customwidget.ProgressDialogFactory;
import com.antoniocappiello.cloudapp.ui.screen.BaseActivity;
import com.orhanobut.logger.Logger;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class LoginActivity extends BaseActivity {

    @Bind(R.id.edit_text_email)
    EditText mEditTextEmailInput;

    @Bind(R.id.edit_text_password)
    EditText mEditTextPasswordInput;

    @Inject
    BackendAdapter mBackendAdapter;

    private Action mOnSignInSucceeded;
    private Action mOnSignInFailed;
    private ProgressDialog mAuthProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        ((App) getApplication()).appComponent().inject(this);
        initPasswordInputListener();
        mAuthProgressDialog = ProgressDialogFactory.getSignInProgressDialog(this);
        mOnSignInSucceeded = new OnSignInSucceeded(this);
        mOnSignInFailed = new OnSignInFailed(this);
    }

    private void initPasswordInputListener() {
        mEditTextPasswordInput.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                signInWithEmailAndPassword();
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        showEmailIfAlreadyEntered();
        mBackendAdapter.addAuthStateListener(mOnSignInSucceeded, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        mBackendAdapter.removeAuthStateListener();
    }

    private void showEmailIfAlreadyEntered() {
        String userEmail = mBackendAdapter.getCurrentUserEmail();
        if (userEmail != null && !userEmail.isEmpty()) {
            mEditTextEmailInput.setText(userEmail);
        }
    }

    @OnClick(R.id.button_sign_in_with_email_and_password)
    public void signIn() {
        signInWithEmailAndPassword();
    }

    @OnClick(R.id.text_view_sign_up)
    public void signUp() {
        startActivity(new Intent(LoginActivity.this, CreateAccountActivity.class));
    }

    public void signInWithEmailAndPassword() {
        String email, password;
        if(BuildConfig.FLAVOR.equalsIgnoreCase("dev")) {
            email = BuildConfig.FIREBASE_TEST_EMAIL;
            password = BuildConfig.FIREBASE_TEST_PW;
            Logger.e(email + "\n" + password);
        }
        else {
            email = mEditTextEmailInput.getText().toString();
            password = mEditTextPasswordInput.getText().toString();
        }
        if(isEmailValid(email) && isPasswordValid(password)) {
            mBackendAdapter.signIn(email, password, mAuthProgressDialog, mOnSignInSucceeded, mOnSignInFailed);
        }
    }

    private boolean isEmailValid(String email) {
        if (email.equals("")) {
            mEditTextEmailInput.setError(getString(R.string.error_cannot_be_empty));
            return false;
        }
        return true;
    }

    private boolean isPasswordValid(String password) {
        if (password.equals("")) {
            mEditTextPasswordInput.setError(getString(R.string.error_cannot_be_empty));
            return false;
        }
        return true;
    }
}