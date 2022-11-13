package com.zoeller.carlobot;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

public class TwitterApi {

    private Configuration config;
    private RequestConfig requestConfig = RequestConfig.custom()
                                            .setCookieSpec(CookieSpecs.STANDARD)
                                            .build();
    private CloseableHttpClient client = HttpClientBuilder.create()
                                            .setDefaultRequestConfig(requestConfig)
                                            .build();
    private HttpHandler.OauthHeaderGenerator authenticator;

    public TwitterApi(Configuration config) {
        this.config = config;
        this.authenticator = new HttpHandler.OauthHeaderGenerator(
            this.config.getConsumerKey(), this.config.getConsumerSecret(),
            this.config.getAccessToken(), this.config.getAccessSecret()
        );
    }

    private URI buildURIWithRequestParameters(String URL, List<BasicNameValuePair> parameters)
            throws URISyntaxException {
        URIBuilder builder = new URIBuilder(URL);
        parameters.forEach(param -> builder.addParameter(param.getName(), param.getValue()));
        return builder.build();
    }
    
    private HttpResponse requestGET(String endpoint, Header[] headers, List<BasicNameValuePair> parameters)
            throws IOException, URISyntaxException {
        HttpGet request = new HttpGet(endpoint);
        request.setHeaders(headers);
        request.setURI(buildURIWithRequestParameters(endpoint, parameters));
        System.out.println(String.format("[INFO] GET %s", request.getURI().toString()));
        return this.client.execute(request);
    }

    private HttpResponse requestPOST(String endpoint, Header[] headers, List<BasicNameValuePair> parameters) throws IOException, URISyntaxException{
        HttpPost request = new HttpPost(endpoint);
        request.setHeaders(headers);
        request.setURI(buildURIWithRequestParameters(endpoint, parameters));
        System.out.println(String.format("[INFO] POST %s", request.getURI().toString()));
        return this.client.execute(request);
    }

    private HttpResponse requestPOST(String endpoint, Header[] headers, HttpEntity entity) throws IOException, URISyntaxException{
        HttpPost request = new HttpPost(endpoint);
        request.setHeaders(headers);
        request.setEntity(entity);
        System.out.println(String.format("[INFO] POST %s", request.getURI().toString()));
        return this.client.execute(request);
    }

    public String requestToken(String callbackURL, String consumerKey) {
        String endpointURL = "https://api.twitter.com/oauth/request_token";
        Map<String, String> parametersMap = new HashMap<>();
        List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parametersMap.put("oauth_callback", callbackURL);
        parametersMap.put("oauth_consumer_key", consumerKey);
        parameters.add(new BasicNameValuePair("oauth_callback", callbackURL));
        Header[] headers = {new BasicHeader("Authorization", authenticator.generateHeader("POST", endpointURL, parametersMap))};
        try {
            HttpResponse response = requestPOST(endpointURL, headers, parameters);
            return IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
        }
        catch (URISyntaxException e) {
        }
        return null;
    }
    public String accessToken() {
        String endpointURL = "https://api.twitter.com/oauth/access_token";
        List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parameters.add(new BasicNameValuePair("oauth_token", "7_yTRQAAAAABi4dWAAABhECqz2c"));
        parameters.add(new BasicNameValuePair("oauth_verifier", "0me2HlADedXJTEoVEgfyK3qjdepGii2Z"));
        try {
            HttpResponse response = requestPOST(endpointURL, null, parameters);
            return IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
        }
        catch (URISyntaxException e) {
        }
        return null;
    }

    private Header createHeaderFromParameters(List<BasicNameValuePair> parameters, String httpMethod, String endpointURL) {
        Map<String, String> parameterMap = new HashMap<>();
        parameters.forEach(pair -> {
            parameterMap.put(pair.getName(), pair.getValue());
        });
        return new BasicHeader("authorization", authenticator.generateHeader(httpMethod, endpointURL, parameterMap));
    }

    public HttpResponse getLikedTweetsFromUserId(String id) {
        String endpointURL = String.format("https://api.twitter.com/2/users/%s/liked_tweets", id);
        List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parameters.add(new BasicNameValuePair("tweet.fields", "created_at,author_id"));
        Header[] headers = {createHeaderFromParameters(parameters, "GET", endpointURL)};
        try {
            HttpResponse response = requestGET(endpointURL, headers, parameters);
            return response;
        }
        catch (IOException e) {
        }
        catch (URISyntaxException e) {
        }
        return null;
    }

    public HttpResponse postTweet(String message) {
        String endpointURL = "https://api.twitter.com/2/tweets";
        List<BasicNameValuePair> parameters = new ArrayList<>();
        JsonObject payload = Json.createObjectBuilder().add("text", message).build();
        // parameters.add(new BasicNameValuePair("tweet.fields", payload));
        Header[] headers = {
            createHeaderFromParameters(parameters, "POST", endpointURL), 
            new BasicHeader("Content-Type", "application/json")
        };
        try {
            HttpResponse response = requestPOST(endpointURL, headers, new StringEntity(payload.toString()));
            System.out.println(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
            return response;
        }
        catch (IOException e) {
        }
        catch (URISyntaxException e) {
        }
        return null;
    }
}
