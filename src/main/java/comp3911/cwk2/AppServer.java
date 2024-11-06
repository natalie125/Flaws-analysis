package comp3911.cwk2;

import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.net.ssl.SSLSocket;

public class AppServer {
  public static void main(String[] args) throws Exception {
    Log.setLog(new StdErrLog());

    // Keeping the start the same.
    ServletHandler handler = new ServletHandler();
    handler.addServletWithMapping(AppServlet.class, "/*");

    Server server = new Server(8080);
    server.setHandler(handler);

    // Add a new https configuration
    HttpConfiguration https = new HttpConfiguration();
    https.addCustomizer(new SecureRequestCustomizer());

    // Use the keystore that was generated from the command line.
    SslContextFactory sslContextFactory = new SslContextFactory();
    sslContextFactory.setKeyStorePath("./certs/localhost/localhost.jks");

    // These are just the passwords that were set up for the keystore.
    sslContextFactory.setKeyStorePassword("password");
    sslContextFactory.setKeyManagerPassword("Thing");

    //
    ServerConnector sslConnector = new ServerConnector(server,
            new SslConnectionFactory(sslContextFactory, "http/1.1"),
            new HttpConnectionFactory(https));
    // Set the port to the default one.
    sslConnector.setPort(8080);

    // overrides it.
    server.setConnectors(new Connector[] {  sslConnector });

    // standard end.
    server.start();
    server.join();
  }
}
