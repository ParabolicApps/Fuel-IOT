package com.codesw.fuelcontroller.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.codesw.fuelcontroller.R;
import com.codesw.fuelcontroller.utils.SQLiteHandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import lecho.lib.hellocharts.animation.ChartAnimationListener;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.Chart;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Frag2FragmentActivity manages the Analytics dashboard.
 * It visualizes historical fuel data (refills and consumption) using HelloCharts.
 * 
 * Features:
 * - Dual chart system for "Total Refuelled" and "Engine Consumed".
 * - Range-based filtering (7D, 30D, 3M, 6M, 1Y).
 * - Interactive, theme-aware tooltips with vertical selection lines.
 * - Dynamic axis formatting (Date/Liters) with custom typography.
 */
public class Frag2FragmentActivity extends Fragment {

	private static final String TAG = "Frag2FragmentActivity";
	private String fontPath = "";

	private Spinner rangeSpinner;
	private LineChartView mainChart;
	private LineChartView secondaryChart;
	private LineChartData mainChartData;

	private View tooltip1;
	private TextView tooltip1Header;
	private TextView tooltip1Value;

	private View tooltip2;
	private TextView tooltip2Header;
	private TextView tooltip2Value;

	private SQLiteHandler db;

	private int numberOfLines = 1;
	private final int maxNumberOfLines = 4;
	private final int numberOfPoints = 12;

	private final float[][] randomNumbersTab = new float[maxNumberOfLines][numberOfPoints];

	private boolean hasAxes = true;
	private boolean hasAxesNames = true;
	private boolean hasLines = true;
	private boolean hasPoints = true;
	private ValueShape shape = ValueShape.CIRCLE;
	private boolean isFilled = false;
	private boolean hasLabels = false;
	private boolean isCubic = false;
	private boolean hasLabelForSelected = true;
	private boolean pointsHaveDifferentColor;

