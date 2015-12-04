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
| f5ThreadTimeout | Timeoue for each F5 thread in seconds. Default value is 30. |  |
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

f5ThreadTimeout: 30

metricPrefix:  "Custom Metrics|F5 Monitor|"
~~~

###Password Encryption
To generate an encrypted password, follow steps below:

1. Navigate to <machine_agent_dir>/monitors/F5Monitor/lib
2. Run command:
   	<pre><code>   
   	java -cp "appd-exts-commons-1.1.2.jar" com.appdynamics.extensions.crypto.Encryptor enter_any_encryption_key enter_plain_text_password
   	
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
| STATISTIC_RULE_ABORTS | Number of rule aborts due to TCL errors. |
| STATISTIC_RULE_AVERAGE_CYCLES | The average number of cycles spent during an execution of this rule event. |
| STATISTIC_RULE_FAILURES | Number of rule failures. |
| STATISTIC_RULE_MAXIMUM_CYCLES | The maximum number of cycles spent during an execution of this rule event. |
| STATISTIC_RULE_MINIMUM_CYCLES | The minimum number of cycles spent during an execution of this rule event. |
| STATISTIC_RULE_TOTAL_EXECUTIONS | Number of rule event executions. |

### Network\|HTTP

| Metric | Description |
| ----- | ----- |
| STATISTIC_HTTP_2XX_RESPONSES | Number of server-side responses in range of 200 to 206 (successful responses) |
| STATISTIC_HTTP_3XX_RESPONSES | Number of server-side responses in range of 300 to 307 (redirection resposes) |
| STATISTIC_HTTP_4XX_RESPONSES | Number of server-side responses in range of 400 to 417 (client errors) |
| STATISTIC_HTTP_5XX_RESPONSES | Number of server-side responses in range of 500 to 505 (server errors) |
| STATISTIC_HTTP_BUCKET_128K_RESPONSES | Number of responses > 64k, up to 128k |
| STATISTIC_HTTP_BUCKET_16K_RESPONSES | Number of responses from 4 - 16k |
| STATISTIC_HTTP_BUCKET_1K_RESPONSES | Number of responses under 1k |
| STATISTIC_HTTP_BUCKET_2M_RESPONSES | Number of responses > 512k, up to 2m |
| STATISTIC_HTTP_BUCKET_32K_RESPONSES | Number of responses from 16 - 32k |
| STATISTIC_HTTP_BUCKET_4K_RESPONSES | Number of responses from 1 - 4k |
| STATISTIC_HTTP_BUCKET_512K_RESPONSES | Number of responses > 128k, up to 512k |
| STATISTIC_HTTP_BUCKET_64K_RESPONSES | Number of responses from 32 - 64k (prior to 10.1.0 was >32k) |
| STATISTIC_HTTP_BUCKET_LARGE_RESPONSES | Number of responses > 2m |
| STATISTIC_HTTP_COOKIE_PERSIST_INSERTS | Number of Set-Cookie header insertions |
| STATISTIC_HTTP_GET_REQUESTS | Total number of GET requests |
| STATISTIC_HTTP_MAXIMUM_KEEPALIVE_REQUESTS | Maximum number of requests made in a connection |
| STATISTIC_HTTP_PASSTHROUGH_CONNECTS |  |
| STATISTIC_HTTP_PASSTHROUGH_EXCESS_CLIENT_HEADERS |  |
| STATISTIC_HTTP_PASSTHROUGH_EXCESS_SERVER_HEADERS |  |
| STATISTIC_HTTP_PASSTHROUGH_IRULES |  |
| STATISTIC_HTTP_PASSTHROUGH_OVERSIZE_CLIENT_HEADERS |  |
| STATISTIC_HTTP_PASSTHROUGH_OVERSIZE_SERVER_HEADERS |  |
| STATISTIC_HTTP_PASSTHROUGH_PIPELINES |  |
| STATISTIC_HTTP_PASSTHROUGH_UNKNOWN_METHODS |  |
| STATISTIC_HTTP_PASSTHROUGH_WEB_SOCKETS |  |
| STATISTIC_HTTP_POST_REQUESTS | Total number of POST requests |
| STATISTIC_HTTP_TOTAL_REQUESTS | Total number of HTTP requests |
| STATISTIC_HTTP_V10_REQUESTS | Total number of version 10 requests |
| STATISTIC_HTTP_V10_RESPONSES | Total number of version 10 responses |
| STATISTIC_HTTP_V11_REQUESTS | Total number of version 11 requests |
| STATISTIC_HTTP_V11_RESPONSES | Total number of version 11 responses |
| STATISTIC_HTTP_V9_REQUESTS | Total number of version 9 requests |
| STATISTIC_HTTP_V9_RESPONSES | Total number of version 9 responses |

