# AppDynamics F5 Load Balancer Monitoring Extension


##Use Case

The F5 load balancer from F5 Networks, Inc. directs traffic away from servers that are overloaded or down to other servers that can handle the load.
 
The F5 load balancer extension collects key performance metrics from an F5 load balancer and presents them in the AppDynamics Metric Browser.

This extension works only with the standalone machine agent.

**Note : By default, the Machine agent and AppServer agent can only send a fixed number of metrics to the controller. This extension potentially reports thousands of metrics, so to change this limit, please follow the instructions mentioned [here](https://docs.appdynamics.com/display/PRO40/Metrics+Limits).** 

##Installation

1. Run 'mvn clean install' from f5-monitoring-extension directory
2. Copy and unzip F5Monitor-\<version\>.zip from 'target' directory into \<machine_agent_dir\>/monitors/
3. Edit config.yaml file in F5Monitor/conf and provide the required configuration (see Configuration section)
4. Restart the Machine Agent.

##Configuration

###config.yaml

**Note: Please avoid using tab (\t) when editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/).**

| Param | Description | Example |
| ----- | ----- | ----- |
| name | The display name used in metric browser. **Note, you must provide a unique name for each F5 to monitor, otherwise it may result in unpredictable metrics. If left null, specified hostname is used instead.** | "MyTestF5" |
| hostname | The hostname or IP address of F5 load balancer | "myf5host" or "1.1.1.2" |
| username | The username to log in to F5 load balancer | "admin" |
| password | The plain text password to log in to F5 load balancer. | "adminPassword" |
| passwordEncrypted | The encrypted password to log in to F5 load balancer (see Password Encryption section). Use this if you do not want to specify the plain text password above. | "Qc6JInVN2wNqE48TmxJepg==" |
| encryptionKey | The key used to generate the encrypted password (see Password Encryption section) | "testKey" |
| poolIncludes | The list of pools to monitor, separated by comma. To monitor a specific pool, you must specify it in the format **/\<partition name\>/\<pool name\>** or use regular expression. By default, value is null for disabled monitoring. | **Specific pools:** "/Common/devcontr7", "/Common/devcontr8" <br/><br/> **Regular Expressions:** "/Common/devcontr.\*", ".\*" |
| poolMemberIncludes | The list of pool members to monitor, separated by comma. Members will only be monitored if pool is also monitored. To monitor a specific pool member, you must specify it in the format **/\<partition name\>/\<pool name\>/\<node name\>/\<port\>** or use regular expression. By default, value is null for disabled monitoring. | **Specific pool members:** "/Common/devcontr7/node1/8080", "/Common/devcontr7/node2/8082" <br/><br/> **Regular Expressions:** "/Common/devcontr.\*", ".\*" |
| snatPoolIncludes | The list of SNAT pools to monitor, separated by comma. To monitor a specific pool, you must specify it in the format **/\<partition name\>/\<snat pool name\>** or use regular expression. By default, value is null for disabled monitoring. | **Specific SNAT pools:** "/Common/snatpool1", "/Common/snatpool2" <br/><br/> **Regular Expressions:** "/Common/snatpool.\*", ".\*" |
| snatPoolMemberIncludes | The list of SNAT pool members to monitor, separated by comma. Members will only be monitored if pool is also monitored. To monitor a specific pool member, you must specify it in the format **/\<partition name\>/\<snat pool name\>/\<member name>** or use regular expression. By default, value is null for disabled monitoring. | **Specific SNAT pool members:** "/Common/snatpool1/member1", "/Common/snatpool1/member2" <br/><br/> **Regular Expressions:** "/Common/snatpool1/.\*", ".\*" |
| virtualServerIncludes | The list of virtual servers to monitor, separated by comma. To monitor a specific virtual server, you must specify it in the format **/\<partition name\>/\<virtual server name\>** or use regular expression. By default, value is null for disabled monitoring. | **Specific virtual servers:** "/Common/devcontr7_VIP", "/Common/devcontr7_VIP_SSL" <br/><br/> **Regular Expressions:** "/Common/devcontr.\*", ".\*" |
| iRuleIncludes | The list of iRules to monitor, separated by comma. To monitor a specific iRule, you must specify it in the format **/\<partition name\>/\<iRule name\>** or use regular expression. By default, value is null for disabled monitoring. | **Specific iRules:** "/Common/_sys_APM_activesync", "/Common/_sys_auth_LDAP" <br/><br/> **Regular Expressions:** "/Common/_sys.\*", ".\*" |
| clientSSLProfileIncludes | The list of client SSL Profiles to monitor, separated by comma. To monitor a specific profile, you must specify it in the format **/\<partition name\>/\<client ssl profile name\>** or use regular expression. By default, value is null for disabled monitoring. | **Specific Client SSL Profile:** "/Common/clientssl", "/Common/clientssl-insecure-compatible" <br/><br/> **Regular Expressions:** "/Common/clientssl.\*", ".\*" |
| serverSSLProfileIncludes | The list of server SSL Profiles to monitor, separated by comma. To monitor a specific profile, you must specify it in the format **/\<partition name\>/\<server ssl profile name\>** or use regular expression. By default, value is null for disabled monitoring. | **Specific Server SSL Profile:** "/Common/serverssl", "/Common/serverssl-insecure-compatible" <br/><br/> **Regular Expressions:** "/Common/serverssl.\*", ".\*" |
| networkInterfaceIncludes | The list of network interfaces to monitor, separated by comma. Regular expression is supported. By default, value is null for disabled monitoring. | **Specific Network Interfaces:** "1.1", "1.2" <br/><br/> **Regular Expressions:** ".\*" |
| numberOfF5Threads | The no of threads to process multiple F5s concurrently. **Note: You don't necessarily have to match the no of threads, to the no of F5 instances configured above, unless you have a lot of CPUs in your machine**. Default value is 3. |  |
| numberOfThreadsPerF5 | The no of threads to process metrics collectors per F5 concurrently. Default value is 3. |  |
| numberOfF5Threads | The no of threads to process multiple F5s concurrently. **Note: You don't necessarily have to match the no of threads, to the no of F5 instances configured above, unless you have a lot of CPUs in your machine**. Default value is 3. |  |
| metricPrefix | The path prefix for viewing metrics in the metric browser. Default value is "Custom Metrics\|F5 Monitor\|" |  |
| **MetricsFilter** | Filters for excluding unwanted metrics - see below. Useful to limit the no of metrics being reported to the Controller. **Note: this applies to all configured F5s**.  |  |
| poolMetricExcludes | The list of pool metrics to exclude, separated by comma. Regular expression is supported. By default, value is null for monitoring all metrics. | **Specific Metric:** "STATISTIC_TOTAL_REQUESTS" <br/><br/> **Regular Expressions:** "STATISTIC_TOTAL.\*" |
| snatPoolMetricExcludes | The list of snat pool metrics to exclude, separated by comma. Regular expression is supported. By default, value is null for monitoring all metrics. | **Specific Metric:** "STATISTIC_TOTAL_REQUESTS" <br/><br/> **Regular Expressions:** "STATISTIC_TOTAL.\*" |
| virtualServerMetricExcludes | The list of virtual server metrics to exclude, separated by comma. Regular expression is supported. By default, value is null for monitoring all metrics. | **Specific Metric:** "STATISTIC_ACL_NO_MATCH" <br/><br/> **Regular Expressions:** "STATISTIC_ACL.\*" |
| iRuleMetricExcludes | The list of iRule metrics to exclude, separated by comma. Regular expression is supported. By default, value is null for monitoring all metrics. | **Specific Metric:** "STATISTIC_RULE_ABORTS" <br/><br/> **Regular Expressions:** "STATISTIC_RULE.\*" |
| clientSSLProfileMetricExcludes | The list of client SSL profile metrics to exclude, separated by comma. Regular expression is supported. By default, value is null for monitoring all metrics. | **Specific Metric:** "STATISTIC_SSL_PROTOCOL_TLSV1" <br/><br/> **Regular Expressions:** "STATISTIC_SSL_PROTOCOL.\*" |
| serverSSLProfileMetricExcludes | The list of server SSL profile metrics to exclude, separated by comma. Regular expression is supported. By default, value is null for monitoring all metrics. | **Specific Metric:** "STATISTIC_SSL_PROTOCOL_TLSV1" <br/><br/> **Regular Expressions:** "STATISTIC_SSL_PROTOCOL.\*" |
| networkInterfaceMetricExcludes | The list of network interface metrics to exclude, separated by comma. Regular expression is supported. By default, value is null for monitoring all metrics. | **Specific Metric:** "STATISTIC_BYTES_IN" <br/><br/> **Regular Expressions:** "STATISTIC_BYTES.\*" |
| tcpMetricExcludes | The list of TCP metrics to exclude, separated by comma. Regular expression is supported. By default, value is null for monitoring all metrics. | **Specific Metric:** "STATISTIC_TCP_ABANDONED_CONNECTIONS" <br/><br/> **Regular Expressions:** "STATISTIC_TCP_ABANDONED.\*" |
| httpMetricExcludes | The list of HTTP metrics to exclude, separated by comma. Regular expression is supported. By default, value is null for monitoring all metrics. | **Specific Metric:** "STATISTIC_HTTP_2XX_RESPONSES" <br/><br/> **Regular Expressions:** "STATISTIC_HTTP_2.\*" |
| httpCompressionMetricExcludes | The list of HTTP Compression metrics to exclude, separated by comma. Regular expression is supported. By default, value is null for monitoring all metrics. | **Specific Metric:** "STATISTIC_HTTPCOMPRESSION_PRE_COMPRESSION_BYTES" <br/><br/> **Regular Expressions:** "STATISTIC_HTTPCOMPRESSION_PRE.\*" |

