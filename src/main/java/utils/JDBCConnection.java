package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class JDBCConnection {
	
	public Connection getConnection(){
		Connection con = null;
		
		try{
			Config config = ConfigProvider.getConfig();
			String con_url = config.getValue("jdbcURL", String.class);
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			con = DriverManager.getConnection(con_url, config.getValue("dbuser", String.class), config.getValue("dbpassword", String.class));
		} 
		catch (SQLException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();;
			 }
	    catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			} 
		catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			} 
		catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			}
		return con;
	}

}
