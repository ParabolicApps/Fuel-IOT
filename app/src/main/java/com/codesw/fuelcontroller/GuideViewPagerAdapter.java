package com.codesw.fuelcontroller;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.widget.FrameLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import androidx.viewpager2.widget.ViewPager2;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

class GuideViewPagerAdapter extends FragmentStateAdapter {
    private List<View> views;
	
	private Fragment fragment;

    GuideViewPagerAdapter(FragmentManager frag, Lifecycle lifec, List<View> views) {
        super(frag, lifec);
        this.views = views;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        //FrameLayout container = new FrameLayout(context);
        //container.addView();
        return new GuideFragment(views.get(position));
    }

    @Override
    public int getItemCount() {
        return views.size();
    }

    public static class GuideFragment extends Fragment {
        View view;

        GuideFragment(View view) {
            this.view = view;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return view;
        }
    }
}


