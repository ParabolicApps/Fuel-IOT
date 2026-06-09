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
import me.itangqi.waveloadingview.WaveLoadingView;

/**
 * this Fragment is used to show the devices and Statistics
 * and daily usage and Everything about The Devices
 * And Also The data is Realtime on There is no Chances of lose
 */
public class Frag1FragmentActivity extends  Fragment implements UrlBroadcastReceiver.UrlBroadcastReceiverListener  {


	private static final String TAG = "Frag1FragmentActivity";
	private double myProgress = 0;
	private final boolean simulate = false;
	private final boolean configured = true;
	private String fontName = "";
	private final String typeface = "";

	private TextView total_input;
	private TextView total_consume;
	private TextView monthly_usage_total;

	private TextView textview1;//total consume
	private TextView textview2;//total input
	private TextView textview3;//total Monthly Usage

	private WaveLoadingView waveLoadingView;
	private SeekBar seekbar1;

	private final Calendar c = Calendar.getInstance();
	SQLiteHandler db;

	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.frag1_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		initializeLogic();
		return _view;
	}
	@Override
	public void onResume(){
		super.onResume();
		Log.d("MainActivity: Fragment: ", "onResume");
		// Don't know if it will work well as from Fragment instead from an Activity

	}
	@Override
	public void onPause() {
		super.onPause();

		Log.d("MainActivity: Fragment: ", "onPause");
	}

	private void initialize(Bundle _savedInstanceState, View _view) {
		total_input = _view.findViewById(R.id.total_input);
		total_consume = _view.findViewById(R.id.total_consume);
		monthly_usage_total = _view.findViewById(R.id.monthly_usage_total);


		textview1 = _view.findViewById(R.id.textview1);
		textview2 = _view.findViewById(R.id.textview2);
		textview3 = _view.findViewById(R.id.textview3);


		waveLoadingView = _view.findViewById(R.id.waveLoadingView);
		seekbar1 = _view.findViewById(R.id.seekbar1);

		db = new SQLiteHandler(getContext());

		// Create The Receiver


		seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged (SeekBar _param1, int _param2, boolean _param3) {
				final int _progressValue = _param2;
				myProgress = _progressValue;
				waveLoadingView.setProgressValue((int)myProgress);
				waveLoadingView.setCenterTitle(myProgress +"%");

			}

			@Override
			public void onStartTrackingTouch(SeekBar _param1) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar _param2) {

			}
		});


	}

	private void initializeLogic() {
		waveLoadingView.setWaveColor(ContextCompat.getColor(getContext(), R.color.neon_green));
		waveLoadingView.setProgressValue((int)myProgress);

		String progressValue = String.valueOf(myProgress);
		//set title within the WaveView
		waveLoadingView.setCenterTitle(progressValue + "%");
		waveLoadingView.setCenterTitleColor(Color.WHITE);


		monthly_usage_total.setText(db.getMonthlyTotal(new SimpleDateFormat("MM").format(c.getTimeInMillis())));
		total_input.setText(String.valueOf(db.getTodayTotalInput()) + " Ltrs");
		total_consume.setText(String.valueOf(db.getTodayTotalUsage()) + " Ltrs");

		// pause charging animation and wave color, depending on charging status

		/*Toast.makeText(context, "isCharging = " + isCharging , Toast.LENGTH_SHORT).show();*/

		_changeActivityFont("greenscr");

		monthly_usage_total.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/ubuntu_medium.ttf"), 0);

		total_input.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/ubuntu_medium.ttf"), 0);
		total_consume.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/ubuntu_medium.ttf"), 0);

		textview1.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/greenscr.ttf"), 0);
		textview2.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/greenscr.ttf"), 0);
		textview3.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/greenscr.ttf"), 0);
		//need to add a configured preference
		if(configured){
			Intent intent=new Intent(getActivity(), Checker.class);
			getActivity().startService(intent);
		}
	}

	@Override
	public void onActivityResult(int _requestCode, int _resultCode, Intent _data) {

		super.onActivityResult(_requestCode, _resultCode, _data);

		switch (_requestCode) {

			default:
			break;
		}
	}

	public void _stopped () {
		waveLoadingView.pauseAnimation();
		//textViewChargeTimeRemaining.setVisibility(View.GONE);
		//textViewEtaHeading.setVisibility(View.GONE);
		//waveLoadingView.setWaveColor(getContext().getResources().getColor(android.R.color.holo_green_dark));
		waveLoadingView.setWaveColor(ContextCompat.getColor(getContext(), R.color.neon_green));

	}


	public void _running () {
		waveLoadingView.resumeAnimation();
		//waveLoadingView.setWaveColor(getContext().getResources().getColor(R.color.colorGreen));  Depreciated getcolor
		waveLoadingView.setWaveColor(ContextCompat.getColor(getContext(), R.color.neon_green));

	}


	public void _changeActivityFont (final String _fontname) {
		fontName = "fonts/".concat(_fontname.concat(".ttf"));
		overrideFonts(getContext(),getView());
	}
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


	@Override
	public void urlReceived(String counter) {
		//tv.setText(tv.getText() + "\n"+counter);
		Log.d(TAG, "urlReceived: "+counter.split("/")[0]);
		String outData = counter.split("/")[0];
		waveLoadingView.setProgressValue((int)Double.parseDouble(counter));
		Log.d(TAG, "urlReceived: "+counter.split("/")[0]);
	}
	public void setProgress(String data){
		double value = Double.parseDouble(data);
		waveLoadingView.setProgressValue((int)value);
		String progressValue = String.valueOf(value);
		waveLoadingView.setCenterTitle(progressValue + "%");

	}
}
