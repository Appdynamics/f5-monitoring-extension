package com.appdynamics.extensions.f5.config;

import java.util.Set;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Florencio Sarmiento
 *
 */
public class MetricsFilter {

	private Set<String> poolMetricExcludes;
	
	private Set<String> snatPoolMetricExcludes;
	
	private Set<String> virtualServerMetricExcludes;
	
	private Set<String> iRuleMetricExcludes;
	
	private Set<String> clientSSLProfileMetricExcludes;
	
	private Set<String> serverSSLProfileMetricExcludes;
	
	private Set<String> networkInterfaceMetricExcludes;
	
	private Set<String> tcpMetricExcludes;
	
	private Set<String> httpMetricExcludes;
	
	private Set<String> httpCompressionMetricExcludes;

	public Set<String> getPoolMetricExcludes() {
		return poolMetricExcludes;
	}

	public void setPoolMetricExcludes(Set<String> poolMetricExcludes) {
		this.poolMetricExcludes = poolMetricExcludes;
	}

	public Set<String> getSnatPoolMetricExcludes() {
		return snatPoolMetricExcludes;
	}

	public void setSnatPoolMetricExcludes(Set<String> snatPoolMetricExcludes) {
		this.snatPoolMetricExcludes = snatPoolMetricExcludes;
	}

	public Set<String> getVirtualServerMetricExcludes() {
		return virtualServerMetricExcludes;
	}

	public void setVirtualServerMetricExcludes(
			Set<String> virtualServerMetricExcludes) {
		this.virtualServerMetricExcludes = virtualServerMetricExcludes;
	}

	public Set<String> getiRuleMetricExcludes() {
		return iRuleMetricExcludes;
	}

	public void setiRuleMetricExcludes(Set<String> iRuleMetricExcludes) {
		this.iRuleMetricExcludes = iRuleMetricExcludes;
	}

	public Set<String> getClientSSLProfileMetricExcludes() {
		return clientSSLProfileMetricExcludes;
	}

	public void setClientSSLProfileMetricExcludes(
			Set<String> clientSSLProfileMetricExcludes) {
		this.clientSSLProfileMetricExcludes = clientSSLProfileMetricExcludes;
	}

	public Set<String> getServerSSLProfileMetricExcludes() {
		return serverSSLProfileMetricExcludes;
	}

	public void setServerSSLProfileMetricExcludes(
			Set<String> serverSSLProfileMetricExcludes) {
		this.serverSSLProfileMetricExcludes = serverSSLProfileMetricExcludes;
	}

	public Set<String> getNetworkInterfaceMetricExcludes() {
		return networkInterfaceMetricExcludes;
	}

	public void setNetworkInterfaceMetricExcludes(
			Set<String> networkInterfaceMetricExcludes) {
		this.networkInterfaceMetricExcludes = networkInterfaceMetricExcludes;
	}

	public Set<String> getTcpMetricExcludes() {
		return tcpMetricExcludes;
	}

	public void setTcpMetricExcludes(Set<String> tcpMetricExcludes) {
		this.tcpMetricExcludes = tcpMetricExcludes;
	}

	public Set<String> getHttpMetricExcludes() {
		return httpMetricExcludes;
	}

	public void setHttpMetricExcludes(Set<String> httpMetricExcludes) {
		this.httpMetricExcludes = httpMetricExcludes;
	}

	public Set<String> getHttpCompressionMetricExcludes() {
		return httpCompressionMetricExcludes;
	}

	public void setHttpCompressionMetricExcludes(
			Set<String> httpCompressionMetricExcludes) {
		this.httpCompressionMetricExcludes = httpCompressionMetricExcludes;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