### Network\|HTTP\|Compression

| Metric | Description |
| ----- | ----- |
| STATISTIC_HTTPCOMPRESSION_AUDIO_POST_COMPRESSION_BYTES | Number of response bytes after compression has taken place for MIME type Audio. |
| STATISTIC_HTTPCOMPRESSION_AUDIO_PRE_COMPRESSION_BYTES | Number of response bytes before compression has taken place for MIME type Audio. |
| STATISTIC_HTTPCOMPRESSION_CSS_POST_COMPRESSION_BYTES | Number of response bytes after compression has taken place for MIME type CSS. |
| STATISTIC_HTTPCOMPRESSION_CSS_PRE_COMPRESSION_BYTES | Number of response bytes before compression has taken place for MIME type CSS. |
| STATISTIC_HTTPCOMPRESSION_HTML_POST_COMPRESSION_BYTES | Number of response bytes after compression has taken place for MIME type HTML. |
| STATISTIC_HTTPCOMPRESSION_HTML_PRE_COMPRESSION_BYTES | Number of response bytes before compression has taken place for MIME type HTML. |
| STATISTIC_HTTPCOMPRESSION_IMAGE_POST_COMPRESSION_BYTES | Number of response bytes after compression has taken place for MIME type Image. |
| STATISTIC_HTTPCOMPRESSION_IMAGE_PRE_COMPRESSION_BYTES | Number of response bytes before compression has taken place for MIME type Image. |
| STATISTIC_HTTPCOMPRESSION_JS_POST_COMPRESSION_BYTES | Number of response bytes after compression has taken place for MIME type Javascript. |
| STATISTIC_HTTPCOMPRESSION_JS_PRE_COMPRESSION_BYTES | Number of response bytes before compression has taken place for MIME type Javascript. |
| STATISTIC_HTTPCOMPRESSION_NULL_COMPRESSION_BYTES | Number of bytes subjected to NULL compression (for license enforcement). |
| STATISTIC_HTTPCOMPRESSION_OCTET_POST_COMPRESSION_BYTES | Number of response bytes after compression has taken place for MIME type Octet. |
| STATISTIC_HTTPCOMPRESSION_OCTET_PRE_COMPRESSION_BYTES | Number of response bytes before compression has taken place for MIME type Octet. |
| STATISTIC_HTTPCOMPRESSION_OTHER_POST_COMPRESSION_BYTES | Number of response bytes after compression has taken place for all other MIME types. |
| STATISTIC_HTTPCOMPRESSION_OTHER_PRE_COMPRESSION_BYTES | Number of response bytes before compression has taken place for all other MIME types. |
| STATISTIC_HTTPCOMPRESSION_PLAIN_POST_COMPRESSION_BYTES | Number of response bytes after compression has taken place for MIME type Plain text. |
| STATISTIC_HTTPCOMPRESSION_PLAIN_PRE_COMPRESSION_BYTES | Number of response bytes before compression has taken place for MIME type Plain text. |
| STATISTIC_HTTPCOMPRESSION_POST_COMPRESSION_BYTES | To number of response bytes after compression has taken place |
| STATISTIC_HTTPCOMPRESSION_PRE_COMPRESSION_BYTES | Total number of response bytes before compression has taken place |
| STATISTIC_HTTPCOMPRESSION_SGML_POST_COMPRESSION_BYTES | Number of response bytes after compression has taken place for MIME type SGML. |
| STATISTIC_HTTPCOMPRESSION_SGML_PRE_COMPRESSION_BYTES | Number of response bytes before compression has taken place for MIME type SGML. |
| STATISTIC_HTTPCOMPRESSION_VIDEO_POST_COMPRESSION_BYTES | Number of response bytes after compression has taken place for MIME type Video. |
| STATISTIC_HTTPCOMPRESSION_VIDEO_PRE_COMPRESSION_BYTES | Number of response bytes before compression has taken place for MIME type Video. |
| STATISTIC_HTTPCOMPRESSION_XML_POST_COMPRESSION_BYTES | Number of response bytes after compression has taken place for MIME type XML. |
| STATISTIC_HTTPCOMPRESSION_XML_PRE_COMPRESSION_BYTES | Number of response bytes before compression has taken place for MIME type XML. |

