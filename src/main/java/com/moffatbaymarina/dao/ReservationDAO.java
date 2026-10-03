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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.moffatbaymarina.config.DatabaseConnection;
import com.moffatbaymarina.model.Reservation;

//DAO class for the reservations table
public class ReservationDAO {

    // Inserts a reservation in the reservations table. Agian we are using caller
    // supplied connection
    // as this occurs along with the slip status update in the same action.
    public Reservation insert(Connection connection, Reservation reservation) throws SQLException {
        String sql = """
                INSERT INTO reservations
                (customer_id, boat_id, slip_id, check_in_date, departure_date,
                 expected_term, monthly_cost, electric_included, status, cancelled_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """; // ADDED DEPARTURE_DATE COLUMN, PLACEHOLDER COUNT NOW MATCHES 10 COLUMNS
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, reservation.getCustomerId());
            statement.setLong(2, reservation.getBoatId());
            statement.setLong(3, reservation.getSlipId());
            statement.setDate(4, Date.valueOf(reservation.getCheckInDate()));

            if (reservation.getDepartureDate() == null) { // ADDED null check for deaparture input 9-1726
                statement.setNull(5, java.sql.Types.DATE); // ADDED binding null for depart
            } else {
                statement.setDate(5, Date.valueOf(reservation.getDepartureDate())); // ADDED BINDING FOR DEPARTURE DATE
            }

            statement.setString(6, reservation.getExpectedTerm()); // Shifted numbers 
            statement.setBigDecimal(7, reservation.getMonthlyCost()); 
            statement.setBoolean(8, reservation.isElectricIncluded()); // addition for elec opt in Max 9-10-26 - SHIFTED FROM PARAMETER 7 TO 8
            statement.setString(9, reservation.getStatus()); 

            if (reservation.getCancelledAt() == null) { // a new reservation is never 'already' canceled. The column
                                                        // still has to be given a null value
                statement.setNull(10, java.sql.Types.TIMESTAMP); 
            } else {
                statement.setTimestamp(10, Timestamp.valueOf(reservation.getCancelledAt())); 
            }
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Reservation insert did not create one row.");
            }

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No reservation_id was generated.");
                }
                reservation.setReservationId(keys.getLong(1));
            }
        }
        return reservation;
    }

    /**
     * Looks up a reservation by primary key, using its own short-lived
     * connection - for read-only lookups outside of any transaction.
   
     */
    public Reservation findById(long reservationId) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE reservation_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reservationId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    // Same as the findbyId lookup. though this takes a caller supplied conn and
    // adds FOR UPDATE to put in a row lock on result.
    // this to be used if the reservation is to be changed in a transaction. in this
    // case the second request
    // touchs the same reservation as the same time has to wait rather then rush to
    // this one.
    public Reservation findByIdForUpdate(Connection connection, long reservationId)
            throws SQLException {
        String sql = "SELECT * FROM reservations WHERE reservation_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reservationId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    // returning each reservation a client made. the most recent first to provide an
    // account history
    public List<Reservation> findByCustomerId(long customerId) throws SQLException {
        String sql = """
                SELECT * FROM reservations
                WHERE customer_id = ?
                ORDER BY created_at DESC, reservation_id DESC
                """;
        List<Reservation> reservations = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    reservations.add(map(result));
                }
            }
        }
        return reservations;
    }

    // backing the look up you reservation page. search possible by ID, email of
    // both. either of these parameters can be left null.
    // So it should cover all search combos to make it easier for the cleint.
    // our sql is build up from Where 1 = 1, which is true so every filter can be
    // appended as a AND clause without having any special case logic.
    public List<Reservation> search(Long reservationId, String email) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT r.* FROM reservations r
                JOIN customers c ON c.customer_id = r.customer_id
                WHERE 1 = 1
                """);
        if (reservationId != null) {
            sql.append(" AND r.reservation_id = ?");
        }
        if (email != null && !email.isBlank()) {
            sql.append(" AND LOWER(c.email) = LOWER(?)");
        }
        sql.append(" ORDER BY r.created_at DESC, r.reservation_id DESC");

        List<Reservation> reservations = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            // As either filter above may have been skipped, the ? placeholders dont land on
            // a fixed parameter value.
            // parameterIndex tracks the next free slot and advances only when value is
            // bound.
            // https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgdg/using-bind-variables.html

            int parameterIndex = 1;

            if (reservationId != null) {
                statement.setLong(parameterIndex++, reservationId);
            }

            if (email != null && !email.isBlank()) {
                statement.setString(parameterIndex, email.trim());
            }

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    reservations.add(map(result));
                }
            }
        }
        return reservations;
    }

    /**
     * Searches reservations that belong to one authenticated customer only.
     * The customer_id condition is always present so a logged-in customer can
     * never retrieve another customer's reservation by guessing an ID or email.
     */
    public List<Reservation> searchForCustomer(long customerId, Long reservationId,
            String email) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT r.* FROM reservations r
                JOIN customers c ON c.customer_id = r.customer_id
                WHERE r.customer_id = ?
                """);

        if (reservationId != null) {
            sql.append(" AND r.reservation_id = ?");
        }
        if (email != null && !email.isBlank()) {
            sql.append(" AND LOWER(c.email) = LOWER(?)");
        }
        sql.append(" ORDER BY r.created_at DESC, r.reservation_id DESC");

        List<Reservation> reservations = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            statement.setLong(parameterIndex++, customerId);

            if (reservationId != null) {
                statement.setLong(parameterIndex++, reservationId);
            }
            if (email != null && !email.isBlank()) {
                statement.setString(parameterIndex, email.trim());
            }

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    reservations.add(map(result));
                }
            }
        }
        return reservations;
    }

    // method to update reservation status, and in cases of canceling will add
    // canceled_at timestamp
    public boolean updateStatus(Connection connection, long reservationId,
            String status, LocalDateTime cancelledAt) throws SQLException {
        String sql = "UPDATE reservations SET status = ?, cancelled_at = ? WHERE reservation_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            if (cancelledAt == null)
                statement.setNull(2, java.sql.Types.TIMESTAMP);
            else
                statement.setTimestamp(2, Timestamp.valueOf(cancelledAt));
            statement.setLong(3, reservationId);
            return statement.executeUpdate() == 1;
        }
    }

    // Converting ResultSet into @link Reservation.
    private Reservation map(ResultSet result) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setReservationId(result.getLong("reservation_id"));
        reservation.setCustomerId(result.getLong("customer_id"));
        reservation.setBoatId(result.getLong("boat_id"));
        reservation.setSlipId(result.getLong("slip_id"));
        Date checkIn = result.getDate("check_in_date");
        reservation.setCheckInDate(checkIn == null ? null : checkIn.toLocalDate());
		Date departure = result.getDate("departure_date"); // ADDED read for depart 9-17-26
        reservation.setDepartureDate(departure == null ? null : departure.toLocalDate()); // ADDED setter for departure 
        reservation.setExpectedTerm(result.getString("expected_term"));
        reservation.setMonthlyCost(result.getBigDecimal("monthly_cost"));
        reservation.setElectricIncluded(result.getBoolean("electric_included")); // added for electrical opt in 
        reservation.setStatus(result.getString("status"));
        Timestamp created = result.getTimestamp("created_at");
        reservation.setCreatedAt(created == null ? null : created.toLocalDateTime());
        Timestamp cancelled = result.getTimestamp("cancelled_at");
        reservation.setCancelledAt(cancelled == null ? null : cancelled.toLocalDateTime());
        return reservation;
    }
}