**Below is an example config for monitoring multiple F5 instances:**

~~~
f5s:
  - name: "ProdF5"
    hostname: "1.1.1.1"
    username: "admin"
    password: ""
    passwordEncrypted: "Qc6JInVN2wNqE48TmxJepg=="
    encryptionKey: "testKey"
    poolIncludes: ["/Common/devcontr.*" ]
    poolMemberIncludes: ["/Common/devcontr.*/.*/8084" ]
    snatPoolIncludes: [ ]
    snatPoolMemberIncludes: [".*" ]
    virtualServerIncludes: [ ]
    iRuleIncludes: [ ]
    clientSSLProfileIncludes: [".*" ]
    serverSSLProfileIncludes: [ ]
    networkInterfaceIncludes: [ ]
  - name: "StagingF5"
    hostname: "1.1.1.2"
    username: "admin"
    password: "test"
    passwordEncrypted: ""
    encryptionKey: ""
    poolIncludes: [".*" ]
    poolMemberIncludes: [".*" ]
    snatPoolIncludes: [".*"  ]
    snatPoolMemberIncludes: ["/Common/devcontr7_agent/devcontr7/8084" ]
    virtualServerIncludes: [".*"  ]
    iRuleIncludes: [".*"  ]
    clientSSLProfileIncludes: [".*" ]
    serverSSLProfileIncludes: [".*"  ]
    networkInterfaceIncludes: [".*"  ]    