### Network\|Interfaces

| Metric | Description |
| ----- | ----- |
| STATISTIC_BYTES_IN | Total number of bytes in. |
| STATISTIC_BYTES_OUT | Total number of bytes out. |
| STATISTIC_COLLISIONS | Total number of collisions. |
| STATISTIC_DROPPED_PACKETS_IN | Total number of dropped packets on ingress. |
| STATISTIC_DROPPED_PACKETS_OUT | Total number of dropped packets on egress. |
| STATISTIC_ERRORS_IN | Total number of errors on ingress. |
| STATISTIC_ERRORS_OUT | Total number of errors on egress. |
| STATISTIC_MULTICASTS_IN | Total number of multicast packets in. |
| STATISTIC_MULTICASTS_OUT | Total number of multicast packets out. |
| STATISTIC_PACKETS_IN | Total number of packets in. |
| STATISTIC_PACKETS_OUT | Total number of packets out. |
| STATUS | Interface status. Possible values are:<br/>0 - UNKNOWN <br/>1 - MEDIA_STATUS_UP <br/>2 - MEDIA_STATUS_DOWN <br/>3 - MEDIA_STATUS_DISABLED <br/>4 - MEDIA_STATUS_UNINITIALIZED <br/>5 - MEDIA_STATUS_LOOPBACK <br/>6 - MEDIA_STATUS_UNPOPULATED|

### Network\|TCP

| Metric | Description |
| ----- | ----- |
| STATISTIC_TCP_ABANDONED_CONNECTIONS | Abandoned connections due to retries/keep-alives. |
| STATISTIC_TCP_ACCEPT_FAILURES | Number of accept failures. |
| STATISTIC_TCP_ACCEPTED_CONNECTIONS | Current connections that have been accepted. |
| STATISTIC_TCP_CLOSE_WAIT_CONNECTIONS | Current connections in CLOSE-WAIT/LAST-ACK state. |
| STATISTIC_TCP_CONNECTION_FAILURES | Number of connection failures. |
| STATISTIC_TCP_ESTABLISHED_CONNECTIONS | Current connections that have been established, but not accepted. |
| STATISTIC_TCP_EXPIRED_CONNECTIONS | Expired connections due to idle timeout. |
| STATISTIC_TCP_FIN_WAIT_CONNECTIONS | Current connections in FIN-WAIT/CLOSING state. |
| STATISTIC_TCP_OPEN_CONNECTIONS | Current open connections. |
| STATISTIC_TCP_RECEIVED_BAD_CHECKSUMS | Number of bad checksum packets received. |
| STATISTIC_TCP_RECEIVED_BAD_SEGMENTS | Number of malformed segments received. |
| STATISTIC_TCP_RECEIVED_BAD_SYN_COOKIES | Number of bad SYN cookies received. |
| STATISTIC_TCP_RECEIVED_OUT_OF_ORDER_SEGMENTS | Number of out-of-order segments received. |
| STATISTIC_TCP_RECEIVED_RESETS | Number of RSTs received. |
| STATISTIC_TCP_RECEIVED_SYN_COOKIES | Number of SYN cookies received. |
| STATISTIC_TCP_RETRANSMITTED_SEGMENTS | Number of retransmitted segments. |
| STATISTIC_TCP_SYN_CACHE_OVERFLOWS | Number of SYN cache overflows. |
| STATISTIC_TCP_TIME_WAIT_CONNECTIONS | Current connections in TIME-WAIT state. |

### Pools

