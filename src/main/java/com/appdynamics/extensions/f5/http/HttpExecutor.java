package com.appdynamics.extensions.f5.http;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * @author Satish Muddam
 */
public class HttpExecutor {

    public static final Logger LOGGER = Logger.getLogger(HttpExecutor.class);

    public static CloseableHttpResponse execute(CloseableHttpClient httpClient, HttpUriRequest httpUriRequest, HttpClientContext context) {

        try {
            CloseableHttpResponse response = httpClient.execute(httpUriRequest, context);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                LOGGER.debug("Successfully executed [" + httpUriRequest.getMethod() + "] on [" + httpUriRequest.getURI() + "]");
                return response;
            }
            LOGGER.info("Received error response [" + statusCode + "] while executing [" + httpUriRequest.getMethod() + "] on [" + httpUriRequest.getURI() + "]");

        } catch (IOException e) {
            LOGGER.error("Error executing [" + httpUriRequest.getMethod() + "] on [" + httpUriRequest.getURI() + "]", e);
        }

        return null;

    }


}
