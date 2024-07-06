package com.treinchauffeur.mijndw.misc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.Calendar;
import java.util.Date;

/**
 * Let's do some fun things & clown around a bit.
 */
public class Circus {

    public static final String TAG = "Circus";
    protected final Context context;
    private final MaterialToolbar toolbar;

    public Circus (Context context, MaterialToolbar toolbar) {
        this.context = context;
        this.toolbar = toolbar;
    }

    public void startTheShow() {
        checkForHolidays();
    }

    /**
     * Initiates the checks for several holidays.
     */
    private void checkForHolidays() {
        easter();
        christmas();
        halloween();
    }

    /**
     * Displays a custom message on the Toolbar.
     * @param message the message to be displayed.
     * <p>
     * In order for the message to be displayed properly, we need to reduce the text size to avoid
     * clipping or wrapping.
     */
    private void toolbarMessage(String message) {
        ((TextView) toolbar.getChildAt(0)).setText(message);
        ((TextView) toolbar.getChildAt(0)).setTextSize(30);
    }

    /**
     * Adds a custom emoji on the Toolbar title.
     * @param emoji the emoji to be displayed.
     */
    @SuppressLint("SetTextI18n")
    private void toolbarEmoji(String emoji) {
        ((TextView) toolbar.getChildAt(0)).setText(((TextView)
                toolbar.getChildAt(0)).getText() + " " + emoji);
    }

    /**
     * Checks whether it's (near) Christmas or new year's & displays a message accordingly.
     */
    public void christmas() {
        Date currentDate = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 20);
        Date startDate = calendar.getTime();

        Calendar calendar2 = Calendar.getInstance();
        calendar2.add(Calendar.YEAR, 1);
        calendar2.set(Calendar.MONTH, Calendar.JANUARY);
        calendar2.set(Calendar.DAY_OF_MONTH, 2);
        Date endDate = calendar2.getTime();

        if(!currentDate.before(startDate) && !currentDate.after(endDate)) {
            Log.d(TAG, "Merry Christmas!");
            toolbarMessage("Fijne feestdagen!" + " \uD83C\uDF85\uD83C\uDF84");
        }
    }

    /**
     * Checks whether it's (near) Easter & displays a message accordingly.
     */
    public void easter() {
        Date currentDate = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, Calendar.APRIL);
        calendar.set(Calendar.DAY_OF_MONTH, 10);
        Date startDate = calendar.getTime();

        calendar.set(Calendar.MONTH, Calendar.APRIL);
        calendar.set(Calendar.DAY_OF_MONTH, 22);
        Date endDate = calendar.getTime();

        if(!currentDate.before(startDate) && !currentDate.after(endDate)) {
            Log.d(TAG, "Happy Easter!");
            toolbarMessage("Vrolijk pasen!" + " \uD83D\uDC23");
        }
    }

    /**
     * Checks whether it's (near) Easter & displays a message accordingly.
     */
    public void halloween() {
        Date currentDate = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, Calendar.OCTOBER);
        calendar.set(Calendar.DAY_OF_MONTH, 25);
        Date startDate = calendar.getTime();

        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 2);
        Date endDate = calendar.getTime();

        if(!currentDate.before(startDate) && !currentDate.after(endDate)) {
            Log.d(TAG, "Happy Halloween!");
            toolbarEmoji("\uD83C\uDF83");
        }
    }
}
