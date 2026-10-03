package com.moffatbaymarina.db;

/*
  Fills every real 50 ft slip with a confirmed reservation, then adds
  3 more customers to the 50 ft wait list on top of that.

 */

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Properties;

public class SlipCapacityDemoData {

    // the marina map's real 50 ft slip numbers, per dock
    private static final String[] DOCKS = {"A", "B", "C"};
    private static final int[] FIFTY_FOOT_POSITIONS = {1, 2, 3, 13, 14, 15};

    // slips that already exist from GreenTeamDataBaseIdea's base seed -
    // these get skipped when creating the missing ones
    private static final String[] ALREADY_SEEDED_SLIPS = {"C1", "C2"};

    // name pools for generating filler customers - not meant to be
    // meaningful personas, just enough variety that 21 accounts don't
    // all look identical
    private static final String[] FIRST_NAMES = {
        "Alex", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Sam",
        "Avery", "Quinn", "Drew"
    };
    private static final String[] LAST_NAMES = {
        "Bennett", "Carter", "Diaz", "Ellis", "Foster", "Grant",
        "Holloway", "Ibarra", "Keller", "Lindqvist"
    };
    private static final String[] CITIES = {"Westport", "Bellevue", "Moffat Bay"};
    private static final String[] ZIPS = {"98595", "98004", "98599"};

