package com.appdynamics.extensions.f5;

/**
 * @author Florencio Sarmiento
 *
 */
public class F5Constants {

	public static final String METRIC_PATH_SEPARATOR = "|";
	
	public static final String DEFAULT_METRIC_PATH = String.format("%s%s%s%s", "Custom Metrics", 
			METRIC_PATH_SEPARATOR, "F5 Monitor", METRIC_PATH_SEPARATOR);
	
	public static final String PATH_SEPARATOR = "/";
	
	public static final String DASH_SEPARATOR = "-";
	
	public static final String CONFIG_ARG = "config-file";
	
	public static final String PORT_SEPARATOR = ":";
	
	public static final String SSL = "SSL";
	
	public static final String CLIENTS = "Clients";
	
	public static final String SYSTEM = "System";
	
	public static final String CPU = "CPU";
	
	public static final String DISKS = "Disks";
	
	public static final String NETWORK = "Network";
	
	public static final String HTTP = "HTTP";
	
	public static final String HOST = "Host";
	
	public static final String TCP = "TCP";
	
	public static final String COMPRESSION = "Compression";
	
	public static final String IRULES = "iRules";
	
	public static final String INTERFACES = "Interfaces";
	
	public static final String POOLS = "Pools";

	public static final String MEMBERS = "Members";
	
	public static final String SNAT_POOLS = "SNAT Pools";
	
	public static final String SERVERS = "Servers";
	
	public static final String VIRTUAL_SERVERS = "Virtual Servers";
	
	public static final String STATUS = "STATUS";
	
	public static final int DEFAULT_NO_OF_THREADS = 3;
	
	public static final String TOTAL_NO_OF_MEMBERS = "Total No of Members";
	
	public static final String TOTAL_NO_OF_MEMBERS_AVAILABLE = "Total No of Available Members";
	
	public static final String TOTAL_NO_OF_MEMBERS_UNAVAILABLE = "Total No of Unavailable Members";
}
