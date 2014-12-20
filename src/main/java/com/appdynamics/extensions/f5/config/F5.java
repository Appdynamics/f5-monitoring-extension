package com.appdynamics.extensions.f5.config;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Florencio Sarmiento
 *
 */
public class F5 {
	
	private String name;
	
	private String hostname;
	
	private String username;
	
	private String password;
	
	private String passwordEncrypted;
	
	private String encryptionKey;
	
	private Set<String> poolIncludes;
	
	private Set<String> poolMemberIncludes;
	
	private Set<String> snatPoolIncludes;
	
	private Set<String> snatPoolMemberIncludes;
	
	private Set<String> virtualServerIncludes;
	
	private Set<String> iRuleIncludes;
	
	private Set<String> clientSSLProfileIncludes;

	private Set<String> serverSSLProfileIncludes;
    
	private Set<String> networkInterfaceIncludes;
	
	private String version;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPasswordEncrypted() {
		return passwordEncrypted;
	}

	public void setPasswordEncrypted(String passwordEncrypted) {
		this.passwordEncrypted = passwordEncrypted;
	}

	public String getEncryptionKey() {
		return encryptionKey;
	}

	public void setEncryptionKey(String encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	public Set<String> getPoolIncludes() {
		return poolIncludes;
	}

	public void setPoolIncludes(Set<String> poolIncludes) {
		this.poolIncludes = poolIncludes;
	}
	
	public Set<String> getPoolMemberIncludes() {
		return poolMemberIncludes;
	}

	public void setPoolMemberIncludes(Set<String> poolMemberIncludes) {
		this.poolMemberIncludes = poolMemberIncludes;
	}

	public Set<String> getSnatPoolIncludes() {
		return snatPoolIncludes;
	}

	public void setSnatPoolIncludes(Set<String> snatPoolIncludes) {
		this.snatPoolIncludes = snatPoolIncludes;
	}

	public Set<String> getSnatPoolMemberIncludes() {
		return snatPoolMemberIncludes;
	}

	public void setSnatPoolMemberIncludes(Set<String> snatPoolMemberIncludes) {
		this.snatPoolMemberIncludes = snatPoolMemberIncludes;
	}

	public Set<String> getVirtualServerIncludes() {
		return virtualServerIncludes;
	}

	public void setVirtualServerIncludes(Set<String> virtualServerIncludes) {
		this.virtualServerIncludes = virtualServerIncludes;
	}

	public Set<String> getiRuleIncludes() {
		return iRuleIncludes;
	}

	public void setiRuleIncludes(Set<String> iRuleIncludes) {
		this.iRuleIncludes = iRuleIncludes;
	}

	public Set<String> getClientSSLProfileIncludes() {
		return clientSSLProfileIncludes;
	}

	public void setClientSSLProfileIncludes(Set<String> clientSSLProfileIncludes) {
		this.clientSSLProfileIncludes = clientSSLProfileIncludes;
	}

	public Set<String> getServerSSLProfileIncludes() {
		return serverSSLProfileIncludes;
	}

	public void setServerSSLProfileIncludes(Set<String> serverSSLProfileIncludes) {
		this.serverSSLProfileIncludes = serverSSLProfileIncludes;
	}

	public Set<String> getNetworkInterfaceIncludes() {
		return networkInterfaceIncludes;
	}

	public void setNetworkInterfaceIncludes(Set<String> networkInterfaceIncludes) {
		this.networkInterfaceIncludes = networkInterfaceIncludes;
	}
	
	public boolean isPreVersion11() {
		return StringUtils.isBlank(this.version) ||
				!this.version.startsWith("BIG-IP_v11");
	}
	
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getDisplayName() {
		return StringUtils.isNotBlank(this.name) ? 
				this.name : this.hostname;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
