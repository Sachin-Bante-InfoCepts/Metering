package com.nimblebi.metering;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.quartz.DateBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

public class MeteringSchedulerServlet extends HttpServlet{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5883421886257957618L;
	private static final Logger LOGGER = Logger.getLogger(MeteringSchedulerServlet.class);	
	private static final SchedulerFactory schedulerFactory = new StdSchedulerFactory();	
	private Scheduler scheduler = null;
	
	public void init() throws ServletException {
		LOGGER.debug("\n\n\n--------- Inside MeteringSchedulerServlet!!!! -----");
		
		try {
			scheduler = schedulerFactory.getScheduler();

			// Initiate JobDetail with job name, job group, and executable job class			
			JobDetail jobDetail = JobBuilder.newJob(SchedulerJob.class).withIdentity("db_refresher", "refresher").build();

			// Initiate Trigger with its name and group name.
			/*SimpleTrigger meteringTrigger = TriggerBuilder.newTrigger()
					.withIdentity(TriggerKey.triggerKey("meteringTrigger", "myTriggerGroup"))
					.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(1).repeatForever())
					.startAt(DateBuilder.futureDate(1, IntervalUnit.MINUTE)).build();
*/
			//For Testing on PROD
			SimpleTrigger meteringTrigger = TriggerBuilder.newTrigger()
					.withIdentity(TriggerKey.triggerKey("meteringTrigger", "myTriggerGroup"))
					.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(10).repeatForever())
					.startAt(DateBuilder.futureDate(1, IntervalUnit.MINUTE)).build();
			//end
			
			scheduler.scheduleJob(jobDetail, meteringTrigger);
			scheduler.start();

		} catch (SchedulerException se) {
			LOGGER.error("\n\n\n***Exception occured while starting the scheduler !!!");
			se.printStackTrace();
		} catch (Exception e) {
			LOGGER.error(e);
		}
	}

}
