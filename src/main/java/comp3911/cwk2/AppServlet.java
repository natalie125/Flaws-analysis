package comp3911.cwk2;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import org.mindrot.jbcrypt.BCrypt;

@SuppressWarnings("serial")
public class AppServlet extends HttpServlet {

    private static final String CONNECTION_URL = "jdbc:sqlite:db.sqlite3";
    private final Configuration fm = new Configuration(Configuration.VERSION_2_3_28);
    private Connection database;
    private static final Logger LOGGER = Logger.getLogger(AppServlet.class.getName());

    // Encrypts the password 
    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    // Validates the user's entered password
    private boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    @Override
    public void init() throws ServletException {
        configureTemplateEngine();
        connectToDatabase();
        try {
            encryptExistingPasswords();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error during password encryption", e);
            throw new ServletException(e);
        }
    }

    private void configureTemplateEngine() throws ServletException {
        try {
            fm.setDirectoryForTemplateLoading(new File("./templates"));
            fm.setDefaultEncoding("UTF-8");
            fm.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
            fm.setLogTemplateExceptions(false);
            fm.setWrapUncheckedExceptions(true);
        } catch (IOException error) {
            LOGGER.log(Level.SEVERE, "Failed to configure template engine", error);
            throw new ServletException(error);
        }
    }

    private void connectToDatabase() throws ServletException {
        try {
            database = DriverManager.getConnection(CONNECTION_URL);
        } catch (SQLException error) {
            LOGGER.log(Level.SEVERE, "Database connection failed", error);
            throw new ServletException(error);
        }
    }
    
    @Override
    public void destroy() {
        try {
            if (database != null && !database.isClosed()) {
                database.close();
            }
        } catch (SQLException error) {
            LOGGER.log(Level.SEVERE, "Error closing the database connection", error);
        }
    }
    

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            Template template = fm.getTemplate("login.html");
            template.process(null, response.getWriter());
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (TemplateException | IOException error) {
            LOGGER.log(Level.SEVERE, "Error in processing the request", error);
            sendError(response, "Error processing request", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        try {
            if (authenticated(username, password)) {
                List<Record> records = searchResults(request.getParameter("surname"));
                Map<String, Object> model = new HashMap<>();
                model.put("records", records);
                Template template = fm.getTemplate("details.html");

                response.setContentType("text/html");
                template.process(model, response.getWriter());
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                Template template = fm.getTemplate("invalid.html");

                response.setContentType("text/html");
                template.process(null, response.getWriter());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (SQLException error) {
            LOGGER.log(Level.SEVERE, "Database error", error);
            sendError(response, "Database error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (IOException | TemplateException error) {
            LOGGER.log(Level.SEVERE, "Error in processing the request", error);
            sendError(response, "Error processing request", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception error) {
            LOGGER.log(Level.SEVERE, "Unexpected error", error);
            sendError(response, "Unexpected error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

        //handle error responses
        private void sendError(HttpServletResponse response, String errorMessage, int statusCode) {
            try {
                response.sendError(statusCode, errorMessage);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to send error response", e);
            }
        }

    private boolean authenticated(String username, String password) throws SQLException {
        String query = "SELECT password FROM user WHERE username = ?";
        try (PreparedStatement stmt = database.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet results = stmt.executeQuery();
            if (results.next()) {
                String hashedPassword = results.getString("password");
                return checkPassword(password, hashedPassword);
            }
            return false;
        }
    }

    // Encrypts passwords in the database
    public void encryptExistingPasswords() throws SQLException {
        String retrieveQuery = "SELECT id, password FROM user";
        try (PreparedStatement retrieveStmt = database.prepareStatement(retrieveQuery)) {
            ResultSet results = retrieveStmt.executeQuery();
            while (results.next()) {
                int userId = results.getInt("id");
                String currentPassword = results.getString("password");
                if (!currentPassword.startsWith("$2a$")) {
                    String newHashedPassword = hashPassword(currentPassword);
                    String updateQuery = "UPDATE user SET password=? WHERE id=?";
                    try (PreparedStatement updateStmt = database.prepareStatement(updateQuery)) {
                        updateStmt.setString(1, newHashedPassword);
                        updateStmt.setInt(2, userId);
                        updateStmt.executeUpdate();
                    }
                }
            }
        }
    }

    private List<Record> searchResults(String surname) throws SQLException {
        String query = "SELECT * FROM patient WHERE surname = ? COLLATE NOCASE";
        try (PreparedStatement stmt = database.prepareStatement(query)) {
            stmt.setString(1, surname);
            ResultSet results = stmt.executeQuery();
            List<Record> records = new ArrayList<>();
            while (results.next()) {
                Record rec = new Record();
                rec.setSurname(results.getString(2));
                rec.setForename(results.getString(3));
                rec.setAddress(results.getString(4));
                rec.setDateOfBirth(results.getString(5));
                rec.setDoctorId(results.getString(6));
                rec.setDiagnosis(results.getString(7));
                records.add(rec);
            }
            return records;
        }
    }
}
