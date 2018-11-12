package org.pentaho.platform.engine.security.event;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.security.userroledao.IUserRoleDao;
import org.pentaho.platform.api.engine.security.userroledao.UncategorizedUserRoleDaoException;
import org.pentaho.platform.api.mt.ITenant;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.TenantUtils;
import org.pentaho.platform.web.http.api.resources.RoleListWrapper;

public class MeteringDataProcessor {

	private static final Log logger = LogFactory.getLog(MeteringDataProcessor.class);

	static String DRIVER = "com.mysql.jdbc.Driver";
	static String CONNECTION_URL = "jdbc:mysql://34.203.164.233:3306/aws_saas";
	static String USERNAME = "root";
	static String PASSWORD = "ax16thst";	

	private static IUserRoleDao roleDao;

	protected void logDataforMetering() throws SQLException {

		IPentahoSession session = PentahoSessionHolder.getSession();
		String loginUserName = session.getName();		
		logger.debug("Entered MeteringDataProcessor.logDataforMetering() ---- Login USERNAME :: "+loginUserName);		
		List<String> loginUserRoleList = getRolesForUserWOFilter(loginUserName).getRoles();

		// get Organization Role - pattern @#@#roleName@#@#
		String orgName = null;
		for (String loginUserRole : loginUserRoleList) {
			logger.debug("===Inside MeteringDataProcessor USER ROLE = " + loginUserRole);
			if (loginUserRole.startsWith("@#@#") && loginUserRole.endsWith("@#@#")) {
				orgName = loginUserRole;
			}
		}
		int meteringFlag = 0;
		
		ResultSet rs = getUserInfo(orgName, loginUserName);
		String custID =null;
		String productCD=null;
		while(rs.next()){
			custID=rs.getString("customerID");
			productCD=rs.getString("productCode");
		}

		if (null != custID && null != productCD) {

			insertMeteringData(loginUserName, custID, productCD, meteringFlag);
			logger.debug("Inserted metering status in db ");

		}
	}

	private ResultSet getUserInfo(String orgnizationName, String username)  {
		logger.debug("Entered getUserInfo()... ");
		
		Connection connection;
		ResultSet resultSet = null;
		try {
			connection = getConnection();
			final String query = "SELECT customerID, productCode from aws_saas.saasuser WHERE orgnizationName = ? and email=?";
			final PreparedStatement ps = connection.prepareStatement(query);			
			ps.setString(1, orgnizationName);
			ps.setString(2, username);
			resultSet = ps.executeQuery();	
			
		} catch (SQLException e) {			
			logger.error("Exception encountered when trying to connect to DB in *getUserInfo()");
			e.printStackTrace();
		}
		return resultSet;		
	}

	private void insertMeteringData(String userName, String customerID, String productID, int meterflag) {
		logger.debug("Entered insertMeteringData()... ");
		
		try {
			java.util.Date date = new java.util.Date();
		    long t = date.getTime();
		    java.sql.Timestamp loginTime = new java.sql.Timestamp(t);
			
			Connection connection = getConnection();
			String insertQuery = "insert into user_meter_time (userName,customerID,productID,loginTime,meterFlag)  values(?,?,?,?,?)";

			final PreparedStatement ps = connection.prepareStatement(insertQuery);
			ps.setString(1, userName);
			ps.setString(2, customerID);
			ps.setString(3, productID);			
			ps.setTimestamp(4, loginTime);
			ps.setInt(5, meterflag);
			ps.execute();
			logger.debug("Inserted metering data... ");
			
			connection.close();
			connection = null;
		} catch (SQLException e) {
			logger.error("Exception encountered when trying to connect to DB in *insertMeteringData()");
			e.printStackTrace();
		}
	}

	public void insertLogOutTime(String loginUserName) {

		logger.debug("Entered insertLogOutTime()... ");
		try {
			java.util.Date date = new java.util.Date();
		    long t = date.getTime();
		    java.sql.Timestamp logOutTime = new java.sql.Timestamp(t);
			Connection connection = getConnection();
			
			String updateQuery = "update user_meter_time set logoutTime = ? where logoutTime IS NULL and userName = ?";

			final PreparedStatement ps = connection.prepareStatement(updateQuery);
			ps.setTimestamp(1, logOutTime);
			ps.setString(2, loginUserName);
			ps.execute();
			logger.debug("Inserted Logout time... ");
			connection.close();
			connection = null;
		} catch (SQLException e) {
			logger.error("Exception encountered when trying to connect to DB in *insertLogOutTime()");
			e.printStackTrace();
		}
	}

	private Connection getConnection() throws SQLException {
		try {

			Class.forName(DRIVER);

		} catch (ClassNotFoundException e) {
			logger.error("MySQL Driver not found " + e);
		}

		return DriverManager.getConnection(CONNECTION_URL, USERNAME, PASSWORD);

	}

	public RoleListWrapper getRolesForUserWOFilter(String user) throws UncategorizedUserRoleDaoException {
		logger.info("===Inside MeteringDataProcessor :: getRolesForUserWOFilter ==== user =" + user);
		ITenant tenant = TenantUtils.getCurrentTenant();		
		return new RoleListWrapper(getRoleDao().getUserRoles(tenant, user));
	}

	private IUserRoleDao getRoleDao() {
		if (roleDao == null) {
			roleDao = PentahoSystem.get(IUserRoleDao.class);
		}
		return roleDao;
	}
	
}
