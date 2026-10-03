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
import com.moffatbaymarina.model.Customer;

// This class is a Data access obj for the customers table in our database. 
// see Erd for table specs 
public class CustomerDAO {

    // Like the boats DAO, this inserts new customer its own 'short lived
    // conneciton'.
    public Customer insert(Customer customer) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return insert(connection, customer);
        }
    }

    // using a caller supplied connection, inserts new custoemr. Like the boat dao.
    // This is used so that this method can participate
    // in a larger transaction event, allowing multiple table entries in the
    // registration process. terminates if either transactions fail.
    // This assumes that password is already hashed, and stores whatever string it
    // is given.
    public Customer insert(Connection connection, Customer customer) throws SQLException {
        String sql = """
                INSERT INTO customers
                (first_name, last_name, phone, street, city, state, zip,
                 email, password_hash, email_verified)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, customer.getFirstName());
            statement.setString(2, customer.getLastName());
            statement.setString(3, customer.getPhone());
            statement.setString(4, customer.getStreet());
            statement.setString(5, customer.getCity());
            statement.setString(6, customer.getState());
            statement.setString(7, customer.getZip());
            statement.setString(8, customer.getEmail());
            statement.setString(9, customer.getPasswordHash());
            statement.setBoolean(10, customer.isEmailVerified());

            if (statement.executeUpdate() != 1) {
                throw new SQLException("Customer insert did not create one row.");
            }

            try (ResultSet keys = statement.getGeneratedKeys()) { // customer id not known until after insert process
                                                                  // runs
                if (!keys.next()) {
                    throw new SQLException("No customer_id was generated.");
                }
                customer.setCustomerId(keys.getLong(1));
            }
        }
        return customer;
    }

    // checking to see if a customer with this email already has an account. this
    // way a registration process can be
    // aborted. We will include a message stating that email already exists.
    public boolean emailExists(String email) throws SQLException {
        return findByEmail(email) != null;
    }

    // looks ups client by email, used in both login and registration as a duplicate
    // check
    public Customer findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM customers WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    /**
     * Looks up a customer by primary key, typically once a session already
     * knows which customer_id is logged in.
     *
     * @return the matching customer, or {@code null} if none exists
     */
    public Customer findById(long customerId) throws SQLException {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    // Changes the customers email_verified indicater after they have verified the
    // email. this is a coller supplied connection
    // so the 'flag' change and the matching markVerified call on the row are
    // commited with the same instance.
    public boolean setEmailVerified(Connection connection, long customerId, boolean verified)
            throws SQLException {
        String sql = "UPDATE customers SET email_verified = ? WHERE customer_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, verified);
            statement.setLong(2, customerId);
            return statement.executeUpdate() == 1;
        }
    }

    // converting a row of customers to a link Customer. cursor is assumed on the
    // row thats valid
    private Customer map(ResultSet result) throws SQLException {
        Customer customer = new Customer();
        customer.setCustomerId(result.getLong("customer_id"));
        customer.setFirstName(result.getString("first_name"));
        customer.setLastName(result.getString("last_name"));
        customer.setPhone(result.getString("phone"));
        customer.setStreet(result.getString("street"));
        customer.setCity(result.getString("city"));
        customer.setState(result.getString("state"));
        customer.setZip(result.getString("zip"));
        customer.setEmail(result.getString("email"));
        customer.setPasswordHash(result.getString("password_hash"));
        customer.setEmailVerified(result.getBoolean("email_verified"));
        Timestamp created = result.getTimestamp("created_at"); // LocalDateTime but only if the column wasn't NULL.
        customer.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return customer;
    }
}
