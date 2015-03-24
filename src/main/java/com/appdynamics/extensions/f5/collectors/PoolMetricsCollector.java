package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.POOLS;
import static com.appdynamics.extensions.f5.F5Constants.STATUS;
import static com.appdynamics.extensions.f5.util.F5Util.changePathSeparator;
import static com.appdynamics.extensions.f5.util.F5Util.convertToStatus;
import static com.appdynamics.extensions.f5.util.F5Util.convertValue;
import static com.appdynamics.extensions.f5.util.F5Util.createPattern;
import static com.appdynamics.extensions.f5.util.F5Util.filterIncludes;
import static com.appdynamics.extensions.f5.util.F5Util.isMetricToMonitor;
import iControl.CommonStatistic;
import iControl.Interfaces;
import iControl.LocalLBObjectStatus;
import iControl.LocalLBPoolPoolStatisticEntry;
import iControl.LocalLBPoolPoolStatistics;

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

/**
 * @author Florencio Sarmiento
 *
 */
public class PoolMetricsCollector implements Callable<F5Metrics> {
	
	public static final Logger LOGGER = Logger.getLogger("com.singularity.extensions.f5.collectors.PoolMetricsCollector");
	
	private F5Metrics f5Metrics = new F5Metrics();
	private Interfaces iControlInterfaces;
	private String f5DisplayName;
	private Set<String> poolIncludes;
	private Set<String> poolMemberIncludes;
	private Set<String> metricExcludes;
	private boolean preVersion11;

	public PoolMetricsCollector(Interfaces iControlInterfaces, F5 f5, MetricsFilter metricsFilter) {
		
		this.iControlInterfaces = iControlInterfaces;
		this.f5DisplayName = f5.getDisplayName();
		this.poolIncludes = f5.getPoolIncludes();
		this.poolMemberIncludes = f5.getPoolMemberIncludes();
		this.metricExcludes = metricsFilter.getPoolMetricExcludes();
		this.preVersion11 = f5.isPreVersion11();
	}

	/*
	 * (non-Javadoc)
	 * Compatible with F5 v9.0
	 * @see https://devcentral.f5.com/wiki/iControl.LocalLB__Pool__get_list.ashx
	 *
	 */
	public F5Metrics call() throws Exception {
		LOGGER.info("Pool metrics collector started...");
		
		if (poolIncludes == null || poolIncludes.isEmpty()) {
			LOGGER.info("No pools were included for monitoring.");
			return f5Metrics;
		}
		
		try {
			String[] pools = iControlInterfaces.getLocalLBPool().get_list();
			
			if (ArrayUtils.isNotEmpty(pools)) {
				Pattern poolIncludesPattern = createPattern(poolIncludes);
				pools = filterIncludes(pools, poolIncludesPattern);
				
				if (ArrayUtils.isNotEmpty(pools)) {
					collectPoolMetrics(pools);
					collectMemberMetrics(pools);
					
				} else {
					LOGGER.info("No Pool matched.");
				}
			} else {
				LOGGER.info("No Pool found.");
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching pool list", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching pool list", e);
		} 
		
		return f5Metrics;
	}

	/*
	 * (non-Javadoc)
	 * Compatible with F5 v9.0
	 * @see https://devcentral.f5.com/wiki/iControl.LocalLB__Pool__get_statistics.ashx
	 *
	 */
	private void collectPoolMetrics(String[] pools) {
		try {
			LocalLBPoolPoolStatistics stats = iControlInterfaces.getLocalLBPool().get_statistics(pools);
			
			if (stats != null) {
				String poolMetricPrefix = getPoolMetricPrefix();
				Pattern excludePatterns = createPattern(metricExcludes);
				
				for (LocalLBPoolPoolStatisticEntry pool : stats.getStatistics()) {
					String poolName = changePathSeparator(pool.getPool_name(), 
							PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);
					
					for (CommonStatistic stat : pool.getStatistics()) {
						if (isMetricToMonitor(stat.getType().getValue(), excludePatterns)) {
							String metricName = String.format("%s%s%s%s", poolMetricPrefix, 
									poolName, METRIC_PATH_SEPARATOR, stat.getType().getValue());
							
							BigInteger value = convertValue(stat.getValue());
							f5Metrics.add(metricName, value);
						}
					}
				}
				
				if (isMetricToMonitor(STATUS, excludePatterns)) {
					collectPoolStatus(pools);
				}
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching pool statistics", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching pool statistics", e);
		} 
	}

	/*
	 * (non-Javadoc)
	 * Compatible with F5 v9.0
	 * @see https://devcentral.f5.com/wiki/iControl.LocalLB__Pool__get_object_status.ashx
	 *
	 */
	private void collectPoolStatus(String[] pools) {
		try {
			LocalLBObjectStatus[] statuses = iControlInterfaces.getLocalLBPool().get_object_status(pools);
			
			if (ArrayUtils.isNotEmpty(statuses)) {
				String poolMetricPrefix = getPoolMetricPrefix();
				int index = 0;
				
				for (LocalLBObjectStatus status : statuses) {
					String poolName = changePathSeparator(pools[index++], 
							PATH_SEPARATOR, METRIC_PATH_SEPARATOR, true);
					
					String metricName = String.format("%s%s%s%s", poolMetricPrefix, 
							poolName, METRIC_PATH_SEPARATOR, STATUS);
					
					BigInteger value = BigInteger.valueOf(convertToStatus(status.getAvailability_status(), 
							status.getEnabled_status()).getValue());
					f5Metrics.add(metricName, value);
				}
			}
			
		} catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching pool status", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching pool status", e);
		} 
	}
	
	private void collectMemberMetrics(String[] pools) {
		if (poolMemberIncludes == null || poolMemberIncludes.isEmpty()) {
			LOGGER.info("No pool members were included for monitoring.");
//			return;
		} 
		
		PoolMemberMetricsCollector memberMetricsCollector;
		
		if (preVersion11) {
			memberMetricsCollector = new PreVersion11PoolMemberMetricsCollector(
					poolMemberIncludes, metricExcludes, iControlInterfaces);
		} else {
			memberMetricsCollector = new PoolMemberMetricsCollector(
					poolMemberIncludes, metricExcludes, iControlInterfaces);
		}
		
		memberMetricsCollector.collectMemberMetrics(getPoolMetricPrefix(), pools, f5Metrics);
	}
	
	private String getPoolMetricPrefix() {
		return String.format("%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, POOLS);
	}

}