	@RequiresApi(api = Build.VERSION_CODES.O)
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// Enable the options menu for chart controls
		setHasOptionsMenu(true);
		View view = inflater.inflate(R.layout.frag2_fragment, container, false);
		initialize(view);
		initializeLogic();
		return view;
	}

	/**
	 * Binds UI elements and sets up the interactive top settings trigger.
	 */
	private void initialize(View view) {
		mainChart = view.findViewById(R.id.chart);
		secondaryChart = view.findViewById(R.id.chart2);
		rangeSpinner = view.findViewById(R.id.range_spinner);

		tooltip1 = view.findViewById(R.id.tooltip1);
		tooltip1Header = view.findViewById(R.id.tooltip1_header);
		tooltip1Value = view.findViewById(R.id.tooltip1_value);

		tooltip2 = view.findViewById(R.id.tooltip2);
		tooltip2Header = view.findViewById(R.id.tooltip2_header);
		tooltip2Value = view.findViewById(R.id.tooltip2_value);

		// Link the top-right gear icon to the Fragment's options menu
		ImageView settingsIcon = view.findViewById(R.id.settings_icon);
		if (settingsIcon != null) {
			settingsIcon.setOnClickListener(v -> {
				if (getActivity() != null) {
					getActivity().openOptionsMenu();
				}
			});
		}

		db = new SQLiteHandler(getContext());
	}

	/**
	 * Configures chart listeners, range selector, and initial data rendering.
	 */
	private void initializeLogic() {
		if (mainChart != null) {
			mainChart.setOnValueTouchListener(new ValueTouchListener(mainChart, tooltip1, tooltip1Header, tooltip1Value, "Refuelled", ContextCompat.getColor(requireContext(), R.color.neon_blue)));
			mainChart.setViewportCalculationEnabled(false);
		}
		
		if (secondaryChart != null) {
			secondaryChart.setOnValueTouchListener(new ValueTouchListener(secondaryChart, tooltip2, tooltip2Header, tooltip2Value, "Consumed", ContextCompat.getColor(requireContext(), R.color.neon_orange)));
			secondaryChart.setViewportCalculationEnabled(false);
		}

		setupRangeSpinner();

		// Apply professional typography across the analytics dashboard
		_changeActivityFont("ubuntu_medium");

		showWeeklyChart();
		resetViewport();
	}

	/**
	 * Populates and styles the period selector spinner.
	 */
	private void setupRangeSpinner() {
		if (rangeSpinner == null) return;

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
				R.array.range_entries, R.layout.spinner_item);
		adapter.setDropDownViewResource(R.layout.spinner_item);
		rangeSpinner.setAdapter(adapter);

		rangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				// Refresh all charts when the date range changes
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					showDailyChart();
				}
				showWeeklyChart();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext());
		boolean isRealMode = prefs.getBoolean("real_mode", false);
		
		// Professional Mode Restriction: Hide chart manipulation tools in Real Mode
		if (!isRealMode) {
			inflater.inflate(R.menu.line_chart, menu);
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_view_daily) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				showDailyChart();
			}
			return true;
		}
		if (id == R.id.action_view_weekly) {
			showWeeklyChart();
			return true;
		}
		if (id == R.id.action_reset) {
			reset();
			generateData();
			return true;
		}
		if (id == R.id.action_add_line) {
			addLineToData();
			return true;
		}
		if (id == R.id.action_toggle_lines) {
			toggleLines();
			return true;
		}
		if (id == R.id.action_toggle_points) {
			togglePoints();
			return true;
		}
		if (id == R.id.action_toggle_cubic) {
			toggleCubic();
			return true;
		}
		if (id == R.id.action_toggle_area) {
			toggleFilled();
			return true;
		}
		if (id == R.id.action_point_color) {
			togglePointColor();
			return true;
		}
		if (id == R.id.action_shape_circles) {
			setCircles();
			return true;
		}
		if (id == R.id.action_shape_square) {
			setSquares();
			return true;
		}
		if (id == R.id.action_shape_diamond) {
			setDiamonds();
			return true;
		}
		if (id == R.id.action_toggle_labels) {
			toggleLabels();
			return true;
		}
		if (id == R.id.action_toggle_axes) {
			toggleAxes();
			return true;
		}
		if (id == R.id.action_toggle_axes_names) {
			toggleAxesNames();
			return true;
		}
		if (id == R.id.action_animate) {
			prepareDataAnimation();
			mainChart.startDataAnimation();
			return true;
		}
		if (id == R.id.action_toggle_selection_mode) {
			toggleLabelForSelected();

			Toast.makeText(getActivity(),
					"Selection mode set to " + mainChart.isValueSelectionEnabled() + " select any point.",
					Toast.LENGTH_SHORT).show();
			return true;
		}
		if (id == R.id.action_toggle_touch_zoom) {
			mainChart.setZoomEnabled(!mainChart.isZoomEnabled());
			Toast.makeText(getActivity(), "IsZoomEnabled " + mainChart.isZoomEnabled(), Toast.LENGTH_SHORT).show();
			return true;
		}
		if (id == R.id.action_zoom_both) {
			mainChart.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);
			return true;
		}
		if (id == R.id.action_zoom_horizontal) {
			mainChart.setZoomType(ZoomType.HORIZONTAL);
			return true;
		}
		if (id == R.id.action_zoom_vertical) {
			mainChart.setZoomType(ZoomType.VERTICAL);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Generates random values for demonstration purposes.
	 */
	private void generateValues() {
		for (int i = 0; i < maxNumberOfLines; ++i) {
			for (int j = 0; j < numberOfPoints; ++j) {
				randomNumbersTab[i][j] = (float) Math.random() * 100f;
			}
		}
	}

	/**
	 * Resets chart settings to their default high-fidelity states.
	 */
	private void reset() {
		numberOfLines = 1;

		hasAxes = true;
		hasAxesNames = true;
		hasLines = true;
		hasPoints = true;
		shape = ValueShape.CIRCLE;
		isFilled = false;
		hasLabels = false;
		isCubic = false;
		hasLabelForSelected = true;
		pointsHaveDifferentColor = false;

		mainChart.setValueSelectionEnabled(true);
		resetViewport();
	}

	/**
	 * Standardizes the chart viewport range for initial view.
	 */
	private void resetViewport() {
		if (mainChart == null) return;
		final Viewport v = new Viewport(mainChart.getMaximumViewport());
		v.bottom = 0;
		v.top = 100;
		v.left = 0;
		v.right = numberOfPoints - 1;
		mainChart.setMaximumViewport(v);
		mainChart.setCurrentViewport(v);
	}

	/**
	 * Generates and applies default data to the main chart based on current settings.
	 */
	private void generateData() {
		List<Line> lines = new ArrayList<>();
		for (int i = 0; i < numberOfLines; ++i) {
			List<PointValue> values = new ArrayList<>();
			for (int j = 0; j < numberOfPoints; ++j) {
				values.add(new PointValue(j, randomNumbersTab[i][j]));
			}

			Line line = new Line(values);
			line.setColor(ChartUtils.COLORS[i]);
			line.setShape(shape);
			line.setCubic(isCubic);
			line.setFilled(isFilled);
			line.setHasLabels(hasLabels);
			line.setHasLabelsOnlyForSelected(true); 
			line.setHasLines(hasLines);
			line.setHasPoints(hasPoints);
			if (pointsHaveDifferentColor){
				line.setPointColor(ChartUtils.COLORS[(i + 1) % ChartUtils.COLORS.length]);
			}
			lines.add(line);
		}

		mainChartData = new LineChartData(lines);

		if (hasAxes) {
			Axis axisX = new Axis();
			Axis axisY = new Axis().setHasLines(true);
			if (hasAxesNames) {
				axisX.setName("Axis X");
				axisY.setName("Axis Y");
			}
			mainChartData.setAxisXBottom(axisX);
			mainChartData.setAxisYLeft(axisY);
		} else {
			mainChartData.setAxisXBottom(null);
			mainChartData.setAxisYLeft(null);
		}

		mainChartData.setBaseValue(Float.NEGATIVE_INFINITY);
		mainChart.setLineChartData(mainChartData);
	}

	/**
	 * Adds a new line to the chart data for multi-series visualization.
	 */
	private void addLineToData() {
		if (mainChartData.getLines().size() >= maxNumberOfLines) {
			Toast.makeText(getActivity(), "Samples app uses max 4 lines!", Toast.LENGTH_SHORT).show();
			return;
		} else {
			++numberOfLines;
		}
		generateData();
	}

	private void toggleLines() {
		hasLines = !hasLines;
		generateData();
	}

	private void togglePoints() {
		hasPoints = !hasPoints;
		generateData();
	}

	/**
	 * Toggles cubic line smoothing and adjusts the viewport with animations.
	 */
	private void toggleCubic() {
		isCubic = !isCubic;
		generateData();

		if (mainChart == null) return;

		if (isCubic) {
			final Viewport v = new Viewport(mainChart.getMaximumViewport());
			v.bottom = -5;
			v.top = 105;
			mainChart.setMaximumViewport(v);
			mainChart.setCurrentViewportWithAnimation(v);
		} else {
			final Viewport v = new Viewport(mainChart.getMaximumViewport());
			v.bottom = 0;
			v.top = 100;

			mainChart.setViewportAnimationListener(new ChartAnimationListener() {
				@Override
				public void onAnimationStarted() {}

				@Override
				public void onAnimationFinished() {
					mainChart.setMaximumViewport(v);
					mainChart.setViewportAnimationListener(null);
				}
			});
			mainChart.setCurrentViewportWithAnimation(v);
		}
	}

	private void toggleFilled() {
		isFilled = !isFilled;
		generateData();
	}

	private void togglePointColor() {
		pointsHaveDifferentColor = !pointsHaveDifferentColor;
		generateData();
	}

	private void setCircles() {
		shape = ValueShape.CIRCLE;
		generateData();
	}

	private void setSquares() {
		shape = ValueShape.SQUARE;
		generateData();
	}

	private void setDiamonds() {
		shape = ValueShape.DIAMOND;
		generateData();
	}

	private void toggleLabels() {
		hasLabels = !hasLabels;
		if (hasLabels) {
			hasLabelForSelected = false;
			mainChart.setValueSelectionEnabled(hasLabelForSelected);
		}
		generateData();
	}

	private void toggleLabelForSelected() {
		hasLabelForSelected = !hasLabelForSelected;
		mainChart.setValueSelectionEnabled(hasLabelForSelected);
		if (hasLabelForSelected) {
			hasLabels = false;
		}
		generateData();
	}

	private void toggleAxes() {
		hasAxes = !hasAxes;
		generateData();
	}

	private void toggleAxesNames() {
		hasAxesNames = !hasAxesNames;
		generateData();
	}

	/**
	 * Prepares data for random animation sequences.
	 */
	private void prepareDataAnimation() {
		for (Line line : mainChartData.getLines()) {
			for (PointValue value : line.getValues()) {
				value.setTarget(value.getX(), (float) Math.random() * 100);
			}
		}
	}

	/**
	 * Internal class to handle chart interactions and dynamic tooltip positioning.
	 */
	private class ValueTouchListener implements LineChartOnValueSelectListener {
		private final LineChartView chart;
		private final View tooltip;
		private final TextView headerText;
		private final TextView valueText;
		private final String prefix;
		private final int color;

		public ValueTouchListener(LineChartView chart, View tooltip, TextView header, TextView value, String prefix, int color) {
			this.chart = chart;
			this.tooltip = tooltip;
			this.headerText = header;
			this.valueText = value;
			this.prefix = prefix;
			this.color = color;
			
			// Dynamic styling for the tooltip border based on the chart's series color
			if (tooltip != null) {
				View tooltipBox = ((ViewGroup) tooltip).getChildAt(0);
				if (tooltipBox.getBackground() instanceof android.graphics.drawable.GradientDrawable) {
					android.graphics.drawable.GradientDrawable drawable = (android.graphics.drawable.GradientDrawable) tooltipBox.getBackground();
					drawable.setStroke(4, color); 
				}
				
				// Apply same color to the vertical indicator line
				if (((ViewGroup) tooltip).getChildCount() > 1) {
					View verticalLine = ((ViewGroup) tooltip).getChildAt(1);
					verticalLine.setBackgroundColor(color);
				}
			}
		}

		@Override
		public void onValueSelected(int lineIndex, int pointIndex, PointValue value) {
			if (tooltip == null) return;

			// Update tooltip content based on the selected point's data
			LineChartData data = chart.getLineChartData();
			if (data != null && data.getAxisXBottom() != null) {
				List<AxisValue> axisValues = data.getAxisXBottom().getValues();
				if (axisValues != null && pointIndex < axisValues.size()) {
					headerText.setText(String.valueOf(axisValues.get(pointIndex).getLabelAsChars()));
				} else {
					headerText.setText("Point " + (pointIndex + 1));
				}
			} else {
				headerText.setText("Point " + (pointIndex + 1));
			}

			valueText.setText(prefix + ": " + String.format(Locale.getDefault(), "%.1f", value.getY()) + " Ltrs");

			tooltip.setVisibility(View.VISIBLE);

			// Dynamically compute screen coordinates for the selected data point
			chart.post(() -> {
				float x = chart.getChartComputator().computeRawX(value.getX());
				float y = chart.getChartComputator().computeRawY(value.getY());

				// Center the tooltip horizontally above the point
				tooltip.setX(x - (tooltip.getWidth() / 2f));
				
				// Position the tooltip box above the point, allowing the vertical line to drop down
				View tooltipBox = ((ViewGroup)tooltip).getChildAt(0);
				tooltip.setY(y - tooltipBox.getHeight() - 10);
			});
		}

		@Override
		public void onValueDeselected() {
			if (tooltip != null) {
				tooltip.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * Fetches weekly data from the SQLite database and updates Refill & Consumption charts.
	 */
	public void showWeeklyChart() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
		
		ArrayList<Float> weeklyRefill = db.getWeekly("in");
		ArrayList<Float> weeklyConsumed = db.getWeekly("out");
		
		List<PointValue> refillValues = new ArrayList<>();
		List<PointValue> consumedValues = new ArrayList<>();
		List<AxisValue> axisValues = new ArrayList<>();

		// Process the last 7 days (Oldest -> Today)
		// db.getWeekly returns [Today, Yesterday, ..., 6 Days Ago]
		for (int i = 0; i < 7; i++) {
			int dataIndex = 6 - i; // Start from 6 days ago (index 6) to today (index 0)
			
			calendar.setTime(new java.util.Date());
			calendar.add(Calendar.DATE, -dataIndex);
			String dateStr = dateFormat.format(calendar.getTime());
			
			refillValues.add(new PointValue(i, weeklyRefill.get(dataIndex)));
			consumedValues.add(new PointValue(i, weeklyConsumed.get(dataIndex)));
			axisValues.add(new AxisValue(i).setLabel(dateStr));
		}

		updateChartDisplay(mainChart, refillValues, axisValues, ContextCompat.getColor(requireContext(), R.color.neon_blue));
		updateChartDisplay(secondaryChart, consumedValues, axisValues, ContextCompat.getColor(requireContext(), R.color.neon_orange));
	}

	/**
	 * Fetches hourly data for the current day from SQLite and updates both dashboard charts.
	 */
	@RequiresApi(api = Build.VERSION_CODES.O)
	public void showDailyChart() {
		ArrayList<HashMap<String, Object>> dailyRefill = db.getDaily("in");
		ArrayList<HashMap<String, Object>> dailyConsumed = db.getDaily("out");
		
		List<PointValue> refillValues = new ArrayList<>();
		List<PointValue> consumedValues = new ArrayList<>();
		List<AxisValue> axisValues = new ArrayList<>();

		// Process Today's Refills
		if (dailyRefill != null) {
			for (int i = 0; i < dailyRefill.size(); i++) {
				try {
					Object valObj = dailyRefill.get(i).get("value");
					float value = valObj != null ? Float.parseFloat(valObj.toString()) : 0f;
					refillValues.add(new PointValue(i, value));
					axisValues.add(new AxisValue(i).setLabel(dailyRefill.get(i).get("hour") + ":00"));
				} catch (Exception e) {
					refillValues.add(new PointValue(i, 0f));
				}
			}
		}

		// Process Today's Consumption
		if (dailyConsumed != null) {
			for (int i = 0; i < dailyConsumed.size(); i++) {
				try {
					Object valObj = dailyConsumed.get(i).get("value");
					float value = valObj != null ? Float.parseFloat(valObj.toString()) : 0f;
					consumedValues.add(new PointValue(i, value));
				} catch (Exception e) {
					consumedValues.add(new PointValue(i, 0f));
				}
			}
		}

		updateChartDisplay(mainChart, refillValues, axisValues, ContextCompat.getColor(requireContext(), R.color.neon_blue));
		updateChartDisplay(secondaryChart, consumedValues, axisValues, ContextCompat.getColor(requireContext(), R.color.neon_orange));
	}

	/**
	 * Core rendering engine for HelloCharts. Applies theme-aware styling and custom axis formatting.
	 */
	private void updateChartDisplay(LineChartView chart, List<PointValue> points, List<AxisValue> axisXValues, int strokeColor) {
		if (points.isEmpty()) {
			points.add(new PointValue(0, 0));
		}

		Line line = new Line(points)
				.setColor(strokeColor)
				.setShape(ValueShape.CIRCLE)
				.setCubic(true)
				.setFilled(true)
				.setHasLines(true)
				.setHasPoints(true)
				.setHasLabelsOnlyForSelected(true);

		List<Line> lines = new ArrayList<>();
		lines.add(line);

		LineChartData data = new LineChartData(lines);

		// Apply Brand Typography to Axes
		if (getContext() != null) {
			Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/ubuntu_medium.ttf");

			// X Axis configuration (Time/Date)
			Axis axisX = new Axis(axisXValues).setHasLines(true).setTextColor(Color.GRAY).setTextSize(10).setTypeface(tf);
			
			// Y Axis configuration (Liters with custom suffix labels)
			List<AxisValue> yValues = new ArrayList<>();
			float maxVal = 0;
			for(PointValue pv : points) if(pv.getY() > maxVal) maxVal = pv.getY();
			int step = (int) Math.max(10, maxVal / 5);
			for(int i=0; i <= (maxVal + step); i+=step){
				yValues.add(new AxisValue(i).setLabel(i + " L"));
			}
			
			Axis axisY = new Axis(yValues).setHasLines(true).setTextColor(Color.GRAY).setTextSize(10).setTypeface(tf);

			data.setAxisXBottom(axisX);
			data.setAxisYLeft(axisY);
		}

		if (chart != null) {
			chart.setLineChartData(data);
			chart.setBackgroundColor(Color.TRANSPARENT);
			resetViewport(chart, data, points.size());
		}
	}

	/**
	 * Recalculates chart boundaries to ensure all data points and tooltips fit within view.
	 */
	private void resetViewport(LineChartView chart, LineChartData data, int maxPoints) {
		float maxVal = 20f;
		if (data != null) {
			for (Line l : data.getLines()) {
				for (PointValue pv : l.getValues()) {
					if (pv.getY() > maxVal) maxVal = pv.getY() + 15f;
				}
			}
		}

		final Viewport v = new Viewport(chart.getMaximumViewport());
		v.bottom = 0;
		v.top = maxVal;
		v.left = 0;
		v.right = Math.max(1, maxPoints - 1);

		chart.setMaximumViewport(v);
		chart.setCurrentViewport(v);
	}

	/**
	 * Callback to refresh charts from external triggers (e.g. data sync events).
	 */
	public void setData(String data) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			showDailyChart();
		}
	}

	/**
	 * Utility to apply global font changes to the fragment's UI.
	 */
	public void _changeActivityFont(final String fontName) {
		this.fontPath = "fonts/".concat(fontName.concat(".ttf"));
		overrideFonts(getView());
	}

	/**
	 * Recursive typeface injector to maintain brand consistency across the analytics stack.
	 */
	private void overrideFonts(final View v) {
		try {
			if (getContext() == null) return;
			Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), fontPath);
			if (v instanceof ViewGroup) {
				ViewGroup vg = (ViewGroup) v;
				for (int i = 0; i < vg.getChildCount(); i++) {
					View child = vg.getChildAt(i);
					overrideFonts(child);
				}
			} else if (v instanceof TextView) {
				((TextView) v).setTypeface(typeface);
			}
		} catch(Exception e) {
			// Quiet fail fallback
		}
	}
}
