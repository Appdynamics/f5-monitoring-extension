package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.*;
import static com.appdynamics.extensions.f5.util.F5Util.*;
import iControl.CommonStatistic;
import iControl.Interfaces;
import iControl.SystemStatisticsBindingStub;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.f5.F5Metrics;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;

/**
 * @author Florencio Sarmiento
 *
 */
public class TCPMetricsCollector implements Callable<F5Metrics> {
	
	public static final Logger LOGGER = Logger.getLogger("com.singularity.extensions.f5.collectors.TCPMetricsCollector");
	
	private F5Metrics f5Metrics = new F5Metrics();
	private Interfaces iControlInterfaces;
	private String f5DisplayName;
	private Set<String> metricExcludes;
	
	public TCPMetricsCollector(Interfaces iControlInterfaces, 
			F5 f5, MetricsFilter metricsFilter) {
		
		this.iControlInterfaces = iControlInterfaces;
		this.f5DisplayName = f5.getDisplayName();
		this.metricExcludes = metricsFilter.getTcpMetricExcludes();
	}

	/*
	 * (non-Javadoc)
	 * Compatible with F5 v9.0
	 * @see https://devcentral.f5.com/wiki/iControl.System__Statistics__get_tcp_statistics.ashx
	 *
	 */
	public F5Metrics call() throws Exception {
		LOGGER.info("TCP metrics collector started...");
		
		try {
			SystemStatisticsBindingStub systemStats = iControlInterfaces.getSystemStatistics();
			
			if (systemStats != null) {
				String tcpMetricPrefix = getTCPMetricPrefix();
				Pattern excludePatterns = createPattern(metricExcludes);
				
				for (CommonStatistic stat : systemStats.get_tcp_statistics().getStatistics()) {
					if (isMetricToMonitor(stat.getType().getValue(), excludePatterns)) {
						String metricName = String.format("%s%s", tcpMetricPrefix, stat.getType().getValue());
						BigInteger value = convertValue(stat.getValue());
						f5Metrics.add(metricName, value);
					}
				}
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching tcp statistics", e);
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching tcp statistics", e);
		}
		
		return f5Metrics;
	}

	private String getTCPMetricPrefix() {
		return String.format("%s%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, 
				"Network", METRIC_PATH_SEPARATOR, TCP, METRIC_PATH_SEPARATOR);
	}
}