metricsFilter:
    poolMetricExcludes: [ ]
    snatPoolMetricExcludes: [ ]
    virtualServerMetricExcludes: [ ]
    iRuleMetricExcludes: [ ]
    clientSSLProfileMetricExcludes: [ ]
    serverSSLProfileMetricExcludes: [ ]
    networkInterfaceMetricExcludes: [ ]
    tcpMetricExcludes: [ ]
    httpMetricExcludes: [ ]
    httpCompressionMetricExcludes: [".*" ]
   
numberOfF5Threads: 2

numberOfThreadsPerF5: 3

metricPrefix:  "Custom Metrics|F5 Monitor|"
~~~

###Password Encryption
To generate an encrypted password, follow steps below:

1. Navigate to <machine_agent_dir>/monitors/F5Monitor/lib
2. Run command:
   	<pre><code>   
   	java -cp "appd-exts-commons-1.1.2.jar" com.appdynamics.extensions.crypto.Encryptor \<enter_any_key\> \<enter_plain_text_password\>
   	
   	For example: 
   	java -cp "appd-exts-commons-1.1.2.jar" com.appdynamics.extensions.crypto.Encryptor testKey adminPassword
   	</code></pre>
   	
3. Add the key and resulting encrypted password in encryptionKey and passwordEncrypted   fields in config.yaml respectively.

