package com.treinchauffeur.mijndw.ui;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.cardview.widget.CardView;

import com.treinchauffeur.mijndw.R;
import com.treinchauffeur.mijndw.misc.MiscTools;

public class StartupBackgroundAnimator {

    private final ViewGroup rootView;
    protected final Context context;
    private boolean finished = true;

    /**
     * Handles all the startup background image animations only.
     *
     * @param rootView the root view to fetch the individual imageviews from.
     * @param context  for sending toasts and such.
     */
    public StartupBackgroundAnimator(ViewGroup rootView, Context context) {
        this.rootView = rootView;
        this.context = context;
    }

    /**
     * Starts the animations for when the side trains should be coming in the screen.
     * Also hints to the user that the infoCard can be dismissed by flashing it a couple of times.
     */
    public void startAnimations() {
        finished = false;
        ImageView bgImageTrainVIRM = rootView.findViewById(R.id.bgImageTrainVIRM);
        ImageView bgImageTrainLoc = rootView.findViewById(R.id.bgImageTrainLoc);
        CardView infoCard = rootView.findViewById(R.id.infoCard);
        int minStartDelay = 0, maxStartDelay = 3000;
        int minTrainPassTime = 6000, maxTrainPassTime = 12000;

        @SuppressLint("Recycle") ObjectAnimator animationVIRM = ObjectAnimator.ofFloat(bgImageTrainVIRM, "translationX", 0f);
        animationVIRM.setDuration(MiscTools.generateRandomNumber(minTrainPassTime, maxTrainPassTime));
        animationVIRM.setInterpolator(new AccelerateDecelerateInterpolator());
        bgImageTrainVIRM.setX(-1500f);
        Runnable virmRunnable = animationVIRM::start;
        bgImageTrainVIRM.postDelayed(virmRunnable, MiscTools.generateRandomNumber(minStartDelay, maxStartDelay));

        @SuppressLint("Recycle") ObjectAnimator animationLoc = ObjectAnimator.ofFloat(bgImageTrainLoc, "translationX", 0f);
        animationLoc.setDuration(MiscTools.generateRandomNumber(minTrainPassTime, maxTrainPassTime));
        animationLoc.setInterpolator(new AccelerateDecelerateInterpolator());
        bgImageTrainLoc.setX(1500f);
        Runnable locRunnable = animationLoc::start;
        bgImageTrainLoc.postDelayed(locRunnable, MiscTools.generateRandomNumber(minStartDelay, maxStartDelay));

        if (infoCard.getVisibility() == View.VISIBLE) {
            final int[] i = {0};
            int max = 5;
            Runnable animationRunnable = new Runnable() {
                @Override
                public void run() {
                    if (infoCard.getVisibility() == View.GONE)
                        return;

                    final long now = SystemClock.uptimeMillis();
                    final MotionEvent pressEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0);
                    infoCard.dispatchTouchEvent(pressEvent);
                    if (i[0] < max) {
                        i[0]++;
                        infoCard.postDelayed(this, 5000);
                    } else {
                        finished = true;
                    }

                    new Handler().postDelayed(() -> {
                        final long now1 = SystemClock.uptimeMillis();
                        final MotionEvent cancelEvent = MotionEvent.obtain(now1, now1, MotionEvent.ACTION_CANCEL, 0, 0, 0);
                        infoCard.dispatchTouchEvent(cancelEvent);
                    }, 100);
                }
            };
            infoCard.postDelayed(animationRunnable, 5000);
        }
    }

    /**
     * Checks whether the animations can start running.
     *
     * @return whether the animations are finished or not running at all.
     */
    public boolean isFinished() {
        return finished;
    }
}
