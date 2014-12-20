package com.appdynamics.extensions.f5;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Florencio Sarmiento
 *
 */
public class F5Metrics {

	private Map<String, BigInteger> metrics;

	public void add(String metricName, BigInteger value) {
		getMetrics().put(metricName, value);
	}
	
	public void addAll(Map<String, BigInteger> metrics) {
		getMetrics().putAll(metrics);
	}
	
	public Map<String, BigInteger> getMetrics() {
		if (this.metrics == null) {
			this.metrics =  new ConcurrentHashMap<String, BigInteger>();
		}
		
		return this.metrics;
	}

	public void setMetrics(Map<String, BigInteger> metrics) {
		this.metrics = new ConcurrentHashMap<String, BigInteger>(metrics);
	}
	
}
