package com.codesw.fuelcontroller.fragments;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.*;
import android.app.*;
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
	private static final double SIGNIFICANT_CHANGE_THRESHOLD = 1.0;
	private double myProgress = 0;
	private double lastInputValue = Double.NaN;
	private double lastOutputValue = Double.NaN;
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

	private final Calendar c = Calendar.getInstance();
	SQLiteHandler db;
	private SharedPreferences prefs;

	private boolean isRefilling = false;
	private double refillStartValue = 0;
	private long lastDataTime = 0;
	private android.os.Handler animationHandler = new android.os.Handler(android.os.Looper.getMainLooper());
	private Runnable nozzleBipRunnable;

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

		// Parent containers for neon glow toggle
		card1 = (LinearLayout) total_input.getParent();
		card2 = (LinearLayout) total_consume.getParent();
		card3 = (LinearLayout) monthly_usage_total.getParent().getParent();

		waveLoadingView = _view.findViewById(R.id.waveLoadingView);
		seekbar1 = _view.findViewById(R.id.seekbar1);
		carLogo = _view.findViewById(R.id.car_logo);
		nozzleLogo = _view.findViewById(R.id.nozzle_logo);

		db = new SQLiteHandler(getContext());
		prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
	}

	/**
	 * Dynamically applies user preferences including measurement units, 
	 * neon mode styling, and latest database statistics.
	 */
	private void applyPreferences() {
		if (prefs == null) return;
		String unit = prefs.getString("measurement_unit", "L");
		boolean neonMode = prefs.getBoolean("neon_mode", true);

		String suffix = "L".equals(unit) ? " Ltrs" : " Gal";

		monthly_usage_total.setText(formatFuelValue(parseFuelValue(db.getMonthlyTotal(new SimpleDateFormat("MM").format(c.getTimeInMillis()))), suffix));
		total_input.setText(formatFuelValue(db.getTodayTotalInput(), suffix));
		total_consume.setText(formatFuelValue(db.getTodayTotalUsage(), suffix));

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

	private String formatFuelValue(double value, String suffix) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			value = 0;
		}
		double sanitizedValue = Math.max(0, value);
		DecimalFormat decimalFormat = new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.getDefault()));
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

		String progressValue = String.valueOf(myProgress);
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
		refillStartValue = myProgress;
		startNozzleBip();
		
		// Simulate a 30L refill increase at 500ms intervals
		android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
		final double targetRefill = 30.0;
		for (int i = 1; i <= targetRefill; i++) {
			final int count = i;
			handler.postDelayed(() -> {
				if (statusText != null) {
					statusText.setText("⛽ Fueling... " + count + "L");
					statusText.setTextColor(ContextCompat.getColor(getContext(), R.color.neon_blue));
				}
				if (count == targetRefill) {
					// Finish simulation with final summary
					handler.postDelayed(() -> finishRefill(targetRefill), 1000);
				}
			}, i * 500); 
		}
	}

	/**
	 * Starts a persistent rapid blinking animation alternating between 
	 * the car logo and the fuel nozzle icon.
	 */
	private void startNozzleBip() {
		if (carLogo == null || nozzleLogo == null) return;
		stopNozzleBip(); 

		nozzleBipRunnable = new Runnable() {
			boolean bipOn = false;
			@Override
			public void run() {
				if (bipOn) {
					carLogo.animate().alpha(0.8f).setDuration(100).start();
					nozzleLogo.animate().alpha(0f).setDuration(100).start();
				} else {
					carLogo.animate().alpha(0f).setDuration(100).start();
					nozzleLogo.animate().alpha(1f).setDuration(100).start();
				}
				bipOn = !bipOn;
				animationHandler.postDelayed(this, 250);
			}
		};
		animationHandler.post(nozzleBipRunnable);
	}

	/**
	 * Stops the nozzle blinking animation and resets visual state to default.
	 */
	private void stopNozzleBip() {
		animationHandler.removeCallbacks(nozzleBipRunnable);
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
		resetStatusText();
		showRefillSummary(refilledAmount);
	}

	/**
	 * Presents a high-fidelity Material dialog showing the total 
	 * fuel added during the recent session.
	 */
	private void showRefillSummary(double amount) {
		if (getContext() == null) return;
		new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
				.setTitle("Refill Complete")
				.setMessage("A total of " + String.format(Locale.getDefault(), "%.1f", amount) + " Ltrs has been added to your tank.")
				.setPositiveButton("OK", null)
				.show();
	}

	/**
	 * Reverts the dashboard status line to showing the engine state 
	 * and current highway location.
	 */
	private void resetStatusText() {
		if (statusText == null) return;
		statusText.setText("● Engine: ON  |  📍 Dhaka-Chittagong Highway");
		statusText.setTextColor(ContextCompat.getColor(getContext(), R.color.neon_green));
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
			Typeface
			typeface = Typeface.createFromAsset(getContext().getAssets(), fontName);
			if ((v instanceof ViewGroup)) {
				ViewGroup vg = (ViewGroup) v;
				for (int i = 0;
				i < vg.getChildCount();
				i++) {
					View child = vg.getChildAt(i);
					overrideFonts(context, child);
				}
			}
			else {
				if ((v instanceof TextView)) {
					((TextView) v).setTypeface(typeface);
				}
				else {
					if ((v instanceof EditText )) {
						((EditText) v).setTypeface(typeface);
					}
					else {
						if ((v instanceof Button)) {
							((Button) v).setTypeface(typeface);
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Toast.makeText(getContext(), "Error Loading Font", Toast.LENGTH_SHORT).show();
		}
	}


	/**
	 * Callback from UrlBroadcastReceiver for legacy counter data.
	 */
	@Override
	public void urlReceived(String counter) {
		Log.d(TAG, "urlReceived: "+counter.split("/")[0]);
		String outData = counter.split("/")[0];
		waveLoadingView.setProgressValue((int)Double.parseDouble(counter));
		Log.d(TAG, "urlReceived: "+counter.split("/")[0]);
	}

	/**
	 * Primary entry point for real-time IOT server data.
	 * Handles data parsing, refill detection, and status updates.
	 * 
	 * @param data Raw fuel percentage string from the hardware.
	 */
	public void setProgress(String data){
		try {
			if (data != null && data.contains("/")) {
				handleTelemetryPacket(data);
				return;
			}

			double value = Double.parseDouble(data);
			long currentTime = System.currentTimeMillis();
			lastDataTime = currentTime;
			
			// Detect significant fuel increase (Auto Refill Start)
			if (!isRefilling && value > myProgress + 1.0) {
				isRefilling = true;
				refillStartValue = myProgress;
				startNozzleBip();
			}
			
			if (isRefilling) {
				// Update real-time fueling counter
				if (statusText != null) {
					statusText.setText("⛽ Fueling... " + String.format(Locale.getDefault(), "%.1f", value) + "L");
					statusText.setTextColor(ContextCompat.getColor(getContext(), R.color.neon_blue));
				}
				
				// Automatically terminate refill session if no data for 30 seconds
				animationHandler.removeCallbacksAndMessages("refill_check");
				animationHandler.postAtTime(() -> {
					if (isRefilling && System.currentTimeMillis() - lastDataTime >= 30000) {
						finishRefill(value - refillStartValue);
					}
				}, "refill_check", android.os.SystemClock.uptimeMillis() + 30000);

			} else {
				// Regular engine monitoring status
				if (statusText != null && !statusText.getText().toString().contains("Fueling")) {
					statusText.setText("● Engine: ON  |  📍 Dhaka-Chittagong Highway");
					statusText.setTextColor(ContextCompat.getColor(getContext(), R.color.neon_green));
				}
			}
			
			// Update core dashboard metric
			myProgress = value;
			waveLoadingView.setProgressValue((int)value);
			String progressValue = String.valueOf(value);
			waveLoadingView.setCenterTitle(progressValue + "%");
		} catch (Exception e) {
			Log.e(TAG, "setProgress Error: " + e);
		}
	}

	private void handleTelemetryPacket(String data) {
		String[] parts = data.split("/");
		if (parts.length < 2) {
			Log.w(TAG, "Invalid telemetry packet: " + data);
			return;
		}

		double inputValue = parseFuelValue(parts[0]);
		double outputValue = parseFuelValue(parts[1]);

		if (Double.isNaN(lastInputValue) || Double.isNaN(lastOutputValue)) {
			lastInputValue = inputValue;
			lastOutputValue = outputValue;
			updateFuelDisplay(inputValue);
			return;
		}

		double inputDelta = inputValue - lastInputValue;
		double outputDelta = outputValue - lastOutputValue;
		double largestDelta = Math.abs(inputDelta) >= Math.abs(outputDelta) ? inputDelta : outputDelta;
		boolean hasSignificantChange = Math.abs(inputDelta) >= SIGNIFICANT_CHANGE_THRESHOLD
				|| Math.abs(outputDelta) >= SIGNIFICANT_CHANGE_THRESHOLD;

		lastInputValue = inputValue;
		lastOutputValue = outputValue;

		if (hasSignificantChange) {
			triggerFuelActivity(inputValue, largestDelta);
		} else if (!isRefilling) {
			resetStatusText();
		}

		updateFuelDisplay(inputValue);
	}

	private void triggerFuelActivity(double currentValue, double delta) {
		lastDataTime = System.currentTimeMillis();

		if (!isRefilling) {
			isRefilling = true;
			refillStartValue = currentValue - delta;
			startNozzleBip();
		}

		if (statusText != null) {
			statusText.setText("Fuel activity... " + String.format(Locale.getDefault(), "%.1f", Math.abs(delta)) + "L");
			statusText.setTextColor(ContextCompat.getColor(getContext(), R.color.neon_blue));
		}

		animationHandler.removeCallbacksAndMessages("refill_check");
		animationHandler.postAtTime(() -> {
			if (isRefilling && System.currentTimeMillis() - lastDataTime >= 30000) {
				finishFuelActivity();
			}
		}, "refill_check", android.os.SystemClock.uptimeMillis() + 30000);
	}

	private void finishFuelActivity() {
		isRefilling = false;
		stopNozzleBip();
		resetStatusText();
	}

	private void updateFuelDisplay(double value) {
		myProgress = value;
		waveLoadingView.setProgressValue((int)value);
		waveLoadingView.setCenterTitle(String.valueOf(value) + "%");
	}
}