    public static void main(String[] args) {
        Properties props = loadProperties();

        String host = props.getProperty("db.host", "localhost");
        String port = props.getProperty("db.port", "3306");
        String dbName = props.getProperty("db.name", "moffat_bay");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName;

        // NOTE: unlike GreenTeamDataBaseIdea, there is no drop/recreate
        // here - this connects straight to the existing database and
        // adds to it. Run GreenTeamDataBaseIdea first.
        try (Connection conn = DriverManager.getConnection(dbUrl, user, password)) {
            System.out.println("Connected to database: " + dbName);

            int fiftyFootTypeId = findFiftyFootSlipTypeId(conn);

            conn.setAutoCommit(false);
            try {
                if (isAlreadyFilled(conn, fiftyFootTypeId)) {
                    System.out.println("50 ft slips already look filled - skipping.");
                } else {
                    fillAllFiftyFootSlips(conn, fiftyFootTypeId);
                    addWaitlistCustomers(conn, fiftyFootTypeId, 3);
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

            printSummary(conn, fiftyFootTypeId);

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = SlipCapacityDemoData.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (in == null) {
                throw new IOException(
                    "db.properties not found on the classpath. "
                    + "Copy the template into your resources folder first.");
            }
            props.load(in);

        } catch (IOException e) {
            throw new RuntimeException("Could not load db.properties", e);
        }
        return props;
    }

    private static int findFiftyFootSlipTypeId(Connection conn) throws SQLException {
        String sql = "SELECT slip_type_id FROM slip_types WHERE size_ft = 50";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (!rs.next()) {
                throw new IllegalStateException(
                    "No 50 ft slip_type found - run GreenTeamDataBaseIdea first.");
            }
            return rs.getInt(1);
        }
    }

    // guards against running this twice in a row and doubling everything up
    private static boolean isAlreadyFilled(Connection conn, int fiftyFootTypeId)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM slips WHERE slip_type_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fiftyFootTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) >= 18;
            }
        }
    }

    private static void fillAllFiftyFootSlips(Connection conn, int fiftyFootTypeId)
            throws SQLException {

        int customerIndex = 0;

        for (String dock : DOCKS) {
            for (int position : FIFTY_FOOT_POSITIONS) {
                String slipNumber = dock + position;

                boolean alreadyExists = false;
                for (String existing : ALREADY_SEEDED_SLIPS) {
                    if (existing.equals(slipNumber)) alreadyExists = true;
                }

                int slipId;
                if (alreadyExists) {
                    slipId = findSlipIdByNumber(conn, slipNumber);
                    // still needs to end up RESERVED like the rest
                    updateSlipStatus(conn, slipId, "RESERVED");
                } else {
                    slipId = insertSlip(conn, fiftyFootTypeId, slipNumber, "RESERVED");
                }

                int customerId = insertFillerCustomer(conn, customerIndex);
                int boatId = insertFillerBoat(conn, customerId, customerIndex);
                insertFillerReservation(conn, customerId, boatId, slipId, customerIndex);

                customerIndex++;
            }
        }

        System.out.println("Filled all 18 fifty-foot slips with confirmed reservations.");
    }

    private static void addWaitlistCustomers(Connection conn, int fiftyFootTypeId, int count)
            throws SQLException {

        // continue the same customerIndex sequence used above (18, 19, 20)
        // so nobody's email collides with the slip-filling customers
        for (int i = 18; i < 18 + count; i++) {
            int customerId = insertFillerCustomer(conn, i);
            int boatId = insertFillerBoat(conn, customerId, i);

            String sql = "INSERT INTO waitlist_entries "
                + "(customer_id, boat_id, slip_type_id, status) VALUES (?, ?, ?, 'WAITING')";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, customerId);
                ps.setInt(2, boatId);
                ps.setInt(3, fiftyFootTypeId);
                ps.executeUpdate();
            }
        }

        System.out.println("Added " + count + " more customers to the 50 ft wait list.");
    }

    private static int findSlipIdByNumber(Connection conn, String slipNumber) throws SQLException {
        String sql = "SELECT slip_id FROM slips WHERE slip_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slipNumber);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void updateSlipStatus(Connection conn, int slipId, String status)
            throws SQLException {
        String sql = "UPDATE slips SET status = ? WHERE slip_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, slipId);
            ps.executeUpdate();
        }
    }

    private static int insertSlip(Connection conn, int slipTypeId, String slipNumber, String status)
            throws SQLException {
        String sql = "INSERT INTO slips (slip_type_id, slip_number, status) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, slipTypeId);
            ps.setString(2, slipNumber);
            ps.setString(3, status);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    // Plain-text passwords here too, matching GreenTeamDataBaseIdea's
    // current no-hashing prototype phase same reasoning applies, don't reuse these anywhere real.
    private static int insertFillerCustomer(Connection conn, int index) throws SQLException {
        String firstName = FIRST_NAMES[index % FIRST_NAMES.length];
        String lastName = LAST_NAMES[index % LAST_NAMES.length];
        String city = CITIES[index % CITIES.length];
        String zip = ZIPS[index % ZIPS.length];
        String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + index + "@example.com";
        String phone = String.format("(360) 555-02%02d", index);
        String password = "FillerPass" + index + "!";

        String sql = "INSERT INTO customers "
            + "(first_name, last_name, phone, street, city, state, zip, "
            + "email, password_hash, email_verified) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, phone);
            ps.setString(4, (index + 1) + " Dockside Way");
            ps.setString(5, city);
            ps.setString(6, "WA");
            ps.setString(7, zip);
            ps.setString(8, email);
            ps.setString(9, password);
            ps.setBoolean(10, true);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    // boat lengths cycle 41-50 ft - anything in that range maps to the
    // 50 ft slip type per SlipTypeDAO.findRequiredForBoatLength's rule
    private static int insertFillerBoat(Connection conn, int customerId, int index)
            throws SQLException {
        BigDecimal length = BigDecimal.valueOf(41 + (index % 10));

        String sql = "INSERT INTO boats "
            + "(customer_id, boat_name, boat_length_ft, boat_type) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerId);
            ps.setString(2, "Boat " + (index + 1));
            ps.setBigDecimal(3, length);
            ps.setString(4, "Sailboat");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    // monthly_cost follows the same rule as everywhere else in the project
    private static void insertFillerReservation(
            Connection conn, int customerId, int boatId, int slipId, int index)
            throws SQLException {

        BigDecimal length = BigDecimal.valueOf(41 + (index % 10));
        boolean electricIncluded = (index % 2 == 0);
        BigDecimal monthlyCost = length.multiply(new BigDecimal("10.50"));
        if (electricIncluded) monthlyCost = monthlyCost.add(BigDecimal.TEN);

        String sql = "INSERT INTO reservations "
			+ "(customer_id, boat_id, slip_id, check_in_date, departure_date, expected_term, "  // added for end date 9-17-26
			+ "monthly_cost, electric_included, status, cancelled_at) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'CONFIRMED', ?)"; // Fixed again, agian and again pay attention to these 
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, boatId);
            ps.setInt(3, slipId);
            LocalDate checkIn = LocalDate.now().plusDays(7);
            ps.setDate(4, Date.valueOf(checkIn)); //adding the departure date, shifting parameter number, matching db java file 9-17-26
            ps.setDate(5, Date.valueOf(checkIn.plusMonths(12)));
            ps.setString(6, "12 months");
            ps.setBigDecimal(7, monthlyCost);
            ps.setBoolean(8, electricIncluded);
            ps.setNull(9, Types.TIMESTAMP);
            ps.executeUpdate();
        }
    }

    private static void printSummary(Connection conn, int fiftyFootTypeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM slips WHERE slip_type_id = ? AND UPPER(status) = 'AVAILABLE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fiftyFootTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                System.out.println("50 ft slips still AVAILABLE: " + rs.getInt(1) + " (should be 0)");
            }
        }
        String waitSql = "SELECT COUNT(*) FROM waitlist_entries WHERE slip_type_id = ? AND UPPER(status) = 'WAITING'";
        try (PreparedStatement ps = conn.prepareStatement(waitSql)) {
            ps.setInt(1, fiftyFootTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                System.out.println("Customers on the 50 ft wait list: " + rs.getInt(1));
            }
        }
    }
}
