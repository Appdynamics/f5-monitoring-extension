package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.*;
import iControl.Interfaces;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.f5.F5Metrics;
import com.appdynamics.extensions.f5.config.F5;

/**
 * @author Florencio Sarmiento
 *
 */
public class SystemMetricsCollector implements Callable<F5Metrics> {
	
	public static final Logger LOGGER = Logger.getLogger("com.singularity.extensions.f5.collectors.SystemMetricsCollector");
	
	private F5Metrics f5Metrics = new F5Metrics();
	private Interfaces iControlInterfaces;
	private String f5DisplayName;
	
	public SystemMetricsCollector(Interfaces iControlInterfaces, F5 f5) {
		
		this.iControlInterfaces = iControlInterfaces;
		this.f5DisplayName = f5.getDisplayName();
	}

	/*
	 * (non-Javadoc)
	 * Compatible with F5 v9.4
	 * @see https://devcentral.f5.com/wiki/iControl.System__SystemInfo__get_uptime.ashx
	 *
	 */	
	public F5Metrics call() throws Exception {
		LOGGER.info("System uptime metrics collector started...");
		
		try {
			BigInteger value = BigInteger.valueOf(iControlInterfaces.getSystemSystemInfo().get_uptime());
			f5Metrics.add(getSystemMetricPrefix() + "Uptime (sec)", value);
			
		}  catch (RemoteException e) {
			LOGGER.error("A connection issue occurred while fetching system uptime", e);
			
		} catch (Exception e) {
			LOGGER.error("An issue occurred while fetching system uptime", e);
		} 
		
		return f5Metrics;
	}
	
	private String getSystemMetricPrefix() {
		return String.format("%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR, 
				SYSTEM, METRIC_PATH_SEPARATOR);
	}

}
