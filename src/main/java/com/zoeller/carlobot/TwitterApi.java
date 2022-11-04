package com.zoeller.carlobot;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.net.URI;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
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
    private CloseableHttpClient client = HttpClientBuilder.create().build();

    public TwitterApi(Configuration config) {
        this.config = config;
    }

    private class TwitterOauthHeaderGenerator {
        private String consumerKey = config.getConsumerKey();
        private String consumerSecret = config.getConsumerSecret();
        private String signatureMethod = "HMAC-SHA1";
        private String token = config.getAccessToken();
        private String tokenSecret = config.getAccessSecret();
        private String version = "1.0";

        private static final String oauth_consumer_key = "oauth_consumer_key";
        private static final String oauth_token = "oauth_token";
        private static final String oauth_signature_method = "oauth_signature_method";
        private static final String oauth_timestamp = "oauth_timestamp";
        private static final String oauth_nonce = "oauth_nonce";
        private static final String oauth_version = "oauth_version";
        private static final String oauth_signature = "oauth_signature";
        private static final String HMAC_SHA1 = "HmacSHA1";

        /**
         * Generates oAuth 1.0a header which can be passed as Authorization header
         * 
         * @param httpMethod
         * @param url
         * @param requestParams
         * @return
         */
        public String generateHeader(String httpMethod, String url, Map<String, String> requestParams) {
            StringBuilder base = new StringBuilder();
            String nonce = getNonce();
            String timestamp = getTimestamp();
            String baseSignatureString = generateSignatureBaseString(httpMethod, url, requestParams, nonce, timestamp);
            String signature = encryptUsingHmacSHA1(baseSignatureString);
            base.append("OAuth ");
            append(base, oauth_consumer_key, consumerKey);
            append(base, oauth_token, token);
            append(base, oauth_signature_method, signatureMethod);
            append(base, oauth_timestamp, timestamp);
            append(base, oauth_nonce, nonce);
            append(base, oauth_version, version);
            append(base, oauth_signature, signature);
            base.deleteCharAt(base.length() - 1);
            System.out.println("[INFO] Generated Header: " + base.toString());
            return base.toString();
        }

        /**
         * Generate base string to generate the oauth_signature
         * 
         * @param httpMethod
         * @param url
         * @param requestParams
         * @return
         */
        private String generateSignatureBaseString(String httpMethod, String url, Map<String, String> requestParams, String nonce, String timestamp) {
            Map<String, String> params = new HashMap<>();
            requestParams.entrySet().forEach(entry -> {
                put(params, entry.getKey(), entry.getValue());
            });
            put(params, oauth_consumer_key, consumerKey);
            put(params, oauth_nonce, nonce);
            put(params, oauth_signature_method, signatureMethod);
            put(params, oauth_timestamp, timestamp);
            put(params, oauth_token, token);
            put(params, oauth_version, version);
            Map<String, String> sortedParams = params.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
            StringBuilder base = new StringBuilder();
            sortedParams.entrySet().forEach(entry -> {
                base.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            });
            base.deleteCharAt(base.length() - 1);
            String baseString = httpMethod.toUpperCase() + "&" + encode(url) + "&" + encode(base.toString());
            return baseString;
        }

        private String encryptUsingHmacSHA1(String input) {
            String secret = new StringBuilder().append(encode(consumerSecret)).append("&").append(encode(tokenSecret)).toString();
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            SecretKey key = new SecretKeySpec(keyBytes, HMAC_SHA1);
            Mac mac;
            try {
                mac = Mac.getInstance(HMAC_SHA1);
                mac.init(key);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                e.printStackTrace();
                return null;
            }
            byte[] signatureBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return new String(Base64.getEncoder().encode(signatureBytes));
        }

        /**
         * Percentage encode String as per RFC 3986, Section 2.1
         * 
         * @param value
         * @return
         */
        private String encode(String value) {
            String encoded = "";
            try {
                encoded = URLEncoder.encode(value, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            String sb = "";
            char focus;
            for (int i = 0; i < encoded.length(); i++) {
                focus = encoded.charAt(i);
                if (focus == '*') {
                    sb += "%2A";
                } else if (focus == '+') {
                    sb += "%20";
                } else if (focus == '%' && i + 1 < encoded.length() && encoded.charAt(i + 1) == '7' && encoded.charAt(i + 2) == 'E') {
                    sb += '~';
                    i += 2;
                } else {
                    sb += focus;
                }
            }
            return sb.toString();
        }

        private void put(Map<String, String> map, String key, String value) {
            map.put(encode(key), encode(value));
        }

        private void append(StringBuilder builder, String key, String value) {
            builder.append(encode(key)).append("=\"").append(encode(value)).append("\",");
        }

        private String getNonce() {
            int leftLimit = 48; // numeral '0'
            int rightLimit = 122; // letter 'z'
            int targetStringLength = 10;
            Random random = new Random();

            String generatedString = random.ints(leftLimit, rightLimit + 1).filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97)).limit(targetStringLength)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
            return generatedString;

        }

        private String getTimestamp() {
            return Math.round((new Date()).getTime() / 1000.0) + "";
        }
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
        return this.client.execute(request);
    }

    private HttpResponse requestPOST(String endpoint, Header[] headers, List<BasicNameValuePair> parameters) throws IOException, URISyntaxException{
        HttpPost request = new HttpPost(endpoint);
        request.setHeaders(headers);
        request.setURI(buildURIWithRequestParameters(endpoint, parameters));
        return this.client.execute(request);
    }

    private HttpResponse requestPOST(String endpoint, Header[] headers, HttpEntity entity) throws IOException, URISyntaxException{
        HttpPost request = new HttpPost(endpoint);
        request.setHeaders(headers);
        request.setEntity(entity);
        return this.client.execute(request);
    }

    public String requestToken(String callbackURL, String consumerKey) {
        String endpointURL = "https://api.twitter.com/oauth/request_token";
        TwitterOauthHeaderGenerator generator = new TwitterOauthHeaderGenerator();
        Map<String, String> parametersMap = new HashMap<>();
        List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parametersMap.put("oauth_callback", callbackURL);
        parametersMap.put("oauth_consumer_key", consumerKey);
        parameters.add(new BasicNameValuePair("oauth_callback", callbackURL));
        Header[] headers = {new BasicHeader("Authorization", generator.generateHeader("POST", endpointURL, parametersMap))};
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
        TwitterOauthHeaderGenerator generator = new TwitterOauthHeaderGenerator();
        Map<String, String> parameterMap = new HashMap<>();
        parameters.forEach(pair -> {
            parameterMap.put(pair.getName(), pair.getValue());
        });
        return new BasicHeader("authorization", generator.generateHeader(httpMethod, endpointURL, parameterMap));
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
