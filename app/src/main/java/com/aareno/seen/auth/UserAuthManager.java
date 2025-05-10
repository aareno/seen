package com.aareno.seen.auth;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.aareno.seen.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class UserAuthManager {
    private static final String TAG = "UserAuthManager";
    private static UserAuthManager instance;

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private Context context;
    
    public interface AuthListener {
        void onAuthSuccess(FirebaseUser user);
        void onAuthFailure(Exception e);
    }

    private UserAuthManager(Context context) {
        this.context = context.getApplicationContext();
        
        try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
                Log.d(TAG, "Firebase initialized in UserAuthManager");
            }
            
            // Check Google Play Services availability before proceeding
            com.google.android.gms.common.GoogleApiAvailability googleApi = 
                    com.google.android.gms.common.GoogleApiAvailability.getInstance();
            int resultCode = googleApi.isGooglePlayServicesAvailable(context);
            
            if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                Log.e(TAG, "Google Play Services unavailable, code: " + resultCode);
                throw new Exception("Google Play Services unavailable: " + resultCode);
            }
            
            firebaseAuth = FirebaseAuth.getInstance();
            
            // Fix: Use a different approach for client ID
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    // First try to use the web client ID from strings.xml
                    .requestIdToken("650240569128-v088v12d38q0fhioi70k30v1e8gsqlb0.apps.googleusercontent.com")
                    .requestEmail()
                    .build();
            
            googleSignInClient = GoogleSignIn.getClient(context, gso);
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase or GoogleSignIn", e);
        }
    }

    public static synchronized UserAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserAuthManager(context);
        }
        return instance;
    }

    public boolean isUserSignedIn() {
        return firebaseAuth != null && firebaseAuth.getCurrentUser() != null;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth != null ? firebaseAuth.getCurrentUser() : null;
    }

    public String getUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public Intent getSignInIntent() {
        if (googleSignInClient != null) {
            return googleSignInClient.getSignInIntent();
        }
        return new Intent(); // Return empty intent if not initialized
    }

    public void handleSignInResult(Intent data, AuthListener listener) {
        if (firebaseAuth == null || googleSignInClient == null) {
            Log.e(TAG, "Cannot handle sign-in: firebaseAuth or googleSignInClient is null");
            if (listener != null) {
                listener.onAuthFailure(new Exception("Authentication is not initialized"));
            }
            return;
        }
        
        if (data == null) {
            Log.e(TAG, "Sign-in intent data is null");
            if (listener != null) {
                listener.onAuthFailure(new Exception("Sign-in data is missing"));
            }
            return;
        }
        
        try {
            Log.d(TAG, "Starting to process sign-in result...");
            
            // Add more context information for debugging
            com.google.android.gms.common.GoogleApiAvailability googleApi = 
                    com.google.android.gms.common.GoogleApiAvailability.getInstance();
            int resultCode = googleApi.isGooglePlayServicesAvailable(context);
            Log.d(TAG, "Google Play Services status while handling sign-in: " + resultCode);
            
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            
            if (task == null) {
                Log.e(TAG, "GoogleSignIn task is null");
                if (listener != null) {
                    listener.onAuthFailure(new Exception("Google Sign-In task is null"));
                }
                return;
            }
            
            if (task.isComplete()) {
                Log.d(TAG, "Google sign-in task is complete. Success: " + task.isSuccessful());
                if (!task.isSuccessful()) {
                    Exception exception = task.getException();
                    Log.e(TAG, "Google sign-in task failed with exception: " + 
                          (exception != null ? exception.getMessage() : "Unknown error"));
                    if (listener != null) {
                        listener.onAuthFailure(exception != null ? exception : 
                                new Exception("Google Sign-In failed with unknown error"));
                    }
                    return;
                }
            } else {
                Log.d(TAG, "Google sign-in task is not complete yet. Adding completion listener.");
                task.addOnCompleteListener(completeTask -> {
                    Log.d(TAG, "Google sign-in task completed. Success: " + completeTask.isSuccessful());
                    if (!completeTask.isSuccessful()) {
                        Exception exception = completeTask.getException();
                        Log.e(TAG, "Google sign-in completion failed with exception: " + 
                              (exception != null ? exception.getMessage() : "Unknown error"));
                        if (listener != null) {
                            listener.onAuthFailure(exception != null ? exception : 
                                    new Exception("Google Sign-In completion failed"));
                        }
                    }
                });
            }
            
            try {
                Log.d(TAG, "Attempting to get GoogleSignInAccount from task");
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account == null) {
                    Log.e(TAG, "GoogleSignInAccount is null");
                    if (listener != null) {
                        listener.onAuthFailure(new Exception("Google Sign-In account is null"));
                    }
                    return;
                }
                
                Log.d(TAG, "Got Google account: " + account.getEmail() + 
                           ", ID: " + account.getId());
                
                String idToken = account.getIdToken();
                Log.d(TAG, "ID token: " + (idToken != null ? "obtained (length: " + idToken.length() + ")" : "null"));
                
                if (idToken == null) {
                    Log.e(TAG, "ID token is null, attempting alternative approach");
                    // If ID token is null but we have a valid account, try a fallback
                    // This can happen due to misc. configuration issues
                    firebaseAuthWithEmail(account.getEmail(), listener);
                    return;
                }
                
                // Continue with normal flow using ID token
                firebaseAuthWithGoogle(idToken, listener);
            } catch (ApiException e) {
                // Improved error handling for status code 10
                if (e.getStatusCode() == 10) {
                    Log.e(TAG, "DEVELOPER_ERROR (10): This usually means your SHA-1 fingerprint in " +
                            "Firebase console doesn't match the one used to sign your app.", e);
                    
                    // Extra logging instructions for this specific error
                    Log.e(TAG, "If testing on emulator, make sure you're using a Google Play image. " +
                            "If on real device, verify SHA-1 in Firebase console matches your debug keystore.");
                    
                    if (listener != null) {
                        String instructions = "Authentication error: Developer configuration issue. " +
                                "Please check app logs and contact the developer.";
                        listener.onAuthFailure(new Exception(instructions, e));
                    }
                    return;
                }
                
                // Provide a more specific error message based on status code
                String errorMsg = "Google Sign-In failed: ";
                switch (e.getStatusCode()) {
                    case 12500: // SIGN_IN_CANCELLED
                        errorMsg += "Sign-in was cancelled by user";
                        break;
                    case 12501: // SIGN_IN_CURRENTLY_IN_PROGRESS
                        errorMsg += "Sign-in already in progress";
                        break;
                    case 12502: // SIGN_IN_FAILED
                        errorMsg += "Sign-in failed";
                        break;
                    default:
                        errorMsg += "Error code " + e.getStatusCode();
                }
                
                Log.e(TAG, errorMsg, e);
                if (listener != null) {
                    listener.onAuthFailure(new Exception(errorMsg, e));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error handling sign-in result", e);
            if (listener != null) {
                listener.onAuthFailure(e);
            }
        }
    }

    // Fallback authentication method if token-based auth fails
    private void firebaseAuthWithEmail(String email, AuthListener listener) {
        Log.d(TAG, "Attempting fallback authentication with email: " + email);
        
        // This is just a demonstration - in a real app, you would need to implement
        // a proper fallback authentication method or guide the user to try again
        
        if (listener != null) {
            listener.onAuthFailure(new Exception("ID token missing. Please verify your Firebase configuration."));
        }
    }

    private void firebaseAuthWithGoogle(String idToken, AuthListener listener) {
        if (firebaseAuth == null) {
            if (listener != null) {
                listener.onAuthFailure(new Exception("Firebase Auth is not initialized"));
            }
            return;
        }
        
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (listener != null) {
                            listener.onAuthSuccess(user);
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        if (listener != null) {
                            listener.onAuthFailure(task.getException());
                        }
                    }
                });
    }

    public void signOut(OnCompleteListener<Void> listener) {
        if (firebaseAuth == null || googleSignInClient == null) {
            if (listener != null) {
                // Create a TaskCompletionSource to return a failed task
                com.google.android.gms.tasks.TaskCompletionSource<Void> taskCompletionSource = 
                    new com.google.android.gms.tasks.TaskCompletionSource<>();
                taskCompletionSource.setException(new Exception("Auth not initialized"));
                listener.onComplete(taskCompletionSource.getTask());
            }
            return;
        }
        
        // Sign out from Firebase
        firebaseAuth.signOut();
        
        // Sign out from Google
        googleSignInClient.signOut().addOnCompleteListener(listener);
    }
}
