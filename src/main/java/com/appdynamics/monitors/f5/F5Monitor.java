/**
 * Copyright 2013 AppDynamics
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */




package com.appdynamics.monitors.f5;

import iControl.CommonStatistic;
import iControl.CommonULong64;
import iControl.SystemStatisticsBindingStub;
import iControl.SystemStatisticsHostStatisticEntry;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class F5Monitor extends AManagedMonitor
{

	private Logger logger;
	private SystemStatisticsBindingStub stats;
	private iControl.Interfaces m_interfaces;
	private List<String> monitoredPoolMembers;
	private List<String> poolMemberMetrics = new ArrayList<String>();
	
	private String metricPath = "Custom Metrics|";

	/**
	 * This method is solely here for debugging purposes. If you would like to know
	 * whether or not the metric retrieval works with the credentials you provide in
	 * monitor.xml, you can simply run this java file and provide the credentials as
	 * arguments.
	 * Notice that the printed values are split up in high-order and low-order 32 bit
	 * values, so they will be special representations of the metric values
	 * @param args
	 */
	public static void main(String args[])
	{

		F5Monitor monitor = new F5Monitor();
		monitor.monitoredPoolMembers = new ArrayList<String>();

		monitor.poolMemberMetrics.add("STATISTIC_SERVER_SIDE_BYTES_IN");
		monitor.poolMemberMetrics.add("STATISTIC_SERVER_SIDE_BYTES_OUT");
		monitor.poolMemberMetrics.add("STATISTIC_SERVER_SIDE_PACKETS_IN");
		monitor.poolMemberMetrics.add("STATISTIC_SERVER_SIDE_PACKETS_OUT");
		monitor.poolMemberMetrics.add("STATISTIC_SERVER_SIDE_CURRENT_CONNECTIONS");
		monitor.poolMemberMetrics.add("STATISTIC_SERVER_SIDE_MAXIMUM_CONNECTIONS");
		monitor.poolMemberMetrics.add("STATISTIC_SERVER_SIDE_TOTAL_CONNECTIONS");
		monitor.poolMemberMetrics.add("STATISTIC_TOTAL_REQUESTS");


		if(args.length != 3 && args.length != 4){
			System.err.println("You have to provide three arguments in the correct order " +
					"(IP, Username, Password)!");
			return;
		}

		if(args.length == 4){
			String[] poolMembers = args[3].split(",");			
			for(String poolMember : poolMembers){
				System.out.println(poolMember);
				monitor.monitoredPoolMembers.add(poolMember);
			}
		}

		try{
			iControl.Interfaces m_interfaces = new iControl.Interfaces();
			m_interfaces.initialize(args[0], args[1], args[2]);
			SystemStatisticsBindingStub stats = m_interfaces.getSystemStatistics();

			System.out.println("-----GET ALL HOST STATISTICS-----");
			for (SystemStatisticsHostStatisticEntry stat : stats.get_all_host_statistics().getStatistics())
			{
				for (CommonStatistic st : stat.getStatistics())
				{
					System.out.println(st.getType() + " : " + monitor.new UsefulU64(st.getValue()).doubleValue());
					//System.out.println(st.getType() + " : " + st.getValue().getHigh());
					//System.out.println(st.getType() + " : " + st.getValue().getLow());
				}
			}

			/*System.out.println("-----GET HOST STATISTICS-----");
			for (CommonStatistic stat : stats.get_authentication_statistics().getStatistics())
			{
				System.out.println(stat.getType() + " : " + stat.getValue());
				//System.out.println(stat.getType() + " : " + stat.getValue().getHigh());
				//System.out.println(stat.getType() + " : " + stat.getValue().getLow());
			}*/

			System.out.println("-----GET CLIENT SSL STATISTICS-----");
			for (CommonStatistic stat : stats.get_client_ssl_statistics().getStatistics())
			{
				if(stat.getType().getValue().equals("STATISTIC_SSL_COMMON_CURRENT_CONNECTIONS")){
					System.out.println(stat.getType() + " : " + monitor.new UsefulU64(stat.getValue()).doubleValue());	
				}
				//System.out.println(stat.getType() + " : " + stat.getValue().getHigh());
				//System.out.println(stat.getType() + " : " + stat.getValue().getLow());
			}

			/*System.out.println("-----GET DIAMETER STATISTICS-----");
			for (CommonStatistic stat : stats.get_diameter_statistics().getStatistics())
			{
				System.out.println(stat.getType() + " : " + stat.getValue());
				//System.out.println(stat.getType() + " : " + stat.getValue().getHigh());
				//System.out.println(stat.getType() + " : " + stat.getValue().getLow());
			}*/

			/*System.out.println("-----GET GLOBAL HOST STATISTICS-----");
			for (CommonStatistic stat : stats.get_global_host_statistics().getStatistics())
			{
				System.out.println(stat.getType() + " : " + stat.getValue());
				//System.out.println(stat.getType() + " : " + stat.getValue().getHigh());
				//System.out.println(stat.getType() + " : " + stat.getValue().getLow());
			}*/

			/*	System.out.println("-----GET GLOBAL STATISTICS-----");
			for (CommonStatistic stat : stats.get_global_statistics().getStatistics())
			{
				System.out.println(stat.getType() + " : " + stat.getValue());
				//System.out.println(stat.getType() + " : " + stat.getValue().getHigh());
				//System.out.println(stat.getType() + " : " + stat.getValue().getLow());
			}*/

			/*System.out.println("-----GET IP STATISTICS-----");
			for (CommonStatistic stat : stats.get_ip_statistics().getStatistics())
			{
				System.out.println(stat.getType() + " : " + stat.getValue());
				//System.out.println(stat.getType() + " : " + stat.getValue().getHigh());
				//System.out.println(stat.getType() + " : " + stat.getValue().getLow());
			}*/

			System.out.println("-----GET TCP STATISTICS-----");
			for (CommonStatistic stat : stats.get_tcp_statistics().getStatistics())
			{
				if(stat.getType().getValue().equals("STATISTIC_TCP_OPEN_CONNECTIONS") ||
						stat.getType().getValue().equals("STATISTIC_TCP_CLOSE_WAIT_CONNECTIONS") ||
						stat.getType().getValue().equals("STATISTIC_TCP_ESTABLISHED_CONNECTIONS")){
					System.out.println(stat.getType() + " : " + monitor.new UsefulU64(stat.getValue()).doubleValue());
				}
				//System.out.println(stat.getType() + " : " + stat.getValue().getHigh());
				//System.out.println(stat.getType() + " : " + stat.getValue().getLow());
			}

			/*System.out.println("-----GET HTTP STATISTICS-----");
			for (CommonStatistic stat : stats.get_http_statistics().getStatistics())
			{
				System.out.println(stat.getType() + " : " + stat.getValue());
				//System.out.println(stat.getType() + " : " + stat.getValue().getHigh());
				//System.out.println(stat.getType() + " : " + stat.getValue().getLow());
			}

			System.out.println("-----GET UDP STATISTICS-----");
			for (CommonStatistic stat : stats.get_udp_statistics().getStatistics())
			{
				System.out.println(stat.getType() + " : " + stat.getValue());
				//System.out.println(stat.getType() + " : " + stat.getValue().getHigh());
				//System.out.println(stat.getType() + " : " + stat.getValue().getLow());
			}*/


			monitor.printPoolMemberStats(m_interfaces);
			//monitor.getPoolMemberStatus(m_interfaces);

		} catch (RemoteException e) {
			System.out.println("Could not retrieve metrics: " + e.getMessage() +
					"... Aborted metrics retrieval.");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Unable to connect to the F5 or initialize stats retrieval. Error Message: " +
					"" + e.getMessage() + "\n\nTerminating monitor.");
		}

	}

	public void printPoolMemberStats(iControl.Interfaces m_interfaces) throws Exception
	{
		String [] pool_list = m_interfaces.getLocalLBPool().get_list();

		iControl.LocalLBPoolMemberStatistics[] memberStats;
		memberStats = m_interfaces.getLocalLBPool().get_all_member_statistics(pool_list);
		for(iControl.LocalLBPoolMemberStatistics memberStatistics:memberStats){
			iControl.LocalLBPoolMemberStatisticEntry[] memberStatsEntries = memberStatistics.getStatistics();
			for(iControl.LocalLBPoolMemberStatisticEntry memberStatsEntry:memberStatsEntries){
				iControl.CommonStatistic[] stats = memberStatsEntry.getStatistics();
				System.out.println("   " + memberStatsEntry.getMember().getAddress() + " =");

				//if(isSupposedToBeMonitored(memberStatsEntry.getMember().getAddress())){
				for(iControl.CommonStatistic stat : stats){
					//if(this.poolMemberMetrics.contains(stat.getType().getValue())){

					System.out.println("    " + "Pool Members" + memberStatsEntry.getMember().getAddress().replaceAll("/", "|") + "|" + stat.getType().getValue() + ": " + (new UsefulU64(stat.getValue()).doubleValue()));//convert the value
					//}
				}
				//}
			}
		}



	}

	public void getPoolMemberStatus(String pool, String member) throws Exception
	{
		String [] pool_list = { pool };
		boolean found = false;

		iControl.LocalLBPoolMemberMemberObjectStatus [][] objStatusAofA = 
				m_interfaces.getLocalLBPoolMember().get_object_status(pool_list);

		iControl.LocalLBPoolMemberMemberObjectStatus [] objStatusA = objStatusAofA[0];
		for(int i=0; i<objStatusA.length; i++)
		{
			String a2 = objStatusA[i].getMember().getAddress();
			Long p2 = objStatusA[i].getMember().getPort();
			String m2 = a2 + ":" + p2;
			if ( member.equals(m2) )
			{
				iControl.LocalLBObjectStatus objStatus = objStatusA[i].getObject_status();
				iControl.LocalLBAvailabilityStatus availability = objStatus.getAvailability_status();
				iControl.LocalLBEnabledStatus enabled = objStatus.getEnabled_status();
				String description = objStatus.getStatus_description();

				System.out.println("Pool " + pool + ", Member " + member + " status:");
				System.out.println("  Availability : " + availability);
				System.out.println("  Enabled      : " + enabled);
				System.out.println("  Description  : " + description);

				found = true;
			}
		}
		if ( ! found )
		{
			System.out.println("Member " + member + " could not be found in pool " + pool);
		}
	}

	private boolean isSupposedToBeMonitored(String poolMemberCandidate){
		for(String poolMember : monitoredPoolMembers){
			if(poolMemberCandidate.substring(poolMemberCandidate.lastIndexOf('/') + 1, poolMemberCandidate.length()).equals(poolMember)){
				return true;
			}
		}
		return false;
	}




	public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskContext) throws TaskExecutionException
	{

		logger = Logger.getLogger(F5Monitor.class);
		
		poolMemberMetrics.add("STATISTIC_SERVER_SIDE_BYTES_IN");
		poolMemberMetrics.add("STATISTIC_SERVER_SIDE_BYTES_OUT");
		poolMemberMetrics.add("STATISTIC_SERVER_SIDE_PACKETS_IN");
		poolMemberMetrics.add("STATISTIC_SERVER_SIDE_PACKETS_OUT");
		poolMemberMetrics.add("STATISTIC_SERVER_SIDE_CURRENT_CONNECTIONS");
		poolMemberMetrics.add("STATISTIC_SERVER_SIDE_MAXIMUM_CONNECTIONS");
		poolMemberMetrics.add("STATISTIC_SERVER_SIDE_TOTAL_CONNECTIONS");
		poolMemberMetrics.add("STATISTIC_TOTAL_REQUESTS");

		if(!taskArguments.containsKey("Hostname") || !taskArguments.containsKey("Username") || !taskArguments.containsKey("Password")){
			logger.error("monitor.xml needs to contain these three arguments: \"Hostname\", \"Username\", and \"Password\". " +
					"Terminating monitor.");
			return null;
		}

		try{
			//connecting to F5, initializing statistics retrieval
			m_interfaces = new iControl.Interfaces();
			m_interfaces.initialize(taskArguments.get("Hostname"), taskArguments.get("Username"), taskArguments.get("Password"));
			stats = m_interfaces.getSystemStatistics();
			monitoredPoolMembers = new ArrayList<String>();

			// fill the Arraylist with metrics to exclude
			if(taskArguments.containsKey("monitored-poolmembers") && !taskArguments.get("monitored-poolmembers").equals(""))
			{
				String[] metrics = taskArguments.get("monitored-poolmembers").split(",");
				for(String metric : metrics){
					monitoredPoolMembers.add(metric.trim());
				}
			}
			
			if(taskArguments.containsKey("metric-path") && !taskArguments.get("metric-path").equals("")){
				metricPath = taskArguments.get("metric-path");
				logger.debug("Metric path: " + metricPath);
				if(!metricPath.endsWith("|")){
					metricPath += "|";
				}
			}

		} catch(Throwable t){
			logger.error("Unable to connect to the F5 or initialize stats retrieval. Error Message: " +
					"" + t.getMessage() + "... Terminating monitor.");
			return null;
		}

		// executing task, once per minute
		while(true){
			(new PrintMetricsThread()).start();
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				logger.error("Monitor interrupted. Terminating Monitor");
				return null;
			}
		}
	}



	/**
	 * Returns the metric to the AppDynamics Controller.
	 * @param 	metricName		Name of the Metric
	 * @param 	metricValue		Value of the Metric
	 * @param 	aggregation		Average OR Observation OR Sum
	 * @param 	timeRollup		Average OR Current OR Sum
	 * @param 	cluster			Collective OR Individual
	 */
	public void printMetric(String metricName, double metricValue, String aggregation, String timeRollup, String cluster)
	{
		MetricWriter metricWriter = getMetricWriter(metricPath + "F5 Monitor|" + metricName, 
				aggregation,
				timeRollup,
				cluster
				);

		// conversion/casting to long necessary for monitor:
		metricWriter.printMetric(String.valueOf((long) metricValue));
	}

	private class PrintMetricsThread extends Thread{
		public void run(){			
			try {

				//System.out.println("-----GET ALL HOST STATISTICS-----");
				for (SystemStatisticsHostStatisticEntry stat : stats.get_all_host_statistics().getStatistics())
				{
					for (CommonStatistic st : stat.getStatistics())
					{

						printMetric("All Host Stats|" + st.getType().toString(), (new UsefulU64(st.getValue())).doubleValue(),
								MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
								MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
								MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
					}
				}

				//System.out.println("-----GET CLIENT SSL STATISTICS-----");
				for (CommonStatistic stat : stats.get_client_ssl_statistics().getStatistics())
				{
					if(stat.getType().getValue().equals("STATISTIC_SSL_COMMON_CURRENT_CONNECTIONS")){
						printMetric("SSL Stats|" + stat.getType().toString(), (new UsefulU64(stat.getValue())).doubleValue(),
								MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
								MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
								MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
					}
				}

				//System.out.println("-----GET TCP STATISTICS-----");
				for (CommonStatistic stat : stats.get_tcp_statistics().getStatistics())
				{
					if(stat.getType().getValue().equals("STATISTIC_TCP_OPEN_CONNECTIONS") ||
							stat.getType().getValue().equals("STATISTIC_TCP_CLOSE_WAIT_CONNECTIONS") ||
							stat.getType().getValue().equals("STATISTIC_TCP_ESTABLISHED_CONNECTIONS")){
						printMetric("TCP Stats|" + stat.getType().toString(), (new UsefulU64(stat.getValue())).doubleValue(),
								MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
								MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
								MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
					}
				}

				reportPoolMemberStats();

			} catch (RemoteException e) {
				logger.error("Could not retrieve metrics: " + e.getMessage() +
						"... Aborted metrics retrieval for this minute.");
			}
		}

		private void reportPoolMemberStats(){
			try {

				String[] pool_list = m_interfaces.getLocalLBPool().get_list();
				iControl.LocalLBPoolMemberStatistics[] memberStats;
				memberStats = m_interfaces.getLocalLBPool().get_all_member_statistics(pool_list);
				for(iControl.LocalLBPoolMemberStatistics memberStatistics:memberStats){
					iControl.LocalLBPoolMemberStatisticEntry[] memberStatsEntries = memberStatistics.getStatistics();
					for(iControl.LocalLBPoolMemberStatisticEntry memberStatsEntry:memberStatsEntries){
						iControl.CommonStatistic[] stats = memberStatsEntry.getStatistics();
						if(isSupposedToBeMonitored(memberStatsEntry.getMember().getAddress())){							
							for(iControl.CommonStatistic stat : stats){
								if(poolMemberMetrics.contains(stat.getType().getValue())){
									printMetric("Pool Members" + memberStatsEntry.getMember().getAddress().replaceAll("/", "|") +  "|" + stat.getType().getValue(), (new UsefulU64(stat.getValue())).doubleValue(),
											MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
											MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
											MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
								}
							}
						}
					}
				}




			} catch (RemoteException e) {
				logger.warn("Can't retrieve statistic for a poolMember. Error Message: " + e.getMessage());
			} catch (Exception e) {
				logger.warn("Can't retrieve statistic for a poolMember. Error Message: " + e.getMessage());
			}
		}

		private boolean isSupposedToBeMonitored(String poolMemberCandidate){
			for(String poolMember : monitoredPoolMembers){
				if(poolMemberCandidate.substring(poolMemberCandidate.lastIndexOf('/') + 1, poolMemberCandidate.length()).equals(poolMember)){
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * 
	 * @author F5 devcentral
	 * https://devcentral.f5.com/tech-tips/articles/a-class-to-handle-ulong64-return-values-in-java#.Ub-yZ6FAT-k
	 *
	 */
	private class UsefulU64 extends CommonULong64 { 
		// The following line is required of all serializable classes, but not utilized in our implementation.
		static final long serialVersionUID = 1; 


		//public UsefulU64() { super(); } 
		//public UsefulU64(long arg0, long arg1) { super(arg0, arg1); } 
		public UsefulU64(CommonULong64 orig) { 
			this.setHigh(orig.getHigh()); 
			this.setLow(orig.getLow()); 
		} 

		public Double doubleValue() { 
			long high = getHigh(); 
			long low = getLow(); 
			Double retVal; 
			// Since getHigh() and getLow() are declared as signed longs but hold unsigned data, make certain that we account for a long 
			// that rolled over into the negatives. An alternative to this would be to hand modify the WSDL4J output, but then we'd  
			// have to rewrite that code each time a new release of iControl came out. It is cleaner (in our opinion) to account for it here. 
			Double rollOver = new Double((double)0x7fffffff); 
			rollOver = new Double(rollOver.doubleValue() + 1.0); 
			if(high >=0) 
				retVal = new Double((high << 32 & 0xffff0000)); 
			else 
				retVal = new Double(((high & 0x7fffffff) << 32) + (0x80000000 << 32)); 

			if(low >=0) 
				retVal = new Double(retVal.doubleValue() + (double)low); 
			else 
				retVal = new Double(retVal.doubleValue() + (double)((low & 0x7fffffff)) + rollOver.doubleValue()); 

			return retVal; 
		} 
	}
}
