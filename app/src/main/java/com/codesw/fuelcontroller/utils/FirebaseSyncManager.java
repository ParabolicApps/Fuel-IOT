package com.codesw.fuelcontroller.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.codesw.fuelcontroller.model.DbModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class FirebaseSyncManager {

    private static final String TAG = "FirebaseSyncManager";
    private static final String ROOT_NODE = "fuel_guard";
    private static ValueEventListener activeListener;

    private FirebaseSyncManager() {
    }

    public static boolean isSignedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    public static void syncLog(String recordKey, String time, String date, String data, String type) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference(ROOT_NODE)
                .child(user.getUid());

        Map<String, Object> payload = new HashMap<>();
        payload.put("sr", recordKey);
        payload.put("time", time);
        payload.put("date", date);
        payload.put("data", data);
        payload.put("type", type);

        userRef.child("logs").child(safeKey(recordKey)).setValue(payload)
                .addOnFailureListener(e -> Log.e(TAG, "syncLog failed", e));
    }

    public static void syncTotal(String date, String type, double total) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference(ROOT_NODE)
                .child(user.getUid());

        Map<String, Object> payload = new HashMap<>();
        payload.put("date", date);
        payload.put("data", total);
        payload.put("type", type);

        String recordKey = safeKey(String.format(Locale.getDefault(), "%s_%s", date, type));
        userRef.child("totals").child(recordKey).setValue(payload)
                .addOnFailureListener(e -> Log.e(TAG, "syncTotal failed", e));
    }

    public static void syncAll(SQLiteHandler db) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || db == null) {
            return;
        }

        // Push local to cloud
        ArrayList<DbModel> logs = db.getLogs();
        for (DbModel log : logs) {
            String recordKey = log.sr != null ? log.sr : buildFallbackKey(log);
            syncLog(recordKey, log.time, log.date, log.data, log.type);
        }

        ArrayList<DbModel> totals = db.getTotals();
        for (DbModel total : totals) {
            String recordKey = total.sr != null ? total.sr : buildFallbackKey(total);
            syncTotal(total.date, total.type, parseDoubleSafe(total.data));
            Log.d(TAG, "Synced total row " + recordKey);
        }

        // Setup listener for cloud to local
        setupRealtimePull(db, user.getUid());
    }

    public static void setupRealtimePull(SQLiteHandler db, String uid) {
        if (activeListener != null) {
            FirebaseDatabase.getInstance().getReference(ROOT_NODE).child(uid).removeEventListener(activeListener);
        }

        activeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                new Thread(() -> {
                    // Pull Logs
                    DataSnapshot logsSnap = snapshot.child("logs");
                    for (DataSnapshot logSnap : logsSnap.getChildren()) {
                        String sr = logSnap.child("sr").getValue(String.class);
                        String date = logSnap.child("date").getValue(String.class);
                        String time = logSnap.child("time").getValue(String.class);
                        String data = logSnap.child("data").getValue(String.class);
                        String type = logSnap.child("type").getValue(String.class);

                        if (sr != null && !db.isRowExist("log", "sr", sr)) {
                            db.importLog(sr, time, date, data, type);
                            Log.d(TAG, "Imported log from Firebase: " + sr);
                        }
                    }

                    // Pull Totals
                    DataSnapshot totalsSnap = snapshot.child("totals");
                    for (DataSnapshot totalSnap : totalsSnap.getChildren()) {
                        String date = totalSnap.child("date").getValue(String.class);
                        String type = totalSnap.child("type").getValue(String.class);
                        Object dataObj = totalSnap.child("data").getValue();
                        String data = dataObj != null ? String.valueOf(dataObj) : "0";

                        // For totals, we use date+type as unique identifier check
                        if (!db.isTotalRowExist(date, type)) {
                            db.importTotal(date, data, type);
                            Log.d(TAG, "Imported total from Firebase: " + date + "_" + type);
                        }
                    }
                }).start();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Realtime pull cancelled", error.toException());
            }
        };

        FirebaseDatabase.getInstance().getReference(ROOT_NODE).child(uid).addValueEventListener(activeListener);
    }

    public static void stopRealtimePull() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && activeListener != null) {
            FirebaseDatabase.getInstance().getReference(ROOT_NODE).child(user.getUid()).removeEventListener(activeListener);
            activeListener = null;
        }
    }

    public static void clearUserData(@NonNull OnSyncCompleteListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            listener.onComplete(false, "User not signed in.");
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference(ROOT_NODE)
                .child(user.getUid());

        // Remove only logs and totals, keeping any other profile/config nodes if they exist
        Map<String, Object> updates = new HashMap<>();
        updates.put("logs", null);
        updates.put("totals", null);

        userRef.updateChildren(updates)
                .addOnSuccessListener(unused -> listener.onComplete(true, "Cloud data cleared successfully."))
                .addOnFailureListener(e -> listener.onComplete(false, e.getMessage()));
    }

    public interface OnSyncCompleteListener {
        void onComplete(boolean success, String message);
    }

    private static String buildFallbackKey(@NonNull DbModel model) {
        return String.valueOf((model.date + "_" + model.time + "_" + model.type + "_" + model.data).hashCode());
    }

    private static String safeKey(String rawKey) {
        if (rawKey == null || rawKey.trim().isEmpty()) {
            return "unknown";
        }
        return rawKey.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private static double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0d;
        }
    }
}
