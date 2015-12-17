package com.appdynamics.extensions.f5;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import iControl.Interfaces;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({F5Monitor.class, PathResolver.class})
@PowerMockIgnore({"org.apache.*, javax.xml.*"})
public class F5MonitorTest {

    private F5Monitor classUnderTest;

    @Mock
    private F5MonitorTask mockF5MonitorTask;

    @Mock
    private MetricWriter mockMetricWriter;

    private F5Metrics testMetrics;

    @Mock
    private F5Monitor monitor;

    @Mock
    private MetricWriter metricWriter;

    private String metricPrefix = F5Constants.DEFAULT_METRIC_PATH;

    @Before
    public void setUp() throws Exception {
        classUnderTest = spy(new F5Monitor());
        mockStatic(PathResolver.class);
        PowerMockito.when(PathResolver.resolveDirectory(AManagedMonitor.class)).thenReturn(new File("./target"));
        whenNew(MetricWriter.class).withArguments(any(AManagedMonitor.class), anyString()).thenReturn(mockMetricWriter);
        setUpTestMetricsAndF5MonitorTask();
    }

    @Test(expected = TaskExecutionException.class)
    public void testWithNoArgs() throws TaskExecutionException {
        classUnderTest.execute(null, null);
    }

    @Test(expected = TaskExecutionException.class)
    public void testWithEmptyArgs() throws TaskExecutionException {
        classUnderTest.execute(new HashMap<String, String>(), null);
    }

    @Test(expected = TaskExecutionException.class)
    public void testWithNonExistentConfigFile() throws TaskExecutionException {
        Map<String, String> args = Maps.newHashMap();
        args.put("config-file", "src/test/resources/conf/no_config.yaml");

        classUnderTest.execute(args, null);
    }

    @Test(expected = TaskExecutionException.class)
    public void testWithInvalidConfigFile() throws TaskExecutionException {
        Map<String, String> args = Maps.newHashMap();
        args.put("config-file", "src/test/resources/conf/invalid_config.yaml");

        classUnderTest.execute(args, null);
    }

    private void setUpTestMetricsAndF5MonitorTask() throws Exception {
        testMetrics = new F5Metrics();
        testMetrics.add("MyTestF5|TEST|METRIC1", BigInteger.ONE);
        testMetrics.add("MyTestF5|TEST|METRIC2", BigInteger.valueOf(2));
        testMetrics.add("MyTestF5|TEST|METRIC3", BigInteger.valueOf(3));
        testMetrics.add("MyTestF5|TEST|METRIC4", BigInteger.valueOf(4));
        testMetrics.add("MyTestF5|TEST|METRIC5", BigInteger.valueOf(5));
        whenNew(F5MonitorTask.class).withArguments(any(F5Monitor.class), any(String.class), any(F5.class),
                any(Interfaces.class), any(MetricsFilter.class), anyInt(), anyInt()).thenReturn(mockF5MonitorTask);
    }

}
