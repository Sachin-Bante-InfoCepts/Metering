package com.nimblebi.metering;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.util.logging.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.amazonaws.services.marketplacemetering.model.BatchMeterUsageRequest;
import com.amazonaws.services.marketplacemetering.model.BatchMeterUsageResult;
import com.amazonaws.services.marketplacemetering.model.UsageRecord;
import com.amazonaws.services.marketplacemetering.model.UsageRecordResult;

public class SchedulerJob  implements Job{

	private static final Log logger = LogFactory.getLog(SchedulerJob.class);
	//InstanceProfileCredentialsProvider instanceObj = new InstanceProfileCredentialsProvider(false);
	//AWSMarketplaceMeteringClient awsMeterClient = new AWSMarketplaceMeteringClient(instanceObj.getCredentials());
	
	private static final String productID = "4j73phesgjepramngwthk9v30";
			
	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {

		logger.info("Inside SchedulerJob.execute()!!!");

		try {
			BatchMeterUsageRequest batchUsageRequest = prepareDataFrMetering();
			logger.info("BatchMeterUsageRequest Content :: "+batchUsageRequest.toString());
			//BatchMeterUsageResult batchUsageResponse = awsMeterClient.batchMeterUsage(batchUsageRequest);
			//processBatchMeterUsageResult(batchUsageResponse);

		} catch (Exception e) {
			logger.error("Exception Occured in SchedulerJob.execute()..");
			e.printStackTrace();
		}

	}
	
	/*public static void main (String[] args){
		SchedulerJob job = new SchedulerJob();
		
		try {
			BatchMeterUsageRequest batchUsageRequest = job.prepareDataFrMetering();
			

		} catch (Exception e) {
			logger.error("Exception Occured in SchedulerJob.execute()..");
			e.printStackTrace();
		}
	}*/

	
	public  BatchMeterUsageRequest prepareDataFrMetering() throws Exception {

		logger.debug("Entered SchedulerJob.prepareDataFrMetering()...");
		
		ArrayList<String> customerIDs = new ArrayList<String>();
		ResultSet resultSet=getCustomerIDs();
		while(resultSet.next()){
			customerIDs.add(resultSet.getString("customerID"));
			
		}
		
		List<UsageRecord> usageRecordsList = new ArrayList<UsageRecord>();		
		BatchMeterUsageRequest batchUsageObj = new BatchMeterUsageRequest();

		if (customerIDs != null) {
			for (String custID : customerIDs) {

				ResultSet rs = getCustomerUsage(custID);
				if (rs != null) {
					
					Date now = new java.util.Date();
					Integer totalTimespent = 0;

					while (rs.next()) {
						
						String userNm = rs.getNString("userName");
						logger.debug("Preparing data for :: " + userNm);						
						
						int flag = rs.getInt("meterFlag");
						Timestamp loginTime = rs.getTimestamp("loginTime");
						Timestamp logoutTime = rs.getTimestamp("logoutTime");
						Timestamp currentTime = new Timestamp(now.getTime());
						int seconds;
						int timeSpntInMinsByOrgUser = 0;
						
						long milliseconds = 0;

						if (flag == 0 && null == logoutTime) {							
							logger.debug("User LOGGED IN this hour..." + userNm + " Current time :" + now);
							milliseconds = currentTime.getTime() - loginTime.getTime();
							seconds = (int) milliseconds / 1000;
						    timeSpntInMinsByOrgUser = (seconds % 3600) / 60;
							//this.setMeteringFlag(custID,1);

						} 
						else if (flag == 1 && null == logoutTime) {
							logger.debug("User NOT LOGGED OUT..." + userNm + " Current time :" + now);
							// If this is the case...timespent is 1 hr= 60 mins
							 timeSpntInMinsByOrgUser = 60;

						} 
						else if ((flag == 1 || flag == 0) && null != logoutTime) {
							logger.debug("User LOGGED OUT..." + userNm + " Current time :" + now);
							milliseconds = (60 * 60000) - (currentTime.getTime() - logoutTime.getTime());
							seconds = (int) milliseconds / 1000;
						    timeSpntInMinsByOrgUser = (seconds % 3600) / 60;
							//this.setMeteringFlag(custID,2);

						}							
						totalTimespent =  totalTimespent + timeSpntInMinsByOrgUser;						
						
						logger.debug(
								"USERNAME : " + userNm + "  Total TIME SPENT in this hour(in mins) " + totalTimespent);		
						System.out.println("USERNAME : " + userNm + "  Total TIME SPENT in this hour(in mins) " + totalTimespent);
						
					}
					UsageRecord usageRecord = new UsageRecord();
					usageRecord.setTimestamp(now);
					usageRecord.setCustomerIdentifier(custID);
					usageRecord.setDimension("User");
					usageRecord.setQuantity(totalTimespent);
					usageRecordsList.add(usageRecord);					
					
				} else {
					logger.debug("DID NOT got record for customer usage!!!");
				}
				
			}
			
			batchUsageObj.setProductCode(productID);
			batchUsageObj.setUsageRecords(usageRecordsList);
			logger.error("\nBatchMeterUsageRequest :: "+batchUsageObj.getUsageRecords().toString());

		} else {
			logger.debug("NO customers LOGGED IN this hour!!!");
		}

		return batchUsageObj;
	}

