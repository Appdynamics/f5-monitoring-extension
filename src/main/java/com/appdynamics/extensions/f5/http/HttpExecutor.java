package com.appdynamics.extensions.f5.http;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * @author Satish Muddam
 */
public class HttpExecutor {

    public static final Logger LOGGER = Logger.getLogger(HttpExecutor.class);

    public static String execute(CloseableHttpClient httpClient, HttpUriRequest httpUriRequest, HttpClientContext context) {

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpUriRequest, context);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                LOGGER.debug("Successfully executed [" + httpUriRequest.getMethod() + "] on [" + httpUriRequest.getURI() + "]");

                String responseString = EntityUtils.toString(response.getEntity());

                return responseString;
            }
            LOGGER.info("Received error response [" + statusCode + "] while executing [" + httpUriRequest.getMethod() + "] on [" + httpUriRequest.getURI() + "]");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Response received [ " + EntityUtils.toString(response.getEntity()) + " ]");
            }

        } catch (IOException e) {
            LOGGER.error("Error executing [" + httpUriRequest.getMethod() + "] on [" + httpUriRequest.getURI() + "]", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LOGGER.error("Error while closing the response", e);
                }
            }
        }
        return null;
    }
}