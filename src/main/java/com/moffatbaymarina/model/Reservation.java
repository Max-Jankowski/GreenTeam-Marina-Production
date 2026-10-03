
/**Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Green team Module 5
Modified by Max on 9-4-26
Modified by Max 9-17-26

*/
package com.moffatbaymarina.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


 // data holder for a row in the reservations table. see dao for the queries 
public class Reservation {
    private long reservationId;
    private long customerId;
    private long boatId;
    private long slipId;
    private LocalDate checkInDate;
	private LocalDate departureDate; // ADDED FIELD FOR DEPARTURE DATE 9-17-26
    private String expectedTerm; // free text here so 6 months 12 months etc
    private boolean electricIncluded; // added for electric 9-10-26 Max
    private BigDecimal monthlyCost; // we still need to work on this, I think we should make elec. optional when calculating cost
    private String status; // status can be pending, confirmed or cancelled
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt; // this is null unless the status is listed as canceled. 

    public Reservation() {}

    // other constructor that field matches  ReservationDAO's map() method
    public Reservation(long reservationId, long customerId, long boatId, long slipId,
                       LocalDate checkInDate, LocalDate departureDate, String expectedTerm, BigDecimal monthlyCost, //ADDED the departure 
                       boolean electricIncluded, String status, LocalDateTime createdAt, LocalDateTime cancelledAt) {
        this.reservationId = reservationId;
        this.customerId = customerId;
        this.boatId = boatId;
        this.slipId = slipId;
        this.checkInDate = checkInDate;
		this.departureDate = departureDate; // The ADDED Parameter 
        this.expectedTerm = expectedTerm;
        this.monthlyCost = monthlyCost;
        this.electricIncluded = electricIncluded; // added 9-10-26 for electrical, 
        this.status = status;
        this.createdAt = createdAt;
        this.cancelledAt = cancelledAt;
    }

    public long getReservationId() { return reservationId; }
    public void setReservationId(long reservationId) { this.reservationId = reservationId; }
    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
    public long getBoatId() { return boatId; }
    public void setBoatId(long boatId) { this.boatId = boatId; }
    public long getSlipId() { return slipId; }
    public void setSlipId(long slipId) { this.slipId = slipId; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
	public LocalDate getDepartureDate() { return departureDate; } // ADDED getter for depart 9-17-26
	public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; } // ADDED setter for depart date 
    public String getExpectedTerm() { return expectedTerm; }
    public void setExpectedTerm(String expectedTerm) { this.expectedTerm = expectedTerm; }
    public BigDecimal getMonthlyCost() { return monthlyCost; }
    public void setMonthlyCost(BigDecimal monthlyCost) { this.monthlyCost = monthlyCost; }
    public boolean isElectricIncluded() { return electricIncluded; }
    public void setElectricIncluded(boolean electricIncluded) { this.electricIncluded = electricIncluded; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
