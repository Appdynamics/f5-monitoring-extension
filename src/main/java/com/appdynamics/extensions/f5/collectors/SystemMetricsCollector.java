package com.appdynamics.extensions.f5.collectors;

import static com.appdynamics.extensions.f5.F5Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.f5.F5Constants.SYSTEM;

import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import iControl.Interfaces;
import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.rmi.RemoteException;

/**
 * @author Florencio Sarmiento
 */
public class SystemMetricsCollector extends AbstractMetricsCollector {

    public static final Logger LOGGER = Logger.getLogger(SystemMetricsCollector.class);

    private Interfaces iControlInterfaces;
    private String f5DisplayName;

    public SystemMetricsCollector(Interfaces iControlInterfaces, F5 f5, F5Monitor monitor, String metricPrefix) {

        super(monitor, metricPrefix);
        this.iControlInterfaces = iControlInterfaces;
        this.f5DisplayName = f5.getDisplayName();
    }

    /*
     * (non-Javadoc)
     * Compatible with F5 v9.4
     * @see https://devcentral.f5.com/wiki/iControl.System__SystemInfo__get_uptime.ashx
     *
     */
    public Void call() {
        LOGGER.info("System uptime metrics collector started...");

        try {
            BigInteger value = BigInteger.valueOf(iControlInterfaces.getSystemSystemInfo().get_uptime());
            printCollectiveObservedCurrent(getSystemMetricPrefix() + "Uptime (sec)", value);

        } catch (RemoteException e) {
            LOGGER.error("A connection issue occurred while fetching system uptime", e);

        } catch (Exception e) {
            LOGGER.error("An issue occurred while fetching system uptime", e);
        }

        return null;
    }

    private String getSystemMetricPrefix() {
        return String.format("%s%s%s%s", f5DisplayName, METRIC_PATH_SEPARATOR,
                SYSTEM, METRIC_PATH_SEPARATOR);
    }

}
