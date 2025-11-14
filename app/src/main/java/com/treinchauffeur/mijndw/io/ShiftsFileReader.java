package com.treinchauffeur.mijndw.io;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
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
import java.util.Objects;
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
    private static String[] originalContents = new String[13];
    private static final String TAG = "Run";
    public static final int REASON_FAILED_READ = 1, REASON_FAILED_PROCESS = 2;
    public static Uri toRead;
    private static FirebaseAnalytics analytics;
    @SuppressLint("SimpleDateFormat")
    static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    public static int staffNumber = -1, weekNumber = -1, yearNumber = -1;
    public static ArrayList<Shift> dw = new ArrayList<>();
    private static final Staff staff = new Staff();
    public Context context;

    private boolean returnDaysOff = false, returOnlyVTA = false;

    private static int errorAtLine = -1;


    public ShiftsFileReader(Context context) {
        this.context = context;
        analytics = FirebaseAnalytics.getInstance(context);
    }

    /**
     * Initiates the conversion of the DW file & acting as a staging method.
     *
     * @param c   context
     * @param uri user-supplied file
     */
    public void startConversion(Context c, Uri uri, boolean returnDaysOff, boolean returnOnlyVTA) {
        this.returnDaysOff = returnDaysOff;
        this.returOnlyVTA = returnOnlyVTA;
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

            int lines = 0;
            while (reader.readLine() != null) lines++;
            reader.close();
            assert inputStream != null;
            inputStream.close();
            Logger.debug(TAG, "File has amount of lines: " + lines);
            fileContents = new String[lines];
            originalContents = new String[lines];

            //Reopen those streams to actually assign the filecontent lines.
            inputStream = c.getContentResolver().openInputStream(uri);
            reader = new BufferedReader(new InputStreamReader(inputStream));

            if (lines >= 13) {
                fileContents[0] = reader.readLine(); //first line: Donderdagse Week van WW-YYYY OR Jaarrooster YYYY

                if (fileContents[0] == null) {
                    Log.d(TAG, "ERROR: First line is empty!");
                    return false;
                } else if (!fileContents[0].contains("Donderdagse Week van") && !fileContents[0].contains("Jaarrooster")) {
                    Log.d(TAG, "ERROR: First line doesn't contain year & week data!");
                    return false;
                }

                for (int i = 1; i < fileContents.length; i++) {
                    String nextLine = reader.readLine();
                    fileContents[i] = nextLine;
                }

            } else {
                Log.d(TAG, "ERROR: File has too little lines!");
                return false;
            }

            reader.close();

            //Some DW files are formatted super weirdly, we're fixing that here.
            //The only lines that SHOULD be empty, would be lines 2 & 4 (indexes 1 & 3).
            //Ps I hate doing it this way, will improve this in the future using recursion.
            if (Objects.equals(fileContents[1], "") && Objects.equals(fileContents[2], "") && Objects.equals(fileContents[3], "") &&
                    Objects.equals(fileContents[5], "") && Objects.equals(fileContents[6], "") && Objects.equals(fileContents[7], "")) {
                ArrayList<String> temp = new ArrayList<>();
                originalContents = fileContents;
                for (int i = 0; i < fileContents.length; i = i + 2) {
                    temp.add(fileContents[i]);
                }

                if (!temp.isEmpty() && temp.get(4).startsWith("Datum")) {
                    fileContents = new String[temp.size()];
                    for (int i = 0; i < fileContents.length; i++) {
                        fileContents[i] = temp.get(i);
                    }
                }
            }

            if (fileContents[12] == null) {
                Log.d(TAG, "ERROR: Line 13 is empty!");
                return false;
            }

            //Temporary fix to be able to read two shifts that apply to one single day.
            if (fileContents[12].startsWith("zo") || fileContents[13].startsWith("zo")) {
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "readFile: ", e);
        }
        Log.d(TAG, "ERROR: Unknown error!");
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
    private boolean processFile(Context context) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        java.util.Date date1 = null;
        java.util.Date date2 = null;
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        long diff;
        long diffMinutes;
        long diffHours;
        long minutes;
        int currentLine = 0;
        boolean isYearSchedule = false; // We assume this to be the case initially.
        try {
            dw.clear();

            String startingLine = fileContents[0].replaceAll("\\s+", " ");
            // Check whether schedule is just for one week or an entire year.
            // We can check this using the first line.
            try {
            if (startingLine.contains("Jaarrooster")) {
                isYearSchedule = true;
                weekNumber = Integer.parseInt(startingLine.split(" ")[1]);
                yearNumber = weekNumber;
            } else {
                String[] dateYear = startingLine.split("-");
                String weekNrString = dateYear[0].substring(Math.max(dateYear[0].length() - 2, 0));
                if (weekNrString.startsWith(" "))
                    weekNumber = Integer.parseInt(Character.toString(weekNrString.charAt(1)));
                else
                    weekNumber = Integer.parseInt(dateYear[0].substring(Math.max(dateYear[0].length() - 2, 0)));
                yearNumber = Integer.parseInt(dateYear[1]);
            }
            } catch (NumberFormatException e) {
                errorAtLine = 0;
                Log.e(TAG, "Week and/or year number couldn't be read properly!", e);
                showErrorDialog(context, REASON_FAILED_READ);
                return true;
            }

            // Third line - staff number
            currentLine = 2;
            String staffNumberLine = fileContents[2].replaceAll("\\s+", " ");

            try {
                staffNumber = Integer.parseInt(staffNumberLine.split(" ")[0]);
            } catch (NumberFormatException e) {
                errorAtLine = 2;
                Log.e(TAG, "Staff number couldn't be read properly!", e);
                showErrorDialog(context, REASON_FAILED_READ);
                return true;
            }

            staff.setStaffNumber(staffNumber);
            staff.setStaffName(staffNumberLine.split(" ")[1] + ". " + staffNumberLine.split(" ")[2]);

            Logger.debug(TAG, "DW FOR " + staffNumberLine + ":");
            Logger.debug(TAG, "//////////// START WEEK " + weekNumber + " OF " + yearNumber + " ////////////");

            //Loop through the actual days of the week for code-efficiency, since all days are created equal.
            for (int dayLine = 6; dayLine < fileContents.length; dayLine++) {
                currentLine = dayLine;
                Shift shift = new Shift();
                shift.setStaff(staff);

                String lineToRead = fileContents[dayLine].replaceAll("\\s+", " ");
                shift.setRawString(lineToRead);
                String modifier = "-1";
                String[] dayArray = lineToRead.split(" ");

                if (lineToRead.equalsIgnoreCase(" ") || lineToRead.equalsIgnoreCase("")) continue;

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
                String startTime = "00:00";
                String endTime = "00:00";

                //Check if this is a day off work
                if (isDayOff(shiftNumber) || dayArray.length < 4) {
                    Logger.debug(TAG, "Staff " + shift.getStaff().getStaffName() + " is free on " + dayArray[1] + ".");
                    if (!returnDaysOff)
                        continue;
                    else if (returOnlyVTA && !isVTAComponent(shiftNumber))
                        continue;
                } else {
                    startTime = dayArray[3];
                    endTime = dayArray[4];
                }

                int month = -1;
                int day = -1;
                try {
                    month = Integer.parseInt(dayArray[1].split("-")[1]);
                    day = Integer.parseInt(dayArray[1].split("-")[0]);
                } catch (NumberFormatException e) {
                    errorAtLine = dayLine;
                    Log.e(TAG, "Month or day couldn't be read properly couldn't be read properly!", e);
                }

                String startDate;
                if ((weekNumber == 52 || weekNumber == 53) && month == 1) {// we passed newyear's
                    startDate = day + "-" + month + "-" + (yearNumber + 1) + " " + startTime;
                } else {
                    startDate = day + "-" + month + "-" + yearNumber + " " + startTime;
                }

                //Make sure the days aren't null. After that, set the shift start time & length.
                try {
                    date1 = format.parse(startTime);
                    date2 = format.parse(endTime);
                } catch (NumberFormatException e) {
                    errorAtLine = dayLine;
                    Log.e(TAG, "Start or end time couldn't be read properly couldn't be parsed properly!", e);
                }
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

                java.util.Date shiftStartDate = null;
                try {
                    shiftStartDate = sdf.parse(startDate);
                } catch (NumberFormatException e) {
                    errorAtLine = dayLine;
                    Log.e(TAG, "Start or end date couldn't be read properly couldn't be parsed properly!", e);
                }

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

            Snackbar successBar;
            if (isYearSchedule)
                successBar = Snackbar.make(((Activity) context).findViewById(R.id.scrollViewMain), "DW geladen: Jaar " + yearNumber, Snackbar.LENGTH_LONG);
            else
                successBar = Snackbar.make(((Activity) context).findViewById(R.id.scrollViewMain), "DW geladen: week " +
                        weekNumber + " van " + yearNumber, Snackbar.LENGTH_LONG);
            successBar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);
            successBar.show();

            Logger.debug(TAG, "Finished scanning " + toRead.getPath());
            return true;
        } catch (ParseException | ArrayIndexOutOfBoundsException | AssertionError e) {
            Log.e(TAG, "Exception occurred whilst reading DW on line: " + (currentLine + 1), e);
            errorAtLine = currentLine;
            return false;
        }
    }

    /**
     * Puts all the shift details from their respective objects in to an iCalendar String using biWeekly.
     *
     * @return the full, completed iCalendar String.
     */
    @SuppressLint("SimpleDateFormat")
    public String getPersonalisedIcs() {
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
        boolean displayModifiers = prefs.getBoolean("displayModifiers", true);
        boolean fullDaysOnly = prefs.getBoolean("fullDaysOnly", false);
        boolean displayProfession = prefs.getBoolean("displayProfession", true);
        String[] toIgnore = prefs.getString("toIgnore", "").split(",");
        String[] replace = prefs.getString("replacement", "").split(",");
        String prefix = prefs.getString("prefix", "");

        String ics = "";
        ICalendar iCal = new ICalendar();

        for (Shift shift : dw) {
            if (shift.getShiftNumber().equals("!!!")) continue;

            boolean shouldSkip = false;
            for(String s : toIgnore) {
                if (shift.getShiftNumber().equalsIgnoreCase(s)) {
                    shouldSkip = true;
                    break;
                }
            }
            if(shouldSkip) continue;

            VEvent event = new VEvent();
            String summaryString;
            if (displayModifiers)
                summaryString = shift.getShiftNumberModifier().equals("-1") ?
                        shift.getLocation() + " " + shift.getNeatShiftNumber() :
                        shift.getLocation() + " " + shift.getShiftNumberModifier() + shift.getNeatShiftNumber();
            else summaryString = shift.getLocation() + " " + shift.getNeatShiftNumber();
            if (fullDaysOnly)
                summaryString += " " + new SimpleDateFormat("HH:mm").format(shift.getStartMillis()) + " - " +
                        new SimpleDateFormat("HH:mm").format(shift.getEndMillis());

            summaryString = displayProfession ? (shift.getProfession() + " " + summaryString) : summaryString;

            //We can add a prefix to the summary string based on the users preferences (advanced settings activity)
            if(!prefix.isEmpty()) summaryString = prefix + " " + summaryString;

            //There is an option to replace the summary string when certain shift numbers are found.
            //This is also a user preference.
            for(String s : replace) {
                if (shift.getShiftNumber().equalsIgnoreCase(s.split(";")[0])) {
                        summaryString = s.split(";")[1];
                    break;
                }
            }

            Summary summary = event.setSummary(summaryString);

            Description description = event.setDescription(shift.getRawString());
            description.setLanguage("nl");
            summary.setLanguage("nl");

            Date start = shift.getStartTime();
            if (fullDaysOnly) event.setDateStart(Utils.atStartOfDay(start));
            else if(returnDaysOff && !returOnlyVTA && isDayOff(shift.getShiftNumber()))
                event.setDateStart(Utils.atStartOfDay(start));
            else if(returnDaysOff && returOnlyVTA && isVTAComponent(shift.getShiftNumber()))
                event.setDateStart(Utils.atStartOfDay(start));
            else event.setDateStart(start);

            if(!returnDaysOff && isDayOff(shift.getShiftNumber())) continue;
            if(returnDaysOff && returOnlyVTA && isRegularRestingDay(shift.getShiftNumber())) continue;

            Duration duration = fullDaysOnly ?
                    new Duration.Builder().days(1).build() :
                    new Duration.Builder().hours(shift.getLengthHours()).minutes(shift.getLengthMinutes()).build();
            event.setDuration(duration);

            if(isDayOff(shift.getShiftNumber())) {
                Duration duration2 = new Duration.Builder().days(1).build();
                event.setDuration(duration2);
            }

            iCal.addEvent(event);
        }

        //Analytics logic
        String standplaats = "";
        for(Shift shift : dw) {
            if(!shift.getLocation().isBlank() && !shift.getLocation().equalsIgnoreCase("!!!"))
                standplaats = shift.getLocation();
        }
        if(!standplaats.equals("!!!") && !standplaats.isBlank()) {
            Bundle params = new Bundle();
            params.putString("standplaats", standplaats);
            analytics.logEvent("standplaats", params);

            for(Shift shift : dw) {
                if(!shift.getLocation().equalsIgnoreCase(standplaats)) {
                    Bundle params2 = new Bundle();
                    params2.putString("wisselende_standplaatsen", "1");
                    analytics.logEvent("wisselende_standplaatsen", params2);
                    break;
                }
            }
        }
        ics += Biweekly.write(iCal).go();

        return ics;
    }

    /**
     * @return the entire (ORIGINAL!) DW file contents in form of a String.
     */
    public static String fullFileString() {
        StringBuilder str = new StringBuilder();
        if(originalContents == null || originalContents.length == 0) return str.toString();

        //We needed to edit the original lines & saved them for debugging in 'originalContents'.
        if (originalContents[0] != null) {
            for (String fileContent : originalContents) {
                str.append(fileContent).append("\n");
            }
        } else {
            for (String fileContent : fileContents) {
                str.append(fileContent).append("\n");
            }
        }
        return str.toString();
    }

    /**
     * Determines whether a symbol or multiple symbols are shift modifiers.
     * This is necessary in order to properly read each day of the week correctly.
     *
     * @param modifier the given symbol(s).
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
    public static boolean isShiftModifier(String modifier) {
        return switch (modifier) {
            case "!", "@", ">", "<", "*", "?", "E", "#", "$", "%", "=", "P", "P!", "P@", "P>", "P<",
                 "P*", "P?", "PE", "P#", "P$", "P%", "P=", "E!", "E@", "E>", "E<", "E*", "E?", "E#",
                 "E$", "E%", "E=", "[" -> true;
            default -> false;
        };
    }

    /**
     * A simpler check that determines whether we need to check for starting and ending times of these shifts.
     * No resting days Should have those.
     * @param shiftNumber the given shift number to check.
     * @return whether it is a day off or not.
     */
    public static boolean isDayOff(String shiftNumber) {
        return isRegularRestingDay(shiftNumber) || isVTAComponent(shiftNumber);
    }

    /**
     * Determines whether a shift number (or rather a title?) is a day off, and should be listed as a shift or not.
     *
     * @param shiftNumber the given shift number to check.
     * @return whether it should be listed or not.
     */
    public static boolean isRegularRestingDay(String shiftNumber) {
        return switch (shiftNumber.toLowerCase()) {
            case "r", "streepjesdag", "--", "wr", "ro", "ow", "rust", "wtv", "wtv rust",
                 "wtv-dag", "wtv dag" -> true;
            default -> false;
        };
    }

    /**
     * Determines whether a shift number (or rather a title?) is a VTA component.
     * These are days off, but should be handled differently because the user might want to see these
     * in their calendar instead of regular resting days.
     *
     * @param shiftNumber the given shift number to check.
     * @return whether it should be listed or not.
     */
    public static boolean isVTAComponent(String shiftNumber) {
        return switch (shiftNumber.toLowerCase()) {
            case "vl", "gvl", "wa", "wv", "co", "cf", "ot", "rt", "mt", "eg", "f", "rust terug",
                 "overuren terug", "wtv vrij opneembaar", "wtv aangewezen", "verlof", "compensatie f-dag", "ziek" -> true;
            default -> false;
        };
    }

    public static boolean isSpecial(String shiftNumber) {
        return switch (shiftNumber.toLowerCase()) {
            case "cursus", "taakgericht werk overleg", "wegleren" -> true;
            default -> false;
        };
    }

    /**
     * Resets all the data. Used when loading a new file after having already loaded a file previously.
     */
    public void resetData() {
        fileContents = new String[13];
        dw = new ArrayList<>();
        errorAtLine = -1;
    }

    /**
     * Shows the user a dialog with an error related to either reading or processing the file.
     *
     * @param c      context
     * @param reason where something went wrong
     */
    private static void showErrorDialog(Context c, int reason) {
        Bundle params = new Bundle();
        if (reason == REASON_FAILED_READ) {
            params.putString("failed_read", "1");
            analytics.logEvent("failed_read", params);
        } else if (reason == REASON_FAILED_PROCESS)  {
            params.putString("failed_process", "1");
            analytics.logEvent("failed_process", params);
        }

        String bodyText = "Er is een fout opgetreden tijdens het inlezen van jouw DW." +
                " Zou je deze willen emailen naar de ontwikkelaar voor analyse zodat deze de app kan verbeteren? :) \n\n" +
                "Je kan eventueel deze mail zelf nog bewerken om je personeelsnummer en andere gevoelige gegevens aan te passen of te verwijderen.";

        //If the error occurred upon the processing of the individual shifts, display the possibility that some shifts were read properly.
        if (reason == REASON_FAILED_PROCESS)
            bodyText += "\n\nToch is het mogelijk dat bepaalde dagen WEL goed" +
                    " verwerkt zijn en deze toe te voegen zijn aan je agenda. Controleer deze goed vóórdat je dit doet!";

        //Give the user information about where it went wrong, they might include this in a screenshot.
        if (errorAtLine != -1 && fileContents.length >= errorAtLine) {
            bodyText += "\n\nDe volgende lijn bevat een fout:\n";
            bodyText += "'" + fileContents[errorAtLine] + "'\n";
        }

        new MaterialAlertDialogBuilder(c, R.style.ThemeOverlay_App_MaterialErrorDialog)
                .setTitle("Fout opgetreden")
                .setIcon(R.drawable.baseline_error_outline_24)
                .setMessage(bodyText)

                .setNegativeButton("DW E-MAILEN", (dialogInterface, i) -> {
                    final Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:"));
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{Settings.DEV_EMAIL});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Fout tijdens " + (reason == REASON_FAILED_READ ? "inlezen" : "verwerken") + " van DW");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Mijn DW versie " + BuildConfig.VERSION_NAME + "\n"
                            + fullFileString() + "\n\n" + "-------- Mocht je nog iets kwijt willen, graag onder deze lijn --------" + "\n\n");
                    c.startActivity(Intent.createChooser(emailIntent, "E-mail versturen.."));
                })
                .setPositiveButton(reason == REASON_FAILED_PROCESS ? "DOORGAAN" : "NEE", (dialogInterface, i) -> dialogInterface.dismiss()).show();
    }
}
