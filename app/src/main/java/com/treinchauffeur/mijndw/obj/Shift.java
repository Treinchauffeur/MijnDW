package com.treinchauffeur.mijndw.obj;

import java.util.Date;

/**
 *
 * @author Leonk Defines a shift as an object
 */

public class Shift {
    private Staff staff;
    private int weekNumber;
    private int yearNumber;
    private Date startTime;
    private String shiftNumber;
    private String shiftNumberModifier;
    private String profession;
    private String location;
    private int lengthHours, lengthMinutes;
    private long startMillis, endMillis;

    private String rawString;

    public Shift() {
        super();
        this.staff = null;
        this.weekNumber = -1;
        this.yearNumber = -1;
        this.startTime = new Date();
        this.shiftNumber = "!!!";
        this.shiftNumberModifier = "!!!";
        this.profession = "!!!";
        this.location = "!!!";
        this.rawString = "!!!";
    }

    public String getRawString() {
        return rawString;
    }

    public void setRawString(String rawString) {
        this.rawString = rawString;
    }

    /**
     * @param weekNumber the weekNumber to set
     */
    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }

    /**
     * @param yearNumber the yearNumber to set
     */
    public void setYearNumber(int yearNumber) {
        this.yearNumber = yearNumber;
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


    public int getWeekNumber() {
        return weekNumber;
    }

    public int getYearNumber() {
        return yearNumber;
    }
}
