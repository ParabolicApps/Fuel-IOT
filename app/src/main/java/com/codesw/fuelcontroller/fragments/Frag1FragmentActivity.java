package com.codesw.fuelcontroller.fragments;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.*;
import android.app.*;
import android.media.metrics.LogSessionId;
import android.os.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.webkit.*;
import android.animation.*;
import android.view.animation.*;
import java.util.*;
import java.util.regex.*;
import java.text.*;
import org.json.*;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.SeekBar;

import com.codesw.fuelcontroller.GuideActivity;
import com.codesw.fuelcontroller.receiver.UrlBroadcastReceiver;
import com.codesw.fuelcontroller.service.Checker;
import com.codesw.fuelcontroller.utils.SQLiteHandler;
import com.google.android.material.button.*;
import android.view.View;
import android.graphics.Typeface;
import com.codesw.fuelcontroller.R;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import me.itangqi.waveloadingview.WaveLoadingView;
import android.content.SharedPreferences;

/**
 * Frag1FragmentActivity serves as the main dashboard of the application.
 * It displays real-time fuel metrics, connection status, and includes
 * interactive simulations for refueling events.
 * 
 * Features:
 * - Real-time fuel level visualization via WaveLoadingView.
 * - Glowing cards for key metrics (Total Input, Consumed, Monthly Usage).
 * - Interactive "bip-bip" nozzle animation during refills.
 * - Support for theme-aware dynamic typography.
 */
public class Frag1FragmentActivity extends  Fragment implements UrlBroadcastReceiver.UrlBroadcastReceiverListener  {


	private static final String TAG = "Frag1FragmentActivity";
	private static final double SIGNIFICANT_CHANGE_THRESHOLD = 0.1;
	private double myProgress = 0;
	private double lastInputValue = Double.NaN;
	private double lastOutputValue = Double.NaN;
	private double baselineInputValue = Double.NaN;
	private double baselineOutputValue = Double.NaN;
	private final boolean simulate = false;
	private final boolean configured = true;
	private String fontName = "";
	private final String typeface = "";

	private TextView total_input;
	private TextView total_consume;
	private TextView monthly_usage_total;
	private TextView statusText;

	private TextView textview1;//total consume
	private TextView textview2;//total input
	private TextView textview3;//total Monthly Usage

	private WaveLoadingView waveLoadingView;
	private SeekBar seekbar1;
	private LinearLayout card1, card2, card3;
	private ImageView carLogo, nozzleLogo;
	private ImageView resetBtn;
	SQLiteHandler db;
	private SharedPreferences prefs;

	private boolean isRefilling = false;
	private double refillStartValue = 0;
	private long lastDataTime = 0;
	private android.os.Handler animationHandler = new android.os.Handler(android.os.Looper.getMainLooper());
	private android.animation.ValueAnimator nozzleAnimator;

	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		// Inflate the main dashboard layout
		View _view = _inflater.inflate(R.layout.frag1_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		return _view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// Setup initial UI states and simulation triggers
		initializeLogic();
	}

	@Override
	public void onResume(){
		super.onResume();
		Log.d("MainActivity: Fragment: ", "onResume");
		// Refresh UI metrics and apply latest theme/unit preferences
		applyPreferences();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopNozzleBip();
	}

