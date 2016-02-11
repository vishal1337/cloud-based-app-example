package com.antoniocappiello.cloudapp.service.backend;

import android.app.ProgressDialog;
import android.support.v7.widget.RecyclerView;

import com.antoniocappiello.cloudapp.model.Account;
import com.antoniocappiello.cloudapp.service.action.Action;
import com.antoniocappiello.cloudapp.ui.screen.itemlist.ItemViewHolder;

import java.util.List;

import rx.Observable;

public interface BackendAdapter<T> {

    void addItemToUserList(T item);

    Observable<List<T>> readItems();

    RecyclerView.Adapter<ItemViewHolder> getRecyclerViewAdapterForUserItemList();

    void cleanup();

    void addAuthStateListener(Action onAuthenticated, Action onUnAuthenticated);

    void removeAuthStateListener();

    void signIn(String email, String password, ProgressDialog signInProgressDialog, Action onAuthSucceeded, Action onAuthFailed);

    void logOut();

    void createUser(Account account, ProgressDialog signUpProgressDialog, Action onSignInFailed, Action onSignUpSucceeded);

    String getCurrentUserEmail();

    void updateItemInUserList(String itemId, T item);
}
