/**Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Green team Module 5
Modified by Max on 9-4-26

*/
package com.moffatbaymarina.model;

import java.time.LocalDateTime;


 // using the email verification code this is a data holder that mirrors one row in the table for ref. see the emailVerification dao. 
public class EmailVerification {
    private long verificationId;
    private long customerId;
    private String tokenHash;// storing a sha256 hash, the demo seed stores plain txt. the dao file accepts either
    private LocalDateTime expiresAt;   
    private LocalDateTime verifiedAt; // null until verification is complete 
    private LocalDateTime createdAt;

    public EmailVerification() {}

    // constructor field order match for EmailVerificationDAO
    public EmailVerification(long verificationId, long customerId, String tokenHash,
                             LocalDateTime expiresAt, LocalDateTime verifiedAt,
                             LocalDateTime createdAt) {
        this.verificationId = verificationId;
        this.customerId = customerId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.verifiedAt = verifiedAt;
        this.createdAt = createdAt;
    }

    public long getVerificationId() { return verificationId; }
    public void setVerificationId(long verificationId) { this.verificationId = verificationId; }
    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
