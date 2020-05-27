package com.osvaldovillalobosperez.redessocialesp87b;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;

public class MainActivity extends FragmentActivity {

    private final String PENDING_ACTION_KEY =
            "com.osvaldovillalobosperez.redessocialesp87b:PendingAction";

    private boolean canDisplayShareDialog;

    private Button postStatusUpdate;
    private CallbackManager callbackManager;
    private PendingAction pendingAction = PendingAction.NONE;

    //Declare a private ShareDialog variable//
    private ShareDialog shareDialog;

    //The result of the “share” action//
    private FacebookCallback<Sharer.Result> shareCallback = new FacebookCallback<Sharer.Result>() {
        //The user cancelled the share//
        @Override
        public void onCancel() {
            //To do//
        }

        //An error occurred//
        @Override
        public void onError(FacebookException error) {
            //To do//
        }

        //The content was shared successfully//
        @Override
        public void onSuccess(Sharer.Result result) {
            //To do//
        }
    };

    private enum PendingAction {
        NONE,
        POST_STATUS
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Initialize an instance of CallbackManager//
        callbackManager = CallbackManager.Factory.create();

        //Register a callback to respond to the user//
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {

                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        handlePendingAction();
                        updateUI();
                    }

                    @Override
                    public void onCancel() {
                        if (pendingAction != PendingAction.NONE) {
                            showAlert();
                            pendingAction = PendingAction.NONE;
                        }
                        updateUI();
                    }

                    //Handle exception//
                    @Override
                    public void onError(FacebookException exception) {
                        if (pendingAction != PendingAction.NONE
                                && exception instanceof FacebookAuthorizationException) {
                            showAlert();
                            pendingAction = PendingAction.NONE;
                        }
                        updateUI();
                    }

                    //Display an error message//
                    private void showAlert() {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.cancelled)
                                .setMessage(R.string.FBexception)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    }
                });

        //Create the ShareDialog//
        shareDialog = new ShareDialog(this);

        //Callback registration//
        shareDialog.registerCallback(
                callbackManager,
                shareCallback
        );

        if (savedInstanceState != null) {
            String name = savedInstanceState.getString(PENDING_ACTION_KEY);
            pendingAction = PendingAction.valueOf(name);
        }

        setContentView(R.layout.activity_main);

        postStatusUpdate = (Button) findViewById(R.id.postStatusUpdate);

        //Listen for the user tapping the postStatusUpdate button//
        postStatusUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPostStatus();
            }
        });

        //Share link callback//
        canDisplayShareDialog = ShareDialog.canShow(
                ShareLinkContent.class
        );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(PENDING_ACTION_KEY, pendingAction.name());
    }

    //Override the onActivityResult method//
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Pass the login result to the CallbackManager//
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void updateUI() {
        boolean enableButtons = AccessToken.isCurrentAccessTokenActive();
        postStatusUpdate.setEnabled(enableButtons || canDisplayShareDialog);
    }

    private void handlePendingAction() {
        PendingAction previouslyPendingAction = pendingAction;
        pendingAction = PendingAction.NONE;

        switch (previouslyPendingAction) {
            case NONE:
                break;
            case POST_STATUS:
                postStatusUpdate();
                break;
        }
    }

    //Check for publish permissions//
    private boolean hasPublishActionPermission() {
        //Load AccessToken.getCurrentAccessToken//

        return AccessToken.isCurrentAccessTokenActive() &&
                AccessToken.getCurrentAccessToken().getPermissions().contains("publish_actions");
    }

    private void publish(PendingAction action, boolean allowNoToken) {
        if (AccessToken.isCurrentAccessTokenActive() || allowNoToken) {
            pendingAction = action;
            handlePendingAction();
        }
    }

    private void onClickPostStatus() {
        publish(PendingAction.POST_STATUS, canDisplayShareDialog);
    }

    private void postStatusUpdate() {
        Profile profile = Profile.getCurrentProfile();

        //Build an instance of our link//
        //Describe the link’s content//
        //Build the link//
        ShareLinkContent linkContent = new ShareLinkContent.Builder()
                .setContentUrl(Uri.parse("https://www.androidauthority.com/"))
                .build();

        //Display the ShareDialog//
        if (canDisplayShareDialog) {
            shareDialog.show(linkContent);
        } else if (profile != null && hasPublishActionPermission()) {
            ShareApi.share(linkContent, shareCallback);
        } else {
            pendingAction = PendingAction.POST_STATUS;
        }
    }
}
