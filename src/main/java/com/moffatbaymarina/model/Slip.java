/**Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Green team Module 5
Modified by Max on 9-5-26

*/

package com.moffatbaymarina.model;


 // holder for data the mirrors a row in the slips table. one slip as numbered in the overveiw chart
public class Slip {
    private long slipId;
    private long slipTypeId; // this indicated the physial size of the slip category this slip fits in 
    private String slipNumber; // This represents the user and worker readable slip number
    private String status; // this can be available, reserved or held

    public Slip() {}

    // DAO mathcing full-argument constructor. Using map() 
    public Slip(long slipId, long slipTypeId, String slipNumber, String status) {
        this.slipId = slipId;
        this.slipTypeId = slipTypeId;
        this.slipNumber = slipNumber;
        this.status = status;
    }

    public long getSlipId() { return slipId; }
    public void setSlipId(long slipId) { this.slipId = slipId; }
    public long getSlipTypeId() { return slipTypeId; }
    public void setSlipTypeId(long slipTypeId) { this.slipTypeId = slipTypeId; }
    public String getSlipNumber() { return slipNumber; }
    public void setSlipNumber(String slipNumber) { this.slipNumber = slipNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