| Metric | Description |
| ----- | ----- |
| STATISTIC_CONNQUEUE_AGE_EXPONENTIAL_DECAY_MAX | Exponential decaying maximum queue entry age milliseconds. |
| STATISTIC_CONNQUEUE_AGE_MAX | Maximum queue entry age milliseconds. |
| STATISTIC_CONNQUEUE_AGE_MOVING_AVG | Exponential moving average queue entry age milliseconds. |
| STATISTIC_CONNQUEUE_AGE_OLDEST_ENTRY | Age of the oldest queue entry milliseconds. |
| STATISTIC_CONNQUEUE_AGGR_AGE_EXPONENTIAL_DECAY_MAX | Same as STATISTIC_CONNQUEUE_AGE_EXPONENTIAL_DECAY_MAX but aggregate for a pool and its members. |
| STATISTIC_CONNQUEUE_AGGR_AGE_MAX | Same as STATISTIC_CONNQUEUE_AGE_MAX but aggregate for a pool and its members. |
| STATISTIC_CONNQUEUE_AGGR_AGE_MOVING_AVG | Same as STATISTIC_CONNQUEUE_AGE_MOVING_AVG but aggregate for a pool and its members. |
| STATISTIC_CONNQUEUE_AGGR_AGE_OLDEST_ENTRY | Same as STATISTIC_CONNQUEUE_AGE_OLDEST_ENTRY but aggregate for a pool and its members. |
| STATISTIC_CONNQUEUE_AGGR_CONNECTIONS | Same as STATISTIC_CONNQUEUE_CONNECTIONS but aggregate for a pool and its members. |
| STATISTIC_CONNQUEUE_AGGR_SERVICED | Same as STATISTIC_CONNQUEUE_SERVICED but aggregate for a pool and its members. |
| STATISTIC_CONNQUEUE_CONNECTIONS | Number of connections currently in queue. |
| STATISTIC_CONNQUEUE_SERVICED | Number of entries that have been removed from the queue (both handed to a pool member and timed out). |
| STATISTIC_CURRENT_PVA_ASSISTED_CONNECTIONS | Current number of connections assisted by PVA. |
| STATISTIC_CURRENT_SESSIONS |  |
| STATISTIC_PVA_SERVER_SIDE_BYTES_IN | Total number of bytes in that are handled by PVA from the server-side of the object. |
| STATISTIC_PVA_SERVER_SIDE_BYTES_OUT | Total number of bytes out that are handled by PVA from the server-side of the object. |
| STATISTIC_PVA_SERVER_SIDE_CURRENT_CONNECTIONS | Current number of connections that are handled by PVA from the server-side of the object. |
| STATISTIC_PVA_SERVER_SIDE_MAXIMUM_CONNECTIONS | Maximum number of connections that are handled by PVA from the server-side of the object. |
| STATISTIC_PVA_SERVER_SIDE_PACKETS_IN | Total number of packets in that are handled by PVA from the server-side of the object. |
| STATISTIC_PVA_SERVER_SIDE_PACKETS_OUT | Total number of packets out that are handled by PVA from the server-side of the object. |
| STATISTIC_PVA_SERVER_SIDE_TOTAL_CONNECTIONS | Total number of connections that are handled by PVA from the server-side of the object. |
| STATISTIC_SERVER_SIDE_BYTES_IN | Total number of bytes in from the server-side of the object. |
| STATISTIC_SERVER_SIDE_BYTES_OUT | Total number of bytes out from the server-side of the object. |
| STATISTIC_SERVER_SIDE_CURRENT_CONNECTIONS | Current number of connections from the server-side of the object. |
| STATISTIC_SERVER_SIDE_MAXIMUM_CONNECTIONS | Maximum number of connections from the server-side of the object. |
| STATISTIC_SERVER_SIDE_PACKETS_IN | Total number of packets in from the server-side of the object. |
| STATISTIC_SERVER_SIDE_PACKETS_OUT | Total number of packets out from the server-side of the object. |
| STATISTIC_SERVER_SIDE_TOTAL_CONNECTIONS | Total number of connections from the server-side of the object. |
| STATISTIC_TOTAL_PVA_ASSISTED_CONNECTIONS | Total number of connections assisted by PVA. |
| STATISTIC_TOTAL_REQUESTS | Total number of requests. |
| STATUS | *Current status. Possible values are:<br/>0 - Unknown <br/>1 - Available and enabled <br/>2 - Offline and enabled <br/>3 - Available but disabled <br/>4 - Offline and disabled |
| Total No of Members | Total number of pool members. |
| Total No of Available Members | Total number of available and enabled members. |
| Total No of Unavailable Members | Total number of non-available members. |

\* **Used to be called STATUS LIGHT in v1.x where values were in reverse order, i.e. 5 - Unknown, 4 - Available/Enable, 3 - Offline/Enable, 2 - Available/Disalbed, 1 - Offline/Disabled. This was changed to align with other STATUS metrics.** 

### SSL|Clients or Servers

