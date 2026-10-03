
/**Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Green team Module 5
Modified by Max on 9-4-26

*/
package com.moffatbaymarina.model;

import java.time.LocalDateTime;

 // like the boats.java file this is data holder that mirrors a row from Customers table. linked to com.moffatbaymarina.Customerdao. for queries that read and write
public class Customer {
    private long customerId;
    private String firstName;
    private String lastName;
    private String phone;
    private String street;
    private String city;
    private String state;
    private String zip;
    // doubles as the login username as coded in CustomerDAO.findByEmail. */
    private String email;
    
	 // expected to be hashed when the field is being filled, its a bad practice to leave this in plain txt
    private String passwordHash;
    
	 // This is turned to true after the verification flow is finished and email verified 
    private boolean emailVerified;
    private LocalDateTime createdAt;

    public Customer() {}

    /** argument constructor for field order match CustomerDAO using map() method. */
    public Customer(long customerId, String firstName, String lastName, String phone,
                    String street, String city, String state, String zip, String email,
                    String passwordHash, boolean emailVerified, LocalDateTime createdAt) {
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.street = street;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.email = email;
        this.passwordHash = passwordHash;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt;
    }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