###BIG-IP user account properties
To use this extension, the user account should have the property `Partition Access = All` selected.
![](https://github.com/Appdynamics/f5-monitoring-extension/raw/master/F5-access-control.png)

##Metrics
Metric path is typically: **Application Infrastructure Performance|\<Tier\>|Custom Metrics|F5 Monitor|** followed by the individual categories/metrics below:

### iRules

| Metric | Description |
| ----- | ----- |
| STATISTIC_RULE_ABORTS |  |
| STATISTIC_RULE_AVERAGE_CYCLES |  |
| STATISTIC_RULE_FAILURES |  |
| STATISTIC_RULE_MAXIMUM_CYCLES |  |
| STATISTIC_RULE_MINIMUM_CYCLES |  |
| STATISTIC_RULE_TOTAL_EXECUTIONS |  |

### Network\|HTTP

| Metric | Description |
| ----- | ----- |
| STATISTIC_HTTP_2XX_RESPONSES |  |
| STATISTIC_HTTP_3XX_RESPONSES |  |
| STATISTIC_HTTP_4XX_RESPONSES |  |
| STATISTIC_HTTP_5XX_RESPONSES |  |
| STATISTIC_HTTP_BUCKET_128K_RESPONSES |  |
| STATISTIC_HTTP_BUCKET_16K_RESPONSES |  |
| STATISTIC_HTTP_BUCKET_1K_RESPONSES |  |
| STATISTIC_HTTP_BUCKET_2M_RESPONSES |  |
| STATISTIC_HTTP_BUCKET_32K_RESPONSES |  |
| STATISTIC_HTTP_BUCKET_4K_RESPONSES |  |
| STATISTIC_HTTP_BUCKET_512K_RESPONSES |  |
| STATISTIC_HTTP_BUCKET_64K_RESPONSES |  |
| STATISTIC_HTTP_BUCKET_LARGE_RESPONSES |  |
| STATISTIC_HTTP_COOKIE_PERSIST_INSERTS |  |
| STATISTIC_HTTP_GET_REQUESTS |  |
| STATISTIC_HTTP_MAXIMUM_KEEPALIVE_REQUESTS |  |
| STATISTIC_HTTP_PASSTHROUGH_CONNECTS |  |
| STATISTIC_HTTP_PASSTHROUGH_EXCESS_CLIENT_HEADERS |  |
| STATISTIC_HTTP_PASSTHROUGH_EXCESS_SERVER_HEADERS |  |
| STATISTIC_HTTP_PASSTHROUGH_IRULES |  |
| STATISTIC_HTTP_PASSTHROUGH_OVERSIZE_CLIENT_HEADERS |  |
| STATISTIC_HTTP_PASSTHROUGH_OVERSIZE_SERVER_HEADERS |  |
| STATISTIC_HTTP_PASSTHROUGH_PIPELINES |  |
| STATISTIC_HTTP_PASSTHROUGH_UNKNOWN_METHODS |  |
| STATISTIC_HTTP_PASSTHROUGH_WEB_SOCKETS |  |
| STATISTIC_HTTP_POST_REQUESTS |  |
| STATISTIC_HTTP_TOTAL_REQUESTS |  |
| STATISTIC_HTTP_V10_REQUESTS |  |
| STATISTIC_HTTP_V10_RESPONSES |  |
| STATISTIC_HTTP_V11_REQUESTS |  |
| STATISTIC_HTTP_V11_RESPONSES |  |
| STATISTIC_HTTP_V9_REQUESTS |  |
| STATISTIC_HTTP_V9_RESPONSES |  |

### Network\|HTTP\|Compression

| Metric | Description |
| ----- | ----- |
| STATISTIC_HTTPCOMPRESSION_AUDIO_POST_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_AUDIO_PRE_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_CSS_POST_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_CSS_PRE_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_HTML_POST_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_HTML_PRE_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_IMAGE_POST_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_IMAGE_PRE_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_JS_POST_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_JS_PRE_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_NULL_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_OCTET_POST_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_OCTET_PRE_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_OTHER_POST_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_OTHER_PRE_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_PLAIN_POST_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_PLAIN_PRE_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_POST_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_PRE_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_SGML_POST_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_SGML_PRE_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_VIDEO_POST_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_VIDEO_PRE_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_XML_POST_COMPRESSION_BYTES |  |
| STATISTIC_HTTPCOMPRESSION_XML_PRE_COMPRESSION_BYTES |  |

### Network\|Interfaces

| Metric | Description |
| ----- | ----- |
| STATISTIC_BYTES_IN |  |
| STATISTIC_BYTES_OUT |  |
| STATISTIC_COLLISIONS |  |
| STATISTIC_DROPPED_PACKETS_IN |  |
| STATISTIC_DROPPED_PACKETS_OUT |  |
| STATISTIC_ERRORS_IN |  |
| STATISTIC_ERRORS_OUT |  |
| STATISTIC_MULTICASTS_IN |  |
| STATISTIC_MULTICASTS_OUT |  |
| STATISTIC_PACKETS_IN |  |
| STATISTIC_PACKETS_OUT |  |
| STATUS |  |

### Network\|TCP

| Metric | Description |
| ----- | ----- |
| STATISTIC_TCP_ABANDONED_CONNECTIONS |  |
| STATISTIC_TCP_ACCEPT_FAILURES |  |
| STATISTIC_TCP_ACCEPTED_CONNECTIONS |  |
| STATISTIC_TCP_CLOSE_WAIT_CONNECTIONS |  |
| STATISTIC_TCP_CONNECTION_FAILURES |  |
| STATISTIC_TCP_ESTABLISHED_CONNECTIONS |  |
| STATISTIC_TCP_EXPIRED_CONNECTIONS |  |
| STATISTIC_TCP_FIN_WAIT_CONNECTIONS |  |
| STATISTIC_TCP_OPEN_CONNECTIONS |  |
| STATISTIC_TCP_RECEIVED_BAD_CHECKSUMS |  |
| STATISTIC_TCP_RECEIVED_BAD_SEGMENTS |  |
| STATISTIC_TCP_RECEIVED_BAD_SYN_COOKIES |  |
| STATISTIC_TCP_RECEIVED_OUT_OF_ORDER_SEGMENTS |  |
| STATISTIC_TCP_RECEIVED_RESETS |  |
| STATISTIC_TCP_RECEIVED_SYN_COOKIES |  |
| STATISTIC_TCP_RETRANSMITTED_SEGMENTS |  |
| STATISTIC_TCP_SYN_CACHE_OVERFLOWS |  |
| STATISTIC_TCP_TIME_WAIT_CONNECTIONS |  |

### Pools

| Metric | Description |
| ----- | ----- |
| STATISTIC_CONNQUEUE_AGE_EXPONENTIAL_DECAY_MAX |  |
| STATISTIC_CONNQUEUE_AGE_MAX |  |
| STATISTIC_CONNQUEUE_AGE_MOVING_AVG |  |
| STATISTIC_CONNQUEUE_AGE_OLDEST_ENTRY |  |
| STATISTIC_CONNQUEUE_AGGR_AGE_EXPONENTIAL_DECAY_MAX |  |
| STATISTIC_CONNQUEUE_AGGR_AGE_MAX |  |
| STATISTIC_CONNQUEUE_AGGR_AGE_MOVING_AVG |  |
| STATISTIC_CONNQUEUE_AGGR_AGE_OLDEST_ENTRY |  |
| STATISTIC_CONNQUEUE_AGGR_CONNECTIONS |  |
| STATISTIC_CONNQUEUE_AGGR_SERVICED |  |
| STATISTIC_CONNQUEUE_CONNECTIONS |  |
| STATISTIC_CONNQUEUE_SERVICED |  |
| STATISTIC_CURRENT_PVA_ASSISTED_CONNECTIONS |  |
| STATISTIC_CURRENT_SESSIONS |  |
| STATISTIC_PVA_SERVER_SIDE_BYTES_IN |  |
| STATISTIC_PVA_SERVER_SIDE_BYTES_OUT |  |
| STATISTIC_PVA_SERVER_SIDE_CURRENT_CONNECTIONS |  |
| STATISTIC_PVA_SERVER_SIDE_MAXIMUM_CONNECTIONS |  |
| STATISTIC_PVA_SERVER_SIDE_PACKETS_IN |  |
| STATISTIC_PVA_SERVER_SIDE_PACKETS_OUT |  |
| STATISTIC_PVA_SERVER_SIDE_TOTAL_CONNECTIONS |  |
| STATISTIC_SERVER_SIDE_BYTES_IN |  |
| STATISTIC_SERVER_SIDE_BYTES_OUT |  |
| STATISTIC_SERVER_SIDE_CURRENT_CONNECTIONS |  |
| STATISTIC_SERVER_SIDE_MAXIMUM_CONNECTIONS |  |
| STATISTIC_SERVER_SIDE_PACKETS_IN |  |
| STATISTIC_SERVER_SIDE_PACKETS_OUT |  |
| STATISTIC_SERVER_SIDE_TOTAL_CONNECTIONS |  |
| STATISTIC_TOTAL_PVA_ASSISTED_CONNECTIONS |  |
| STATISTIC_TOTAL_REQUESTS |  |
| STATUS |  |

### SSL|Clients or Servers

| Metric | Description |
| ----- | ----- |
| STATISTIC_SSL_CIPHER_ADH_KEY_EXCHANGE |  |
| STATISTIC_SSL_CIPHER_AES_BULK |  |
| STATISTIC_SSL_CIPHER_AES_GCM_BULK |  |
| STATISTIC_SSL_CIPHER_DES_BULK |  |
| STATISTIC_SSL_CIPHER_DH_RSA_KEY_EXCHANGE |  |
| STATISTIC_SSL_CIPHER_DHE_DSS_KEY_EXCHANGE |  |
| STATISTIC_SSL_CIPHER_ECDH_ECDSA_KEY_EXCHANGE |  |
| STATISTIC_SSL_CIPHER_ECDH_RSA_KEY_EXCHANGE |  |
| STATISTIC_SSL_CIPHER_ECDHE_ECDSA_KEY_EXCHANGE |  |
| STATISTIC_SSL_CIPHER_ECDHE_RSA_KEY_EXCHANGE |  |
| STATISTIC_SSL_CIPHER_EDH_RSA_KEY_EXCHANGE |  |
| STATISTIC_SSL_CIPHER_IDEA_BULK |  |
| STATISTIC_SSL_CIPHER_MD5_DIGEST |  |
| STATISTIC_SSL_CIPHER_NULL_BULK |  |
| STATISTIC_SSL_CIPHER_NULL_DIGEST |  |
| STATISTIC_SSL_CIPHER_RC2_BULK |  |
| STATISTIC_SSL_CIPHER_RC4_BULK |  |
| STATISTIC_SSL_CIPHER_RSA_KEY_EXCHANGE |  |
| STATISTIC_SSL_CIPHER_SHA_DIGEST |  |
| STATISTIC_SSL_COMMON_BAD_RECORDS |  |
| STATISTIC_SSL_COMMON_CURRENT_COMPATIBLE_MODE_CONNECTIONS |  |
| STATISTIC_SSL_COMMON_CURRENT_CONNECTIONS |  |
| STATISTIC_SSL_COMMON_CURRENT_NATIVE_CONNECTIONS |  |
| STATISTIC_SSL_COMMON_DECRYPTED_BYTES_IN |  |
| STATISTIC_SSL_COMMON_DECRYPTED_BYTES_OUT |  |
| STATISTIC_SSL_COMMON_ENCRYPTED_BYTES_IN |  |
| STATISTIC_SSL_COMMON_ENCRYPTED_BYTES_OUT |  |
| STATISTIC_SSL_COMMON_FATAL_ALERTS |  |
| STATISTIC_SSL_COMMON_FULLY_HW_ACCELERATED_CONNECTIONS |  |
| STATISTIC_SSL_COMMON_FWDP_CONNS |  |
| STATISTIC_SSL_COMMON_FWDP_DESTINATION_IP_BYPASSES |  |
| STATISTIC_SSL_COMMON_FWDP_HOSTNAME_BYPASSES |  |
| STATISTIC_SSL_COMMON_FWDP_SOURCE_IP_BYPASSES |  |
| STATISTIC_SSL_COMMON_HANDSHAKE_FAILURES |  |
| STATISTIC_SSL_COMMON_INSECURE_HANDSHAKE_ACCEPTS |  |
| STATISTIC_SSL_COMMON_INSECURE_HANDSHAKE_REJECTS |  |
| STATISTIC_SSL_COMMON_INSECURE_RENEGOTIATION_REJECTS |  |
| STATISTIC_SSL_COMMON_INVALID_PEER_CERTIFICATES |  |
| STATISTIC_SSL_COMMON_MAXIMUM_COMPATIBLE_MODE_CONNECTIONS |  |
| STATISTIC_SSL_COMMON_MAXIMUM_CONNECTIONS |  |
| STATISTIC_SSL_COMMON_MAXIMUM_NATIVE_CONNECTIONS |  |
| STATISTIC_SSL_COMMON_MIDSTREAM_RENEGOTIATIONS |  |
| STATISTIC_SSL_COMMON_NO_PEER_CERTIFICATES |  |
| STATISTIC_SSL_COMMON_NON_HW_ACCELERATED_CONNECTIONS |  |
| STATISTIC_SSL_COMMON_NOT_SSL_HANDSHAKE_FAILURES |  |
| STATISTIC_SSL_COMMON_PARTIALLY_HW_ACCELERATED_CONNECTIONS |  |
| STATISTIC_SSL_COMMON_PREMATURE_DISCONNECTS |  |
| STATISTIC_SSL_COMMON_RECORDS_IN |  |
| STATISTIC_SSL_COMMON_RECORDS_OUT |  |
| STATISTIC_SSL_COMMON_SECURE_HANDSHAKES |  |
| STATISTIC_SSL_COMMON_SESSION_CACHE_CURRENT_ENTRIES |  |
| STATISTIC_SSL_COMMON_SESSION_CACHE_HITS |  |
| STATISTIC_SSL_COMMON_SESSION_CACHE_INVALIDATIONS |  |
| STATISTIC_SSL_COMMON_SESSION_CACHE_LOOKUPS |  |
| STATISTIC_SSL_COMMON_SESSION_CACHE_OVERFLOWS |  |
| STATISTIC_SSL_COMMON_SNI_REJECTS |  |
| STATISTIC_SSL_COMMON_TOTAL_COMPATIBLE_MODE_CONNECTIONS |  |
| STATISTIC_SSL_COMMON_TOTAL_NATIVE_CONNECTIONS |  |
| STATISTIC_SSL_COMMON_VALID_PEER_CERTIFICATES |  |
| STATISTIC_SSL_PROTOCOL_DTLSV1 |  |
| STATISTIC_SSL_PROTOCOL_SSLV2 |  |
| STATISTIC_SSL_PROTOCOL_SSLV3 |  |
| STATISTIC_SSL_PROTOCOL_TLSV1 |  |
| STATISTIC_SSL_PROTOCOL_TLSV1_1 |  |
| STATISTIC_SSL_PROTOCOL_TLSV1_2 |  |
| STATISTIC_SSL_SESSTICK_REUSE_FAILED |  |
| STATISTIC_SSL_SESSTICK_REUSED |  |

### System

| Metric | Description |
| ----- | ----- |
| Uptime (sec) |  |

### System|CPU

| Metric | Description |
| ----- | ----- |
| CPU % BUSY |  |

### System|Disks

| Metric | Description |
| ----- | ----- |
| Space Available |  |
| Space Used |  |

### Virtual Servers

| Metric | Description |
| ----- | ----- |
| STATISTIC_ACL_NO_MATCH |  |
| STATISTIC_CLIENT_SIDE_BYTES_IN |  |
| STATISTIC_CLIENT_SIDE_BYTES_OUT |  |
| STATISTIC_CLIENT_SIDE_PACKETS_IN |  |
| STATISTIC_CLIENT_SIDE_PACKETS_OUT |  |
| STATISTIC_CLIENT_SIDE_CURRENT_CONNECTIONS |  |
| STATISTIC_CLIENT_SIDE_MAXIMUM_CONNECTIONS |  |
| STATISTIC_CLIENT_SIDE_TOTAL_CONNECTIONS |  |
| STATISTIC_CURRENT_PVA_ASSISTED_CONNECTIONS |  |
| STATISTIC_EPHEMERAL_BYTES_IN |  |
| STATISTIC_EPHEMERAL_BYTES_OUT |  |
| STATISTIC_EPHEMERAL_PACKETS_IN |  |
| STATISTIC_EPHEMERAL_PACKETS_OUT |  |
| STATISTIC_EPHEMERAL_CURRENT_CONNECTIONS |  |
| STATISTIC_EPHEMERAL_MAXIMUM_CONNECTIONS |  |
| STATISTIC_EPHEMERAL_TOTAL_CONNECTIONS |  |
| STATISTIC_MINIMUM_CONNECTION_DURATION |  |
| STATISTIC_MEAN_CONNECTION_DURATION |  |
| STATISTIC_MAXIMUM_CONNECTION_DURATION |  |
| STATISTIC_NO_NODE_ERRORS |  |
| STATISTIC_PVA_CLIENT_SIDE_BYTES_IN |  |
| STATISTIC_PVA_CLIENT_SIDE_BYTES_OUT |  |
| STATISTIC_PVA_CLIENT_SIDE_PACKETS_IN |  |
| STATISTIC_PVA_CLIENT_SIDE_PACKETS_OUT |  |
| STATISTIC_PVA_CLIENT_SIDE_CURRENT_CONNECTIONS |  |
| STATISTIC_PVA_CLIENT_SIDE_MAXIMUM_CONNECTIONS |  |
| STATISTIC_PVA_CLIENT_SIDE_TOTAL_CONNECTIONS |  |
| STATISTIC_TOTAL_REQUESTS |  |
| STATISTIC_TOTAL_PVA_ASSISTED_CONNECTIONS |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_HW_INSTANCES |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_SW_INSTANCES |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_CACHE_USAGE |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_CACHE_OVERFLOWS |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_SW_TOTAL |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_SW_ACCEPTS |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_SW_REJECTS |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_HW_TOTAL |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_HW_ACCEPTS |  |
| STATISTIC_VIRTUAL_SERVER_TOTAL_CPU_CYCLES |  |
| STATISTIC_VIRTUAL_SERVER_FIVE_SEC_AVG_CPU_USAGE |  |
| STATISTIC_VIRTUAL_SERVER_ONE_MIN_AVG_CPU_USAGE |  |
| STATISTIC_VIRTUAL_SERVER_FIVE_MIN_AVG_CPU_USAGE |  |

##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/f5-monitoring-extension).

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/eXchange/F5-Monitoring-Extension/idi-p/2063) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).

