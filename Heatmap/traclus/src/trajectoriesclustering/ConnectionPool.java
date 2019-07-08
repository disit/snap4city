//this class provides a database connection pool, using org.apache.commons.dbcp and org.apache.commons.pool libraries
package trajectoriesclustering;

import java.io.IOException;
import java.util.Properties;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import javax.sql.DataSource;

/**
 *
 * @author Daniele Cenni, daniele.cenni@unifi.it
 */
public class ConnectionPool {

    /**
     *
     */
    public static final String DRIVER = "com.mysql.jdbc.Driver";

    /**
     *
     */
    public static String URL;

    /**
     *
     */
    public static String USERNAME;

    /**
     *
     */
    public static String PASSWORD;

    /**
     *
     */
    public static int connections;
    private GenericObjectPool connectionPool = null;

    /**
     *
     * @param url
     * @param username
     * @param password
     * @throws IOException
     */
    public ConnectionPool(String url, String username, String password) throws IOException {
        URL = url;
        USERNAME = username;
        PASSWORD = password;
        connections = 10;
    }

    /**
     *
     * @return @throws Exception
     */
    public DataSource setUp() throws Exception {
        /**
         * Load JDBC Driver class.
         */
        Class.forName(ConnectionPool.DRIVER).newInstance();

        /**
         * Creates an instance of GenericObjectPool that holds our pool of
         * connections object.
         */
        connectionPool = new GenericObjectPool();
        // set the max number of connections
        connectionPool.setMaxActive(connections);
        // if the pool is exhausted (i.e., the maximum number of active objects has been reached), the borrowObject() method should simply create a new object anyway
        connectionPool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_GROW);

        /**
         * Creates a connection factory object which will be used by the pool to
         * create the connection object. We pass the JDBC url info, username and
         * password.
         */
        ConnectionFactory cf = new DriverManagerConnectionFactory(
                ConnectionPool.URL,
                ConnectionPool.USERNAME,
                ConnectionPool.PASSWORD);

        /**
         * Creates a PoolableConnectionFactory that will wrap the connection
         * object created by the ConnectionFactory to add object pooling
         * functionality.
         */
        PoolableConnectionFactory pcf
                = new PoolableConnectionFactory(cf, connectionPool,
                        null, null, false, true);
        return new PoolingDataSource(connectionPool);
    }

    /**
     *
     * @return
     */
    public GenericObjectPool getConnectionPool() {
        return connectionPool;
    }

    // Prints connection pool status
    private void printStatus() {
        System.out.println("Max   : " + getConnectionPool().getMaxActive() + "; "
                + "Active: " + getConnectionPool().getNumActive() + "; "
                + "Idle  : " + getConnectionPool().getNumIdle());
    }
}
