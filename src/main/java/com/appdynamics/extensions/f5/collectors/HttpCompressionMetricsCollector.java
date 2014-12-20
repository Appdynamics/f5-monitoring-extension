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
 * 
 * @author Florencio Sarmiento
 *
 */
public class HttpCompressionMetricsCollector implements Callable<F5Metrics> {
	
	public static final Logger LOGGER = Logger.getLogger("com.singularity.extensions.f5.collectors.HttpCompressionMetricsCollector");
	
	private F5Metrics f5Metrics = new F5Metrics();
	private Interfaces iControlInterfaces;
	private String f5DisplayName;
	private Set<String> metricExcludes;
	private boolean preVersion11;
	
	public HttpCompressionMetricsCollector(Interfaces iControlInterfaces, 
			F5 f5, MetricsFilter metricsFilter) {
		
		this.iControlInterfaces = iControlInterfaces;
		this.f5DisplayName = f5.getDisplayName();
		this.metricExcludes = metricsFilter.getHttpCompressionMetricExcludes();
		this.preVersion11 = f5.isPreVersion11();
	}

	/*
	 * (non-Javadoc)
	 * Compatible with F5 v11.0
	 * @see https://devcentral.f5.com/wiki/iControl.System__Statistics__get_httpcompression_statistics.ashx
	 */
	public F5Metrics call() throws Exception {
		if (preVersion11) {
			LOGGER.info("HTTP Compression metrics collector not supported in this version.");
			return f5Metrics;
		}
		
		LOGGER.info("HTTP Compression metrics collector started...");
		
		try {
			SystemStatisticsBindingStub systemStats = iControlInterfaces.getSystemStatistics();
			
			if (systemStats != null) {
				String httpCompressionMetricPrefix = getHttpCompressionMetricPrefix();
				Pattern excludePatterns = createPattern(metricExcludes);
				
				for (CommonStatistic stat : systemStats.get_httpcompression_statistics().getStatistics()) {
					if (isMetricToMonitor(stat.getType().getValue(), excludePatterns)) {
						String metricName = String.format("%s%s", httpCompressionMetricPrefix, stat.getType().getValue());
						BigInteger value = convertValue(stat.getValue());
						f5Metrics.add(metricName, value);
					}
				}
			}
			
		} catch (RemoteException e) {
			LOGGER.error("An issue occurred while fetching http compression statistics", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching http compression statistics", e);
		}
		
		return f5Metrics;
	}
	
	private String getHttpCompressionMetricPrefix() {
		return String.format("%s%s%s%s%s%s%s%s", 
				f5DisplayName, METRIC_PATH_SEPARATOR, 
				NETWORK, METRIC_PATH_SEPARATOR,
				HTTP, METRIC_PATH_SEPARATOR, 
				COMPRESSION, METRIC_PATH_SEPARATOR);
	}

}
