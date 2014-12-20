package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.*;
import static com.appdynamics.extensions.f5.util.F5Util.convertValue;
import static com.appdynamics.extensions.f5.util.F5Util.createPattern;
import static com.appdynamics.extensions.f5.util.F5Util.filterIncludes;
import static com.appdynamics.extensions.f5.util.F5Util.isMetricToMonitor;
import iControl.CommonStatistic;
import iControl.Interfaces;
import iControl.NetworkingInterfacesInterfaceStatisticEntry;
import iControl.NetworkingInterfacesInterfaceStatistics;
import iControl.NetworkingMediaStatus;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import com.appdynamics.extensions.f5.F5Metrics;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import com.appdynamics.extensions.f5.util.F5Util.NetworkInterfaceStatus;

/**
 * @author Florencio Sarmiento
 *
 */
public class NetworkInterfaceMetricsCollector implements Callable<F5Metrics> {
	
	public static final Logger LOGGER = Logger.getLogger("com.singularity.extensions.f5.collectors.NetworkInterfaceMetricsCollector");
	
	private F5Metrics f5Metrics = new F5Metrics();
	private Interfaces iControlInterfaces;
	private String f5DisplayName;
	private Set<String> networkInterfaceIncludes;
	private Set<String> metricExcludes;
	
	public NetworkInterfaceMetricsCollector(Interfaces iControlInterfaces, 
			F5 f5, MetricsFilter metricsFilter) {
	
		this.iControlInterfaces = iControlInterfaces;
		this.f5DisplayName = f5.getDisplayName();
		this.networkInterfaceIncludes = f5.getNetworkInterfaceIncludes();
		this.metricExcludes = metricsFilter.getNetworkInterfaceMetricExcludes();
	}

	/*
	 * (non-Javadoc)
	 * Compatible with F5 v9.0
	 * @see https://devcentral.f5.com/wiki/iControl.Networking__Interfaces__get_list.ashx
	 *
	 */
	public F5Metrics call() throws Exception {
		LOGGER.info("Network interface metrics collector started...");
		
		if (networkInterfaceIncludes == null || networkInterfaceIncludes.isEmpty()) {
			LOGGER.info("No network interfaces were included for monitoring.");
			return f5Metrics;
		}
		
		try {
			String[] networkInterfaces = iControlInterfaces.getNetworkingInterfaces().get_list();
			
			if (ArrayUtils.isNotEmpty(networkInterfaces)) {
				Pattern networkInterfaceIncludesPattern = createPattern(networkInterfaceIncludes);
				networkInterfaces = filterIncludes(networkInterfaces, networkInterfaceIncludesPattern);
				
				if (ArrayUtils.isNotEmpty(networkInterfaces)) {
					collectNetworkInterfaceMetrics(networkInterfaces);
					
				} else {
					LOGGER.info("No network interface matched.");
				}
			} else {
				LOGGER.info("No network interface found.");
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching network interface list", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching network interface list", e);
		}
		
		return f5Metrics;
	}

	/*
	 * (non-Javadoc)
	 * Compatible with F5 v9.0
	 * @see https://devcentral.f5.com/wiki/iControl.Networking__Interfaces__get_statistics.ashx
	 *
	 */
	private void collectNetworkInterfaceMetrics(String[] networkInterfaces) {
		try {
			NetworkingInterfacesInterfaceStatistics netStats = 
					iControlInterfaces.getNetworkingInterfaces().get_statistics(networkInterfaces);
			
			if (netStats != null) {
				String networkInterfaceMetricPrefix = getNetworkInterfaceMetricPrefix();
				Pattern excludePatterns = createPattern(metricExcludes);
				
				for (NetworkingInterfacesInterfaceStatisticEntry netStatEntry : netStats.getStatistics()) {
					for (CommonStatistic stat : netStatEntry.getStatistics()) {
						if (isMetricToMonitor(stat.getType().getValue(), excludePatterns)) {
							String metricName = String.format("%s%s%s%s", networkInterfaceMetricPrefix, 
									netStatEntry.getInterface_name(), METRIC_PATH_SEPARATOR, stat.getType().getValue());
							BigInteger value = convertValue(stat.getValue());
							f5Metrics.add(metricName, value);
						}
					}
				}
				
				if (isMetricToMonitor(STATUS, excludePatterns)) {
					collectNetworkInterfaceStatus(networkInterfaces);
				}
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching network interface statistics", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching network interface statistics", e);
		} 
	}

	/*
	 * Status is in the same order as the network interface in the array
	 * 
	 * Compatible with F5 v9.0
	 * @see https://devcentral.f5.com/wiki/iControl.Networking__Interfaces__get_media_status.ashx
	 */
	private void collectNetworkInterfaceStatus(String[] networkInterfaces) {
		try {
			int networkInterfaceIndex = 0;
			
			String networkInterfaceMetricPrefix = getNetworkInterfaceMetricPrefix();
			
			for (NetworkingMediaStatus status: iControlInterfaces.getNetworkingInterfaces().get_media_status(networkInterfaces)) {
				String metricName = String.format("%s%s%s%s", networkInterfaceMetricPrefix, 
						networkInterfaces[networkInterfaceIndex++], 
						METRIC_PATH_SEPARATOR, STATUS);
				BigInteger value = BigInteger.valueOf(NetworkInterfaceStatus.getValue(status.getValue()));
				f5Metrics.add(metricName, value);
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching network interface status", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching network interface status", e);
		} 
	}
	
	private String getNetworkInterfaceMetricPrefix() {
		return String.format("%s%s%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, 
				NETWORK, METRIC_PATH_SEPARATOR, INTERFACES, METRIC_PATH_SEPARATOR);
	}

}
