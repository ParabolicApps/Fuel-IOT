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
 * Frag2FragmentActivity manages the Analytics dashboard of the Fuel-IOT application.
 *
 * Despite the "Activity" suffix in its name, this class is a {@link Fragment} that provides
 * deep insights into fuel consumption and refueling patterns using interactive charts.
 *
 * Key Features:
 * <ul>
 *   <li><b>Dual-Chart Visualization:</b> Separate charts for Refuelled and Consumed data.</li>
 *   <li><b>Interactive Tooltips:</b> Custom-styled tooltips showing precise data points on touch.</li>
 *   <li><b>Dynamic Filtering:</b> Switch between Daily and Weekly views via a range spinner.</li>
 *   <li><b>Advanced Chart Controls:</b> Options to toggle lines, points, cubic interpolation, and area filling.</li>
 *   <li><b>Theming:</b> Support for custom typography and neon-styled accents.</li>
 * </ul>
 *
 * Libraries Used:
 * <ul>
 *   <li><a href="https://github.com/lecho/hellocharts-android">HelloCharts</a>: For high-performance charting.</li>
 * </ul>
 */
public class Frag2FragmentActivity extends Fragment {

	private static final String TAG = "Frag2FragmentActivity";
	private String fontPath = "";

	// UI Components - Charts and Navigation
	private Spinner rangeSpinner;
	private LineChartView mainChart;
	private LineChartView secondaryChart;
	private LineChartData mainChartData;

	// Tooltip Components for Main Chart
	private View tooltip1;
	private TextView tooltip1Header;
	private TextView tooltip1Value;

	// Tooltip Components for Secondary Chart
	private View tooltip2;
	private TextView tooltip2Header;
	private TextView tooltip2Value;

	// Dashboard Metric Labels
	private TextView totalRefillText;
	private TextView totalConsumeText;

	// Data Management
	private SQLiteHandler db;

	// Default Chart Configuration
	private int numberOfLines = 1;
	private final int maxNumberOfLines = 4;
	private final int numberOfPoints = 12;
	private final float[][] randomNumbersTab = new float[maxNumberOfLines][numberOfPoints];

	// Chart Visual Flags
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

