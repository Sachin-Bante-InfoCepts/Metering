package com.nimblebi.metering;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class DBConnection {
	
	private static final Log logger = LogFactory.getLog(DBConnection.class);
	
	 static String DRIVER="com.mysql.jdbc.Driver"; 
	 static String CONNECTION_URL="jdbc:mysql://34.203.164.233:3306/aws_saas";   
	 static String USERNAME="root"; 
	 static String PASSWORD="ax16thst";
	 
	 public static Connection getConnection()
			throws SQLException {
		 logger.debug("Entered DBConnection.getConnection()... ");
	   try {

			Class.forName(DRIVER);

		} catch (ClassNotFoundException e) {
			logger.error("Exception when trying to connect to DB !!!");
			e.printStackTrace();
		}

		return DriverManager.getConnection(CONNECTION_URL, USERNAME, PASSWORD);

	}	
}
