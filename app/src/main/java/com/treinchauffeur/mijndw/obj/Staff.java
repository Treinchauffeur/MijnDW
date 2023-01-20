package com.treinchauffeur.mijndw.obj;

import java.util.ArrayList;

/**
 *
 * @author Leonk
 * Defines a staff as an object
 */

public class Staff {

    private String staffName;
    private int staffNumber;
    private ArrayList<Shift> shifts;

    public Staff(String staffName, int staffNumber, ArrayList<Shift> shifts) {
        super();
        this.staffName = staffName;
        this.staffNumber = staffNumber;
        this.shifts = shifts;
    }

    public Staff() {
        this.staffName = "";
        this.staffNumber = -1;
        this.shifts = null;
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
    public int getStaffNumber() {
        return staffNumber;
    }

    /**
     * @param staffNumber the staffNumber to set
     */
    public void setStaffNumber(int staffNumber) {
        this.staffNumber = staffNumber;
    }



}
