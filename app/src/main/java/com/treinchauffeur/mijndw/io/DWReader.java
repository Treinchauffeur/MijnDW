package com.treinchauffeur.mijndw.io;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

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

    private static final String[] fileContents = new String[13];
    private static final String TAG = "Run";
    public static Uri toRead;
    @SuppressLint("SimpleDateFormat")
    static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    public static int staffNumber = -1, weekNumber = -1, yearNumber = -1;

    public static ArrayList<Shift> dw = new ArrayList<>();
    private static final Shift mondayShift = new Shift();
    private static final Shift tuesdayShift = new Shift();
    private static final Shift wednesdayShift = new Shift();
    private static final Shift thursdayShift = new Shift();
    private static final Shift fridayShift = new Shift();
    private static final Shift saturdayShift = new Shift();
    private static final Shift sundayShift = new Shift();
    private static final Staff staff = new Staff();
    public Context context;

    public DWReader(Context context) {
        this.context = context;
    }

    /**
     * Read the DW file & save to raw data
     * <p>
     * Line indexing: 0=days in text, 1=date in 31-12 format, 2=shift
     * number/letters, 3=starttime formatted to 24:60, 4=endtime formatted
     * to 24:60, 5=profession, 6=location
     *
     * @param context
     */

    @SuppressWarnings("deprecation")
    private static void processFile(Context context) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
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
            String emptyLineOne = fileContents[1].replaceAll("\\s+", " ");

            // Third line - staff number
            String staffNumberLine = fileContents[2].replaceAll("\\s+", " ");
            staffNumber = Integer.parseInt(staffNumberLine.split(" ")[0]);

            staff.setStaffNumber(staffNumber);
            staff.setStaffName(staffNumberLine.split(" ")[1] + ". " + staffNumberLine.split(" ")[2]);
            mondayShift.setStaff(staff);
            tuesdayShift.setStaff(staff);
            wednesdayShift.setStaff(staff);
            thursdayShift.setStaff(staff);
            fridayShift.setStaff(staff);
            saturdayShift.setStaff(staff);
            sundayShift.setStaff(staff);

            // Fourth line - empty for formatting purposes
            String emptyLineTwo = fileContents[3].replaceAll("\\s+", " ");

            // Fifth line - headers for formatting
            String headersLine = fileContents[4].replaceAll("\\s+", " ");

            // Sixth line - lines underneath headers, also worthless
            String underscoresLine = fileContents[5].replaceAll("\\s+", " ");

            Log.d(TAG, "DW FOR " + staffNumberLine + ":");
            Log.d(TAG, "//////////// START WEEK " + weekNumber + " OF " + yearNumber + " ////////////");

            // Seventh line - first actual day listing
            String mondayLine = fileContents[6].replaceAll("\\s+", " ");
            String mondayModifier = "-1";
            String[] mondayArray = mondayLine.split(" ");

            //Check if we have modifiers
            if (mondayArray[2].equals("!") || mondayArray[2].equals("@") || mondayArray[2].equals(">")
                    || mondayArray[2].equals("<") || mondayArray[2].equals("*") || mondayArray[2].equals("?")
                    || mondayArray[2].equals("E") || mondayArray[2].equals("#") || mondayArray[2].equals("$") || mondayArray[2].equals("%")) {
                mondayModifier = mondayArray[2];
                for (int i = 2; i < mondayArray.length - 1; i++) { // If so, get rid of them but save them. They mess
                    // things up later on
                    mondayArray[i] = mondayArray[i + 1];
                }
            }

            String mondayProfession = "";
            String mondayLocation = "";
            if (mondayArray.length > 6) { // Things like CURS don't have a function etc listed
                mondayProfession = mondayArray[5].substring(0, 1).toUpperCase()
                        + mondayArray[5].substring(1).toLowerCase();
                mondayLocation = mondayArray[6].substring(0, 1).toUpperCase()
                        + mondayArray[6].substring(1).toLowerCase();
            }
            String mondayShiftNumber = mondayArray[2];
            String mondayStartTime = "";
            String mondayEndTime = "";
            if (!mondayShiftNumber.equalsIgnoreCase("R") && !mondayShiftNumber.equalsIgnoreCase("streepjesdag")
                    && !mondayShiftNumber.contains("VL")) {
                mondayStartTime = mondayArray[3];
                mondayEndTime = mondayArray[4];
            }

            String mondayTitle = "";

            if (mondayArray.length == 3)
                mondayTitle = mondayShiftNumber;
            else if (mondayArray.length == 5)
                mondayTitle = mondayShiftNumber + " " + mondayStartTime + "-" + mondayEndTime;
            else if (mondayModifier != "-1")
                mondayTitle = mondayProfession + " " + mondayLocation + " " + mondayModifier + "" + mondayShiftNumber
                        + " " + mondayStartTime + "-" + mondayEndTime;
            else
                mondayTitle = mondayProfession + " " + mondayLocation + " " + mondayShiftNumber + " " + mondayStartTime
                        + "-" + mondayEndTime;

            Log.d(TAG, mondayTitle);

            int mondayMonth = Integer.parseInt(mondayArray[1].split("-")[1]);
            int mondayDay = Integer.parseInt(mondayArray[1].split("-")[0]);
            if (mondayArray.length <= 3)
                Log.d(TAG, "Staff " + mondayShift.getStaff().getStaffName() + " is free on monday.");
            else {
                String mondayStartDate = "";
                if ((weekNumber == 52 || weekNumber == 53) && mondayMonth == 1) {// we passed newyear's
                    mondayStartDate = "" + mondayDay + "-" + mondayMonth + "-" + yearNumber + 1 + " " + mondayStartTime
                            + "";
                } else {
                    mondayStartDate = "" + mondayDay + "-" + mondayMonth + "-" + yearNumber + " " + mondayStartTime
                            + "";
                }
                date1 = format.parse(mondayStartTime);
                date2 = format.parse(mondayEndTime);
                cal1.setTime(date1);
                cal2.setTime(date2);
                if (cal2.getTimeInMillis() - cal1.getTimeInMillis() < 0)
                    cal2.add(Calendar.DATE, 1);

                diff = cal2.getTimeInMillis() - cal1.getTimeInMillis();
                diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                diffHours = TimeUnit.MILLISECONDS.toHours(diff);
                minutes = diffMinutes - (diffHours * 60);

                java.util.Date mondayShiftStartDate = sdf.parse(mondayStartDate);
                long mondayStartMillis = mondayShiftStartDate.getTime();
                long mondayEndMillis = mondayShiftStartDate.getTime() + diff;

                mondayShift.setShiftNumber(mondayShiftNumber);
                mondayShift.setShiftNumberModifier(mondayModifier);
                mondayShift.setLocation(mondayLocation);
                mondayShift.setStartTime(mondayShiftStartDate);
                mondayShift.setProfession(mondayProfession);
                mondayShift.setWeekNumber(weekNumber);
                mondayShift.setYearNumber(yearNumber);
                mondayShift.setLengthHours((int) diffHours);
                mondayShift.setLengthMinutes((int) minutes);
                mondayShift.setStartMillis(mondayStartMillis);
                mondayShift.setEndMillis(mondayEndMillis);

                dw.add(mondayShift);

                Log.d(TAG,
                        "staff " + mondayShift.getStaff().getStaffName() + " runs shift " + mondayShift.getLocation()
                                + mondayShift.getShiftNumber() + " at " + mondayShift.getStartTime()
                                + " with a shift length of " + mondayShift.getLengthHours() + " hours and "
                                + mondayShift.getLengthMinutes() + " minutes.");
            }

            // Eighth line - second day
            String tuesdayLine = fileContents[7].replaceAll("\\s+", " ");
            String tuesdayModifier = "-1";
            String[] tuesdayArray = tuesdayLine.split(" ");
            if (tuesdayArray[2].equals("!") || tuesdayArray[2].equals("@") || tuesdayArray[2].equals(">")
                    || tuesdayArray[2].equals("<") || tuesdayArray[2].equals("*") || tuesdayArray[2].equals("?")
                    || tuesdayArray[2].equals("E") || tuesdayArray[2].equals("#") || tuesdayArray[2].equals("$") || tuesdayArray[2].equals("%")) { // Check
                // if
                // we
                // have
                // modifiers
                tuesdayModifier = tuesdayArray[2];
                for (int i = 2; i < tuesdayArray.length - 1; i++) { // If so, get rid of them but save them. They mess
                    // things up later on
                    tuesdayArray[i] = tuesdayArray[i + 1];
                }
            }

            String tuesdayProfession = "";
            String tuesdayLocation = "";
            if (tuesdayArray.length > 6) { // Things like CURS don't have a function etc listed
                tuesdayProfession = tuesdayArray[5].substring(0, 1).toUpperCase()
                        + tuesdayArray[5].substring(1).toLowerCase();
                tuesdayLocation = tuesdayArray[6].substring(0, 1).toUpperCase()
                        + tuesdayArray[6].substring(1).toLowerCase();
            }
            String tuesdayShiftNumber = tuesdayArray[2];
            String tuesdayStartTime = "";
            String tuesdayEndTime = "";
            if (!tuesdayShiftNumber.equalsIgnoreCase("R") && !tuesdayShiftNumber.equalsIgnoreCase("streepjesdag") && !tuesdayShiftNumber.contains("VL")) {
                tuesdayStartTime = tuesdayArray[3];
                tuesdayEndTime = tuesdayArray[4];
            }

            int tuesdayMonth = Integer.parseInt(tuesdayArray[1].split("-")[1]);
            int tuesdayDay = Integer.parseInt(tuesdayArray[1].split("-")[0]);
            if (tuesdayArray.length <= 3)
                Log.d(TAG, "Staff " + tuesdayShift.getStaff().getStaffName() + " is free on tuesday.");
            else {
                String tuesdayStartDate = "";
                if ((weekNumber == 52 || weekNumber == 53) && tuesdayMonth == 1) {// we passed newyear's
                    tuesdayStartDate = "" + tuesdayDay + "-" + tuesdayMonth + "-" + yearNumber + 1 + " "
                            + tuesdayStartTime + "";
                } else {
                    tuesdayStartDate = "" + tuesdayDay + "-" + tuesdayMonth + "-" + yearNumber + " " + tuesdayStartTime
                            + "";
                }
                date1 = format.parse(tuesdayStartTime);
                date2 = format.parse(tuesdayEndTime);
                cal1.setTime(date1);
                cal2.setTime(date2);
                if (cal2.getTimeInMillis() - cal1.getTimeInMillis() < 0)
                    cal2.add(Calendar.DATE, 1);

                diff = cal2.getTimeInMillis() - cal1.getTimeInMillis();
                diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                diffHours = TimeUnit.MILLISECONDS.toHours(diff);
                minutes = diffMinutes - (diffHours * 60);

                java.util.Date tuesdayShiftStartDate = sdf.parse(tuesdayStartDate);
                long tuesdayStartMillis = tuesdayShiftStartDate.getTime();
                long tuesdayEndMillis = tuesdayShiftStartDate.getTime() + diff;

                tuesdayShift.setShiftNumber(tuesdayShiftNumber);
                tuesdayShift.setShiftNumberModifier(tuesdayModifier);
                tuesdayShift.setLocation(tuesdayLocation);
                tuesdayShift.setStartTime(tuesdayShiftStartDate);
                tuesdayShift.setProfession(tuesdayProfession);
                tuesdayShift.setWeekNumber(weekNumber);
                tuesdayShift.setYearNumber(yearNumber);
                tuesdayShift.setLengthHours((int) diffHours);
                tuesdayShift.setLengthMinutes((int) minutes);
                tuesdayShift.setStartMillis(tuesdayStartMillis);
                tuesdayShift.setEndMillis(tuesdayEndMillis);

                dw.add(tuesdayShift);

                Log.d(TAG,
                        "staff " + tuesdayShift.getStaff().getStaffName() + " runs shift " + tuesdayShift.getLocation()
                                + tuesdayShift.getShiftNumber() + " at " + tuesdayShift.getStartTime()
                                + " with a shift length of " + tuesdayShift.getLengthHours() + " hours and "
                                + tuesdayShift.getLengthMinutes() + " minutes.");
            }

            // Ninth line - third day
            String wednesdayLine = fileContents[8].replaceAll("\\s+", " ");
            String wednesdayModifier = "-1";
            String[] wednesdayArray = wednesdayLine.split(" ");
            if (wednesdayArray[2].equals("!") || wednesdayArray[2].equals("@") || wednesdayArray[2].equals(">")
                    || wednesdayArray[2].equals("<") || wednesdayArray[2].equals("*") || wednesdayArray[2].equals("?")
                    || wednesdayArray[2].equals("E") || wednesdayArray[2].equals("#")
                    || wednesdayArray[2].equals("$") || wednesdayArray[2].equals("%")) { // Check if we have modifiers
                wednesdayModifier = wednesdayArray[2];
                for (int i = 2; i < wednesdayArray.length - 1; i++) { // If so, get rid of them but save them. They mess
                    // things up later on
                    wednesdayArray[i] = wednesdayArray[i + 1];
                }
            }

            String wednesdayProfession = "";
            String wednesdayLocation = "";
            if (wednesdayArray.length > 6) { // Things like CURS don't have a function etc listed
                wednesdayProfession = wednesdayArray[5].substring(0, 1).toUpperCase()
                        + wednesdayArray[5].substring(1).toLowerCase();
                wednesdayLocation = wednesdayArray[6].substring(0, 1).toUpperCase()
                        + wednesdayArray[6].substring(1).toLowerCase();
            }
            String wednesdayShiftNumber = wednesdayArray[2];
            String wednesdayStartTime = "";
            String wednesdayEndTime = "";
            if (!wednesdayShiftNumber.equalsIgnoreCase("R") && !wednesdayShiftNumber.equalsIgnoreCase("streepjesdag") && !wednesdayShiftNumber.contains("VL")) {
                wednesdayStartTime = wednesdayArray[3];
                wednesdayEndTime = wednesdayArray[4];
            }

            int wednesdayMonth = Integer.parseInt(wednesdayArray[1].split("-")[1]);
            int wednesdayDay = Integer.parseInt(wednesdayArray[1].split("-")[0]);
            if (wednesdayArray.length <= 3)
                Log.d(TAG, "Staff " + wednesdayShift.getStaff().getStaffName() + " is free on wednesday.");
            else {
                String wednesdayStartDate = "";
                if ((weekNumber == 52 || weekNumber == 53) && wednesdayMonth == 1) {// we passed newyear's
                    wednesdayStartDate = "" + wednesdayDay + "-" + wednesdayMonth + "-" + yearNumber + 1 + " "
                            + wednesdayStartTime + "";
                } else {
                    wednesdayStartDate = "" + wednesdayDay + "-" + wednesdayMonth + "-" + yearNumber + " "
                            + wednesdayStartTime + "";
                }
                date1 = format.parse(wednesdayStartTime);
                date2 = format.parse(wednesdayEndTime);
                cal1.setTime(date1);
                cal2.setTime(date2);
                if (cal2.getTimeInMillis() - cal1.getTimeInMillis() < 0)
                    cal2.add(Calendar.DATE, 1);

                diff = cal2.getTimeInMillis() - cal1.getTimeInMillis();
                diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                diffHours = TimeUnit.MILLISECONDS.toHours(diff);
                minutes = diffMinutes - (diffHours * 60);

                java.util.Date wednesdayShiftStartDate = sdf.parse(wednesdayStartDate);
                long wednesdayStartMillis = wednesdayShiftStartDate.getTime();
                long wednesdayEndMillis = wednesdayShiftStartDate.getTime() + diff;

                wednesdayShift.setShiftNumber(wednesdayShiftNumber);
                wednesdayShift.setShiftNumberModifier(wednesdayModifier);
                wednesdayShift.setLocation(wednesdayLocation);
                wednesdayShift.setStartTime(wednesdayShiftStartDate);
                wednesdayShift.setProfession(wednesdayProfession);
                wednesdayShift.setWeekNumber(weekNumber);
                wednesdayShift.setYearNumber(yearNumber);
                wednesdayShift.setLengthHours((int) diffHours);
                wednesdayShift.setLengthMinutes((int) minutes);
                wednesdayShift.setStartMillis(wednesdayStartMillis);
                wednesdayShift.setEndMillis(wednesdayEndMillis);

                dw.add(wednesdayShift);

                Log.d(TAG, "staff " + wednesdayShift.getStaff().getStaffName() + " runs shift "
                        + wednesdayShift.getLocation() + wednesdayShift.getShiftNumber() + " at "
                        + wednesdayShift.getStartTime() + " with a shift length of " + wednesdayShift.getLengthHours()
                        + " hours and " + wednesdayShift.getLengthMinutes() + " minutes.");
            }

            // Tenth line - fourth day
            String thursdayLine = fileContents[9].replaceAll("\\s+", " ");
            String thursdayModifier = "-1";
            String[] thursdayArray = thursdayLine.split(" ");
            if (thursdayArray[2].equals("!") || thursdayArray[2].equals("@") || thursdayArray[2].equals(">")
                    || thursdayArray[2].equals("<") || thursdayArray[2].equals("*") || thursdayArray[2].equals("?")
                    || thursdayArray[2].equals("E") || thursdayArray[2].equals("#") || thursdayArray[2].equals("$") || thursdayArray[2].equals("%")) { // Check
                // if
                // we
                // have
                // modifiers
                thursdayModifier = thursdayArray[2];
                for (int i = 2; i < thursdayArray.length - 1; i++) { // If so, get rid of them but save them. They mess
                    // things up later on
                    thursdayArray[i] = thursdayArray[i + 1];
                }
            }

            String thursdayProfession = "";
            String thursdayLocation = "";
            if (thursdayArray.length > 6) { // Things like CURS don't have a function etc listed
                thursdayProfession = thursdayArray[5].substring(0, 1).toUpperCase()
                        + thursdayArray[5].substring(1).toLowerCase();
                thursdayLocation = thursdayArray[6].substring(0, 1).toUpperCase()
                        + thursdayArray[6].substring(1).toLowerCase();
            }
            String thursdayShiftNumber = thursdayArray[2];
            String thursdayStartTime = "";
            String thursdayEndTime = "";
            if (!thursdayShiftNumber.equalsIgnoreCase("R") && !thursdayShiftNumber.equalsIgnoreCase("streepjesdag") && !thursdayShiftNumber.contains("VL")) {
                thursdayStartTime = thursdayArray[3];
                thursdayEndTime = thursdayArray[4];
            }

            int thursdayMonth = Integer.parseInt(thursdayArray[1].split("-")[1]);
            int thursdayDay = Integer.parseInt(thursdayArray[1].split("-")[0]);
            if (thursdayArray.length <= 3)
                Log.d(TAG, "Staff " + thursdayShift.getStaff().getStaffName() + " is free on thursday.");
            else {
                String thursdayStartDate = "";
                if ((weekNumber == 52 || weekNumber == 53) && thursdayMonth == 1) {// we passed newyear's
                    thursdayStartDate = "" + thursdayDay + "-" + thursdayMonth + "-" + yearNumber + 1 + " "
                            + thursdayStartTime + "";
                } else {
                    thursdayStartDate = "" + thursdayDay + "-" + thursdayMonth + "-" + yearNumber + " "
                            + thursdayStartTime + "";
                }
                date1 = format.parse(thursdayStartTime);
                date2 = format.parse(thursdayEndTime);
                cal1.setTime(date1);
                cal2.setTime(date2);
                if (cal2.getTimeInMillis() - cal1.getTimeInMillis() < 0)
                    cal2.add(Calendar.DATE, 1);

                diff = cal2.getTimeInMillis() - cal1.getTimeInMillis();
                diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                diffHours = TimeUnit.MILLISECONDS.toHours(diff);
                minutes = diffMinutes - (diffHours * 60);

                java.util.Date thursdayShiftStartDate = sdf.parse(thursdayStartDate);
                long thursdayStartMillis = thursdayShiftStartDate.getTime();
                long thursdayEndMillis = thursdayShiftStartDate.getTime() + diff;

                thursdayShift.setShiftNumber(thursdayShiftNumber);
                thursdayShift.setShiftNumberModifier(thursdayModifier);
                thursdayShift.setLocation(thursdayLocation);
                thursdayShift.setStartTime(thursdayShiftStartDate);
                thursdayShift.setProfession(thursdayProfession);
                thursdayShift.setWeekNumber(weekNumber);
                thursdayShift.setYearNumber(yearNumber);
                thursdayShift.setLengthHours((int) diffHours);
                thursdayShift.setLengthMinutes((int) minutes);
                thursdayShift.setStartMillis(thursdayStartMillis);
                thursdayShift.setEndMillis(thursdayEndMillis);

                dw.add(thursdayShift);

                Log.d(TAG, "staff " + thursdayShift.getStaff().getStaffName() + " runs shift "
                        + thursdayShift.getLocation() + thursdayShift.getShiftNumber() + " at "
                        + thursdayShift.getStartTime() + " with a shift length of " + thursdayShift.getLengthHours()
                        + " hours and " + thursdayShift.getLengthMinutes() + " minutes.");
            }

            // Eleventh line - fifth day
            String fridayLine = fileContents[10].replaceAll("\\s+", " ");
            String fridayModifier = "-1";
            String[] fridayArray = fridayLine.split(" ");
            if (fridayArray[2].equals("!") || fridayArray[2].equals("@") || fridayArray[2].equals(">")
                    || fridayArray[2].equals("<") || fridayArray[2].equals("*") || fridayArray[2].equals("?")
                    || fridayArray[2].equals("E") || fridayArray[2].equals("#") || fridayArray[2].equals("$") || fridayArray[2].equals("%")) { // Check
                // if
                // we
                // have
                // modifiers
                fridayModifier = fridayArray[2];
                for (int i = 2; i < fridayArray.length - 1; i++) { // If so, get rid of them but save them. They mess
                    // things up later on
                    fridayArray[i] = fridayArray[i + 1];
                }
            }

            String fridayProfession = "";
            String fridayLocation = "";
            if (fridayArray.length > 6) { // Things like CURS don't have a function etc listed
                fridayProfession = fridayArray[5].substring(0, 1).toUpperCase()
                        + fridayArray[5].substring(1).toLowerCase();
                fridayLocation = fridayArray[6].substring(0, 1).toUpperCase()
                        + fridayArray[6].substring(1).toLowerCase();
            }
            String fridayShiftNumber = fridayArray[2];
            String fridayStartTime = "";
            String fridayEndTime = "";
            if (!fridayShiftNumber.equalsIgnoreCase("R") && !fridayShiftNumber.equalsIgnoreCase("streepjesdag") && !fridayShiftNumber.contains("VL")) {
                fridayStartTime = fridayArray[3];
                fridayEndTime = fridayArray[4];
            }

            int fridayMonth = Integer.parseInt(fridayArray[1].split("-")[1]);
            int fridayDay = Integer.parseInt(fridayArray[1].split("-")[0]);
            if (fridayArray.length <= 3)
                Log.d(TAG, "Staff " + fridayShift.getStaff().getStaffName() + " is free on friday.");
            else {
                String fridayStartDate = "";
                if ((weekNumber == 52 || weekNumber == 53) && fridayMonth == 1) {// we passed newyear's
                    fridayStartDate = "" + fridayDay + "-" + fridayMonth + "-" + yearNumber + 1 + " " + fridayStartTime
                            + "";
                } else {
                    fridayStartDate = "" + fridayDay + "-" + fridayMonth + "-" + yearNumber + " " + fridayStartTime
                            + "";
                }
                date1 = format.parse(fridayStartTime);
                date2 = format.parse(fridayEndTime);
                cal1.setTime(date1);
                cal2.setTime(date2);
                if (cal2.getTimeInMillis() - cal1.getTimeInMillis() < 0)
                    cal2.add(Calendar.DATE, 1);

                diff = cal2.getTimeInMillis() - cal1.getTimeInMillis();
                diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                diffHours = TimeUnit.MILLISECONDS.toHours(diff);
                minutes = diffMinutes - (diffHours * 60);

                java.util.Date fridayShiftStartDate = sdf.parse(fridayStartDate);
                long fridayStartMillis = fridayShiftStartDate.getTime();
                long fridayEndMillis = fridayShiftStartDate.getTime() + diff;

                fridayShift.setShiftNumber(fridayShiftNumber);
                fridayShift.setShiftNumberModifier(fridayModifier);
                fridayShift.setLocation(fridayLocation);
                fridayShift.setStartTime(fridayShiftStartDate);
                fridayShift.setProfession(fridayProfession);
                fridayShift.setWeekNumber(weekNumber);
                fridayShift.setYearNumber(yearNumber);
                fridayShift.setLengthHours((int) diffHours);
                fridayShift.setLengthMinutes((int) minutes);
                fridayShift.setStartMillis(fridayStartMillis);
                fridayShift.setEndMillis(fridayEndMillis);

                dw.add(fridayShift);

                Log.d(TAG,
                        "staff " + fridayShift.getStaff().getStaffName() + " runs shift " + fridayShift.getLocation()
                                + fridayShift.getShiftNumber() + " at " + fridayShift.getStartTime()
                                + " with a shift length of " + fridayShift.getLengthHours() + " hours and "
                                + fridayShift.getLengthMinutes() + " minutes.");
            }

            // Twelveth line - sixth day
            String saturdayLine = fileContents[11].replaceAll("\\s+", " ");
            String saturdayModifier = "-1";
            String[] saturdayArray = saturdayLine.split(" ");
            if (saturdayArray[2].equals("!") || saturdayArray[2].equals("@") || saturdayArray[2].equals(">")
                    || saturdayArray[2].equals("<") || saturdayArray[2].equals("*") || saturdayArray[2].equals("?")
                    || saturdayArray[2].equals("E") || saturdayArray[2].equals("#") || saturdayArray[2].equals("$") || saturdayArray[2].equals("%")) { // Check
                // if
                // we
                // have
                // modifiers
                saturdayModifier = saturdayArray[2];
                for (int i = 2; i < saturdayArray.length - 1; i++) { // If so, get rid of them but save them. They mess
                    // things up later on
                    saturdayArray[i] = saturdayArray[i + 1];
                }
            }

            String saturdayProfession = "";
            String saturdayLocation = "";
            if (saturdayArray.length > 6) { // Things like CURS don't have a function etc listed
                saturdayProfession = saturdayArray[5].substring(0, 1).toUpperCase()
                        + saturdayArray[5].substring(1).toLowerCase();
                saturdayLocation = saturdayArray[6].substring(0, 1).toUpperCase()
                        + saturdayArray[6].substring(1).toLowerCase();
            }
            String saturdayShiftNumber = saturdayArray[2];
            String saturdayStartTime = "";
            String saturdayEndTime = "";
            if (!saturdayShiftNumber.equalsIgnoreCase("R") && !saturdayShiftNumber.equalsIgnoreCase("streepjesdag") && !saturdayShiftNumber.contains("VL")) {
                saturdayStartTime = saturdayArray[3];
                saturdayEndTime = saturdayArray[4];
            }

            int saturdayMonth = Integer.parseInt(saturdayArray[1].split("-")[1]);
            int saturdayDay = Integer.parseInt(saturdayArray[1].split("-")[0]);
            if (saturdayArray.length <= 3)
                Log.d(TAG, "Staff " + saturdayShift.getStaff().getStaffName() + " is free on saturday.");
            else {
                String saturdayStartDate = "";
                if ((weekNumber == 52 || weekNumber == 53) && saturdayMonth == 1) {// we passed newyear's
                    saturdayStartDate = "" + saturdayDay + "-" + saturdayMonth + "-" + yearNumber + 1 + " "
                            + saturdayStartTime + "";
                } else {
                    saturdayStartDate = "" + saturdayDay + "-" + saturdayMonth + "-" + yearNumber + " "
                            + saturdayStartTime + "";
                }
                date1 = format.parse(saturdayStartTime);
                date2 = format.parse(saturdayEndTime);
                cal1.setTime(date1);
                cal2.setTime(date2);
                if (cal2.getTimeInMillis() - cal1.getTimeInMillis() < 0)
                    cal2.add(Calendar.DATE, 1);

                diff = cal2.getTimeInMillis() - cal1.getTimeInMillis();
                diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                diffHours = TimeUnit.MILLISECONDS.toHours(diff);
                minutes = diffMinutes - (diffHours * 60);

                java.util.Date saturdayShiftStartDate = sdf.parse(saturdayStartDate);
                long saturdayStartMillis = saturdayShiftStartDate.getTime();
                long saturdayEndMillis = saturdayShiftStartDate.getTime() + diff;

                saturdayShift.setShiftNumber(saturdayShiftNumber);
                saturdayShift.setShiftNumberModifier(saturdayModifier);
                saturdayShift.setLocation(saturdayLocation);
                saturdayShift.setStartTime(saturdayShiftStartDate);
                saturdayShift.setProfession(saturdayProfession);
                saturdayShift.setWeekNumber(weekNumber);
                saturdayShift.setYearNumber(yearNumber);
                saturdayShift.setLengthHours((int) diffHours);
                saturdayShift.setLengthMinutes((int) minutes);
                saturdayShift.setStartMillis(saturdayStartMillis);
                saturdayShift.setEndMillis(saturdayEndMillis);

                dw.add(saturdayShift);

                Log.d(TAG, "staff " + saturdayShift.getStaff().getStaffName() + " runs shift "
                        + saturdayShift.getLocation() + saturdayShift.getShiftNumber() + " at "
                        + saturdayShift.getStartTime() + " with a shift length of " + saturdayShift.getLengthHours()
                        + " hours and " + saturdayShift.getLengthMinutes() + " minutes.");
            }

            // Thirteenth line - seventh day
            String sundayLine = fileContents[12].replaceAll("\\s+", " ");
            String sundayModifier = "-1";
            String[] sundayArray = sundayLine.split(" ");
            if (sundayArray[2].equals("!") || sundayArray[2].equals("@") || sundayArray[2].equals(">")
                    || sundayArray[2].equals("<") || sundayArray[2].equals("*") || sundayArray[2].equals("?")
                    || sundayArray[2].equals("E") || sundayArray[2].equals("#") || sundayArray[2].equals("$") || sundayArray[2].equals("%")) { // Check
                // if
                // we
                // have
                // modifiers
                sundayModifier = sundayArray[2];
                for (int i = 2; i < sundayArray.length - 1; i++) { // If so, get rid of them but save them. They mess
                    // things up later on
                    sundayArray[i] = sundayArray[i + 1];
                }
            }

            String sundayProfession = "";
            String sundayLocation = "";
            if (sundayArray.length > 6) { // Things like CURS don't have a function etc listed
                sundayProfession = sundayArray[5].substring(0, 1).toUpperCase()
                        + sundayArray[5].substring(1).toLowerCase();
                sundayLocation = sundayArray[6].substring(0, 1).toUpperCase()
                        + sundayArray[6].substring(1).toLowerCase();
            }
            String sundayShiftNumber = sundayArray[2];
            String sundayStartTime = "";
            String sundayEndTime = "";
            if (!sundayShiftNumber.equalsIgnoreCase("R") && !sundayShiftNumber.equalsIgnoreCase("streepjesdag") && !sundayShiftNumber.contains("VL")) {
                sundayStartTime = sundayArray[3];
                sundayEndTime = sundayArray[4];
            }

            int sundayMonth = Integer.parseInt(sundayArray[1].split("-")[1]);
            int sundayDay = Integer.parseInt(sundayArray[1].split("-")[0]);
            if (sundayArray.length <= 3)
                Log.d(TAG, "Staff " + sundayShift.getStaff().getStaffName() + " is free on sunday.");
            else {
                String sundayStartDate = "";
                if ((weekNumber == 52 || weekNumber == 53) && sundayMonth == 1) {// we passed newyear's
                    sundayStartDate = "" + sundayDay + "-" + sundayMonth + "-" + yearNumber + 1 + " " + sundayStartTime
                            + "";
                } else {
                    sundayStartDate = "" + sundayDay + "-" + sundayMonth + "-" + yearNumber + " " + sundayStartTime
                            + "";
                }
                date1 = format.parse(sundayStartTime);
                date2 = format.parse(sundayEndTime);
                cal1.setTime(date1);
                cal2.setTime(date2);
                if (cal2.getTimeInMillis() - cal1.getTimeInMillis() < 0)
                    cal2.add(Calendar.DATE, 1);

                diff = cal2.getTimeInMillis() - cal1.getTimeInMillis();
                diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                diffHours = TimeUnit.MILLISECONDS.toHours(diff);
                minutes = diffMinutes - (diffHours * 60);

                java.util.Date sundayShiftStartDate = sdf.parse(sundayStartDate);
                long sundayStartMillis = sundayShiftStartDate.getTime();
                long sundayEndMillis = sundayShiftStartDate.getTime() + diff;

                sundayShift.setShiftNumber(sundayShiftNumber);
                sundayShift.setShiftNumberModifier(sundayModifier);
                sundayShift.setLocation(sundayLocation);
                sundayShift.setStartTime(sundayShiftStartDate);
                sundayShift.setProfession(sundayProfession);
                sundayShift.setWeekNumber(weekNumber);
                sundayShift.setYearNumber(yearNumber);
                sundayShift.setLengthHours((int) diffHours);
                sundayShift.setLengthMinutes((int) minutes);
                sundayShift.setStartMillis(sundayStartMillis);
                sundayShift.setEndMillis(sundayEndMillis);

                dw.add(sundayShift);

                Log.d(TAG,
                        "staff " + sundayShift.getStaff().getStaffName() + " runs shift " + sundayShift.getLocation()
                                + sundayShift.getShiftNumber() + " at " + sundayShift.getStartTime()
                                + " with a shift length of " + sundayShift.getLengthHours() + " hours and "
                                + sundayShift.getLengthMinutes() + " minutes.");
            }

            Log.d(TAG, "//////////// END OF WEEK ////////////");

            Toast.makeText(context, "DW geladen: week " + weekNumber + " van " + yearNumber, Toast.LENGTH_SHORT).show();

            Log.d(TAG, "Finished scanning " + toRead.getPath());
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    /**
     * Reads the given file Uri & saves the contents to a String array.
     *
     * @param uri User-supplied file
     * @param c   context to use to create inputstream
     */
    private static void readFile(Uri uri, Context c) {
        try {
            InputStream inputStream = c.getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            fileContents[0] = reader.readLine(); //first line: Donderdagse Week van WW-YYYY
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
            Log.d(TAG, fileContents[12]);
        } catch (IOException e) {
            Log.e(TAG, "readFile: ", e);
        }
    }

    /**
     * Initiates the conversion of the DW file & acting as a staging method.
     *
     * @param c   context
     * @param uri user-supplied file
     */
    public void startConversion(Context c, Uri uri) {
        Log.d(TAG, "started reading file: ");
        toRead = uri;

        if (toRead == null) {
            Log.e(TAG, "No valid DW files were present to read.");
            return;
        }

        Log.d(TAG, "Using file: " + uri.getPath());
        readFile(uri, c);
        processFile(context);
    }

    /**
     * Puts all the shift details from their respective objects in to an iCalendar String using biWeekly.
     *
     * @return the full, completed iCalendar String.
     */
    public String getCalendarICS() {
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.sharedPrefs), Context.MODE_PRIVATE);
        boolean displayModifiers = prefs.getBoolean("displayModifiers", true);
        boolean fullDaysOnly = prefs.getBoolean("fullDaysOnly", false);

        String ics = "";
        ICalendar iCal = new ICalendar();
        if (!mondayShift.getShiftNumber().equals("!!!")) {

            VEvent mondayEvent = new VEvent();
            String summaryString;
            if (displayModifiers)
                summaryString = mondayShift.getShiftNumberModifier().equals("-1") ?
                        mondayShift.getLocation() + " " + mondayShift.getShiftNumber() :
                        mondayShift.getLocation() + " " + mondayShift.getShiftNumberModifier() + mondayShift.getShiftNumber();
            else summaryString = mondayShift.getLocation() + " " + mondayShift.getShiftNumber();
            Summary summary = mondayEvent.setSummary(summaryString);

            Description description = mondayEvent.setDescription(fileContents[6]);
            description.setLanguage("nl");
            summary.setLanguage("nl");

            Date start = mondayShift.getStartTime();
            if (fullDaysOnly) mondayEvent.setDateStart(Utils.atStartOfDay(start));
            else mondayEvent.setDateStart(start);

            Duration duration = fullDaysOnly ?
                    new Duration.Builder().days(1).build() :
                    new Duration.Builder().hours(mondayShift.getLengthHours()).minutes(mondayShift.getLengthMinutes()).build();
            mondayEvent.setDuration(duration);

            iCal.addEvent(mondayEvent);
        }
        if (!tuesdayShift.getShiftNumber().equals("!!!")) {

            VEvent tuesdayEvent = new VEvent();
            String summaryString;
            if (displayModifiers)
                summaryString = tuesdayShift.getShiftNumberModifier().equals("-1") ?
                        tuesdayShift.getLocation() + " " + tuesdayShift.getShiftNumber() :
                        tuesdayShift.getLocation() + " " + tuesdayShift.getShiftNumberModifier() + tuesdayShift.getShiftNumber();
            else summaryString = tuesdayShift.getLocation() + " " + tuesdayShift.getShiftNumber();
            Summary summary = tuesdayEvent.setSummary(summaryString);

            Description description = tuesdayEvent.setDescription(fileContents[7]);
            description.setLanguage("nl");
            summary.setLanguage("nl");

            Date start = tuesdayShift.getStartTime();
            if (fullDaysOnly) tuesdayEvent.setDateStart(Utils.atStartOfDay(start));
            else tuesdayEvent.setDateStart(start);

            Duration duration = fullDaysOnly ?
                    new Duration.Builder().days(1).build() :
                    new Duration.Builder().hours(tuesdayShift.getLengthHours()).minutes(tuesdayShift.getLengthMinutes()).build();
            tuesdayEvent.setDuration(duration);

            iCal.addEvent(tuesdayEvent);
        }
        if (!wednesdayShift.getShiftNumber().equals("!!!")) {

            VEvent wednesdayEvent = new VEvent();
            String summaryString;
            if (displayModifiers)
                summaryString = wednesdayShift.getShiftNumberModifier().equals("-1") ?
                        wednesdayShift.getLocation() + " " + wednesdayShift.getShiftNumber() :
                        wednesdayShift.getLocation() + " " + wednesdayShift.getShiftNumberModifier() + wednesdayShift.getShiftNumber();
            else
                summaryString = wednesdayShift.getLocation() + " " + wednesdayShift.getShiftNumber();
            Summary summary = wednesdayEvent.setSummary(summaryString);

            Description description = wednesdayEvent.setDescription(fileContents[8]);
            description.setLanguage("nl");
            summary.setLanguage("nl");

            Date start = wednesdayShift.getStartTime();
            if (fullDaysOnly) wednesdayEvent.setDateStart(Utils.atStartOfDay(start));
            else wednesdayEvent.setDateStart(start);

            Duration duration = fullDaysOnly ?
                    new Duration.Builder().days(1).build() :
                    new Duration.Builder().hours(wednesdayShift.getLengthHours()).minutes(wednesdayShift.getLengthMinutes()).build();
            wednesdayEvent.setDuration(duration);

            iCal.addEvent(wednesdayEvent);
        }
        if (!thursdayShift.getShiftNumber().equals("!!!")) {

            VEvent thursdayEvent = new VEvent();
            String summaryString;
            if (displayModifiers)
                summaryString = thursdayShift.getShiftNumberModifier().equals("-1") ?
                        thursdayShift.getLocation() + " " + thursdayShift.getShiftNumber() :
                        thursdayShift.getLocation() + " " + thursdayShift.getShiftNumberModifier() + thursdayShift.getShiftNumber();
            else summaryString = thursdayShift.getLocation() + " " + thursdayShift.getShiftNumber();
            Summary summary = thursdayEvent.setSummary(summaryString);

            Description description = thursdayEvent.setDescription(fileContents[9]);
            description.setLanguage("nl");
            summary.setLanguage("nl");

            Date start = thursdayShift.getStartTime();
            if (fullDaysOnly) thursdayEvent.setDateStart(Utils.atStartOfDay(start));
            else thursdayEvent.setDateStart(start);

            Duration duration = fullDaysOnly ?
                    new Duration.Builder().days(1).build() :
                    new Duration.Builder().hours(thursdayShift.getLengthHours()).minutes(thursdayShift.getLengthMinutes()).build();
            thursdayEvent.setDuration(duration);

            iCal.addEvent(thursdayEvent);
        }
        if (!fridayShift.getShiftNumber().equals("!!!")) {

            VEvent fridayEvent = new VEvent();
            String summaryString;
            if (displayModifiers)
                summaryString = fridayShift.getShiftNumberModifier().equals("-1") ?
                        fridayShift.getLocation() + " " + fridayShift.getShiftNumber() :
                        fridayShift.getLocation() + " " + fridayShift.getShiftNumberModifier() + fridayShift.getShiftNumber();
            else summaryString = fridayShift.getLocation() + " " + fridayShift.getShiftNumber();
            Summary summary = fridayEvent.setSummary(summaryString);

            Description description = fridayEvent.setDescription(fileContents[10]);
            description.setLanguage("nl");
            summary.setLanguage("nl");

            Date start = fridayShift.getStartTime();
            if (fullDaysOnly) fridayEvent.setDateStart(Utils.atStartOfDay(start));
            else fridayEvent.setDateStart(start);

            Duration duration = fullDaysOnly ?
                    new Duration.Builder().days(1).build() :
                    new Duration.Builder().hours(fridayShift.getLengthHours()).minutes(fridayShift.getLengthMinutes()).build();
            fridayEvent.setDuration(duration);

            iCal.addEvent(fridayEvent);
        }
        if (!saturdayShift.getShiftNumber().equals("!!!")) {

            VEvent saturdayEvent = new VEvent();
            String summaryString;
            if (displayModifiers)
                summaryString = saturdayShift.getShiftNumberModifier().equals("-1") ?
                        saturdayShift.getLocation() + " " + saturdayShift.getShiftNumber() :
                        saturdayShift.getLocation() + " " + saturdayShift.getShiftNumberModifier() + saturdayShift.getShiftNumber();
            else summaryString = saturdayShift.getLocation() + " " + saturdayShift.getShiftNumber();
            Summary summary = saturdayEvent.setSummary(summaryString);

            Description description = saturdayEvent.setDescription(fileContents[11]);
            description.setLanguage("nl");
            summary.setLanguage("nl");

            Date start = saturdayShift.getStartTime();
            if (fullDaysOnly) saturdayEvent.setDateStart(Utils.atStartOfDay(start));
            else saturdayEvent.setDateStart(start);

            Duration duration = fullDaysOnly ?
                    new Duration.Builder().days(1).build() :
                    new Duration.Builder().hours(saturdayShift.getLengthHours()).minutes(saturdayShift.getLengthMinutes()).build();
            saturdayEvent.setDuration(duration);

            iCal.addEvent(saturdayEvent);
        }
        if (!sundayShift.getShiftNumber().equals("!!!")) {

            VEvent sundayEvent = new VEvent();
            String summaryString;
            if (displayModifiers)
                summaryString = sundayShift.getShiftNumberModifier().equals("-1") ?
                        sundayShift.getLocation() + " " + sundayShift.getShiftNumber() :
                        sundayShift.getLocation() + " " + sundayShift.getShiftNumberModifier() + sundayShift.getShiftNumber();
            else summaryString = sundayShift.getLocation() + " " + sundayShift.getShiftNumber();
            Summary summary = sundayEvent.setSummary(summaryString);

            Description description = sundayEvent.setDescription(fileContents[12]);
            description.setLanguage("nl");
            summary.setLanguage("nl");

            Date start = sundayShift.getStartTime();
            if (fullDaysOnly) sundayEvent.setDateStart(Utils.atStartOfDay(start));
            else sundayEvent.setDateStart(start);

            Duration duration = fullDaysOnly ?
                    new Duration.Builder().days(1).build() :
                    new Duration.Builder().hours(sundayShift.getLengthHours()).minutes(sundayShift.getLengthMinutes()).build();
            sundayEvent.setDuration(duration);

            iCal.addEvent(sundayEvent);
        }

        ics += Biweekly.write(iCal).go();
        return ics;

    }

    /**
     * @return the entire original DW file contents in form of a String.
     */
    public String fullFileString() {
        String str = "";
        for (String fileContent : fileContents) {
            str += fileContent + "\n";
        }
        return str;
    }
}
