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
import com.moffatbaymarina.model.Boat;

// This is a DAO object for the boats table.
// each boat belongs to only on client and a client can have multiple boats.
public class BoatDAO {

    // inserts a boat using its connection, overlaod for callers that arent inside
    // the transaction
    public Boat insert(Boat boat) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return insert(connection, boat);
        }
    }

    // using caller made connection to insert a new boat. this can be used in a
    // larger 'transaction' when registering a client along with the first boat.
    // connection opened but jnot closed using this method. The boat is inserted and
    // boat_id generated. Throw exeptiopn if inserts doesnt affect a row.
    public Boat insert(Connection connection, Boat boat) throws SQLException {
        String sql = """
                INSERT INTO boats
                (customer_id, boat_name, boat_length_ft, boat_type, registration_number)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, boat.getCustomerId());
            statement.setString(2, boat.getBoatName());
            statement.setBigDecimal(3, boat.getBoatLengthFt());
            // boat_type and registration_number are optional fields on the form as of now
            // a blank becomes a sql null as opposed to a empty "" string
            statement.setString(4, blankToNull(boat.getBoatType()));
            statement.setString(5, blankToNull(boat.getRegistrationNumber()));
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Boat insert did not create one row.");
            }
            // reads back from gen keys. boat id isnt known until after the insert runs
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No boat_id was generated.");
                }
                boat.setBoatId(keys.getLong(1));
            }
        }
        return boat;
    }

    // looking up a boat by the primary key, no ownership check is being used.
    // returns matching boat. Null if record not found
    public Boat findById(long boatId) throws SQLException {
        String sql = "SELECT * FROM boats WHERE boat_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, boatId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    // Searchs boat by the primary key. returns only if it belongs to the given
    // customer. this is used when a client is looking for
    // "their vessel". This way other customers cant view other cleints boat
    // information.
    // Searchs boat by the primary key. returns only if it belongs to the given
    // customer. this is used when a client is looking for
    // "their vessel". This way other customers cant view other cleints boat
    // information.
    public Boat findForCustomer(long boatId, long customerId) throws SQLException {
        String sql = "SELECT * FROM boats WHERE boat_id = ? AND customer_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, boatId);
            statement.setLong(2, customerId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    // looking up one clients boats by case sensative name. This is used during the
    // reservation process so a returning client can pick one of their
    // existing boats. This rather then having them re-enter boat data.
    public Boat findByCustomerAndName(long customerId, String boatName) throws SQLException {
        String sql = """
                SELECT * FROM boats
                WHERE customer_id = ? AND LOWER(boat_name) = LOWER(?)
                LIMIT 1
                """;
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setString(2, boatName);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    // returns each boat a client owns, old first to display on account profile
    public List<Boat> findByCustomerId(long customerId) throws SQLException {
        String sql = "SELECT * FROM boats WHERE customer_id = ? ORDER BY created_at, boat_id";
        List<Boat> boats = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    boats.add(map(result));
                }
            }
        }
        return boats;
    }

    // one row of boats is converted into @link Boat. this assumes that cursor is on
    // a valid row.
    private Boat map(ResultSet result) throws SQLException {
        Boat boat = new Boat();
        boat.setBoatId(result.getLong("boat_id"));
        boat.setCustomerId(result.getLong("customer_id"));
        boat.setBoatName(result.getString("boat_name"));
        boat.setBoatLengthFt(result.getBigDecimal("boat_length_ft"));
        boat.setBoatType(result.getString("boat_type"));
        boat.setRegistrationNumber(result.getString("registration_number"));
        // Timestamp: LocalDateTime, but only if the column wasn't null
        Timestamp created = result.getTimestamp("created_at");
        boat.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return boat;
    }

    // treating whitespace or null as no value. returns null. if not then returns
    // trimmed value.
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