	private ResultSet getCustomerUsage(String custID) {

		logger.debug("Entered SchedulerJob.getCustomerUsage()...");
		Connection connection;
		ResultSet resultSet = null;
		try {
			connection = DBConnection.getConnection();
			String query = "SELECT * from aws_saas.user_meter_time WHERE meterFlag != 2 and customerID = ?";
			final PreparedStatement ps = connection.prepareStatement(query);
			ps.setString(1, custID);
			resultSet = ps.executeQuery();
			logger.debug("Query executed to get CUSTOMER IDS");
			//ps.close();
			//connection.close();

		} catch (SQLException e) {
			logger.error("Exception while getting Customer data!!!");
			e.printStackTrace();
		}

		return resultSet;
	}

	private ResultSet getCustomerIDs() {

		logger.debug("Entered SchedulerJob.getCustomerIDs()...");
		Connection connection;
		ResultSet rs = null;
		try {
			connection = DBConnection.getConnection();
			String query = "SELECT distinct(customerID) from aws_saas.user_meter_time WHERE meterFlag != 2";
			final PreparedStatement ps = connection.prepareStatement(query);
			rs = ps.executeQuery();
			
		} catch (SQLException e) {
			logger.error("Exception while getting  distinct CustomerIDs!!!");
			e.printStackTrace();
		}

		return rs;
	}

	private void setMeteringFlag(String customerId,int flagVal) {
		logger.debug("Entered SchedulerJob.getCustomerIDs()...");
		Connection connection;
		try {
			connection = DBConnection.getConnection();
			//String insertQuery = "insert into user_meter_time (meterFlag) values(?)";
			String updateMeterFlag = "Update user_meter_time SET meterFlag = ? where customerID =?";
			final PreparedStatement ps = connection.prepareStatement(updateMeterFlag);
			ps.setInt(1, flagVal);
			ps.setString(2, customerId);
			ps.execute();
			logger.debug("METERING -FLAG set to : "+flagVal);
			ps.close();
			connection.close();
			connection = null;
		} catch (SQLException e) {
			logger.error("Exception while setting the METERING-FLAG to : " + flagVal);
			e.printStackTrace();
		}

	}
	
	private void processBatchMeterUsageResult(BatchMeterUsageResult response) {

		List<UsageRecordResult> usageResultList = response.getResults();
		for (UsageRecordResult usageRecordResult : usageResultList) {

			String status = usageRecordResult.getStatus();
			if (status == "200") {
				logger.debug("Metering record processed SUCCESSFULLY!!!");
				logger.debug("Metering ID : " + usageRecordResult.getMeteringRecordId());

			}else {
				logger.error("PROBLEM OCCURED while processing metering record...");
				logger.error("Metering ID : " + usageRecordResult.getMeteringRecordId() + " Status Code : " + status);
			}

		}
		List<UsageRecord> unprocessedRecordsList = response.getUnprocessedRecords();
		
		if(!unprocessedRecordsList.isEmpty()){
			
			logger.debug("Processing the Unprocessed Metering Records List...");
			BatchMeterUsageRequest batchUsageRequest = new BatchMeterUsageRequest();			
			
			batchUsageRequest.setProductCode(productID);
			batchUsageRequest.setUsageRecords(unprocessedRecordsList);
			
			//For Local Testing
			//BatchMeterUsageResult batchUsageResponse = awsMeterClient.batchMeterUsage(batchUsageRequest);
			//this.processBatchMeterUsageResult(batchUsageResponse);
		}else{
			logger.debug("Finished Processing Metering Record List!");
		}
	}

}
