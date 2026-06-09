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
import android.widget.TextView;
import android.graphics.Typeface;
import me.itangqi.library.*;
import com.codesw.fuelcontroller.R;
import com.codesw.fuelcontroller.utils.SQLiteHandler;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;

/**
 * This Fragment Shows Statistics 0f Daily And Weekly Usage
 */
public class Frag2FragmentActivity extends  Fragment  {

	private static final String TAG = "Frag2FragmentActivity";
	private String fontName = "";
	private final String typeace = "";
	
	private LinearLayout linear1;
	private TextView textview1;
	private LinearLayout line_chart2;
	private TextView textview2;
	private LinearLayout line_chart;

	private  LineChart lineChart;
	private  LineChart2 lineChart2;

	private SQLiteHandler db;
	@RequiresApi(api = Build.VERSION_CODES.O)
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.frag2_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		
		linear1 = _view.findViewById(R.id.linear1);
		textview1 = _view.findViewById(R.id.textview1);
		line_chart2 = _view.findViewById(R.id.line_chart2);
		textview2 = _view.findViewById(R.id.textview2);
		line_chart = _view.findViewById(R.id.line_chart);
		db = new SQLiteHandler(getContext());
	}
	
	@RequiresApi(api = Build.VERSION_CODES.O)
	private void initializeLogic() {
		//getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		
		//Chart 1

		//lineChart = new LineChart(getActivity());
		//line_chart.addView(lineChart);
		//lineChart.setLineColor(ContextCompat.getColor(getContext() , R.color.second));
		//lineChart.setPointColor(ContextCompat.getColor(getContext(), R.color.teal_700));


		// Static Representation
		//lineChart.addData(8f, 10f, 5f, 7f, 4f, 6f);
		
		//Chart 2
		//lineChart2 = new LineChart2(getActivity());
		//line_chart2.addView(lineChart2);
		//lineChart2.setLineColor(ContextCompat.getColor(getContext(),R.color.second));
		//lineChart2.setPointColor(ContextCompat.getColor(getContext(), R.color.teal_700));


		showWeeklyChart();
		// Will Show Empty Blank View if Daily Doesn't have any data
		showDailyChart();
		//lineChart2.addData(8f, 10f, 5f, 7f, 4f, 6f);

		
		textview1.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/greenscr.ttf"), 0);
		textview2.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/greenscr.ttf"), 0);
		_changeActivityFont("greenscr");

	}
	public void showWeeklyChart(){
		//get last 7 days

		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd");

		// get weekly from Sqlite, even if theres no usage of a day, the day should return 0 and
		// total item should be 7
		ArrayList<Float> weekly = db.getWeekly();

		for (int i = 0; i < 7; i++) {

			String date = dateFormat.format(calendar.getTime());
			//lineChart2.addData(weekly.get(i), date);
			// With Today Date Also
			calendar.add(Calendar.DATE, -1);
		}

	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	public void showDailyChart(){
		//get hourly data

		ArrayList<HashMap<String, Object>> daily = db.getDaily();

		for (int i = 0; i < daily.size(); i++) {

			String time = daily.get(i).get("hour").toString();
			//lineChart.addData(Float.valueOf(daily.get(i).get("value").toString()), time);

		}
	}
	public void setData(String data){

	}
	
	@Override
	public void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
		
		super.onActivityResult(_requestCode, _resultCode, _data);
		
		switch (_requestCode) {
			
			default:
			break;
		}
	}
	

	public class LineChart extends View {
		
		    final List<ChartData> list = new ArrayList<>();
		    final Rect textBounds = new Rect();
		    float max = -1;
		    int divideItemBy = 2;
		    boolean drawHelperLine = true;
		    float textPadding = 8;
		    float pointRadius = 6;
		    float firstPointPadding = 16;
		    float lastPointPadding = 16;
		
		    final Paint helperPaint = new Paint();
		    final Paint linePaint = new Paint();
		    final Paint pointPaint = new Paint();
		    final Paint textPaint = new Paint();
		
		    public LineChart(Context context) {
			        this(context, null);
			    }
		
		    public LineChart(Context context, AttributeSet attrs) {
			        this(context, attrs, 0);
			    }
		
		    public LineChart(Context context, AttributeSet attrs, int defStyleAttr) {
			        super(context, attrs, defStyleAttr);
			
			        helperPaint.setStyle(Paint.Style.STROKE);
			        helperPaint.setColor(Color.LTGRAY);
			        helperPaint.setStrokeWidth(context.getResources().getDisplayMetrics().density);
			
			        linePaint.setStyle(Paint.Style.STROKE);
			        linePaint.setColor(Color.RED);
			        linePaint.setStrokeWidth(2 * context.getResources().getDisplayMetrics().density);
			
			        pointPaint.setColor(Color.BLUE);
			        pointPaint.setStyle(Paint.Style.FILL);
			
			        textPaint.setColor(Color.BLACK);
			        textPaint.setTextSize(15 * context.getResources().getDisplayMetrics().scaledDensity);
			
			        lastPointPadding *= context.getResources().getDisplayMetrics().density;
			        firstPointPadding *= context.getResources().getDisplayMetrics().density;
			        pointRadius *= context.getResources().getDisplayMetrics().density;
			        textPadding *= context.getResources().getDisplayMetrics().density;
			    }
		
		    public void setFirstPointPadding(float firstPointPadding) {
			        this.firstPointPadding = firstPointPadding;
			        invalidate();
			    }
		
		    public void setLastPointPadding(float lastPointPadding) {
			        this.lastPointPadding = lastPointPadding;
			        invalidate();
			    }
		
		    public void setDivideItemBy(int divideItemBy) {
			        this.divideItemBy = divideItemBy;
			        invalidate();
			    }
		
		    public void setDrawHelperLine(boolean drawHelperLine) {
			        this.drawHelperLine = drawHelperLine;
			        invalidate();
			    }
		
		    public void setMaxValue(float max) {
			        this.max = max;
			        invalidate();
			    }
		
		    public void setPointColor(int color) {
			        pointPaint.setColor(color);
			        invalidate();
			    }
		
		    public void setLineColor(int color) {
			        linePaint.setColor(color);
			        invalidate();
			    }
		
		    public void setHelperLineColor(int color) {
			        helperPaint.setColor(color);
			        invalidate();
			    }
		
		    public void setHelperLineWidth(float width) {
			        helperPaint.setStrokeWidth(width);
			        invalidate();
			    }
		
		    public void setLineWidth(float width) {
			        linePaint.setStrokeWidth(width);
			        invalidate();
			    }
		
		    public void setTypeface(Typeface typeface) {
			        textPaint.setTypeface(typeface);
			        invalidate();
			    }
		
		    public void setTextColor(int color) {
			        textPaint.setColor(color);
			        invalidate();
			    }
		
		    public void setTextSize(float size) {
			        textPaint.setTextSize(size * getContext().getResources().getDisplayMetrics().scaledDensity);
			        invalidate();
			    }
		
		    public void setTextPadding(float padding) {
			        textPadding = padding;
			        invalidate();
			    }
		
		    public void addData(float data, String text){
			        list.add(new ChartData(data, text));
			        invalidate();
			    }
		
		    public void addData(float... values){
			        for (int i = 1; i <= values.length; i++)
			            list.add(new ChartData(values[i - 1], String.valueOf(i)));
			        invalidate();
			    }
		
		    @Override
		    protected void onDraw(Canvas canvas) {
			        super.onDraw(canvas);
			        if (list.size() == 0) return;
			
			        float maxData = max;
			        if (maxData == -1) {
				            maxData = 0;
				            for (ChartData d : list)
				                maxData = Math.max(maxData, d.data);
				        }
			        maxData = Math.max(1, maxData);
			
			
			        float pr = pointRadius * 2;
			        float left = getPaddingLeft() + pr;
			        float top = getPaddingTop() + pr;
			        float right = getMeasuredWidth() - getPaddingRight() - pr;
			        float bottom = getMeasuredHeight() - getPaddingBottom();
			        float height = bottom - top;
			
			        textPaint.getTextBounds("AMIR", 0, 1, textBounds);
			        float height2 = (int) (height - pr - textBounds.height() - textPadding);
			
			        float linePadding = height2 / (maxData + 1);
			
			        if (drawHelperLine) {
				            for (int i = 0; i <= maxData; ) {
					                final float t = top + i * linePadding;
					                if (t > height2) break;
					                canvas.drawLine(left, t, right, t, helperPaint);
					                i += Math.max(divideItemBy, 1);
					            }
				        }
			
			        left += firstPointPadding;
			        right -= lastPointPadding;
			        float width = right - left;
			
			        final int count = list.size();
			        float xPadding = (width - count * pr) / Math.max(count - 1, 1);
			
			        for (int i = 0; i < count; i++){
				            ChartData mData = list.get(i);
				            float y = (maxData - mData.data) * linePadding + top;
				            float x = left + (i * xPadding) + (i * pr);
				
				            if (i+1 < count) {
					                ChartData nextData = list.get(i + 1);
					                float y2 = (maxData - nextData.data) * linePadding + top;
					                float x2 = left + ((i+1) * xPadding) + ((i+1) * pr);
					                canvas.drawLine(x, y, x2, y2, linePaint);
					            }
				            canvas.drawCircle(x, y, pointRadius, pointPaint);
				
				            textPaint.getTextBounds(mData.text, 0, mData.text.length(), textBounds);
				            float tx = x - textBounds.width() / 2f;
				            canvas.drawText(mData.text, tx, bottom - textPadding - textBounds.height(), textPaint);
				        }
			    }
		
		    public class ChartData {
			        float data;
			        String text;
			
			        public ChartData(float data, String text) {
				            this.data = data;
				            this.text = text;
				        }
			    }
	}
	

	public class LineChart2 extends View {
		
		    final List<ChartData> list = new ArrayList<>();
		    final Rect textBounds = new Rect();
		    float max = -1;
		    int divideItemBy = 2;
		    boolean drawHelperLine = true;
		    float textPadding = 8;
		    float pointRadius = 6;
		    float firstPointPadding = 16;
		    float lastPointPadding = 16;
		
		    final Paint helperPaint = new Paint();
		    final Paint fillPaint = new Paint();
		    final Paint linePaint = new Paint();
		    final Paint pointPaint = new Paint();
		    final Paint textPaint = new Paint();
		
		    final Path fillPath = new Path();
		    final Path linePath = new Path();
		    final HashMap<Float, Float> points = new HashMap<>();
		
		    public LineChart2(Context context) {
			        this(context, null);
			    }
		
		    public LineChart2(Context context, AttributeSet attrs) {
			        this(context, attrs, 0);
			    }
		
		    public LineChart2(Context context, AttributeSet attrs, int defStyleAttr) {
			        super(context, attrs, defStyleAttr);
			
			        helperPaint.setStyle(Paint.Style.STROKE);
			        helperPaint.setColor(Color.LTGRAY);
			        helperPaint.setStrokeWidth(context.getResources().getDisplayMetrics().density);
			
			        fillPaint.setStyle(Paint.Style.FILL);
			
			        linePaint.setStyle(Paint.Style.STROKE);
			        linePaint.setColor(Color.RED);
			        linePaint.setStrokeWidth(2 * context.getResources().getDisplayMetrics().density);
			
			        pointPaint.setColor(Color.BLUE);
			        pointPaint.setStyle(Paint.Style.FILL);
			
			        textPaint.setColor(Color.BLACK);
			        textPaint.setTextSize(15 * context.getResources().getDisplayMetrics().scaledDensity);
			
			        lastPointPadding *= context.getResources().getDisplayMetrics().density;
			        firstPointPadding *= context.getResources().getDisplayMetrics().density;
			        pointRadius *= context.getResources().getDisplayMetrics().density;
			        textPadding *= context.getResources().getDisplayMetrics().density;
			    }
		
		    private int lastHeight = -1;
		
		    @Override
		    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			        loadShader();
			    }
		
		    private void loadShader(){
			        int h = getMeasuredHeight();
			        if (lastHeight != h)
			            fillPaint.setShader(new LinearGradient(0, 0, 0, lastHeight = h,
			                    Color.argb(150, Color.red(linePaint.getColor()),
			                            Color.green(linePaint.getColor()),
			                            Color.blue(linePaint.getColor())),
			                    Color.TRANSPARENT, Shader.TileMode.MIRROR));
			    }
		
		    public void setFirstPointPadding(float firstPointPadding) {
			        this.firstPointPadding = firstPointPadding;
			        invalidate();
			    }
		
		    public void setLastPointPadding(float lastPointPadding) {
			        this.lastPointPadding = lastPointPadding;
			        invalidate();
			    }
		
		    public void setDivideItemBy(int divideItemBy) {
			        this.divideItemBy = divideItemBy;
			        invalidate();
			    }
		
		    public void setDrawHelperLine(boolean drawHelperLine) {
			        this.drawHelperLine = drawHelperLine;
			        invalidate();
			    }
		
		    public void setMaxValue(float max) {
			        this.max = max;
			        invalidate();
			    }
		
		    public void setPointColor(int color) {
			        pointPaint.setColor(color);
			        invalidate();
			    }
		
		    public void setLineColor(int color) {
			        linePaint.setColor(color);
			        lastHeight = -1;
			        loadShader();
			        invalidate();
			    }
		
		    public void setHelperLineColor(int color) {
			        helperPaint.setColor(color);
			        invalidate();
			    }
		
		    public void setHelperLineWidth(float width) {
			        helperPaint.setStrokeWidth(width);
			        invalidate();
			    }
		
		    public void setLineWidth(float width) {
			        linePaint.setStrokeWidth(width);
			        invalidate();
			    }
		
		    public void setTypeface(Typeface typeface) {
			        textPaint.setTypeface(typeface);
			        invalidate();
			    }
		
		    public void setTextColor(int color) {
			        textPaint.setColor(color);
			        invalidate();
			    }
		
		    public void setTextSize(float size) {
			        textPaint.setTextSize(size * getContext().getResources().getDisplayMetrics().scaledDensity);
			        invalidate();
			    }
		
		    public void setTextPadding(float padding) {
			        textPadding = padding;
			        invalidate();
			    }
		
		    public void addData(float data, String text) {
			        list.add(new ChartData(data, text));
			        invalidate();
			    }
		
		    public void addData(float... values) {
			        for (int i = 1; i <= values.length; i++)
			            list.add(new ChartData(values[i - 1], String.valueOf(i)));
			        invalidate();
			    }
		
		    @Override
		    protected void onDraw(Canvas canvas) {
			        super.onDraw(canvas);
			        if (list.size() == 0) return;
			
			        float maxData = max;
			        if (maxData == -1) {
				            maxData = 0;
				            for (ChartData d : list)
				                maxData = Math.max(maxData, d.data);
				        }
			        maxData = Math.max(1, maxData);
			
			
			        float pr = pointRadius * 2;
			        float left = getPaddingLeft() + pr;
			        float top = getPaddingTop() + pr;
			        float right = getMeasuredWidth() - getPaddingRight() - pr;
			        float bottom = getMeasuredHeight() - getPaddingBottom();
			        float height = bottom - top;
			
			        textPaint.getTextBounds("AMIR", 0, 1, textBounds);
			        float height2 = (int) (height - pr - textBounds.height() - textPadding);
			
			        float linePadding = height2 / (maxData + 1);
			
			        if (drawHelperLine) {
				            for (int i = 0; i <= maxData; ) {
					                final float t = top + i * linePadding;
					                if (t > height2) break;
					                canvas.drawLine(left, t, right, t, helperPaint);
					                i += Math.max(divideItemBy, 1);
					            }
				        }
			
			        left += firstPointPadding;
			        right -= lastPointPadding;
			        float width = right - left;
			
			        final int count = list.size();
			        float xPadding = (width - count * pr) / Math.max(count - 1, 1);
			
			        fillPath.reset();
			        points.clear();
			
			        for (int i = 0; i < count; i++) {
				            ChartData mData = list.get(i);
				            float y = (maxData - mData.data) * linePadding + top;
				            float x = left + (i * xPadding) + (i * pr);
				
				            if (i == 0)
				                fillPath.moveTo(x, y);
				
				            if (i + 1 < count) {
					                ChartData nextData = list.get(i + 1);
					                float y2 = (maxData - nextData.data) * linePadding + top;
					                float x2 = left + ((i + 1) * xPadding) + ((i + 1) * pr);
					                //canvas.drawLine(x, y, x2, y2, linePaint);
					                fillPath.cubicTo((x + x2)/2, y, (x + x2)/2, y2, x2, y2);
					
					            }
				            points.put(x, y);
				            //canvas.drawCircle(x, y, pointRadius, pointPaint);
				
				            if (i == count - 1) {
					                linePath.set(fillPath);
					                fillPath.lineTo(x, height2 - helperPaint.getStrokeWidth());
					                fillPath.lineTo(left, height2 - helperPaint.getStrokeWidth());
					                fillPath.close();
					            }
				
				            textPaint.getTextBounds(mData.text, 0, mData.text.length(), textBounds);
				            float tx = x - textBounds.width() / 2f;
				            canvas.drawText(mData.text, tx, bottom - textPadding - textBounds.height(), textPaint);
				        }
			
			        canvas.drawPath(fillPath, fillPaint);
			        canvas.drawPath(linePath, linePaint);
			        for (Map.Entry<Float, Float> entry : points.entrySet()) {
				            canvas.drawCircle(entry.getKey(), entry.getValue(), pointRadius, pointPaint);
				        }
			
			    }
		
		    public class ChartData {
			        float data;
			        String text;
			
			        public ChartData(float data, String text) {
				            this.data = data;
				            this.text = text;
				        }
			    }
	}

	
	public void _changeActivityFont (final String _fontname) {
		fontName = "fonts/".concat(_fontname.concat(".ttf"));
		overrideFonts(getContext(),getView()); 
	} 
	private void overrideFonts(final android.content.Context context, final View v) {
		
		try {
			Typeface 
			typeace = Typeface.createFromAsset(getContext().getAssets(), fontName);
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
					((TextView) v).setTypeface(typeace);
				}
				else {
					if ((v instanceof EditText )) {
						((EditText) v).setTypeface(typeace);
					}
					else {
						if ((v instanceof Button)) {
							((Button) v).setTypeface(typeace);
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
	
	
	
}




