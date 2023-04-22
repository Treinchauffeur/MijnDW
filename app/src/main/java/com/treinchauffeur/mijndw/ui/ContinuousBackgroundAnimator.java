package com.treinchauffeur.mijndw.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.treinchauffeur.mijndw.R;
import com.treinchauffeur.mijndw.misc.MiscTools;
import com.treinchauffeur.mijndw.misc.Settings;

public class ContinuousBackgroundAnimator {

    private final ImageView bgTrainIcm, bgTrainVelaro;
    protected final ViewGroup rootView;
    protected final Context context;
    boolean icmIsMoving = false, icmIsLeft = true;
    boolean velaroIsMoving = false, velaroIsLeft = false;

    /**
     * Handles all the continuous background image animations.
     *
     * @param rootView the root view to fetch the individual imageviews from.
     * @param context  for sending toasts and such.
     */
    public ContinuousBackgroundAnimator(ViewGroup rootView, Context context) {
        this.rootView = rootView;
        this.context = context;
        bgTrainIcm = rootView.findViewById(R.id.bgImageTrainICM);
        bgTrainVelaro = rootView.findViewById(R.id.bgImageTrainVelaro);
    }

    /**
     * When animations are not ran due to power management restrictions or other reasons,
     * set a defined place for the trains to remain at.
     */
    public void setStaticLocations() {
        bgTrainVelaro.setX(2900f);
        bgTrainIcm.setX(950f);
    }

    /**
     * Starts the animations for both the top and bottom trains.
     * Sets a persistent runnable to keep moving them back and forth
     *
     * @param initial to set whether this is the initial movement or the app is resumed.
     */
    public void startAnimations(boolean initial) {
        Handler trainMoveHandler = new Handler();
        Runnable trainMover = new Runnable() {
            @Override
            public void run() {
                if (MiscTools.generateRandomNumber(0, 1) == 1) {
                    if (!velaroIsMoving) moveVelaro(bgTrainVelaro);
                    else if (!icmIsMoving) moveIcm(bgTrainIcm);
                } else {
                    if (!icmIsMoving) moveIcm(bgTrainIcm);
                    else if (!velaroIsMoving) moveVelaro(bgTrainVelaro);
                }
                trainMoveHandler.postDelayed(this, MiscTools.generateRandomNumber(1000, 3000));
            }
        };

        bgTrainVelaro.postDelayed(trainMover, MiscTools.generateRandomNumber(1000, 3000));

        //Start animating persistently-moving trains.
        if (initial) {
            moveVelaro(bgTrainVelaro);
            moveIcm(bgTrainIcm);
            spinImage(rootView.findViewById(R.id.bgImageClock));
        }
    }

    /**
     * Spins the clock that's situated on the background by one rotation x seconds (x defined in Settings).
     *
     * @param toSpin sets the image to spin, so it can be applied to any imageview
     */
    private void spinImage(ImageView toSpin) {
        RotateAnimation rotateAnimation = new RotateAnimation(0, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);

        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(Settings.CLOCK_SPIN_DURATION);
        rotateAnimation.setRepeatCount(Animation.INFINITE);

        toSpin.startAnimation(rotateAnimation);
    }

    /**
     * Method for moving the velaro train from either left to right or right to left.
     * This is where we handle which location the train is currently at and whether it's currently moving.
     *
     * @param bgImageTrainVelaro the imageview to move
     */
    private void moveVelaro(ImageView bgImageTrainVelaro) {
        if (velaroIsMoving) return;

        if (!velaroIsLeft) {
            @SuppressLint("Recycle") ObjectAnimator moveVelaroToLeft = ObjectAnimator.ofFloat(bgImageTrainVelaro, "translationX", 0f);
            moveVelaroToLeft.setDuration(MiscTools.generateRandomNumber(Settings.velaroMinSpeed, Settings.velaroMaxSpeed));
            moveVelaroToLeft.setInterpolator(new LinearInterpolator());
            moveVelaroToLeft.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    velaroIsMoving = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    velaroIsMoving = false;
                    velaroIsLeft = true;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    velaroIsMoving = false;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            bgImageTrainVelaro.setX(5000f);
            Runnable velaroRunnable = moveVelaroToLeft::start;
            bgImageTrainVelaro.postDelayed(velaroRunnable, MiscTools.generateRandomNumber(0, 1000));
        } else {
            @SuppressLint("Recycle") ObjectAnimator moveVelaroToLeft = ObjectAnimator.ofFloat(bgImageTrainVelaro, "translationX", 5000f);
            moveVelaroToLeft.setDuration(MiscTools.generateRandomNumber(Settings.velaroMinSpeed, Settings.velaroMaxSpeed));
            moveVelaroToLeft.setInterpolator(new LinearInterpolator());
            moveVelaroToLeft.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    velaroIsMoving = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    velaroIsMoving = false;
                    velaroIsLeft = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    velaroIsMoving = false;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            bgImageTrainVelaro.setX(-5000f);
            Runnable velaroRunnable = moveVelaroToLeft::start;
            bgImageTrainVelaro.postDelayed(velaroRunnable, MiscTools.generateRandomNumber(0, 1000));
        }
    }

    /**
     * Method for moving the ICM train from either left to right or right to left.
     * This is where we handle which location the train is currently at and whether it's currently moving.
     *
     * @param bgImageTrainICM the imageview to move
     */
    private void moveIcm(ImageView bgImageTrainICM) {
        if (icmIsMoving) return;

        if (!icmIsLeft) {
            @SuppressLint("Recycle") ObjectAnimator moveIcmToLeft = ObjectAnimator.ofFloat(bgImageTrainICM, "translationX", 0f);
            moveIcmToLeft.setDuration(MiscTools.generateRandomNumber(Settings.icmMinSpeed, Settings.icmMaxSpeed));
            moveIcmToLeft.setInterpolator(new LinearInterpolator());
            moveIcmToLeft.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    icmIsMoving = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    icmIsMoving = false;
                    icmIsLeft = true;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    velaroIsMoving = false;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            bgImageTrainICM.setX(5000f);
            Runnable velaroRunnable = moveIcmToLeft::start;
            bgImageTrainICM.postDelayed(velaroRunnable, MiscTools.generateRandomNumber(0, 1000));
        } else {
            @SuppressLint("Recycle") ObjectAnimator moveIcmToLeft = ObjectAnimator.ofFloat(bgImageTrainICM, "translationX", 5000f);
            moveIcmToLeft.setDuration(MiscTools.generateRandomNumber(Settings.icmMinSpeed, Settings.icmMaxSpeed));
            moveIcmToLeft.setInterpolator(new LinearInterpolator());
            moveIcmToLeft.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    icmIsMoving = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    icmIsMoving = false;
                    icmIsLeft = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    icmIsMoving = false;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            bgImageTrainICM.setX(-5000f);
            Runnable icmRunnable = moveIcmToLeft::start;
            bgImageTrainICM.postDelayed(icmRunnable, MiscTools.generateRandomNumber(0, 1000));
        }
    }
}
