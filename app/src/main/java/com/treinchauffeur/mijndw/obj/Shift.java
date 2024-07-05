package com.treinchauffeur.mijndw.obj;

import java.util.Date;

/**
 * @author treinchauffeur
 * Defines a shift as an object
 */
public class Shift {
    private Staff staff;
    private Date startTime;
    private String shiftNumber;
    private String shiftNumberModifier;
    private String profession;
    private String location;
    private int lengthHours, lengthMinutes;
    private long startMillis, endMillis;

    private String rawString;

    /**
     * A shift can have a number of values like the member of staff, a starting time, a number (or other shift title),
     * a profession and more. We use these details to convert them to a .iCal file.
     */
    public Shift() {
        super();
        this.staff = null;
        this.startTime = new Date();
        this.shiftNumber = "!!!";
        this.shiftNumberModifier = "!!!";
        this.profession = "!!!";
        this.location = "!!!";
        this.rawString = "!!!";
    }

    /**
     * @return the raw line string for that specific shift.
     * This is used as the description of the calendar event.
     */
    public String getRawString() {
        return rawString;
    }

    /**
     * Sets the raw line string for that specific shift.
     * This is used as the description of the calendar event.
     */
    public void setRawString(String rawString) {
        this.rawString = rawString;
    }

    /**
     * @return the startTime
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the shiftNumber
     */
    public String getShiftNumber() {
        return shiftNumber;
    }

    /**
     * @param shiftNumber the shiftNumber to set
     */
    public void setShiftNumber(String shiftNumber) {
        this.shiftNumber = shiftNumber;
    }

    /**
     * @return the shiftNumberModifier
     */
    public String getShiftNumberModifier() {
        return shiftNumberModifier;
    }

    /**
     * @param shiftNumberModifier the shiftNumberModifier to set
     */
    public void setShiftNumberModifier(String shiftNumberModifier) {
        this.shiftNumberModifier = shiftNumberModifier;
    }

    /**
     * @return the profession
     */
    public String getProfession() {
        return profession;
    }

    /**
     * @param profession the profession to set
     */
    public void setProfession(String profession) {
        this.profession = profession;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return the staff
     */
    public Staff getStaff() {
        return staff;
    }

    /**
     * @param staff the staff to set
     */
    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    /**
     * @return the lengthHours
     */
    public int getLengthHours() {
        return lengthHours;
    }

    /**
     * @param lengthHours the lengthHours to set
     */
    public void setLengthHours(int lengthHours) {
        this.lengthHours = lengthHours;
    }

    /**
     * @return the lengthMinutes
     */
    public int getLengthMinutes() {
        return lengthMinutes;
    }

    /**
     * @param lengthMinutes the lengthMinutes to set
     */
    public void setLengthMinutes(int lengthMinutes) {
        this.lengthMinutes = lengthMinutes;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public void setStartMillis(long startMillis) {
        this.startMillis = startMillis;
    }

    public long getEndMillis() {
        return endMillis;
    }

    public void setEndMillis(long endMillis) {
        this.endMillis = endMillis;
    }

    public String getNeatShiftNumber() {
        switch(shiftNumber.toLowerCase()) {
            case "r":
                return "Rustdag";
            case "streepjesdag":
            case "--":
                return "Streepjesdag";
            case "gvl":
                return "Gegarandeerd verlof";
            case "vl":
                return "Verlof";
            case "wr":
                return "WTV rustdag";
            case "wa":
                return "WTV aangewezen rustdag";
            case "wv":
                return "WTV vrij opgenomen rustdag";
            case "ro":
                return "RO-dag";
            case "ow":
                return "OW-dag";
            case "rt":
                return "Rustdag terug";
            case "ot":
                return "Overuren terug";
            case "co":
                return "Compensatie onregelmatigheid";
            case "cf":
                return "Compensatie feestdag";
            case "f":
                return "Feestdag";
            case "mt":
                return "Meeruren terug";
            case "curs":
                return "Cursus";
            case "p":
                return "Praktijk";
            case "mons":
                return "Medisch onderzoek";
            case "pons":
                return "Psychologisch onderzoek";
            case "mat":
                return "Materieel(cursus)";
            case "w":
                return "Wegleren/wegonderhoud";
            case "ba":
                return "Betaald afwezig";
            case "oa":
                return "Onbetaald afwezig";
            case "eg":
                return "Extra gezinsverlof";
            default:
                return shiftNumber;
        }
    }
}
