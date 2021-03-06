package com.antoniocappiello.cloudapp.service.backend;

import android.app.ProgressDialog;
import android.support.v7.widget.RecyclerView;

import com.antoniocappiello.cloudapp.service.action.Action;
import com.antoniocappiello.cloudapp.ui.screen.itemlist.ItemViewHolder;
import com.antoniocappiello.socialauth.model.Account;
import com.antoniocappiello.socialauth.provider.AuthProviderType;

import java.util.List;
import java.util.Map;

import rx.Observable;

public interface BackendAdapter<T> extends com.antoniocappiello.socialauth.BackendAdapter{

    void addItemToUserList(T item);

    /**
     Example of approach #2, in http://antoniocappiello.com/2016/02/09/getting-started-with-firebase-while-moving-away-from-parse/
     */
    Observable<List<T>> readItems();

    RecyclerView.Adapter<ItemViewHolder> getRecyclerViewAdapterForUserItemList();

    void cleanup();

    void addAuthStateListener(Action onAuthenticated, Action onUnAuthenticated);

    void removeAuthStateListener();

    /**
    Example of approach #3, in http://antoniocappiello.com/2016/02/09/getting-started-with-firebase-while-moving-away-from-parse/
     */
    void signIn(String email, String password, ProgressDialog signInProgressDialog, Action onAuthSucceeded, Action onAuthFailed);

    void logOut();

    void createUser(Account account, ProgressDialog signUpProgressDialog, Action onSignInFailed, Action onSignUpSucceeded);

    String getCurrentUserEmail();

    void updateItemInUserList(String itemId, T item);

}