	/**
	 * Binds UI components and initializes core data handlers.
	 */
	private void initialize(Bundle _savedInstanceState, View _view) {
		total_input = _view.findViewById(R.id.total_input);
		total_consume = _view.findViewById(R.id.total_consume);
		monthly_usage_total = _view.findViewById(R.id.monthly_usage_total);
		statusText = _view.findViewById(R.id.status_text);

		textview1 = _view.findViewById(R.id.textview1);
		textview2 = _view.findViewById(R.id.textview2);
		textview3 = _view.findViewById(R.id.textview3);
		resetBtn = _view.findViewById(R.id.reset_button);
		// Parent containers for neon glow toggle
		card1 = (LinearLayout) total_input.getParent();
		card2 = (LinearLayout) total_consume.getParent();
		card3 = (LinearLayout) monthly_usage_total.getParent().getParent();

		waveLoadingView = _view.findViewById(R.id.waveLoadingView);
		seekbar1 = _view.findViewById(R.id.seekbar1);
		carLogo = _view.findViewById(R.id.car_logo);
		nozzleLogo = _view.findViewById(R.id.nozzle_logo);

		statusText.setOnClickListener(v -> {
			if (!isRefilling) {
				resetStatusText();
			}
		});

		resetBtn.setOnClickListener(v -> {
			if (!isRefilling) {
				resetStatusText();
			}
		});

		db = SQLiteHandler.getInstance(getContext());
		prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
	}

	/**
	 * Dynamically applies user preferences including measurement units, 
	 * neon mode styling, and latest database statistics.
	 */
	private void applyPreferences() {
		if (prefs == null || db == null) return;
		boolean neonMode = prefs.getBoolean("neon_mode", true);

		updateTodayTotals();
		
		// Initialize Tank Status from last known values in DB
		double lastIn = db.getLastInput();
		double lastOut = db.getLastOutput();
		updateFuelDisplay(lastIn - lastOut);

		textview1.setText("Last 30 Days");
		textview2.setText("Last 30 Days");

		// Toggle Neon Glow effects on card backgrounds
		if (neonMode) {
			card1.setBackgroundResource(R.drawable.card_glow_blue);
			card2.setBackgroundResource(R.drawable.card_glow_orange);
			card3.setBackgroundResource(R.drawable.card_glow_green);
		} else {
			card1.setBackgroundResource(R.drawable.button_shape);
			card2.setBackgroundResource(R.drawable.button_shape);
			card3.setBackgroundResource(R.drawable.button_shape);
		}
	}

	/**
	 * Refreshes the 30-day historical totals from the database.
	 */
	private void updateTodayTotals() {
		if (prefs == null || db == null || !isAdded() || getView() == null) return;

		try {
			String unit = prefs.getString("measurement_unit", "L");
			String suffix = "L".equals(unit) ? " Ltrs" : " Gal";

			double input30Days = db.get30DaysTotal("in");
			double consume30Days = db.get30DaysTotal("out");

			total_input.setText(formatFuelValue(input30Days, suffix));
			total_consume.setText(formatFuelValue(consume30Days, suffix));
		} catch (Exception e) {
			Log.e(TAG, "Update Error: " + e);
		}
	}

