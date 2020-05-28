/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.f5;

import com.appdynamics.extensions.crypto.Encryptor;
import com.appdynamics.extensions.util.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by abey.tom on 5/23/17.
 */
public class AuthTokenFetcherTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void getTokenNoLoginRef() throws IOException {
        Map<String, Object> server = createServerMap();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpPost post = invocation.getArgumentAt(0, HttpPost.class);
                JsonNode request = mapper.readTree(post.getEntity().getContent());
                validateCommonReqAttr(post, request);
                Assert.assertNull(request.get("loginReference"));
                return createHttpResponse(post, 200);
            }
        }).when(httpClient).execute(Mockito.any(HttpUriRequest.class));
        HashMap<String, ObjectMapper> config = new HashMap<String, ObjectMapper>();
        AuthTokenFetcher fetcher = new AuthTokenFetcher(httpClient);
        String token = fetcher.getToken(server);
        Assert.assertEquals("492D3316E5456378B4AC9B5E2FA923595F0DA65A",token);
    }

    @Test
    public void getTokenWithLoginRefAndEncryptedPwd() throws IOException {
        Map<String, Object> server = createServerMap();
        server.remove(Constants.PASSWORD);
        server.put("encryptedPassword",new Encryptor("encryptionKey").encrypt("welcome"));
        server.put("encryptionKey","encryptionKey");
        final String loginRef = "https://localhost/mgmt/cm/system/authn/providers/ldap/-id-/login";
        server.put("loginReference", loginRef);
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpPost post = invocation.getArgumentAt(0, HttpPost.class);
                JsonNode request = mapper.readTree(post.getEntity().getContent());
                validateCommonReqAttr(post, request);
                Assert.assertEquals(loginRef,JsonUtils.getTextValue(request,"loginReference","link"));
                return createHttpResponse(post, 200);
            }
        }).when(httpClient).execute(Mockito.any(HttpUriRequest.class));
        HashMap<String, Object> config = new HashMap<String, Object>();
        AuthTokenFetcher fetcher = new AuthTokenFetcher(httpClient);
        String token = fetcher.getToken(server);
        Assert.assertEquals("492D3316E5456378B4AC9B5E2FA923595F0DA65A",token);
    }

    /**
     * The authType is not set
     * @throws IOException
     */
    @Test
    public void getTokenNoAuthType() throws IOException {
        Map<String, Object> server = Collections.emptyMap();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        HashMap<String, ObjectMapper> config = new HashMap<String, ObjectMapper>();
        AuthTokenFetcher fetcher = new AuthTokenFetcher(httpClient);
        String token = fetcher.getToken(server);
        Assert.assertNull(token);
    }
    /**
     * The Credentials are not set
     * @throws IOException
     */
    @Test(expected = RuntimeException.class)
    public void getTokenNoServerData() throws IOException {
        Map<String, Object> server = Collections.singletonMap("authType",(Object) "TOKEN");
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        HashMap<String, ObjectMapper> config = new HashMap<String, ObjectMapper>();
        AuthTokenFetcher fetcher = new AuthTokenFetcher(httpClient);
        String token = fetcher.getToken(server);
        Assert.assertNull(token);
    }

    @Test
    public void getTokenNon200Response() throws IOException {
        Map<String, Object> server = createServerMap();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpPost post = invocation.getArgumentAt(0, HttpPost.class);
                JsonNode request = mapper.readTree(post.getEntity().getContent());
                validateCommonReqAttr(post, request);
                CloseableHttpResponse httpResponse = createHttpResponse(post,400);
                return httpResponse;
            }
        }).when(httpClient).execute(Mockito.any(HttpUriRequest.class));
        HashMap<String, ObjectMapper> config = new HashMap<String, ObjectMapper>();
        AuthTokenFetcher fetcher = new AuthTokenFetcher(httpClient);
        String token = fetcher.getToken(server);
        Assert.assertNull(token);
    }

    private void validateCommonReqAttr(HttpPost post, JsonNode request) {
        Assert.assertEquals("http://192.168.1.132:8080/mgmt/shared/authn/login",post.getURI().toString());
        Assert.assertEquals("abey", JsonUtils.getTextValue(request,"username"));
        Assert.assertEquals("welcome", JsonUtils.getTextValue(request,"password"));
    }

    private Map<String, Object> createServerMap() {
        Map<String, Object> server = new HashMap<String, Object>();
        server.put("uri","http://192.168.1.132:8080");
        server.put(Constants.USER_NAME,"abey");
        server.put(Constants.PASSWORD,"welcome");
        server.put("authType","TOKEN");
        return server;
    }

    private CloseableHttpResponse createHttpResponse(HttpPost post, int statusCode) throws IOException {
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        InputStream in = getClass().getResourceAsStream("/token/token-response.json");
        Mockito.doReturn(in).when(httpEntity).getContent();
        Mockito.doReturn(statusCode).when(statusLine).getStatusCode();
        Mockito.doReturn(statusLine).when(response).getStatusLine();
        Mockito.doReturn(httpEntity).when(response).getEntity();
        return response;
    }

}