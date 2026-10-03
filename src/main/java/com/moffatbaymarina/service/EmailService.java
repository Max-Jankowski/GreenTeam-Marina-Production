package com.moffatbaymarina.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

/**
 * EmailService
 *
 * Handles outgoing emails for the Moffat Bay Marina website.
 *
 * Currently supports:
 * - Account verification emails
 * - HTML and plain-text email content
 * - SMTP authentication
 *
 * Email credentials are retrieved from environment variables instead
 * of being stored directly in the source code.
 *
 * Required environment variables:
 *
 * MOFFAT_EMAIL
 * MOFFAT_EMAIL_PASSWORD
 *
 * Optional:
 *
 * MOFFAT_BASE_URL
 *
 * Example development URL:
 *
 * http://localhost:8080/MoffatBayMarina
 *
 * @author Alex Baldree
 */
public final class EmailService {

    /*
     * ---------------------------------------------------------
     * SMTP CONFIGURATION
     * ---------------------------------------------------------
     */

    private static final String SMTP_HOST = "smtp.gmail.com";

    private static final String SMTP_PORT = "587";

    /*
     * Email address used to send Moffat Bay Marina emails.
     *
     * Example:
     *
     * moffatbaymarina@gmail.com
     */
    private static final String EMAIL_USERNAME = System.getenv("MOFFAT_EMAIL");

    /*
     * For Gmail this should be a Google App Password,
     * NOT the normal Gmail account password.
     */
    private static final String EMAIL_PASSWORD = System.getenv("MOFFAT_EMAIL_PASSWORD");

    /*
     * Website URL.
     *
     * Development:
     * http://localhost:8080/MoffatBayMarina
     *
     * Production:
     * https://www.moffatbaymarina.com
     */
    private static final String BASE_URL = getEnvironmentVariable(
            "MOFFAT_BASE_URL",
            "http://localhost:8080/MoffatBayMarina");

    /*
     * Display name users will see in their inbox.
     */
    private static final String FROM_NAME = "Moffat Bay Marina";

    /*
     * Prevent object creation because this is a utility/service class.
     */
    private EmailService() {
    }

    /**
     * Creates the Jakarta Mail SMTP session.
     *
     * @return configured mail Session
     */
    private static Session createMailSession() {

        validateEmailConfiguration();

        Properties properties = new Properties();

        properties.put(
                "mail.smtp.auth",
                "true");

        properties.put(
                "mail.smtp.starttls.enable",
                "true");

        properties.put(
                "mail.smtp.starttls.required",
                "true");

        properties.put(
                "mail.smtp.host",
                SMTP_HOST);

        properties.put(
                "mail.smtp.port",
                SMTP_PORT);

        /*
         * Connection timeout:
         * 10 seconds
         */
        properties.put(
                "mail.smtp.connectiontimeout",
                "10000");

        /*
         * Socket timeout:
         * 10 seconds
         */
        properties.put(
                "mail.smtp.timeout",
                "10000");

        /*
         * Write timeout:
         * 10 seconds
         */
        properties.put(
                "mail.smtp.writetimeout",
                "10000");

        return Session.getInstance(
                properties,
                new Authenticator() {

                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {

                        return new PasswordAuthentication(
                                EMAIL_USERNAME,
                                EMAIL_PASSWORD);
                    }
                });
    }