| Metric | Description |
| ----- | ----- |
| STATISTIC_SSL_CIPHER_ADH_KEY_EXCHANGE | Anonymous Diffie-Hellman. |
| STATISTIC_SSL_CIPHER_AES_BULK | Advances Encryption Standard (CBC). |
| STATISTIC_SSL_CIPHER_AES_GCM_BULK |  |
| STATISTIC_SSL_CIPHER_DES_BULK | Digital Encryption Standard (CBC). |
| STATISTIC_SSL_CIPHER_DH_RSA_KEY_EXCHANGE | Diffie-Hellman w/ RSA certificate. |
| STATISTIC_SSL_CIPHER_DHE_DSS_KEY_EXCHANGE |  |
| STATISTIC_SSL_CIPHER_ECDH_ECDSA_KEY_EXCHANGE |  |
| STATISTIC_SSL_CIPHER_ECDH_RSA_KEY_EXCHANGE |  |
| STATISTIC_SSL_CIPHER_ECDHE_ECDSA_KEY_EXCHANGE |  |
| STATISTIC_SSL_CIPHER_ECDHE_RSA_KEY_EXCHANGE |  |
| STATISTIC_SSL_CIPHER_EDH_RSA_KEY_EXCHANGE | Ephemeral Diffie-Hellman w/ RSA cert. |
| STATISTIC_SSL_CIPHER_IDEA_BULK | IDEA (old SSLv2 cipher). |
| STATISTIC_SSL_CIPHER_MD5_DIGEST | Message Digest 5. |
| STATISTIC_SSL_CIPHER_NULL_BULK | No encryption. |
| STATISTIC_SSL_CIPHER_NULL_DIGEST | No message authentication. |
| STATISTIC_SSL_CIPHER_RC2_BULK | Rivest Cipher 2. |
| STATISTIC_SSL_CIPHER_RC4_BULK | Rivest Cipher 4. |
| STATISTIC_SSL_CIPHER_RSA_KEY_EXCHANGE | RSA certificate. |
| STATISTIC_SSL_CIPHER_SHA_DIGEST | Secure Hash Algorithm. |
| STATISTIC_SSL_COMMON_BAD_RECORDS | Total bad records. |
| STATISTIC_SSL_COMMON_CURRENT_COMPATIBLE_MODE_CONNECTIONS | Currently open compatible-mode connections. |
| STATISTIC_SSL_COMMON_CURRENT_CONNECTIONS | Currently open connections. |
| STATISTIC_SSL_COMMON_CURRENT_NATIVE_CONNECTIONS | Currently open native connections. |
| STATISTIC_SSL_COMMON_DECRYPTED_BYTES_IN | Total decrypted bytes received. |
| STATISTIC_SSL_COMMON_DECRYPTED_BYTES_OUT | Total decrypted bytes sent. |
| STATISTIC_SSL_COMMON_ENCRYPTED_BYTES_IN | Total encrypted bytes received. |
| STATISTIC_SSL_COMMON_ENCRYPTED_BYTES_OUT | Total encrypted bytes sent. |
| STATISTIC_SSL_COMMON_FATAL_ALERTS | Total fatal alerts. |
| STATISTIC_SSL_COMMON_FULLY_HW_ACCELERATED_CONNECTIONS | Total offloaded connections. |
| STATISTIC_SSL_COMMON_FWDP_CONNS |  |
| STATISTIC_SSL_COMMON_FWDP_DESTINATION_IP_BYPASSES |  |
| STATISTIC_SSL_COMMON_FWDP_HOSTNAME_BYPASSES |  |
| STATISTIC_SSL_COMMON_FWDP_SOURCE_IP_BYPASSES |  |
| STATISTIC_SSL_COMMON_HANDSHAKE_FAILURES | Total handshake failures. |
| STATISTIC_SSL_COMMON_INSECURE_HANDSHAKE_ACCEPTS | Total number of handshakes, including mid-stream renegotiations, performed with peers not supporting SSL secure renegotiation. |
| STATISTIC_SSL_COMMON_INSECURE_HANDSHAKE_REJECTS | Total number of rejected initial handshakes with peers not supporting SSL secure renegotiation. |
| STATISTIC_SSL_COMMON_INSECURE_RENEGOTIATION_REJECTS | Total number of rejected renegotiation attempts by peers not supporting SSL secure renegotiation. |
| STATISTIC_SSL_COMMON_INVALID_PEER_CERTIFICATES | Total invalid certificates. |
| STATISTIC_SSL_COMMON_MAXIMUM_COMPATIBLE_MODE_CONNECTIONS | Maximum simultaneous compatible-mode connections. |
| STATISTIC_SSL_COMMON_MAXIMUM_CONNECTIONS | Maximum simultaneous connections. |
| STATISTIC_SSL_COMMON_MAXIMUM_NATIVE_CONNECTIONS | Maximum simultaneous native connections. |
| STATISTIC_SSL_COMMON_MIDSTREAM_RENEGOTIATIONS | Total mid-connection handshakes. |
| STATISTIC_SSL_COMMON_NO_PEER_CERTIFICATES | Total connections without certificates. |
| STATISTIC_SSL_COMMON_NON_HW_ACCELERATED_CONNECTIONS | Total software connections. |
| STATISTIC_SSL_COMMON_NOT_SSL_HANDSHAKE_FAILURES | Total bad client greetings. |
| STATISTIC_SSL_COMMON_PARTIALLY_HW_ACCELERATED_CONNECTIONS | Total assisted connections. |
| STATISTIC_SSL_COMMON_PREMATURE_DISCONNECTS | Total unclean shutdowns. |
| STATISTIC_SSL_COMMON_RECORDS_IN | Total records received. |
| STATISTIC_SSL_COMMON_RECORDS_OUT | Total records transmitted. |
| STATISTIC_SSL_COMMON_SECURE_HANDSHAKES | Total number of handshakes, including mid-stream renegotiations, performed with peers supporting SSL secure renegotiation. |
| STATISTIC_SSL_COMMON_SESSION_CACHE_CURRENT_ENTRIES | Current entries in this cache. |
| STATISTIC_SSL_COMMON_SESSION_CACHE_HITS | Total cache hits. |
| STATISTIC_SSL_COMMON_SESSION_CACHE_INVALIDATIONS | Total session invalidations. |
| STATISTIC_SSL_COMMON_SESSION_CACHE_LOOKUPS | Total cache lookups. |
| STATISTIC_SSL_COMMON_SESSION_CACHE_OVERFLOWS | Total cache overflows. |
| STATISTIC_SSL_COMMON_SNI_REJECTS |  |
| STATISTIC_SSL_COMMON_TOTAL_COMPATIBLE_MODE_CONNECTIONS | Total compatible-mode connections. |
| STATISTIC_SSL_COMMON_TOTAL_NATIVE_CONNECTIONS | Total native connections. |
| STATISTIC_SSL_COMMON_VALID_PEER_CERTIFICATES | Total valid certificates. |
| STATISTIC_SSL_PROTOCOL_DTLSV1 |  |
| STATISTIC_SSL_PROTOCOL_SSLV2 | Total connections for SSLv2 protocol. |
| STATISTIC_SSL_PROTOCOL_SSLV3 | Total connections for SSLv3 protocol. |
| STATISTIC_SSL_PROTOCOL_TLSV1 | Total connections for TLSv1 protocol. |
| STATISTIC_SSL_PROTOCOL_TLSV1_1 |  |
| STATISTIC_SSL_PROTOCOL_TLSV1_2 |  |
| STATISTIC_SSL_SESSTICK_REUSE_FAILED |  |
| STATISTIC_SSL_SESSTICK_REUSED |  |

