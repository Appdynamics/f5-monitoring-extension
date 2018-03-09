/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.f5;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.http.Http4ClientBuilder;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.util.JsonUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Created by abey.tom on 5/23/17.
 * <p>
 * https://devcentral.f5.com/wiki/iControl.Authentication_with_the_F5_REST_API.ashx
 */
public class AuthTokenFetcher {
    public static final Logger logger = LoggerFactory.getLogger(AuthTokenFetcher.class);
    private final Map<String, ?> config;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper mapper;

    public AuthTokenFetcher(Map<String, ?> config, CloseableHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
        mapper = new ObjectMapper();
    }

    public String getToken(Map<String, Object> server) {
        Object authType = server.get("authType");
        if ("TOKEN".equals(authType)) {
            String uri = (String) server.get("uri");
            logger.debug("Fetching the token from the server {}", uri);
            String username = (String) server.get(TaskInputArgs.USER);
            String password = Http4ClientBuilder.getPassword(server, config);
            if (!StringUtils.isBlank(uri) && !StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
                ObjectNode request = createRequestJson(mapper, server, username, password);
                UrlBuilder urlBuilder = UrlBuilder.fromYmlServerConfig(server).path("mgmt/shared/authn/login");
                String url = urlBuilder.build();
                HttpPost post = new HttpPost(url);
                HttpEntity entity = null;
                try {
                    post.setEntity(new StringEntity(mapper.writeValueAsString(request)));
                    post.setHeader("Content-Type", "application/json");
                    post.setHeader("Accept", "application/json");
                    CloseableHttpResponse response = httpClient.execute(post);
                    if (response != null && response.getStatusLine().getStatusCode() == 200) {
                        entity = response.getEntity();
                        JsonNode responseNode = mapper.readTree(entity.getContent());
                        String token = JsonUtils.getTextValue(responseNode, "token", "token");
                        if (token != null) {
                            return token;
                        } else {
                            logger.info("Cannot get the authentication token from the response {}", responseNode);
                        }
                    } else {
                        HttpClientUtils.printError(response, url);
                    }
                } catch (IOException e) {
                    logger.error("", e);
                } finally {
                    if (entity != null) {
                        EntityUtils.consumeQuietly(entity);
                    }
                }
            } else {
                throw new RuntimeException("The username, password and the uri must be present for the server entries. Please check the config.yml");
            }
        } else {
            logger.info("Token authType is not TOKEN for server={}. Skipping the token auth", server.get("uri"));
        }
        return null;
    }

    protected ObjectNode createRequestJson(ObjectMapper mapper, Map<String, Object> server, String username, String password) {
        ObjectNode request = mapper.createObjectNode();
        request.put("username", username);
        request.put("password", password);
        String loginReference = (String) server.get("loginReference");
        if (!StringUtils.isBlank(loginReference)) {
            ObjectNode linkNode = mapper.createObjectNode();
            linkNode.put("link", loginReference);
            request.put("loginReference", linkNode);
        }
        return request;
    }
}
