package com.appdynamics.extensions.f5;

import static com.appdynamics.extensions.f5.F5Constants.DEFAULT_NO_OF_THREADS;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.f5.collectors.CPUMemoryMetricsCollector;
import com.appdynamics.extensions.f5.collectors.ClientSSLMetricsCollector;
import com.appdynamics.extensions.f5.collectors.DiskMetricsCollector;
import com.appdynamics.extensions.f5.collectors.HttpCompressionMetricsCollector;
import com.appdynamics.extensions.f5.collectors.HttpMetricsCollector;
import com.appdynamics.extensions.f5.collectors.IRuleMetricsCollector;
import com.appdynamics.extensions.f5.collectors.NetworkInterfaceMetricsCollector;
import com.appdynamics.extensions.f5.collectors.PoolMetricsCollector;
import com.appdynamics.extensions.f5.collectors.ServerSSLMetricsCollector;
import com.appdynamics.extensions.f5.collectors.SnatPoolMetricsCollector;
import com.appdynamics.extensions.f5.collectors.TCPMetricsCollector;
import com.appdynamics.extensions.f5.collectors.VirtualServerMetricsCollector;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import com.appdynamics.extensions.f5.http.HttpExecutor;
import com.appdynamics.extensions.http.Http4ClientBuilder;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Florencio Sarmiento
 * @author Satish M
 */
public class F5MonitorTask implements Callable<Boolean> {

    public static final Logger LOGGER = Logger.getLogger(F5MonitorTask.class);

    private F5 f5;


    private MetricsFilter metricsFilter;

    private int noOfThreads;

    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;

    private ExecutorService threadPool;

    private F5Monitor monitor;
    private String metricPrefix;

    private int f5ThreadTimeout = 30;

    public F5MonitorTask(F5Monitor monitor, String metricPrefix, F5 f5, MetricsFilter metricsFilter, int noOfThreads, int f5ThreadTimeout) throws TaskExecutionException {
        this.monitor = monitor;
        this.metricPrefix = metricPrefix;
        this.f5 = f5;
        this.metricsFilter = metricsFilter;
        this.noOfThreads = noOfThreads > 0 ? noOfThreads : DEFAULT_NO_OF_THREADS;
        this.f5ThreadTimeout = f5ThreadTimeout;

        buildHttpClient(f5);
    }

