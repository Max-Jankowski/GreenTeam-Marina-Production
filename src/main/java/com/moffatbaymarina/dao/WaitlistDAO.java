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
import java.util.ArrayList;
import java.util.List;

import com.moffatbaymarina.config.DatabaseConnection;
import com.moffatbaymarina.model.WaitlistEntry;

// creating a DAO for the waitlist_entries table
public class WaitlistDAO {

    // adding a cust/boat to the the list for a particular slip type. used it own
    // short-lived' connection as opposed to the other reservation daos
    public WaitlistEntry insert(WaitlistEntry entry) throws SQLException {
        String sql = """
                INSERT INTO waitlist_entries
                (customer_id, boat_id, slip_type_id, status)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, entry.getCustomerId());
            statement.setLong(2, entry.getBoatId());
            statement.setLong(3, entry.getSlipTypeId());
            statement.setString(4, entry.getStatus());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Waitlist insert did not create one row.");
            }

            try (ResultSet keys = statement.getGeneratedKeys()) { // auto increment
                if (!keys.next())
                    throw new SQLException("No waitlist_id was generated.");
                entry.setWaitlistId(keys.getLong(1));
            }
        }
        return entry;
    }

    // checking to see if this boat is already on the waitlist of this slip type.
    // this prevent a double sign up
    public boolean hasActiveEntry(long customerId, long boatId, long slipTypeId)
            throws SQLException {
        String sql = """
                SELECT waitlist_id FROM waitlist_entries
                WHERE customer_id = ? AND boat_id = ? AND slip_type_id = ?
                  AND UPPER(status) IN ('WAITING', 'CONTACTED')
                LIMIT 1
                """;
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setLong(2, boatId);
            statement.setLong(3, slipTypeId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    // returning every list entry a client has, starting at oldest
    public List<WaitlistEntry> findByCustomerId(long customerId) throws SQLException {
        String sql = """
                SELECT * FROM waitlist_entries
                WHERE customer_id = ?
                ORDER BY joined_at, waitlist_id
                """;
        List<WaitlistEntry> entries = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next())
                    entries.add(map(result));
            }
        }
        return entries;
    }

    // calculating the 1 based postion of the entry for the slip. counts the entries
    // that are still waiting
    // the calculation happens on the fly from the joined_at rather then a
    // pre-stored value. This in concept
    // should stay current as other clients leave the list.
    public int getPosition(WaitlistEntry entry) throws SQLException {
        if (entry.getJoinedAt() == null)
            return 0;
        String sql = """
                SELECT COUNT(*) FROM waitlist_entries
                WHERE slip_type_id = ? AND UPPER(status) = 'WAITING'
                  AND (joined_at < ? OR (joined_at = ? AND waitlist_id <= ?))
                """;
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            Timestamp joined = Timestamp.valueOf(entry.getJoinedAt());
            statement.setLong(1, entry.getSlipTypeId());
            statement.setTimestamp(2, joined);
            statement.setTimestamp(3, joined);
            statement.setLong(4, entry.getWaitlistId());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    // added to count customers currently 'WAITING' for a given slip type, with NO
    // customer_id filter at all. name-free number the public wait list page still needs: show counts only, never who is on the list. 
    public int countWaiting(long slipTypeId) throws SQLException {
        // made an addtion here on  9-19 same shape as SlipDAO.countAvailable, just against waitlist_entries
        String sql = """
                SELECT COUNT(*) FROM waitlist_entries
                WHERE slip_type_id = ? AND UPPER(status) = 'WAITING'
                """;
        try (Connection connection = DatabaseConnection.getConnection(); // Large addtion to method below
                PreparedStatement statement = connection.prepareStatement(sql)) { 
            statement.setLong(1, slipTypeId); 
            try (ResultSet result = statement.executeQuery()) { 
                result.next();
                return result.getInt(1); 
            }
        }
    }

    // conversion to @link
    private WaitlistEntry map(ResultSet result) throws SQLException {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setWaitlistId(result.getLong("waitlist_id"));
        entry.setCustomerId(result.getLong("customer_id"));
        entry.setBoatId(result.getLong("boat_id"));
        entry.setSlipTypeId(result.getLong("slip_type_id"));
        Timestamp joined = result.getTimestamp("joined_at");
        entry.setJoinedAt(joined == null ? null : joined.toLocalDateTime());
        entry.setStatus(result.getString("status"));
        return entry;
    }
}