### System

| Metric | Description |
| ----- | ----- |
| Uptime (sec) | Total no of seconds the system is up. |

### System|CPU

| Metric | Description |
| ----- | ----- |
| CPU % BUSY | CPU Usage |

### System|Disks

| Metric | Description |
| ----- | ----- |
| Space Available | The no of space available in MB |
| Space Used | The no of space used in MB |

### System|Memory

| Metric | Description |
| ----- | ----- |
| STATISTIC_MEMORY_TOTAL_BYTES | The total memory in bytes |
| STATISTIC_MEMORY_USED_BYTES | The total used memory in bytes |

### Virtual Servers

| Metric | Description |
| ----- | ----- |
| STATISTIC_ACL_NO_MATCH |  |
| STATISTIC_CLIENT_SIDE_BYTES_IN | Total number of bytes in from the client-side of the object. |
| STATISTIC_CLIENT_SIDE_BYTES_OUT | Total number of bytes out from the client-side of the object. |
| STATISTIC_CLIENT_SIDE_PACKETS_IN | Total number of packets in from the client-side of the object. |
| STATISTIC_CLIENT_SIDE_PACKETS_OUT | Total number of packets out from the client-side of the object. |
| STATISTIC_CLIENT_SIDE_CURRENT_CONNECTIONS | Current number of connections from the client-side of the object. |
| STATISTIC_CLIENT_SIDE_MAXIMUM_CONNECTIONS | Maximum number of connections from the client-side of the object. |
| STATISTIC_CLIENT_SIDE_TOTAL_CONNECTIONS | Total number of connections from the client-side of the object. |
| STATISTIC_CURRENT_PVA_ASSISTED_CONNECTIONS | Current number of connections assisted by PVA. |
| STATISTIC_EPHEMERAL_BYTES_IN | Total number of bytes in through the ephemeral port. |
| STATISTIC_EPHEMERAL_BYTES_OUT | Total number of bytes out through the ephemeral port. |
| STATISTIC_EPHEMERAL_PACKETS_IN | Total number of packets in through the ephemeral port. |
| STATISTIC_EPHEMERAL_PACKETS_OUT | Total number of packets out through the ephemeral port. |
| STATISTIC_EPHEMERAL_CURRENT_CONNECTIONS | Current number of connections through the ephemeral port. |
| STATISTIC_EPHEMERAL_MAXIMUM_CONNECTIONS | Maximum number of connections through the ephemeral port. |
| STATISTIC_EPHEMERAL_TOTAL_CONNECTIONS | Total number of connections through the ephemeral port. |
| STATISTIC_MINIMUM_CONNECTION_DURATION | The minimum duration of connection. |
| STATISTIC_MEAN_CONNECTION_DURATION | The mean duration of connection. |
| STATISTIC_MAXIMUM_CONNECTION_DURATION | The maximum duration of connection. |
| STATISTIC_NO_NODE_ERRORS | Number of times a virtual server has been unabled to direct connection to a node. |
| STATISTIC_PVA_CLIENT_SIDE_BYTES_IN | Total number of bytes in that are handled by PVA from the client-side of the object. |
| STATISTIC_PVA_CLIENT_SIDE_BYTES_OUT | Total number of bytes out that are handled by PVA from the client-side of the object. |
| STATISTIC_PVA_CLIENT_SIDE_PACKETS_IN | Total number of packets in that are handled by PVA from the client-side of the object. |
| STATISTIC_PVA_CLIENT_SIDE_PACKETS_OUT | Total number of packets out that are handled by PVA from the client-side of the object. |
| STATISTIC_PVA_CLIENT_SIDE_CURRENT_CONNECTIONS | Current number of connections that are handled by PVA from the client-side of the object. |
| STATISTIC_PVA_CLIENT_SIDE_MAXIMUM_CONNECTIONS | Maximum number of connections that are handled by PVA from the client-side of the object. |
| STATISTIC_PVA_CLIENT_SIDE_TOTAL_CONNECTIONS | Total number of connections that are handled by PVA from the client-side of the object. |
| STATISTIC_TOTAL_REQUESTS | Total number of requests. |
| STATISTIC_TOTAL_PVA_ASSISTED_CONNECTIONS | Total number of connections assisted by PVA. |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_HW_INSTANCES |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_SW_INSTANCES |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_CACHE_USAGE |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_CACHE_OVERFLOWS |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_SW_TOTAL |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_SW_ACCEPTS |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_SW_REJECTS |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_HW_TOTAL |  |
| STATISTIC_VIRTUAL_SERVER_SYNCOOKIE_HW_ACCEPTS |  |
| STATISTIC_VIRTUAL_SERVER_TOTAL_CPU_CYCLES | Total CPU cycles spent processing traffic for a virtual server. |
| STATISTIC_VIRTUAL_SERVER_FIVE_SEC_AVG_CPU_USAGE | Percentage of CPU time spent processing traffic for a virtual server (avg five sec ago with now). |
| STATISTIC_VIRTUAL_SERVER_ONE_MIN_AVG_CPU_USAGE | Percentage of CPU time spent processing traffic for a virtual server (weighted exponential moving avg over the last minute). |
| STATISTIC_VIRTUAL_SERVER_FIVE_MIN_AVG_CPU_USAGE | Percentage of CPU time spent processing traffic for a virtual server (weighted exponential moving avg over the last five minutes). |

##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/f5-monitoring-extension).

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/eXchange/F5-Monitoring-Extension/idi-p/2063) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).