    private void buildHttpClient(F5 f5) throws TaskExecutionException {

        Map<String, List<Map<String, String>>> map = new HashMap<String, List<Map<String, String>>>();
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        map.put("servers", list);
        HashMap<String, String> server = new HashMap<String, String>();
        server.put("uri", "https://" + f5.getHostname());
        server.put("username", f5.getUsername());
        server.put("password", getPassword());
        list.add(server);


        //Workaround to ignore the certificate mismatch issue.
        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Unable to create SSL context", e);
            throw new TaskExecutionException("Unable to create SSL context", e);
        } catch (KeyManagementException e) {
            LOGGER.error("Unable to create SSL context", e);
            throw new TaskExecutionException("Unable to create SSL context", e);
        } catch (KeyStoreException e) {
            LOGGER.error("Unable to create SSL context", e);
            throw new TaskExecutionException("Unable to create SSL context", e);
        }
        HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, (X509HostnameVerifier) hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();

        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        HttpClientBuilder builder = Http4ClientBuilder.getBuilder(map);
        builder.setConnectionManager(connMgr);

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(f5.getUsername(), getPassword()));

        httpClient = builder.setSSLSocketFactory(sslSocketFactory).setDefaultCredentialsProvider(credentialsProvider).build();

        httpContext = HttpClientContext.create();
        httpContext.setCredentialsProvider(credentialsProvider);
    }

    public Boolean call() {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("F5 monitoring task for [%s] has started...",
                    f5.getDisplayName()));
        }

        if (!checkCredentials()) {
            LOGGER.error(String.format(
                    "Unable to initialise F5 [%s]. Check your connection and credentials.",
                    f5.getDisplayName()));

            return Boolean.FALSE;
        }

        try {
            threadPool = Executors.newFixedThreadPool(noOfThreads);
            List<Callable<Void>> metricCollectors = createMetricCollectors();
            runConcurrentTasks(metricCollectors);

        } finally {
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown();
            }
        }

        return Boolean.TRUE;
    }

    public Boolean callSequential() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("F5 monitoring task has started initialization...");
        }

        if (!checkCredentials()) {
            LOGGER.error(String.format(
                    "Unable to initialise F5 [%s]. Check your connection and credentials.",
                    f5.getDisplayName()));

            return Boolean.FALSE;
        }

        List<Callable<Void>> metricCollectors = createMetricCollectors();

        for (Callable<Void> metricCollector : metricCollectors) {
            try {
                metricCollector.call();
            } catch (Exception e) {
                LOGGER.error("Error in initializing F5 monitor", e);
            }
        }
        return Boolean.TRUE;
    }


    private String getPassword() {
        String password = null;

        if (StringUtils.isNotBlank(f5.getPassword())) {
            password = f5.getPassword();

        } else {
            try {
                Map<String, String> args = Maps.newHashMap();
                args.put(TaskInputArgs.PASSWORD_ENCRYPTED, f5.getPasswordEncrypted());
                args.put(TaskInputArgs.ENCRYPTION_KEY, f5.getEncryptionKey());
                password = CryptoUtil.getPassword(args);

            } catch (IllegalArgumentException e) {
                String msg = "Encryption Key not specified. Please set the value in config.yaml.";
                LOGGER.error(msg);
                throw new IllegalArgumentException(msg);
            }
        }

        return password;
    }

    private boolean checkCredentials() {
        boolean success = false;

        try {
            HttpGet httpGet = new HttpGet("https://" + f5.getHostname() + "/mgmt/tm/ltm");
            CloseableHttpResponse response = HttpExecutor.execute(httpClient, httpGet, httpContext);

            if (response == null) {
                LOGGER.info("Unable to get any response from F5");
                return false;
            }

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                success = true;
            } else {
                LOGGER.error("Received response code [" + statusCode + "] when tried to connect to F5");
            }

        } catch (Exception e) {
            LOGGER.error("Unable to connect with the credentials provided.", e);
        }

        return success;
    }

    private List<Callable<Void>> createMetricCollectors() {
        List<Callable<Void>> metricCollectors = new ArrayList<Callable<Void>>();

        if (f5.getPoolIncludes() != null && f5.getPoolIncludes().size() > 0) {
            PoolMetricsCollector poolMetricsCollector = new PoolMetricsCollector(
                    httpClient, httpContext, f5, metricsFilter, monitor, metricPrefix);
            metricCollectors.add(poolMetricsCollector);
        }

        if (f5.getSnatPoolIncludes() != null && f5.getSnatPoolIncludes().size() > 0) {
            SnatPoolMetricsCollector snatPoolMetricsCollector = new SnatPoolMetricsCollector(
                    httpClient, httpContext, f5, metricsFilter, monitor, metricPrefix);
            metricCollectors.add(snatPoolMetricsCollector);
        }

        if (f5.getVirtualServerIncludes() != null && f5.getVirtualServerIncludes().size() > 0) {
            VirtualServerMetricsCollector vsMetricsCollector = new VirtualServerMetricsCollector(
                    httpClient, httpContext, f5, metricsFilter, monitor, metricPrefix);
            metricCollectors.add(vsMetricsCollector);
        }

        if (f5.getiRuleIncludes() != null && f5.getiRuleIncludes().size() > 0) {
            IRuleMetricsCollector iRuleMetricsCollector = new IRuleMetricsCollector(
                    httpClient, httpContext, f5, metricsFilter, monitor, metricPrefix);
            metricCollectors.add(iRuleMetricsCollector);
        }

        if (f5.getClientSSLProfileIncludes() != null && f5.getClientSSLProfileIncludes().size() > 0) {
            ClientSSLMetricsCollector clientSSLMetricsCollector = new ClientSSLMetricsCollector(
                    httpClient, httpContext, f5, metricsFilter, monitor, metricPrefix);
            metricCollectors.add(clientSSLMetricsCollector);
        }

        if (f5.getServerSSLProfileIncludes() != null && f5.getServerSSLProfileIncludes().size() > 0) {
            ServerSSLMetricsCollector serverSSLMetricsCollector = new ServerSSLMetricsCollector(
                    httpClient, httpContext, f5, metricsFilter, monitor, metricPrefix);
            metricCollectors.add(serverSSLMetricsCollector);
        }

        if (f5.getNetworkInterfaceIncludes() != null && f5.getNetworkInterfaceIncludes().size() > 0) {
            NetworkInterfaceMetricsCollector networkInterfaceMetricsCollector = new NetworkInterfaceMetricsCollector(
                    httpClient, httpContext, f5, metricsFilter, monitor, metricPrefix);
            metricCollectors.add(networkInterfaceMetricsCollector);
        }


        TCPMetricsCollector tcpMetricsCollector = new TCPMetricsCollector(
                httpClient, httpContext, f5, metricsFilter, monitor, metricPrefix);
        metricCollectors.add(tcpMetricsCollector);

        HttpMetricsCollector httpMetricsCollector = new HttpMetricsCollector(
                httpClient, httpContext, f5, metricsFilter, monitor, metricPrefix);
        metricCollectors.add(httpMetricsCollector);

        HttpCompressionMetricsCollector httpCompressionMetricsCollector = new HttpCompressionMetricsCollector(
                httpClient, httpContext, f5, metricsFilter, monitor, metricPrefix);
        metricCollectors.add(httpCompressionMetricsCollector);

        /*SystemMetricsCollector systemMetricsCollector = new SystemMetricsCollector(f5, monitor, metricPrefix);
        metricCollectors.add(systemMetricsCollector);*/

        DiskMetricsCollector diskMetricsCollector = new DiskMetricsCollector(httpClient, httpContext, f5, monitor, metricPrefix);
        metricCollectors.add(diskMetricsCollector);

        CPUMemoryMetricsCollector cpuMemoryMetricsCollector = new CPUMemoryMetricsCollector(httpClient, httpContext, f5, monitor, metricPrefix);
        metricCollectors.add(cpuMemoryMetricsCollector);

        return metricCollectors;
    }

    private void runConcurrentTasks(List<Callable<Void>> metricCollectors) {

        List<Future<Void>> futures = new ArrayList<Future<Void>>();

        for (Callable<Void> metricCollector : metricCollectors) {
            futures.add(threadPool.submit(metricCollector));
        }

        for (Future<Void> future : futures) {
            try {
                future.get(f5ThreadTimeout, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.error("Task interrupted. ", e);
            } catch (ExecutionException e) {
                LOGGER.error("Task execution failed. ", e);
            } catch (TimeoutException e) {
                LOGGER.error("Task timed out. ", e);
            }
        }
    }
}