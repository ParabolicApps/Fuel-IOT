package com.codesw.fuelcontroller.fragments;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.codesw.fuelcontroller.MainActivity;
import com.codesw.fuelcontroller.R;
import com.codesw.fuelcontroller.utils.FirebaseSyncManager;
import com.codesw.fuelcontroller.utils.SQLiteHandler;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * SettingsFragment provides the app preferences and Firebase account controls.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Preference firebaseHeaderPref;
    private Preference firebaseLoginPref;
    private Preference firebaseSyncPref;
    private Preference firebaseSignOutPref;

    private PreferenceCategory firebaseCategory;
    private PreferenceCategory appearanceCategory;
    private PreferenceCategory fuelCategory;
    private PreferenceCategory alertsCategory;
    private PreferenceCategory deviceCategory;
    private PreferenceCategory dataCategory;

    private FirebaseAuth firebaseAuth;
    private SQLiteHandler db;
    private String lastSyncedUid = "";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        firebaseAuth = FirebaseAuth.getInstance();
        db = new SQLiteHandler(requireContext());

        bindPreferences();
        updateFirebaseUi(firebaseAuth.getCurrentUser());
    }

    private void bindPreferences() {
        firebaseHeaderPref = findPreference("firebase_header");
        firebaseLoginPref = findPreference("firebase_login");
        firebaseSyncPref = findPreference("firebase_sync_now");
        firebaseSignOutPref = findPreference("firebase_sign_out");

        firebaseCategory = findPreference("firebase_category");
        appearanceCategory = findPreference("pref_category_appearance");
        fuelCategory = findPreference("pref_category_fuel");
        alertsCategory = findPreference("pref_category_alerts");
        deviceCategory = findPreference("pref_category_device");
        dataCategory = findPreference("pref_category_data");

        if (firebaseLoginPref != null) {
            firebaseLoginPref.setOnPreferenceClickListener(preference -> {
                showFirebaseAuthDialog();
                return true;
            });
        }

        if (firebaseSyncPref != null) {
            firebaseSyncPref.setOnPreferenceClickListener(preference -> {
                syncLocalDataToFirebase();
                return true;
            });
        }

        if (firebaseSignOutPref != null) {
            firebaseSignOutPref.setOnPreferenceClickListener(preference -> {
                showSignOutConfirmation();
                return true;
            });
        }

        Preference clearDbPref = findPreference("clear_db");
        if (clearDbPref != null) {
            clearDbPref.setOnPreferenceClickListener(preference -> {
                showClearDbConfirmation();
                return true;
            });
        }

        Preference clearCloudPref = findPreference("clear_cloud_db");
        if (clearCloudPref != null) {
            clearCloudPref.setOnPreferenceClickListener(preference -> {
                showClearCloudDbConfirmation();
                return true;
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (firebaseAuth != null && authStateListener != null) {
            firebaseAuth.addAuthStateListener(authStateListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (firebaseAuth != null && authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addBottomNavPadding();
        _changeActivityFont(view, "ubuntu_medium");
    }

    private void addBottomNavPadding() {
        RecyclerView preferenceList = getListView();
        preferenceList.setClipToPadding(false);
        preferenceList.post(() -> {
            int bottomNavHeight = 0;
            if (getActivity() != null) {
                View bottomNav = getActivity().findViewById(R.id.main_bnv);
                if (bottomNav != null) {
                    bottomNavHeight = bottomNav.getHeight();
                }
            }

            int extraSpace = (int) (24 * getResources().getDisplayMetrics().density);
            preferenceList.setPadding(
                    preferenceList.getPaddingLeft(),
                    preferenceList.getPaddingTop(),
                    preferenceList.getPaddingRight(),
                    bottomNavHeight + extraSpace
            );
        });
    }

    private final FirebaseAuth.AuthStateListener authStateListener = firebaseAuth -> {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        updateFirebaseUi(user);
        if (user != null && !user.getUid().equals(lastSyncedUid)) {
            syncLocalDataToFirebase(user);
            FirebaseSyncManager.setupRealtimePull(db, user.getUid());
            lastSyncedUid = user.getUid();
        }
        if (user == null) {
            FirebaseSyncManager.stopRealtimePull();
            lastSyncedUid = "";
        }
    };

    private void updateFirebaseUi(@Nullable FirebaseUser user) {
        boolean signedIn = user != null;
        String subtitle = signedIn
                ? "Signed in as " + safeText(user.getEmail())
                : "Sign in to sync local logs to Firebase. You can still manage preferences in Guest Mode.";

        if (firebaseHeaderPref != null) {
            firebaseHeaderPref.setTitle(signedIn ? "Welcome, " + resolveDisplayName(user) : "Guest Mode");
            firebaseHeaderPref.setSummary(subtitle);
            firebaseHeaderPref.setVisible(true);
        }

        setPreferenceGroupVisible(firebaseCategory, true);
        setPreferenceVisible(firebaseLoginPref, !signedIn);
        setPreferenceVisible(firebaseSyncPref, signedIn);
        setPreferenceVisible(firebaseSignOutPref, signedIn);

        // Guest mode allows all preferences, but they stay local unless signed in
        setPreferenceGroupVisible(appearanceCategory, true);
        setPreferenceGroupVisible(fuelCategory, true);
        setPreferenceGroupVisible(alertsCategory, true);
        setPreferenceGroupVisible(deviceCategory, true);
        setPreferenceGroupVisible(dataCategory, true);

        if (firebaseLoginPref != null) {
            firebaseLoginPref.setSummary(signedIn
                    ? "This account is already connected."
                    : "Sign in to enable cloud synchronization.");
        }
    }

    private void setPreferenceGroupVisible(@Nullable PreferenceCategory category, boolean visible) {
        if (category != null) {
            category.setVisible(visible);
        }
    }

    private void setPreferenceVisible(@Nullable Preference preference, boolean visible) {
        if (preference != null) {
            preference.setVisible(visible);
        }
    }

    private void showFirebaseAuthDialog() {
        if (getContext() == null) {
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_firebase_login, null);
        EditText nameField = dialogView.findViewById(R.id.firebase_name);
        EditText emailField = dialogView.findViewById(R.id.firebase_email);
        EditText passwordField = dialogView.findViewById(R.id.firebase_password);

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Firebase Account")
                .setMessage("Sign in to sync local fuel data, or create a Firebase account to start fresh.")
                .setView(dialogView)
                .setPositiveButton("Sign In", (dialog, which) -> attemptSignIn(emailField, passwordField))
                .setNeutralButton("Create", (dialog, which) -> attemptCreateAccount(nameField, emailField, passwordField))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void attemptSignIn(EditText emailField, EditText passwordField) {
        String email = safeText(emailField.getText());
        String password = safeText(passwordField.getText());

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            toast("Email and password are required.");
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    updateFirebaseUi(user);
                    toast("Signed in successfully.");
                })
                .addOnFailureListener(e -> toast(e.getMessage()));
    }

    private void attemptCreateAccount(EditText nameField, EditText emailField, EditText passwordField) {
        String name = safeText(nameField.getText());
        String email = safeText(emailField.getText());
        String password = safeText(passwordField.getText());

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            toast("Name, email, and password are required.");
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        toast("Account created, but login failed.");
                        return;
                    }

                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();

                    user.updateProfile(profileUpdates)
                            .addOnSuccessListener(unused -> {
                                updateFirebaseUi(user);
                                toast("Account created successfully.");
                            })
                            .addOnFailureListener(e -> {
                                updateFirebaseUi(user);
                                toast("Account created, but profile update failed.");
                            });
                })
                .addOnFailureListener(e -> toast(e.getMessage()));
    }

    private void showSignOutConfirmation() {
        if (getContext() == null) {
            return;
        }

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Sign out?")
                .setMessage("This will disconnect the current Firebase account from this screen.")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    FirebaseSyncManager.stopRealtimePull();
                    firebaseAuth.signOut();
                    lastSyncedUid = "";
                    updateFirebaseUi(null);
                    toast("Signed out.");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void syncLocalDataToFirebase() {
        syncLocalDataToFirebase(firebaseAuth != null ? firebaseAuth.getCurrentUser() : null);
    }

    private void syncLocalDataToFirebase(@Nullable FirebaseUser user) {
        if (user == null) {
            toast("Sign in first.");
            return;
        }

        new Thread(() -> {
            FirebaseSyncManager.syncAll(db);
            toast("Local data synced to Firebase.");
        }).start();
    }

    private void showClearCloudDbConfirmation() {
        if (getContext() == null) return;
        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Clear Cloud Data?")
                .setMessage("This will permanently delete all logs and totals from Firebase. Your account information will remain intact.")
                .setPositiveButton("Clear Cloud", (dialog, which) -> {
                    FirebaseSyncManager.clearUserData((success, message) -> {
                        toast(message);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearDbConfirmation() {
        if (getContext() == null) return;
        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Clear Database?")
                .setMessage("This will permanently delete all fuel logs and statistics. This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    SQLiteHandler database = new SQLiteHandler(getContext());
                    database.deleteLogs();
                    Toast.makeText(getContext(), "Database cleared successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String resolveDisplayName(@NonNull FirebaseUser user) {
        String displayName = user.getDisplayName();
        if (!TextUtils.isEmpty(displayName)) {
            return displayName;
        }

        String email = user.getEmail();
        if (!TextUtils.isEmpty(email) && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }

        return "User";
    }

    private String safeText(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void toast(String message) {
        if (getContext() != null && !TextUtils.isEmpty(message)) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void _changeActivityFont(View view, String fontName) {
        String fontPath = "fonts/".concat(fontName.concat(".ttf"));
        overrideFonts(view, fontPath);
    }

    private void overrideFonts(final View v, String fontPath) {
        try {
            if (getContext() == null) return;
            Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), fontPath);
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    overrideFonts(child, fontPath);
                }
            } else if (v instanceof TextView) {
                ((TextView) v).setTypeface(typeface);
            }
        } catch(Exception e) {
            // Fail silently to maintain default UI if assets are missing
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getPreferenceScreen() != null && getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }
        updateFirebaseUi(firebaseAuth != null ? firebaseAuth.getCurrentUser() : null);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getPreferenceScreen() != null && getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("dark_mode".equals(key)) {
            boolean darkMode = sharedPreferences.getBoolean(key, true);
            if (darkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                activity.findViewById(R.id.home).performClick();
            }
        } else if ("real_mode".equals(key)) {
            boolean isRealMode = sharedPreferences.getBoolean(key, false);
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                activity.handleModeSwitch(isRealMode);
            }
        }
    }
}
