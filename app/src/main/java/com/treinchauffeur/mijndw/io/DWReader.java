package com.treinchauffeur.mijndw.io;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.treinchauffeur.mijndw.R;
import com.treinchauffeur.mijndw.misc.Utils;
import com.treinchauffeur.mijndw.obj.Shift;
import com.treinchauffeur.mijndw.obj.Staff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.Description;
import biweekly.property.Summary;
import biweekly.util.Duration;


/**
 * @author Leonk A basic bot to load workdays from a DW (donderdagse week)
 * weekly planning .txt file into something that's actually useful like
 * Google Calendar. ACTUALLY just for now we're gonna generate a .ics
 * file for manual importing.
 */

public class DWReader {

    private static String[] fileContents = new String[13];
    private static final String TAG = "Run";
    public static Uri toRead;
    @SuppressLint("SimpleDateFormat")
    static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    public static int staffNumber = -1, weekNumber = -1, yearNumber = -1;

    public static ArrayList<Shift> dw = new ArrayList<>();
    private static final Staff staff = new Staff();
    public Context context;

    public DWReader(Context context) {
        this.context = context;
    }

    /**
     * Initiates the conversion of the DW file & acting as a staging method.
     *
     * @param c   context
     * @param uri user-supplied file
     */
    public void startConversion(Context c, Uri uri) {
        dw.clear();
        Log.d(TAG, "started reading file: ");
        toRead = uri;

        if (toRead == null) {
            Log.e(TAG, "No valid DW files were present to read.");
            return;
        }

        Log.d(TAG, "Using file: " + uri.getPath());

        if (readFile(uri, c))
            processFile(context);
        else {
            Toast.makeText(c, "Fout in het laden van bestand. Is dit wel een DW bestand?", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Reads the given file Uri & saves the contents to a String array.
     *
     * @param uri User-supplied file
     * @param c   context to use to create inputstream
     */
    private static boolean readFile(Uri uri, Context c) {
        try {
            InputStream inputStream = c.getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            fileContents[0] = reader.readLine(); //first line: Donderdagse Week van WW-YYYY

            if (fileContents[0] == null)
                return false;
            else if (!fileContents[0].contains("Donderdagse Week van"))
                return false;

            fileContents[1] = reader.readLine(); // Empty for formatting
            fileContents[2] = reader.readLine(); //Staff number + name
            fileContents[3] = reader.readLine(); //Empty again
            fileContents[4] = reader.readLine(); //Table titles
            fileContents[5] = reader.readLine(); //Table formatting
            fileContents[6] = reader.readLine(); //Monday
            fileContents[7] = reader.readLine(); //Tuesday
            fileContents[8] = reader.readLine(); //Wednesday
            fileContents[9] = reader.readLine(); //Thursday
            fileContents[10] = reader.readLine(); //Friday
            fileContents[11] = reader.readLine(); //Saturday
            fileContents[12] = reader.readLine(); //Sunday

            reader.close();

            if (fileContents[12].startsWith("zo"))
                return true;
        } catch (IOException e) {
            Log.e(TAG, "readFile: ", e);
        }
        return false;
    }

    /**
     * Read the DW file & save to raw data
     * <p>
     * Line indexing: 0=days in text, 1=date in 31-12 format, 2=shift
     * number/letters, 3=starttime formatted to 24:60, 4=endtime formatted
     * to 24:60, 5=profession, 6=location
     *
     * @param context app context used for sending toasts
     */
    @SuppressWarnings("deprecation")
    private static void processFile(Context context) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        java.util.Date date1;
        java.util.Date date2;
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        long diff;
        long diffMinutes;
        long diffHours;
        long minutes;
        try {
            dw.clear();

            // First line - weeknumber + year
            String startingLine = fileContents[0].replaceAll("\\s+", " ");
            String[] dateYear = startingLine.split("-");
            String weekNrString = dateYear[0].substring(Math.max(dateYear[0].length() - 2, 0));
            if (weekNrString.startsWith(" "))
                weekNumber = Integer.parseInt(Character.toString(weekNrString.charAt(1)));
            else
                weekNumber = Integer.parseInt(dateYear[0].substring(Math.max(dateYear[0].length() - 2, 0)));
            yearNumber = Integer.parseInt(dateYear[1]);

            // Second line - empty for formatting purposes
            //String emptyLineOne = fileContents[1].replaceAll("\\s+", " ");

            // Third line - staff number
            String staffNumberLine = fileContents[2].replaceAll("\\s+", " ");
            staffNumber = Integer.parseInt(staffNumberLine.split(" ")[0]);

            staff.setStaffNumber(staffNumber);
            staff.setStaffName(staffNumberLine.split(" ")[1] + ". " + staffNumberLine.split(" ")[2]);

            Log.d(TAG, "DW FOR " + staffNumberLine + ":");
            Log.d(TAG, "//////////// START WEEK " + weekNumber + " OF " + yearNumber + " ////////////");

            for (int dayLine = 6; dayLine < 13; dayLine++) {
                Shift shift = new Shift();
                shift.setStaff(staff);
                String lineToRead = fileContents[dayLine].replaceAll("\\s+", " ");
                shift.setRawString(lineToRead);
                String modifier = "-1";
                String[] dayArray = lineToRead.split(" ");

                //Check if we have modifiers, if yes save them but remove from line.
                //They'll mess things up, like, big time.
                if (isShiftModifier(dayArray[2])) {
                    modifier = dayArray[2];
                    for (int i = 2; i < dayArray.length - 1; i++) {
                        dayArray[i] = dayArray[i + 1];
                    }
                }

                String profession = "";
                String location = "";
                if (dayArray.length > 6) { // Things like CURS don't have a function etc listed
                    profession = dayArray[5].substring(0, 1).toUpperCase()
                            + dayArray[5].substring(1).toLowerCase();
                    location = dayArray[6].substring(0, 1).toUpperCase()
                            + dayArray[6].substring(1).toLowerCase();
                }

                String shiftNumber = dayArray[2];
                String startTime;
                String endTime;

                //Check if this is a day off work
                if (isRestingday(shiftNumber)) {
                    Log.d(TAG, "Staff " + shift.getStaff().getStaffName() + " is free on " + dayArray[1] + ".");
                    continue;
                } else {
                    startTime = dayArray[3];
                    endTime = dayArray[4];
                }

                int month = Integer.parseInt(dayArray[1].split("-")[1]);
                int day = Integer.parseInt(dayArray[1].split("-")[0]);

                String startDate;
                if ((weekNumber == 52 || weekNumber == 53) && month == 1) {// we passed newyear's
                    startDate = "" + day + "-" + month + "-" + yearNumber + 1 + " " + startTime
                            + "";
                } else {
                    startDate = "" + day + "-" + month + "-" + yearNumber + " " + startTime
                            + "";
                }
                date1 = format.parse(startTime);
                date2 = format.parse(endTime);
                assert date1 != null;
                cal1.setTime(date1);
                assert date2 != null;
                cal2.setTime(date2);
                if (cal2.getTimeInMillis() - cal1.getTimeInMillis() < 0)
                    cal2.add(Calendar.DATE, 1);

                diff = cal2.getTimeInMillis() - cal1.getTimeInMillis();
                diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                diffHours = TimeUnit.MILLISECONDS.toHours(diff);
                minutes = diffMinutes - (diffHours * 60);

                java.util.Date shiftStartDate = sdf.parse(startDate);
                assert shiftStartDate != null;
                long StartMillis = shiftStartDate.getTime();
                long EndMillis = shiftStartDate.getTime() + diff;

                shift.setShiftNumber(shiftNumber);
                shift.setShiftNumberModifier(modifier);
                shift.setLocation(location);
                shift.setStartTime(shiftStartDate);
                shift.setProfession(profession);
                shift.setWeekNumber(weekNumber);
                shift.setYearNumber(yearNumber);
                shift.setLengthHours((int) diffHours);
                shift.setLengthMinutes((int) minutes);
                shift.setStartMillis(StartMillis);
                shift.setEndMillis(EndMillis);

                dw.add(shift);

                Log.d(TAG,
                        "staff " + shift.getStaff().getStaffName() + " runs shift " + shift.getLocation()
                                + shift.getShiftNumber() + " at " + shift.getStartTime()
                                + " with a shift length of " + shift.getLengthHours() + " hours and "
                                + shift.getLengthMinutes() + " minutes.");
            }

            Log.d(TAG, "//////////// END OF WEEK ////////////");

            Snackbar successBar = Snackbar.make(((Activity) context).findViewById(R.id.mainView), "DW geladen: week " +
                    weekNumber + " van " + yearNumber, Snackbar.LENGTH_LONG);
            successBar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);
            successBar.show();

            Log.d(TAG, "Finished scanning " + toRead.getPath());
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    /**
     * Puts all the shift details from their respective objects in to an iCalendar String using biWeekly.
     *
     * @return the full, completed iCalendar String.
     */
    @SuppressLint("SimpleDateFormat")
    public String getCalendarICS() {
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
        boolean displayModifiers = prefs.getBoolean("displayModifiers", true);
        boolean fullDaysOnly = prefs.getBoolean("fullDaysOnly", false);
        boolean displayProfession = prefs.getBoolean("displayProfession", true);

        String ics = "";
        ICalendar iCal = new ICalendar();

        for (Shift shift : dw) {
            if (shift.getShiftNumber().equals("!!!")) continue;

            VEvent event = new VEvent();
            String summaryString;
            if (displayModifiers)
                summaryString = shift.getShiftNumberModifier().equals("-1") ?
                        shift.getLocation() + " " + shift.getShiftNumber() :
                        shift.getLocation() + " " + shift.getShiftNumberModifier() + shift.getShiftNumber();
            else summaryString = shift.getLocation() + " " + shift.getShiftNumber();
            if (fullDaysOnly)
                summaryString += " " + new SimpleDateFormat("HH:mm").format(shift.getStartMillis()) + " - " +
                        new SimpleDateFormat("HH:mm").format(shift.getEndMillis());

            summaryString = displayProfession ? (shift.getProfession() + " " + summaryString) : summaryString;
            Summary summary = event.setSummary(summaryString);

            Description description = event.setDescription(shift.getRawString());
            description.setLanguage("nl");
            summary.setLanguage("nl");

            Date start = shift.getStartTime();
            if (fullDaysOnly) event.setDateStart(Utils.atStartOfDay(start));
            else event.setDateStart(start);

            Duration duration = fullDaysOnly ?
                    new Duration.Builder().days(1).build() :
                    new Duration.Builder().hours(shift.getLengthHours()).minutes(shift.getLengthMinutes()).build();
            event.setDuration(duration);

            iCal.addEvent(event);
        }

        ics += Biweekly.write(iCal).go();
        return ics;

    }

    /**
     * @return the entire original DW file contents in form of a String.
     */
    public String fullFileString() {
        StringBuilder str = new StringBuilder();
        for (String fileContent : fileContents) {
            str.append(fileContent).append("\n");
        }
        return str.toString();
    }

    /**
     * Determines whether a symbol or multiple symbols are shift modifiers.
     * This is necessary in order to properly read each day of the week correctly.
     *
     * @param s the given symbol(s).
     * @return whether it is a shift modifier or not.
     * <p>
     * Known modifiers for Regio Twente:
     * # Guaranteed
     * > Pupil
     * E Extra
     * *
     * !
     * @
     */
    public static boolean isShiftModifier(String s) {
        switch (s) {
            case "!":
            case "@":
            case ">":
            case "<":
            case "*":
            case "?":
            case "E":
            case "#":
            case "$":
            case "%":
            case "P":
            case "P!":
            case "P@":
            case "P>":
            case "P<":
            case "P*":
            case "P?":
            case "PE":
            case "P#":
            case "P$":
            case "P%":
                return true;
            default:
                return false;

        }
    }

    private static boolean isRestingday(String shiftNumber) {
        switch (shiftNumber.toLowerCase()) {
            case "r":
            case "streepjesdag":
            case "vl":
            case "gvl":
                return true;
            default:
                return false;
        }
    }

    public void resetData() {
        fileContents = new String[13];
        dw = new ArrayList<>();
    }
}
