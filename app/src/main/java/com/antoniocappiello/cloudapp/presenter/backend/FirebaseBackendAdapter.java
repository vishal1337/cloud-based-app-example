package com.antoniocappiello.cloudapp.presenter.backend;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.v7.widget.RecyclerView;

import com.antoniocappiello.cloudapp.BuildConfig;
import com.antoniocappiello.cloudapp.R;
import com.antoniocappiello.cloudapp.Utils;
import com.antoniocappiello.cloudapp.model.Account;
import com.antoniocappiello.cloudapp.model.Item;
import com.antoniocappiello.cloudapp.model.User;
import com.antoniocappiello.cloudapp.presenter.command.Command;
import com.antoniocappiello.cloudapp.presenter.command.OnAuthFailed;
import com.antoniocappiello.cloudapp.presenter.command.OnSignUpSucceeded;
import com.antoniocappiello.cloudapp.Constants;
import com.antoniocappiello.cloudapp.view.list.ItemViewHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.GenericTypeIndicator;
import com.firebase.client.ServerValue;
import com.firebase.ui.FirebaseRecyclerAdapter;
import com.orhanobut.logger.Logger;
import com.pixplicity.easyprefs.library.Prefs;
import com.soikonomakis.rxfirebase.RxFirebase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Func1;

public class FirebaseBackendAdapter implements BackendAdapter<Item> {

    private static final String LIST = "list";
    public static final String FIREBASE_PROPERTY_TIMESTAMP = "timestamp";
    private static final String UID_MAPPING = "uid_mapping";
    private static final String USERS = "users";
    public static final String UID_MAPPINGS = "uidMappings";

    private Firebase refRoot;
    private Firebase refList;
    private Firebase refUidMapping;

    private FirebaseRecyclerAdapter<Item, ItemViewHolder> mAdapter;
    private Firebase.AuthStateListener mAuthStateListener;

    public FirebaseBackendAdapter(Context context) {
        Firebase.setAndroidContext(context);
        refRoot = new Firebase(BuildConfig.FIREBASE_ROOT_URL);
        refList = refRoot.child(LIST);
        refUidMapping = refRoot.child(UID_MAPPING);
    }

    @Override
    public void add(Item item) {
        refList.push().setValue(item);
        Logger.d(item.toString());
    }

    @Override
    public Observable<List<Item>> readItems() {
        return RxFirebase.getInstance()
                .observeValueEvent(refList)
                .map((Func1<DataSnapshot, List<Item>>) snapshot -> {
                    Map<String, Item> map = snapshot.getValue(new GenericTypeIndicator<Map<String, Item>>() {});
                    return new ArrayList<>(map.values());
                });
    }

    @Override
    public RecyclerView.Adapter<ItemViewHolder> getItemRecyclerViewAdapter() {
        if(mAdapter == null){
            mAdapter = new FirebaseRecyclerAdapter<Item, ItemViewHolder>(Item.class, R.layout.item, ItemViewHolder.class, refList) {
                @Override
                public void populateViewHolder(ItemViewHolder itemViewHolder, Item item, int position) {
                    itemViewHolder.getNameTextView().setText(item.getItemName());
                    itemViewHolder.getTimestampTextView().setText(item.getTimestamp());
                }
            };
        }
        return mAdapter;
    }

    @Override
    public void cleanup() {
        mAdapter.cleanup();
    }

    @Override
    public void addAuthStateListener(ProgressDialog authProgressDialog, Command onAuthSucceeded) {
        mAuthStateListener = authData -> {
            authProgressDialog.dismiss();
            if (authData != null) {
                onAuthSucceeded.execute();
            }
        };
        authProgressDialog.show();
        refRoot.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void removeAuthStateListener() {
        refRoot.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    public void signIn(String email, String password, ProgressDialog authProgressDialog, Command onAuthSucceeded, Command onAuthFailed) {
        Firebase.AuthResultHandler authenticationResultHandler = new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                authProgressDialog.dismiss();
                onAuthSucceeded.execute();
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                authProgressDialog.dismiss();
                onAuthFailed.execute(firebaseError.getMessage());
            }
        };
        authProgressDialog.show();
        refRoot.authWithPassword(email, password, authenticationResultHandler);
    }

