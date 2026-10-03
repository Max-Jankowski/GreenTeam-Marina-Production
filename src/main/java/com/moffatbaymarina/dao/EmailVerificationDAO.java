/** 

Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Green team Module 5
Modified by Max on 9-3-26

*/
package com.moffatbaymarina.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import com.moffatbaymarina.config.DatabaseConnection;
import com.moffatbaymarina.model.EmailVerification;

// The DAO for the email_verifications table, that satisfies the UST-04 story. Token is gernerated and email to the 
// Client. This is matched back against the table once link is selected by customer. 
public class EmailVerificationDAO {

    // Inserts a record for a new verification. takes a caller supplied conenction,
    // as this is made using a registration action
    public EmailVerification insert(Connection connection, EmailVerification verification)
            throws SQLException {
        String sql = """
                INSERT INTO email_verifications
                (customer_id, token_hash, expires_at, verified_at)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, verification.getCustomerId());
            statement.setString(2, verification.getTokenHash());
            statement.setTimestamp(3, Timestamp.valueOf(verification.getExpiresAt()));
            if (verification.getVerifiedAt() == null)
                statement.setNull(4, java.sql.Types.TIMESTAMP); // begins as null for new verification, gets set later
                                                                // when markVerified triggers
            else
                statement.setTimestamp(4, Timestamp.valueOf(verification.getVerifiedAt()));
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Email verification insert failed.");
            }

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next())
                    throw new SQLException("No verification_id was generated.");
                verification.setVerificationId(keys.getLong(1));
            }
        }
        return verification;
    }

    // as discussed on team chat, the registration will store SHA256 token hashes.
    // The seed data stores demo token in text, for this excercise both are
    // accecpted
    public EmailVerification findValid(String rawToken, String hashedToken) throws SQLException {
        String sql = """
                SELECT * FROM email_verifications
                WHERE (token_hash = ? OR token_hash = ?)
                  AND verified_at IS NULL
                  AND expires_at >= CURRENT_TIMESTAMP
                ORDER BY verification_id DESC
                LIMIT 1
                """;
        // order match the sql. checks hased form, then checks the plain txt form.
        // Either a real hash token or txt will match.
        // verified_at is null and expires at in the future keep al or expired tokens
        // from matching
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hashedToken);
            statement.setString(2, rawToken);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    // marks a verification as complete by placing a current timestamp at
    // verified_at cell. only happens once per record.
    // this prevents a change even if the client re-clicks the verification link.
    public boolean markVerified(Connection connection, long verificationId) throws SQLException {
        String sql = """
                UPDATE email_verifications
                SET verified_at = CURRENT_TIMESTAMP
                WHERE verification_id = ? AND verified_at IS NULL
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, verificationId);
            return statement.executeUpdate() == 1;
        }
    }

    // Like other DAOs converts the email_verifications into linked.
    private EmailVerification map(ResultSet result) throws SQLException {
        EmailVerification verification = new EmailVerification();
        verification.setVerificationId(result.getLong("verification_id"));
        verification.setCustomerId(result.getLong("customer_id"));
        verification.setTokenHash(result.getString("token_hash"));
        Timestamp expires = result.getTimestamp("expires_at");
        verification.setExpiresAt(expires == null ? null : expires.toLocalDateTime());
        Timestamp verified = result.getTimestamp("verified_at");
        verification.setVerifiedAt(verified == null ? null : verified.toLocalDateTime());
        Timestamp created = result.getTimestamp("created_at");
        verification.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return verification;
    }
}
