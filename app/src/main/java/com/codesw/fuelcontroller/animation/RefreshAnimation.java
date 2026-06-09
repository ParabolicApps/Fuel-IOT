package com.codesw.fuelcontroller.animation;

import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.codesw.fuelcontroller.R;


/**
 * Created by Air on 3/25/2017.
 */

public class RefreshAnimation {
    public static void showRefreshAnimation(ImageView imageView, Context context) {
        Animation animation = AnimationUtils.loadAnimation( context, R.anim.refresh);
        //imageView.setAnimation(rotateAnimation);
        //imageView.startAnimation(rotateAnimation);
        animation.setRepeatMode(Animation.RESTART);
        animation.setRepeatCount(Animation.INFINITE);
        imageView.startAnimation(animation);
    }

    public static void hideRefreshAnimation(ImageView imageView) {
        if (imageView != null) {
            imageView.clearAnimation();
            //imageView.setImageResource(0);
        }
    }
}