    @Override
    public void createUser(Account account, ProgressDialog signUpProgressDialog, OnAuthFailed onAuthFailed, OnSignUpSucceeded onSignUpSucceeded) {
        signUpProgressDialog.show();
        refRoot.createUser(account.getUserEmail(), account.getPassword(), new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(final Map<String, Object> result) {
                sendConfirmationEmailWithNewPassword(result, account, signUpProgressDialog, onSignUpSucceeded);
            }

            @Override
            public void onError(FirebaseError firebaseError) {
                Logger.e(firebaseError.toString());
                signUpProgressDialog.dismiss();
                onAuthFailed.execute(firebaseError.getMessage());

            }
        });
    }

    private void sendConfirmationEmailWithNewPassword(Map<String, Object> result, Account account, ProgressDialog signUpProgressDialog, Command onSignUpSucceeded) {
        refRoot.resetPassword(account.getUserEmail(), new Firebase.ResultHandler() {
            @Override
            public void onSuccess() {
                Logger.d("Password reset ok");
                refRoot.authWithPassword(account.getUserEmail(), account.getPassword(), new Firebase.AuthResultHandler() {
                    @Override
                    public void onAuthenticated(AuthData authData) {
                        Logger.d("onAuthenticated");
                        signUpProgressDialog.dismiss();
                        Prefs.putString(Constants.KEY_SIGNUP_EMAIL, account.getUserEmail());
                        addUidAndUserMapping((String) result.get("uid"), account);
                        onSignUpSucceeded.execute();
                    }

                    @Override
                    public void onAuthenticationError(FirebaseError firebaseError) {
                        Logger.e(firebaseError.getMessage());
                    }
                });

            }

            @Override
            public void onError(FirebaseError firebaseError) {
                Logger.e(firebaseError.toString());
                signUpProgressDialog.dismiss();
            }
        });
    }

    private void addUidAndUserMapping(final String authUserId, Account account) {
        final String encodedEmail = Utils.encodeEmail(account.getUserEmail());

        HashMap<String, Object> uidAndUserMapping = createUidAndUserMap(encodedEmail, authUserId, account);

        // Try to update the database; if there is already a user, this will fail
        refRoot.updateChildren(uidAndUserMapping, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                Logger.d("creating user");
                if (firebaseError != null) {
                    // Try just making a uid mapping
                    refUidMapping.child(authUserId).setValue(encodedEmail);
                }
                signOut();
            }
        });

    }

    private HashMap<String, Object> createUidAndUserMap(String encodedEmail, String authUserId, Account account) {
        HashMap<String, Object> uidAndUserMapping = new HashMap<String, Object>();
        uidAndUserMapping.put("/" + USERS + "/" + encodedEmail, createUserMap(account));
        uidAndUserMapping.put("/" + UID_MAPPINGS + "/" + authUserId, encodedEmail);
        return uidAndUserMapping;
    }

    private HashMap<String, Object> createUserMap(Account account) {
        final String encodedEmail = Utils.encodeEmail(account.getUserEmail());
        User newUser = new User(account.getUserName(), encodedEmail, createTimestampJoinedMap());
        HashMap<String, Object> newUserMap = (HashMap<String, Object>) new ObjectMapper().convertValue(newUser, Map.class);
        return newUserMap;
    }

    private HashMap<String, Object> createTimestampJoinedMap() {
        HashMap<String, Object> timestampJoined = new HashMap<>();
        timestampJoined.put(FirebaseBackendAdapter.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);
        return timestampJoined;
    }

    @Override
    public void signOut() {
        refRoot.unauth();
    }


}

