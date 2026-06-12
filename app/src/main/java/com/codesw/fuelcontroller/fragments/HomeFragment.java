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
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import androidx.viewpager.widget.ViewPager;
import com.codesw.fuelcontroller.R;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import androidx.viewpager.widget.ViewPager.OnAdapterChangeListener;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.DialogFragment;


public class HomeFragment extends  Fragment  {


    private static final String TAG = "HomeFragmentActivity";
    private String fontName = "";
    private final String typeace = "";

    private TabLayout tablayout1;
    private ViewPager viewpager1;
    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.fragment_home, _container, false);
        initialize(_savedInstanceState, _view);
        initializeLogic();
        return _view;
    }

    private void initialize(Bundle _savedInstanceState, View _view) {

        tablayout1 = _view.findViewById(R.id.tablayout1);
        viewpager1 = _view.findViewById(R.id.viewpager1);
    }

    private void initializeLogic() {
        viewpager1.setAdapter(new MyFragmentAdapter(getContext(), getChildFragmentManager(), 2));
        tablayout1.setupWithViewPager(viewpager1);
        changeTabsFont(tablayout1);
        _changeActivityFont("greenscr");


    }



    public class MyFragmentAdapter extends FragmentStatePagerAdapter {
        Context context;
        int tabCount;

        public MyFragmentAdapter(Context context, FragmentManager fm, int tabCount) {
            super(fm);
            this.context = context;
            this.tabCount = tabCount;
        }

        @Override
        public int getCount(){
            return tabCount;
        }

        @Override
        public CharSequence getPageTitle(int _position) {
            if (_position == 0) {
                return "Fuel";
            }
            else {
                    return "Statistica";
            }
            //return null;
        }

        @Override
        public Fragment getItem(int _position) {
            if (_position == 0) {
                return new Frag1FragmentActivity();
                //return new HomeFragmentActivity();
            }
            else {
                return new Frag2FragmentActivity();

            }
            //return null;
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
    public void changeTabsFont(TabLayout tabLayout){
        ViewGroup vg = (ViewGroup) tabLayout.getChildAt(0);
        int tabsCount = vg.getChildCount();
        for (int j = 0; j < tabsCount; j++) {
            ViewGroup vgTab = (ViewGroup) vg.getChildAt(j);
            int tabChildsCount = vgTab.getChildCount();
            for (int i = 0; i < tabChildsCount; i++) {
                View tabViewChild = vgTab.getChildAt(i);
                if (tabViewChild instanceof TextView) {
                    AssetManager mgr = getContext().getAssets();
                    Typeface tf = Typeface.createFromAsset(mgr, "fonts/greenscr.ttf");//Font file in /assets
                    ((TextView) tabViewChild).setTypeface(tf);
                }
            }
        }
    }


    @Deprecated
    public void showMessage(String _s) {
        Toast.makeText(getContext(), _s, Toast.LENGTH_SHORT).show();
    }

    @Deprecated
    public int getLocationX(View _v) {
        int[] _location = new int[2];
        _v.getLocationInWindow(_location);
        return _location[0];
    }

    @Deprecated
    public int getLocationY(View _v) {
        int[] _location = new int[2];
        _v.getLocationInWindow(_location);
        return _location[1];
    }

    @Deprecated
    public int getRandom(int _min, int _max) {
        Random random = new Random();
        return random.nextInt(_max - _min + 1) + _min;
    }

    @Deprecated
    public ArrayList<Double> getCheckedItemPositionsToArray(ListView _list) {
        ArrayList<Double> _result = new ArrayList<Double>();
        SparseBooleanArray _arr = _list.getCheckedItemPositions();
        for (int _iIdx = 0; _iIdx < _arr.size(); _iIdx++) {
            if (_arr.valueAt(_iIdx))
                _result.add((double)_arr.keyAt(_iIdx));
        }
        return _result;
    }

    @Deprecated
    public float getDip(int _input){
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, _input, getResources().getDisplayMetrics());
    }

    @Deprecated
    public int getDisplayWidthPixels(){
        return getResources().getDisplayMetrics().widthPixels;
    }

    @Deprecated
    public int getDisplayHeightPixels(){
        return getResources().getDisplayMetrics().heightPixels;
    }
    public void setProgress(String data){
        Log.d(TAG, "setProgress: "+data);
        if(viewpager1.getCurrentItem() == 0){
            // TODO: NOTE: Child or Parent? = Child
            Frag1FragmentActivity fragment = (Frag1FragmentActivity)getChildFragmentManager().getFragments().get(0);
            fragment.setProgress(data);
        } else if (viewpager1.getCurrentItem()==1) {

            Frag2FragmentActivity fragment1 = (Frag2FragmentActivity)getChildFragmentManager().getFragments().get(1);
            fragment1.setData("set");
        }

    }
}
