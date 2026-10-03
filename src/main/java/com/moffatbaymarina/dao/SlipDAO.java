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
import java.util.ArrayList;
import java.util.List;

import com.moffatbaymarina.config.DatabaseConnection;
import com.moffatbaymarina.model.Slip;

// DAO for the slips table
public class SlipDAO {

    // Looks up a slip by the primary key
    public Slip findById(long slipId) throws SQLException {
        String sql = "SELECT * FROM slips WHERE slip_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, slipId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    // Looks up slip by the person readable value, so something like A8 as the
    // overview has presented it to us.
    // This will also aid in working with the future interactive feature
    public Slip findBySlipNumber(String slipNumber) throws SQLException {
        String sql = "SELECT * FROM slips WHERE slip_number = ? LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, slipNumber);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    // returns every slip of a given size, this is order by slip number for
    // availiable display
    public List<Slip> findBySlipType(long slipTypeId) throws SQLException {
        String sql = "SELECT * FROM slips WHERE slip_type_id = ? ORDER BY slip_number";
        List<Slip> slips = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, slipTypeId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    slips.add(map(result));
                }
            }
        }
        return slips;
    }

    // locates 1 available slipe of a particular type and locks the row with FOR
    // UPDATE. It can then be claimed as part of a cleint reservation.
    // This is an important part that prevents overbooking. So if 2 customers try to
    // reserve the last slip of a specific size
    // the second transaction block on this query until the first client commits or
    // the process is rolled back. The last thing we want if to both be able to
    // successfully read the same slip availability and book it.
    // This is called with auto commit off and the action should update the slip
    // status.
    public Slip findAvailableForUpdate(Connection connection, long slipTypeId)
            throws SQLException {
        String sql = """
                SELECT * FROM slips
                WHERE slip_type_id = ? AND UPPER(status) = 'AVAILABLE'
                ORDER BY slip_number
                LIMIT 1
                FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, slipTypeId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    // counting how many slip of a size are still available. Value shown on the
    // 'availability' page just display count for now.
    public int countAvailable(long slipTypeId) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM slips
                WHERE slip_type_id = ? AND UPPER(status) = 'AVAILABLE'
                """;
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, slipTypeId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    // updating the slip status, from available to reserved when reservation action
    // is created. Or back to available when cancel action taken

    public boolean updateStatus(Connection connection, long slipId, String status)
            throws SQLException {
        String sql = "UPDATE slips SET status = ? WHERE slip_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, slipId);
            return statement.executeUpdate() == 1;
        }
    }

    // converts one row of slips to link slips
    private Slip map(ResultSet result) throws SQLException {
        return new Slip(
                result.getLong("slip_id"),
                result.getLong("slip_type_id"),
                result.getString("slip_number"),
                result.getString("status"));
    }
}
