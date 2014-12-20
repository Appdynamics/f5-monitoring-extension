package com.appdynamics.extensions.f5.config;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Florencio Sarmiento
 *
 */
public class Configuration {
	
	private String metricPrefix;
	
	private List<F5> f5s;
	
	private MetricsFilter metricsFilter;
	
	private int numberOfF5Threads;
	
	private int numberOfThreadsPerF5;
	
	public String getMetricPrefix() {
		return metricPrefix;
	}

	public void setMetricPrefix(String metricPrefix) {
		if (StringUtils.isNotBlank(metricPrefix)) {
			this.metricPrefix = metricPrefix.trim();
			
		} else {
			this.metricPrefix = metricPrefix;
		}
	}

	public List<F5> getF5s() {
		return f5s;
	}

	public void setF5s(List<F5> f5s) {
		this.f5s = f5s;
	}

	public int getNumberOfF5Threads() {
		return numberOfF5Threads;
	}

	public void setNumberOfF5Threads(int numberOfF5Threads) {
		this.numberOfF5Threads = numberOfF5Threads;
	}

	public int getNumberOfThreadsPerF5() {
		return numberOfThreadsPerF5;
	}

	public void setNumberOfThreadsPerF5(int numberOfThreadsPerF5) {
		this.numberOfThreadsPerF5 = numberOfThreadsPerF5;
	}
	
	public MetricsFilter getMetricsFilter() {
		return metricsFilter;
	}

	public void setMetricsFilter(MetricsFilter metricsFilter) {
		this.metricsFilter = metricsFilter;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