	/**
	 * Fragment lifecycle method. Sets up the options menu and initializes the view hierarchy.
	 */
	@RequiresApi(api = Build.VERSION_CODES.O)
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		View view = inflater.inflate(R.layout.frag2_fragment, container, false);
		initialize(view);
		initializeLogic();
		return view;
	}

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

		totalRefillText = view.findViewById(R.id.total_refill_text);
		totalConsumeText = view.findViewById(R.id.total_consume_text);

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
	 * Sets up event listeners, interactive chart behaviors, and loads the initial dataset.
	 */
	private void initializeLogic() {
		if (mainChart != null) {
			// Register custom touch listener for the 'Refuelled' chart
			mainChart.setOnValueTouchListener(new ValueTouchListener(mainChart, tooltip1, tooltip1Header, tooltip1Value, "Refuelled", ContextCompat.getColor(requireContext(), R.color.neon_blue)));
			mainChart.setViewportCalculationEnabled(false);
		}
		
		if (secondaryChart != null) {
			// Register custom touch listener for the 'Consumed' chart
			secondaryChart.setOnValueTouchListener(new ValueTouchListener(secondaryChart, tooltip2, tooltip2Header, tooltip2Value, "Consumed", ContextCompat.getColor(requireContext(), R.color.neon_orange)));
			secondaryChart.setViewportCalculationEnabled(false);
		}

		setupRangeSpinner();
		_changeActivityFont("ubuntu_medium");

		showWeeklyChart();
		resetViewport();
	}

	private void setupRangeSpinner() {
		if (rangeSpinner == null) return;

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
				R.array.range_entries, R.layout.spinner_item);
		adapter.setDropDownViewResource(R.layout.spinner_item);
		rangeSpinner.setAdapter(adapter);

		rangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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
			Toast.makeText(getActivity(), "Selection mode set to " + mainChart.isValueSelectionEnabled(), Toast.LENGTH_SHORT).show();
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

	private void addLineToData() {
		if (mainChartData.getLines().size() >= maxNumberOfLines) {
			Toast.makeText(getActivity(), "Max 4 lines!", Toast.LENGTH_SHORT).show();
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

	private void prepareDataAnimation() {
		for (Line line : mainChartData.getLines()) {
			for (PointValue value : line.getValues()) {
				value.setTarget(value.getX(), (float) Math.random() * 100);
			}
		}
	}

	/**
	 * Custom listener that handles touch events on chart data points.
	 * When a value is selected, it positions and populates a floating tooltip view
	 * with precise metrics and labels.
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
			
			if (tooltip != null) {
				View tooltipBox = ((ViewGroup) tooltip).getChildAt(0);
				if (tooltipBox.getBackground() instanceof android.graphics.drawable.GradientDrawable) {
					android.graphics.drawable.GradientDrawable drawable = (android.graphics.drawable.GradientDrawable) tooltipBox.getBackground();
					drawable.setStroke(4, color); 
				}
				if (((ViewGroup) tooltip).getChildCount() > 1) {
					View verticalLine = ((ViewGroup) tooltip).getChildAt(1);
					verticalLine.setBackgroundColor(color);
				}
			}
		}

		@Override
		public void onValueSelected(int lineIndex, int pointIndex, PointValue value) {
			if (tooltip == null) return;
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
			chart.post(() -> {
				float x = chart.getChartComputator().computeRawX(value.getX());
				float y = chart.getChartComputator().computeRawY(value.getY());
				tooltip.setX(x - (tooltip.getWidth() / 2f));
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
	 * Populates both charts with data from the last 7 days.
	 * Calculates dates dynamically based on current system time.
	 */
	public void showWeeklyChart() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
		
		ArrayList<Float> weeklyRefill = db.getWeekly("in");
		ArrayList<Float> weeklyConsumed = db.getWeekly("out");
		
		List<PointValue> refillValues = new ArrayList<>();
		List<PointValue> consumedValues = new ArrayList<>();
		List<AxisValue> axisValues = new ArrayList<>();

		for (int i = 0; i < 7; i++) {
			int dataIndex = 6 - i; 
			calendar.setTime(new java.util.Date());
			calendar.add(Calendar.DATE, -dataIndex);
			String dateStr = dateFormat.format(calendar.getTime());
			refillValues.add(new PointValue(i, weeklyRefill.get(dataIndex)));
			consumedValues.add(new PointValue(i, weeklyConsumed.get(dataIndex)));
			axisValues.add(new AxisValue(i).setLabel(dateStr));
		}

		updateChartDisplay(mainChart, refillValues, axisValues, ContextCompat.getColor(requireContext(), R.color.neon_blue));
		updateChartDisplay(secondaryChart, consumedValues, axisValues, ContextCompat.getColor(requireContext(), R.color.neon_orange));
		update7DayTotals();
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	public void showDailyChart() {
		ArrayList<HashMap<String, Object>> dailyRefill = db.getDaily("in");
		ArrayList<HashMap<String, Object>> dailyConsumed = db.getDaily("out");
		List<PointValue> refillValues = new ArrayList<>();
		List<PointValue> consumedValues = new ArrayList<>();
		List<AxisValue> axisValues = new ArrayList<>();

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
		update7DayTotals();
	}

	private void update7DayTotals() {
		if (getContext() == null || totalRefillText == null || totalConsumeText == null) return;
		SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext());
		String unit = prefs.getString("measurement_unit", "L");
		String suffix = "L".equals(unit) ? " Ltrs" : " Gal";
		double weeklyRefill = db.getWeeklyTotal("in");
		double weeklyConsume = db.getWeeklyTotal("out");
		totalRefillText.setText(String.format(Locale.getDefault(), "%.1f\n%s Total", weeklyRefill, suffix));
		totalConsumeText.setText(String.format(Locale.getDefault(), "%.1f\n%s Total", weeklyConsume, suffix));
	}

	/**
	 * Helper method to configure visual properties for a chart instance.
	 * Sets up lines, colors, axis styling (labels/fonts), and background transparency.
	 *
	 * @param chart The LineChartView to update.
	 * @param points The data points to plot.
	 * @param axisXValues The custom labels for the X-axis.
	 * @param strokeColor The primary line and fill color.
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
		if (getContext() != null) {
			Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/ubuntu_medium.ttf");
			Axis axisX = new Axis(axisXValues).setHasLines(true).setTextColor(Color.GRAY).setTextSize(10).setTypeface(tf);
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

	public void setData(String data) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			showWeeklyChart();
			//showDailyChart();
		}
	}

	public void _changeActivityFont(final String fontName) {
		this.fontPath = "fonts/".concat(fontName.concat(".ttf"));
		overrideFonts(getView());
	}

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
		}
	}
}
