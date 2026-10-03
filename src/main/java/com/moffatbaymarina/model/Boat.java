/**Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Green team Module 5
Modified by Max on 9-4-26

*/

package com.moffatbaymarina.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;


 // data holder that mirrors a row from boats table. linked to com.moffatbaymarina.dao.boatDAO for queries that read and write
public class Boat {
    private long boatId;
    private long customerId;
    private String boatName;   
    private BigDecimal boatLengthFt; //boat length in ft, this required for size lookup
    private String boatType; // This can be used as optional info, maybe used for future purposes. Can also be null    
    private String registrationNumber;
    private LocalDateTime createdAt;

    public Boat() {}

	// argument constructor. matches boatDAO's map method 
    public Boat(long boatId, long customerId, String boatName, BigDecimal boatLengthFt,
                String boatType, String registrationNumber, LocalDateTime createdAt) {
        this.boatId = boatId;
        this.customerId = customerId;
        this.boatName = boatName;
        this.boatLengthFt = boatLengthFt;
        this.boatType = boatType;
        this.registrationNumber = registrationNumber;
        this.createdAt = createdAt;
    }

    public long getBoatId() { return boatId; }
    public void setBoatId(long boatId) { this.boatId = boatId; }
    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
    public String getBoatName() { return boatName; }
    public void setBoatName(String boatName) { this.boatName = boatName; }
    public BigDecimal getBoatLengthFt() { return boatLengthFt; }
    public void setBoatLengthFt(BigDecimal boatLengthFt) { this.boatLengthFt = boatLengthFt; }
    public String getBoatType() { return boatType; }
    public void setBoatType(String boatType) { this.boatType = boatType; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
