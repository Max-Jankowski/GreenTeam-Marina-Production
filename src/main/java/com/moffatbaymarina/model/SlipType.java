/**Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Green team Module 5
Modified by Max on 9-5-26

*/

package com.moffatbaymarina.model;

import java.math.BigDecimal;


 //holder for row data the mirrors the slips_type table. 
public class SlipType {
    private long slipTypeId;
    private BigDecimal sizeFt;// slip type is in feet units, the slip needs to be greater then or equal to boat. 
    private int totalCapacity; // holds the ammount of slips in this size that the marina has in total 
    private BigDecimal ratePerFoot;// Again this is combined with the elec. which we still need to discuss if it is optional for the user/ baot reservation
    private BigDecimal electricFee;

    public SlipType() {}

    // agian a full-argument constructor, see the relevent DAO
    public SlipType(long slipTypeId, BigDecimal sizeFt, int totalCapacity,
                    BigDecimal ratePerFoot, BigDecimal electricFee) {
        this.slipTypeId = slipTypeId;
        this.sizeFt = sizeFt;
        this.totalCapacity = totalCapacity;
        this.ratePerFoot = ratePerFoot;
        this.electricFee = electricFee;
    }

    public long getSlipTypeId() { return slipTypeId; }
    public void setSlipTypeId(long slipTypeId) { this.slipTypeId = slipTypeId; }
    public BigDecimal getSizeFt() { return sizeFt; }
    public void setSizeFt(BigDecimal sizeFt) { this.sizeFt = sizeFt; }
    public int getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }
    public BigDecimal getRatePerFoot() { return ratePerFoot; }
    public void setRatePerFoot(BigDecimal ratePerFoot) { this.ratePerFoot = ratePerFoot; }
    public BigDecimal getElectricFee() { return electricFee; }
    public void setElectricFee(BigDecimal electricFee) { this.electricFee = electricFee; }
}