    /**
     * Sends an email verification message to a newly registered user.
     *
     * @param recipientEmail    user's email address
     * @param firstName         user's first name
     * @param verificationToken unique verification token
     * @throws MessagingException when the email cannot be sent
     */
    public static void sendVerificationEmail(
            String recipientEmail,
            String firstName,
            String verificationToken)
            throws MessagingException {

        validateRecipient(recipientEmail);

        if (verificationToken == null
                || verificationToken.isBlank()) {

            throw new IllegalArgumentException(
                    "Verification token cannot be empty.");
        }

        /*
         * Encode the token before placing it into the URL.
         */
        String encodedToken = URLEncoder.encode(
                verificationToken,
                StandardCharsets.UTF_8);

        String verificationLink = BASE_URL
                + "/verification.html?token="
                + encodedToken;

        String safeFirstName = firstName == null || firstName.isBlank()
                ? "Customer"
                : firstName.trim();

        String subject = "Verify Your Moffat Bay Marina Account";

        /*
         * -----------------------------------------------------
         * PLAIN-TEXT VERSION
         * -----------------------------------------------------
         */

        String plainTextBody = "Hello " + safeFirstName + ",\n\n"
                + "Welcome to Moffat Bay Marina!\n\n"
                + "Please verify your email address by "
                + "clicking the link below:\n\n"
                + verificationLink
                + "\n\n"
                + "This verification link will expire. "
                + "If you did not create a Moffat Bay Marina "
                + "account, you can ignore this email.\n\n"
                + "Thank you,\n"
                + "Moffat Bay Marina";

        /*
         * -----------------------------------------------------
         * HTML VERSION
         * -----------------------------------------------------
         */

        String htmlBody = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">

                    <title>Verify Your Email</title>
                </head>

                <body style="
                    margin: 0;
                    padding: 0;
                    background-color: #f4f6f8;
                    font-family: Arial, Helvetica, sans-serif;
                    color: #172033;
                ">

                    <table width="100%%"
                           cellspacing="0"
                           cellpadding="0"
                           border="0"
                           style="
                               background-color: #f4f6f8;
                               padding: 40px 15px;
                           ">

                        <tr>
                            <td align="center">

                                <table width="100%%"
                                       cellspacing="0"
                                       cellpadding="0"
                                       border="0"
                                       style="
                                           max-width: 600px;
                                           background-color: #ffffff;
                                           border-radius: 10px;
                                           overflow: hidden;
                                           box-shadow:
                                           0 5px 20px
                                           rgba(0,0,0,0.08);
                                       ">

                                    <!-- HEADER -->
                                    <tr>
                                        <td style="
                                            background-color: #061b3a;
                                            padding: 30px;
                                            text-align: center;
                                            border-bottom:
                                            4px solid #eab53f;
                                        ">

                                            <h1 style="
                                                color: #ffffff;
                                                margin: 0;
                                                font-size: 28px;
                                            ">
                                                Moffat Bay Marina
                                            </h1>

                                            <p style="
                                                color: #eab53f;
                                                margin:
                                                8px 0 0 0;
                                                font-size: 14px;
                                            ">
                                                Your Gateway to the Water
                                            </p>

                                        </td>
                                    </tr>


                                    <!-- CONTENT -->
                                    <tr>
                                        <td style="
                                            padding: 40px;
                                        ">

                                            <h2 style="
                                                color: #061b3a;
                                                margin-top: 0;
                                            ">
                                                Verify Your Email
                                            </h2>

                                            <p style="
                                                font-size: 16px;
                                                line-height: 1.7;
                                            ">
                                                Hello %s,
                                            </p>

                                            <p style="
                                                font-size: 16px;
                                                line-height: 1.7;
                                            ">
                                                Thank you for creating
                                                your Moffat Bay Marina
                                                account.
                                            </p>

                                            <p style="
                                                font-size: 16px;
                                                line-height: 1.7;
                                            ">
                                                Before you can access
                                                your account and manage
                                                your marina reservations,
                                                please verify your email
                                                address.
                                            </p>


                                            <!-- BUTTON -->
                                            <table
                                                cellspacing="0"
                                                cellpadding="0"
                                                border="0"
                                                width="100%%">

                                                <tr>
                                                    <td
                                                    align="center"
                                                    style="
                                                    padding:
                                                    25px 0;
                                                    ">

                                                        <a href="%s"
                                                           style="
                                                           background-color:
                                                           #eab53f;

                                                           color:
                                                           #061b3a;

                                                           padding:
                                                           15px 30px;

                                                           border-radius:
                                                           6px;

                                                           text-decoration:
                                                           none;

                                                           font-weight:
                                                           bold;

                                                           font-size:
                                                           16px;

                                                           display:
                                                           inline-block;
                                                           ">
                                                            Verify My Email
                                                        </a>

                                                    </td>
                                                </tr>

                                            </table>


                                            <p style="
                                                font-size: 14px;
                                                line-height: 1.6;
                                                color: #667085;
                                            ">
                                                If the button above does
                                                not work, copy and paste
                                                the following link into
                                                your browser:
                                            </p>

                                            <p style="
                                                font-size: 13px;
                                                line-height: 1.5;
                                                word-break: break-all;
                                                color: #102b52;
                                            ">
                                                %s
                                            </p>

                                            <hr style="
                                                border: none;
                                                border-top:
                                                1px solid #d9dde5;
                                                margin: 30px 0;
                                            ">

                                            <p style="
                                                font-size: 13px;
                                                line-height: 1.6;
                                                color: #667085;
                                            ">
                                                This verification link
                                                will expire for security
                                                purposes.
                                            </p>

                                            <p style="
                                                font-size: 13px;
                                                line-height: 1.6;
                                                color: #667085;
                                            ">
                                                If you did not create a
                                                Moffat Bay Marina account,
                                                you can safely ignore
                                                this email.
                                            </p>

                                        </td>
                                    </tr>


                                    <!-- FOOTER -->
                                    <tr>
                                        <td style="
                                            background-color: #061b3a;
                                            color: #ffffff;
                                            padding: 25px;
                                            text-align: center;
                                        ">

                                            <p style="
                                                margin: 0;
                                                font-size: 13px;
                                            ">
                                                © %d Moffat Bay Marina
                                            </p>

                                            <p style="
                                                margin: 8px 0 0 0;
                                                color: #eab53f;
                                                font-size: 12px;
                                            ">
                                                Thank you for choosing
                                                Moffat Bay Marina.
                                            </p>

                                        </td>
                                    </tr>

                                </table>

                            </td>
                        </tr>

                    </table>

                </body>
                </html>
                """.formatted(
                escapeHtml(safeFirstName),
                verificationLink,
                verificationLink,
                Year.now().getValue());

        sendEmail(
                recipientEmail,
                subject,
                plainTextBody,
                htmlBody);
    }

    /**
     * Sends a basic plain-text email.
     *
     * Can later be reused for:
     *
     * - Reservation confirmations
     * - Password reset emails
     * - Reservation cancellation emails
     * - Wait-list notifications
     *
     * @param recipientEmail recipient
     * @param subject        email subject
     * @param body           email body
     * @throws MessagingException if sending fails
     */
    public static void sendPlainTextEmail(
            String recipientEmail,
            String subject,
            String body)
            throws MessagingException {

        validateRecipient(recipientEmail);

        Session session = createMailSession();

        MimeMessage message = new MimeMessage(session);

        configureMessageSender(message);

        message.setRecipient(
                Message.RecipientType.TO,
                new InternetAddress(recipientEmail));

        message.setSubject(
                subject,
                StandardCharsets.UTF_8.name());

        message.setText(
                body,
                StandardCharsets.UTF_8.name());

        Transport.send(message);
    }

    /**
     * Sends an email containing both plain-text and HTML versions.
     *
     * Email clients that support HTML display the HTML version.
     * Other clients automatically use the plain-text version.
     *
     * @param recipientEmail recipient
     * @param subject        subject
     * @param plainTextBody  plain-text content
     * @param htmlBody       HTML content
     * @throws MessagingException if sending fails
     */
    private static void sendEmail(
            String recipientEmail,
            String subject,
            String plainTextBody,
            String htmlBody)
            throws MessagingException {

        Session session = createMailSession();

        MimeMessage message = new MimeMessage(session);

        configureMessageSender(message);

        message.setRecipient(
                Message.RecipientType.TO,
                new InternetAddress(recipientEmail));

        message.setSubject(
                subject,
                StandardCharsets.UTF_8.name());

        /*
         * Create the plain-text portion.
         */
        MimeBodyPart textPart = new MimeBodyPart();

        textPart.setText(
                plainTextBody,
                StandardCharsets.UTF_8.name());

        /*
         * Create the HTML portion.
         */
        MimeBodyPart htmlPart = new MimeBodyPart();

        htmlPart.setContent(
                htmlBody,
                "text/html; charset=UTF-8");

        /*
         * "alternative" tells the email client that both
         * parts contain the same message in different formats.
         */
        Multipart multipart = new MimeMultipart("alternative");

        multipart.addBodyPart(textPart);
        multipart.addBodyPart(htmlPart);

        message.setContent(multipart);

        /*
         * Send through the configured SMTP server.
         */
        Transport.send(message);
    }

    /**
     * Configures the From address.
     *
     * @param message MimeMessage
     * @throws MessagingException email configuration error
     */
    private static void configureMessageSender(
            MimeMessage message)
            throws MessagingException {

        try {

            InternetAddress fromAddress = new InternetAddress(
                    EMAIL_USERNAME,
                    FROM_NAME,
                    StandardCharsets.UTF_8.name());

            message.setFrom(fromAddress);

        } catch (UnsupportedEncodingException exception) {

            throw new MessagingException(
                    "Unable to configure email sender.",
                    exception);
        }
    }

    /**
     * Confirms SMTP credentials have been configured.
     */
    private static void validateEmailConfiguration() {

        if (EMAIL_USERNAME == null
                || EMAIL_USERNAME.isBlank()) {

            throw new IllegalStateException(
                    "MOFFAT_EMAIL environment variable "
                            + "has not been configured.");
        }

        if (EMAIL_PASSWORD == null
                || EMAIL_PASSWORD.isBlank()) {

            throw new IllegalStateException(
                    "MOFFAT_EMAIL_PASSWORD environment variable "
                            + "has not been configured.");
        }
    }

    /**
     * Performs a basic recipient email validation.
     *
     * Full validation occurs when Jakarta Mail constructs the
     * InternetAddress.
     *
     * @param email email address
     */
    private static void validateRecipient(
            String email) {

        if (email == null
                || email.isBlank()) {

            throw new IllegalArgumentException(
                    "Recipient email cannot be empty.");
        }

        try {

            InternetAddress address = new InternetAddress(
                    email.trim());

            address.validate();

        } catch (AddressException exception) {

            throw new IllegalArgumentException(
                    "Invalid recipient email address: "
                            + email,
                    exception);
        }
    }

    /**
     * Retrieves an environment variable.
     *
     * If the environment variable does not exist,
     * the supplied default value is returned.
     *
     * @param name         environment variable name
     * @param defaultValue default value
     * @return environment variable or default
     */
    private static String getEnvironmentVariable(
            String name,
            String defaultValue) {

        String value = System.getenv(name);

        if (value == null
                || value.isBlank()) {

            return defaultValue;
        }

        return value.trim();
    }

    /**
     * Performs basic HTML escaping before displaying user-entered
     * text inside the HTML email.
     *
     * @param value value to escape
     * @return escaped HTML
     */
    private static String escapeHtml(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}