	private String formatFuelValue(double value, String suffix) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			value = 0;
		}
		double sanitizedValue = Math.max(0, value);
		DecimalFormat decimalFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.getDefault()));
		return decimalFormat.format(sanitizedValue) + suffix;
	}

	private double parseFuelValue(String value) {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Configures initial component properties, starting services, 
	 * and setting up interaction listeners.
	 */
	private void initializeLogic() {
		waveLoadingView.setWaveColor(ContextCompat.getColor(getContext(), R.color.neon_green));
		waveLoadingView.setProgressValue((int)myProgress);

		String progressValue = String.format(Locale.getDefault(), "%.2f", myProgress);
		waveLoadingView.setCenterTitle(progressValue + "%");
		
		// Adjust title color for better readability against wave
		boolean isDarkMode = prefs != null && prefs.getBoolean("dark_mode", true);
		waveLoadingView.setCenterTitleColor(isDarkMode ? Color.WHITE : Color.DKGRAY);

		applyPreferences();

		// Apply professional typography
		_changeActivityFont("ubuntu_medium");

		// Auto-start background sync service if configured
		if(configured){
			Intent intent=new Intent(getActivity(), Checker.class);
			getActivity().startService(intent);
		}
		
		// Enable manual fueling simulation via long-click
		carLogo.setOnLongClickListener(v -> {
			triggerFuelingSimulation();
			return true;
		});
	}

	/**
	 * Initiates a multi-second fueling demo, including rapid nozzle blinking
	 * and a status text counter.
	 */
	private void triggerFuelingSimulation() {
		if (isRefilling) return;
		isRefilling = true;
		animationHandler.removeCallbacksAndMessages("status_reset");
		refillStartValue = myProgress;
		lastDataTime = android.os.SystemClock.uptimeMillis();
		startNozzleBip();
		//Log.d("TAG")
		// Simulate a 30L refill increase at 500ms intervals
		android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
		final double targetRefill = 30.0;
		for (int i = 1; i <= targetRefill; i++) {
			final int count = i;
			handler.postDelayed(() -> {
				if (statusText != null) {
					statusText.setText("⛽ Fueling... " + String.format(Locale.getDefault(), "%.2f", (double)count) + "L");
					statusText.setTextColor(ContextCompat.getColor(getContext(), R.color.neon_blue));
				}
				// Refresh Today cards during simulation too
				updateTodayTotals();
				
				if (count == targetRefill) {
					handler.postDelayed(() -> finishRefill(targetRefill), 1000);
				}
			}, i * 500); 
		}
	}

	private void startNozzleBip() {
		if (carLogo == null || nozzleLogo == null) return;
		if (nozzleAnimator != null && nozzleAnimator.isRunning()) return;

		nozzleAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f);
		nozzleAnimator.setDuration(400);
		nozzleAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
		nozzleAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
		nozzleAnimator.addUpdateListener(animation -> {
			float val = (float) animation.getAnimatedValue();
			if (val > 0.5f) {
				carLogo.setAlpha(0f);
				nozzleLogo.setAlpha(1f);
			} else {
				carLogo.setAlpha(0.8f);
				nozzleLogo.setAlpha(0f);
			}
		});
		nozzleAnimator.start();
	}

	private void stopNozzleBip() {
		if (nozzleAnimator != null) {
			nozzleAnimator.cancel();
			nozzleAnimator = null;
		}
		if (carLogo != null) carLogo.setAlpha(0.8f);
		if (nozzleLogo != null) nozzleLogo.setAlpha(0f);
	}

	/**
	 * Concludes a refill session, resets the dashboard status, 
	 * and presents a summary dialog.
	 */
	private void finishRefill(double refilledAmount) {
		isRefilling = false;
		stopNozzleBip();
		if (statusText != null) {
			statusText.setText("⛽ Refill Complete: " + String.format(Locale.getDefault(), "%.2f", refilledAmount) + "L");
			statusText.setTextColor(ContextCompat.getColor(getContext(), R.color.neon_blue));
		}
		showRefillSummary(refilledAmount);
		scheduleStatusReset();
	}

	/**
	 * Presents a high-fidelity Material dialog showing the total 
	 * fuel added during the recent session.
	 */
	private void showRefillSummary(double amount) {
		if (getContext() == null) return;
		new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
				.setTitle("Refill Complete")
				.setMessage("A total of " + String.format(Locale.getDefault(), "%.2f", amount) + " Ltrs has been added to your tank.")
				.setPositiveButton("OK", null)
				.show();
	}

	/**
	 * Reverts the dashboard status line to showing the engine state 
	 * and current highway location.
	 */
	private void resetStatusText() {
		if (statusText == null || isRefilling) return;
		statusText.setText("");

		statusText.setTextColor(ContextCompat.getColor(getContext(), R.color.neon_green));
	}

	private void scheduleStatusReset() {
		animationHandler.removeCallbacksAndMessages("status_reset");
		animationHandler.postAtTime(this::resetStatusText, "status_reset", android.os.SystemClock.uptimeMillis() + 240000);
	}

	@Override
	public void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
		super.onActivityResult(_requestCode, _resultCode, _data);
		switch (_requestCode) {
			default:
			break;
		}
	}

	/**
	 * Pauses visual animations when the monitoring service is stopped.
	 */
	public void _stopped () {
		waveLoadingView.pauseAnimation();
		waveLoadingView.setWaveColor(ContextCompat.getColor(getContext(), R.color.neon_green));

	}

	/**
	 * Resumes visual animations when the monitoring service is running.
	 */
	public void _running () {
		waveLoadingView.resumeAnimation();
		waveLoadingView.setWaveColor(ContextCompat.getColor(getContext(), R.color.neon_green));

	}

	/**
	 * Utility to construct font path and trigger recursive application.
	 */
	public void _changeActivityFont (final String _fontname) {
		fontName = "fonts/".concat(_fontname.concat(".ttf"));
		overrideFonts(getContext(),getView());
	}

	/**
	 * Recursive function that ensures consistent brand typography 
	 * across all child views of the fragment.
	 */
	private void overrideFonts(final android.content.Context context, final View v) {
		try {
			Typeface tf = Typeface.createFromAsset(context.getAssets(), fontName);
			applyTypefaceRecursive(v, tf);
		} catch (Exception e) {
			Log.e(TAG, "Font error: " + e);
		}
	}

	private void applyTypefaceRecursive(View v, Typeface tf) {
		if (v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			for (int i = 0; i < vg.getChildCount(); i++) {
				applyTypefaceRecursive(vg.getChildAt(i), tf);
			}
		} else if (v instanceof TextView) {
			((TextView) v).setTypeface(tf);
		} else if (v instanceof Button) {
			((Button) v).setTypeface(tf);
		} else if (v instanceof EditText) {
			((EditText) v).setTypeface(tf);
		}
	}


	@Override
	public void urlReceived(String counter) {
		Log.d(TAG, "urlReceived: " + counter);
		setProgress(counter);
	}

	/**
	 * Primary entry point for real-time IOT server data.
	 * Handles data parsing, refill detection, and status updates.
	 * 
	 * @param data Raw fuel percentage string from the hardware.
	 */
	public void setProgress(String data){
		try {
			Log.d(TAG, "TRACE: setProgress called in Frag1FragmentActivity with: " + data);
			// Optional: Toast.makeText(getContext(), "Sync: " + data, Toast.LENGTH_SHORT).show();
			
			if (data != null && data.contains("/")) {
				Log.d(TAG, "TRACE: Data contains separator. Routing to handleTelemetryPacket");
				handleTelemetryPacket(data);
				updateTodayTotals();
				return;
			}
            
			Log.d(TAG, "TRACE: Data is partial/single value. Updating totals and recalculating balance.");
			updateTodayTotals();
			try {
				// Fallback: If we receive partial data (e.g. from MainActivity routing),
				// recalculate the current tank status from the database to maintain balance.
				double lastIn = db.getLastInput();
				double lastOut = db.getLastOutput();
				updateFuelDisplay(lastIn - lastOut);
			} catch (Exception ignored) {}
		} catch (Exception e) {
			Log.e(TAG, "setProgress Error: " + e);
		}
	}

	private void handleTelemetryPacket(String data) {
		String[] parts = data.split("/");
		if (parts.length < 2) return;

		double inputValue = parseFuelValue(parts[0]);
		double outputValue = parseFuelValue(parts[1]);
		double currentLevel = inputValue - outputValue;

		if (Double.isNaN(baselineInputValue)) {
			baselineInputValue = inputValue;
			baselineOutputValue = outputValue;
			lastInputValue = inputValue;
			lastOutputValue = outputValue;
			updateFuelDisplay(currentLevel);
			return;
		}

		// Calculate Deltas with Reset Detection
		double inputDelta = (inputValue < lastInputValue) ? inputValue : (inputValue - lastInputValue);
		double outputDelta = (outputValue < lastOutputValue) ? outputValue : (outputValue - lastOutputValue);
		
		boolean hasSignificantChange = Math.abs(inputDelta) >= SIGNIFICANT_CHANGE_THRESHOLD
				|| Math.abs(outputDelta) >= SIGNIFICANT_CHANGE_THRESHOLD;

		Log.d(TAG, "Telemetry Packet: " + data + " | Delta: " + (Math.max(Math.abs(inputDelta), Math.abs(outputDelta))));

		if (hasSignificantChange) {
			double deltaForAnimation = Math.abs(inputDelta) >= Math.abs(outputDelta) ? inputDelta : outputDelta;
			triggerFuelActivity(inputValue, deltaForAnimation);
		} else if (!isRefilling) {
			resetStatusText();
		}

		lastInputValue = inputValue;
		lastOutputValue = outputValue;
		updateFuelDisplay(currentLevel);
	}

	private void triggerFuelActivity(double currentValue, double delta) {
		Log.d(TAG, "triggerFuelActivity: Value=" + currentValue + " Delta=" + delta + " AlreadyRefilling=" + isRefilling);
		lastDataTime = android.os.SystemClock.uptimeMillis();

		if (!isRefilling) {
			isRefilling = true;
			if (getContext() != null) {
				Toast.makeText(getContext(), "Refill Started: Detecting fuel increase...", Toast.LENGTH_SHORT).show();
			}
			animationHandler.removeCallbacksAndMessages("status_reset");
			refillStartValue = currentValue - delta;
			Log.d(TAG, "Starting nozzle animation, baseline=" + refillStartValue);
			startNozzleBip();
		}

		if (statusText != null) {
			double accumulatedDelta = currentValue - refillStartValue;
			statusText.setText("Fuel activity... " + String.format(Locale.getDefault(), "%.2f", Math.abs(accumulatedDelta)) + "L");
			statusText.setTextColor(ContextCompat.getColor(getContext(), R.color.neon_blue));
		}

		// Update the 'Today' cards whenever there is a nozzle animation / data change
		updateTodayTotals();

		animationHandler.removeCallbacksAndMessages("refill_check");
		animationHandler.postAtTime(() -> {
			if (isRefilling && android.os.SystemClock.uptimeMillis() - lastDataTime >= 30000) {
				finishFuelActivity();
			}
		}, "refill_check", android.os.SystemClock.uptimeMillis() + 30000);
	}

	private void finishFuelActivity() {
		double accumulatedDelta = lastInputValue - refillStartValue;
		isRefilling = false;
		stopNozzleBip();
		if (statusText != null) {
			statusText.setText("Activity Complete: " + String.format(Locale.getDefault(), "%.2f", Math.abs(accumulatedDelta)) + "L (Tap to Reset)");
			statusText.setTextColor(ContextCompat.getColor(getContext(), R.color.neon_blue));
		}
		// Final refresh of totals once the activity ends
		updateTodayTotals();
		// Removed scheduleStatusReset() to keep text until manual reset
	}

	private void updateFuelDisplay(double value) {
		if (!isAdded() || getView() == null) {
			Log.w(TAG, "updateFuelDisplay: Fragment not attached, skipping UI update");
			return;
		}
		
		animationHandler.post(() -> {
			if (!isAdded() || getView() == null) return;
			myProgress = value;
			Log.d(TAG, "UI Update: Setting fuel level to " + value);
			
			// Update Tank Status Display
			if (monthly_usage_total != null) {
				String unit = prefs.getString("measurement_unit", "L");
				String suffix = "L".equals(unit) ? " Ltrs" : " Gal";
				monthly_usage_total.setText(formatFuelValue(value, suffix));
			}

			// Update Wave Animation
			// Using 100 as max tank capacity for percentage display
			waveLoadingView.setProgressValue((int)Math.max(0, Math.min(100, value)));
			waveLoadingView.setCenterTitle(String.format(Locale.getDefault(), "%.2f", value) + "%");
			
			// If we are NOT in the middle of a nozzle animation, 
			// still ensure Today values are correct for small fluctuations
			if (!isRefilling) {
				updateTodayTotals();
			}
		});
	}
}
