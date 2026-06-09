package com.codesw.fuelcontroller.fragments;

import android.content.Context;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
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
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.Chart;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Frag2FragmentActivity handles the display of fuel statistics using HelloCharts.
 * It provides both daily and weekly usage visualizations with interactive features.
 */
public class Frag2FragmentActivity extends Fragment {

	private static final String TAG = "Frag2FragmentActivity";
	private String fontPath = "";

	private RadioGroup radioGroup;
	private LineChartView mainChart;
	private LineChartView secondaryChart;
	private LineChartData mainChartData;

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
	private boolean hasLabelForSelected = false;
	private boolean pointsHaveDifferentColor;

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

	/**
	 * Initializes the UI components by finding them in the inflated view.
	 *
	 * @param view The root view of the fragment.
	 */
	private void initialize(View view) {
		mainChart = view.findViewById(R.id.chart);
		secondaryChart = view.findViewById(R.id.chart2);
		radioGroup = view.findViewById(R.id.radio_group);

		db = new SQLiteHandler(getContext());
	}

	/**
	 * Sets up the initial logic for the charts, including listeners and data generation.
	 */
	private void initializeLogic() {
		if (mainChart != null) {
			mainChart.setOnValueTouchListener(new ValueTouchListener());
			mainChart.setViewportCalculationEnabled(false);
		}
		
		if (secondaryChart != null) {
			secondaryChart.setOnValueTouchListener(new ValueTouchListener());
			secondaryChart.setViewportCalculationEnabled(false);
		}

		generateValues();
		generateData();
		resetViewport();
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		inflater.inflate(R.menu.line_chart, menu);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int id = item.getItemId();
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
	 * Resets chart settings to their default states.
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
		hasLabelForSelected = false;
		pointsHaveDifferentColor = false;

		mainChart.setValueSelectionEnabled(hasLabelForSelected);
		resetViewport();
	}

	/**
	 * Resets the chart viewport to default ranges.
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
	 * Generates and applies data to the main chart based on current settings.
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
			line.setHasLabelsOnlyForSelected(hasLabelForSelected);
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
	 * Adds a new line to the chart data if the maximum number of lines has not been reached.
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
	 * Toggles cubic line smoothing and adjusts the viewport accordingly.
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
	 * Prepares data for animation by setting target values.
	 */
	private void prepareDataAnimation() {
		for (Line line : mainChartData.getLines()) {
			for (PointValue value : line.getValues()) {
				value.setTarget(value.getX(), (float) Math.random() * 100);
			}
		}
	}

	/**
	 * Implementation of {@link LineChartOnValueSelectListener} to handle point selection.
	 */
	private class ValueTouchListener implements LineChartOnValueSelectListener {
		@Override
		public void onValueSelected(int lineIndex, int pointIndex, PointValue value) {
			Toast.makeText(getActivity(), "Selected: " + value, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onValueDeselected() {}
	}

	/**
	 * Fetches weekly data from the database and updates the chart.
	 */
	public void showWeeklyChart() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd", Locale.getDefault());
		ArrayList<Float> weekly = db.getWeekly();

		List<PointValue> values = new ArrayList<>();
		List<String> axisLabels = new ArrayList<>();

		if (weekly != null && !weekly.isEmpty()) {
			for (int i = 0; i < Math.min(7, weekly.size()); i++) {
				String date = dateFormat.format(calendar.getTime());
				values.add(new PointValue(i, weekly.get(i)));
				axisLabels.add(date);
				calendar.add(Calendar.DATE, -1);
			}
		}

		Collections.reverse(values);
		Collections.reverse(axisLabels);

		updateChartDisplay(values, "Weekly History", ContextCompat.getColor(requireContext(), R.color.neon_blue));
	}

	/**
	 * Fetches daily data from the database and updates the chart.
	 */
	@RequiresApi(api = Build.VERSION_CODES.O)
	public void showDailyChart() {
		ArrayList<HashMap<String, Object>> daily = db.getDaily();
		List<PointValue> values = new ArrayList<>();

		if (daily != null) {
			for (int i = 0; i < daily.size(); i++) {
				try {
					Object valObj = daily.get(i).get("value");
					float value = valObj != null ? Float.parseFloat(valObj.toString()) : 0f;
					values.add(new PointValue(i, value));
				} catch (Exception e) {
					values.add(new PointValue(i, 0f));
				}
			}
		}

		updateChartDisplay(values, "Daily Usage", ContextCompat.getColor(requireContext(), R.color.neon_orange));
	}

	/**
	 * Updates the chart display with the provided points and styling.
	 *
	 * @param points      List of PointValue to display.
	 * @param axisYName   Name for the Y-axis.
	 * @param strokeColor Color for the line.
	 */
	private void updateChartDisplay(List<PointValue> points, String axisYName, int strokeColor) {
		if (points.isEmpty()) {
			points.add(new PointValue(0, 0));
		}

		Line line = new Line(points)
				.setColor(strokeColor)
				.setShape(ValueShape.CIRCLE)
				.setCubic(true)
				.setFilled(true)
				.setHasLines(true)
				.setHasPoints(true);

		List<Line> lines = new ArrayList<>();
		lines.add(line);

		mainChartData = new LineChartData(lines);

		Axis axisX = new Axis().setName("Time Sequence").setTextColor(Color.GRAY);
		Axis axisY = new Axis().setName(axisYName).setHasLines(true).setTextColor(Color.GRAY).setLineColor(Color.parseColor("#33FFFFFF"));
		mainChartData.setAxisXBottom(axisX);
		mainChartData.setAxisYLeft(axisY);

		if (mainChart != null) {
			mainChart.setLineChartData(mainChartData);
			mainChart.setBackgroundColor(Color.TRANSPARENT);
		}
		resetViewport(points.size());
	}

	/**
	 * Resets the chart viewport based on the number of points and their values.
	 *
	 * @param maxPoints The number of points in the data.
	 */
	private void resetViewport(int maxPoints) {
		float maxVal = 100f;
		if (mainChartData != null) {
			for (Line l : mainChartData.getLines()) {
				for (PointValue pv : l.getValues()) {
					if (pv.getY() > maxVal) maxVal = pv.getY() + 15f;
				}
			}
		}

		if (mainChart != null) {
			final Viewport v = new Viewport(mainChart.getMaximumViewport());
			v.bottom = 0;
			v.top = maxVal;
			v.left = 0;
			v.right = Math.max(1, maxPoints - 1);

			mainChart.setMaximumViewport(v);
			mainChart.setCurrentViewport(v);
		}
	}

	/**
	 * Sets the data for the fragment and refreshes the daily chart.
	 *
	 * @param data The data to set (currently expects "set" to trigger refresh).
	 */
	public void setData(String data) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			showDailyChart();
		}
	}

	/**
	 * Updates activity-wide font settings.
	 *
	 * @param fontName Name of the font to apply.
	 */
	public void _changeActivityFont(final String fontName) {
		this.fontPath = "fonts/".concat(fontName.concat(".ttf"));
		overrideFonts(getView());
	}

	/**
	 * Recursively applies custom fonts to all supported views in the hierarchy.
	 *
	 * @param v The root view to start applying fonts from.
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
			// Quiet fail fallback if asset files missing
		}
	}
}
