package com.treinchauffeur.mijndw.io;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.treinchauffeur.mijndw.BuildConfig;
import com.treinchauffeur.mijndw.R;
import com.treinchauffeur.mijndw.misc.Logger;
import com.treinchauffeur.mijndw.misc.Settings;
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
 * @author treinchauffeur
 * A basic bot to load workdays from a DW (donderdagse week)
 * weekly planning .txt file into something that's actually useful like
 * Google Calendar.
 */
public class ShiftsFileReader {

    private static String[] fileContents = new String[13];
    private static final String TAG = "Run";
    public static final int REASON_FAILED_READ = 1, REASON_FAILED_PROCESS = 2;
    public static Uri toRead;
    @SuppressLint("SimpleDateFormat")
    static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    public static int staffNumber = -1, weekNumber = -1, yearNumber = -1;

    public static ArrayList<Shift> dw = new ArrayList<>();
    private static final Staff staff = new Staff();
    public Context context;

    public ShiftsFileReader(Context context) {
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
        Logger.debug(TAG, "started reading file: ");
        toRead = uri;

        if (toRead == null) {
            Log.e(TAG, "No valid DW files were present to read.");
            return;
        }

        Logger.debug(TAG, "Using file: " + uri.getPath());

        if (readFile(uri, c)) {
            boolean success = processFile(context);
            if (!success) {
                showErrorDialog(c, REASON_FAILED_PROCESS);
            }
        } else {
            showErrorDialog(c, REASON_FAILED_READ);
        }
    }

    private void showErrorDialog(Context c, int reason) {
        String bodyText = (reason == REASON_FAILED_READ) ? "Er is een fout opgetreden tijdens het inlezen van jouw DW." +
                " Zou je deze willen emailen naar de ontwikkelaar voor analyse zodat deze de app kan verbeteren? :) \n\n" +
                "Je kan eventueel deze mail zelf nog bewerken om je personeelsnummer en andere gevoelige gegevens aan te passen of te verwijderen." :

                "Er is een fout opgetreden tijdens het verwerken van jouw DW. " +
                        " Zou je deze willen emailen naar de ontwikkelaar voor analyse zodat deze de app kan verbeteren? :) \n\n" +
                        "Je kan eventueel deze mail zelf nog bewerken om je personeelsnummer en andere gevoelige gegevens aan te passen of te verwijderen. \n\n" +
                        "Toch is het mogelijk dat bepaalde dagen wel goed verwerkt zijn en deze toe te voegen zijn aan je agenda. Controleer deze goed vóórdat je dit doet!";

        new MaterialAlertDialogBuilder(c, R.style.ThemeOverlay_App_MaterialErrorDialog)
                .setTitle("Fout opgetreden")
                .setIcon(R.drawable.baseline_error_outline_24)
                .setMessage(bodyText)

                .setPositiveButton("DW E-MAILEN", (dialogInterface, i) -> {
                    final Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:"));
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{Settings.DEV_EMAIL});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Fout tijdens " + (reason == REASON_FAILED_READ ? "inlezen" : "verwerken") + " van DW");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Mijn DW versie " + BuildConfig.VERSION_NAME + "\n"
                            + fullFileString() + "\n\n" + "-------- Mocht je nog iets kwijt willen, graag onder deze lijn --------" + "\n\n");
                    c.startActivity(Intent.createChooser(emailIntent, "E-mail versturen.."));
                })
                .setNegativeButton("NEE", (dialogInterface, i) -> dialogInterface.dismiss()).show();
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

            if (fileContents.length >= 13) {
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

            } else {
                return false;
            }

            reader.close();

            if (fileContents[12] == null) return false;

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
    private static boolean processFile(Context context) {
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

            // Third line - staff number
            String staffNumberLine = fileContents[2].replaceAll("\\s+", " ");
            staffNumber = Integer.parseInt(staffNumberLine.split(" ")[0]);

            staff.setStaffNumber(staffNumber);
            staff.setStaffName(staffNumberLine.split(" ")[1] + ". " + staffNumberLine.split(" ")[2]);

            Logger.debug(TAG, "DW FOR " + staffNumberLine + ":");
            Logger.debug(TAG, "//////////// START WEEK " + weekNumber + " OF " + yearNumber + " ////////////");

            //Loop through the actual days of the week for code-efficiency, since all days are created equal.
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
                if (isRestingDay(shiftNumber) || dayArray.length < 4) {
                    Logger.debug(TAG, "Staff " + shift.getStaff().getStaffName() + " is free on " + dayArray[1] + ".");
                    continue;
                } else {
                    startTime = dayArray[3];
                    endTime = dayArray[4];
                }

                int month = Integer.parseInt(dayArray[1].split("-")[1]);
                int day = Integer.parseInt(dayArray[1].split("-")[0]);

                String startDate;
                if ((weekNumber == 52 || weekNumber == 53) && month == 1) {// we passed newyear's
                    startDate = "" + day + "-" + month + "-" + (yearNumber + 1) + " " + startTime
                            + "";
                } else {
                    startDate = "" + day + "-" + month + "-" + yearNumber + " " + startTime
                            + "";
                }

                //Make sure the days aren't null. After that, set the shift start time & length.
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

                //Finally collect all the details into a Shift object.
                shift.setShiftNumber(shiftNumber);
                shift.setShiftNumberModifier(modifier);
                shift.setLocation(location);
                shift.setStartTime(shiftStartDate);
                shift.setProfession(profession);
                shift.setLengthHours((int) diffHours);
                shift.setLengthMinutes((int) minutes);
                shift.setStartMillis(StartMillis);
                shift.setEndMillis(EndMillis);

                dw.add(shift);

                Logger.debug(TAG,
                        "staff " + shift.getStaff().getStaffName() + " runs shift " + shift.getLocation()
                                + shift.getShiftNumber() + " at " + shift.getStartTime()
                                + " with a shift length of " + shift.getLengthHours() + " hours and "
                                + shift.getLengthMinutes() + " minutes.");
            }

            Logger.debug(TAG, "//////////// END OF WEEK ////////////");

            Snackbar successBar = Snackbar.make(((Activity) context).findViewById(R.id.scrollViewMain), "DW geladen: week " +
                    weekNumber + " van " + yearNumber, Snackbar.LENGTH_LONG);
            successBar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);
            successBar.show();

            Logger.debug(TAG, "Finished scanning " + toRead.getPath());
            return true;
        } catch (ParseException | ArrayIndexOutOfBoundsException | AssertionError e) {
            e.printStackTrace();
            return false;
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

    /**
     * Determines whether a shift number (or rather a title?) is a day off, and should be listed as a shift or not.
     *
     * @param shiftNumber the given shift number to check.
     * @return whether it should be listed or not.
     */
    private static boolean isRestingDay(String shiftNumber) {
        switch (shiftNumber.toLowerCase()) {
            case "r":
            case "streepjesdag":
            case "vl":
            case "gvl":
            case "wa":
            case "wr":
            case "wv":
                return true;
            default:
                return false;
        }
    }

    /**
     * Resets all the data. Used when loading a new file after having already loaded a file previously.
     */
    public void resetData() {
        fileContents = new String[13];
        dw = new ArrayList<>();
    }
}
