package com.treinchauffeur.mijndw.obj;

import android.annotation.SuppressLint;

/**
 * @author Leonk
 * Defines a staff as an object
 */
public class Staff {

    private String staffName;
    private int staffNumber;

    /**
     * A staff member object has a name and a staff number.
     */
    public Staff() {
        this.staffName = "";
        this.staffNumber = -1;
    }

    /**
     * @return the staffName
     */
    public String getStaffName() {
        return staffName;
    }

    /**
     * @param staffName the staffName to set
     */
    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    /**
     * @return the staffNumber
     */
    @SuppressLint("UnusedResources")
    protected int getStaffNumber() {
        return staffNumber;
    }

    /**
     * @param staffNumber the staffNumber to set
     */
    public void setStaffNumber(int staffNumber) {
        this.staffNumber = staffNumber;
    }



}
