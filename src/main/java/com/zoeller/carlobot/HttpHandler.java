package com.zoeller.carlobot;

import java.io.IOException;
import java.io.InputStream;
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

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.apache.http.HttpResponse;

public final class HttpHandler {
    public HttpHandler() {
    }

    private static void invalidEventStateException(String stateKey, Event event) throws IOException {
        System.out.println("Object key " + stateKey + ": ");
        throw new IOException(
            String.format("[WARN] JSON_PARSER_EXCEPTION - Invalid EVENT state %s, ignoring Object.", event.toString())
        );
    }

    
    public static HashMap<String, Object> parseObject(JsonParser parser) throws IOException{
        HashMap<String, Object> content = new HashMap<>();
        String key = null, value = null;
        Event event = parser.next();
        while (event != Event.END_OBJECT) {
            if (event == Event.KEY_NAME) {
                key = parser.getString();
            }
            else if (event.toString().startsWith("VALUE_")) {
                value = parser.getString();
                content.put(key, value);
            }
            /*
             * In case there is no VALUE_{STRING, NUMBER, NULL, etc.} after KEY_NAME it
             * means the object most likely has a list of values attributed to current key.
             */
            // TO-DO: ADD LOGIC TO PARSE OBJECT INTO A LIST HERE.
            // List<Object> values = new ArrayList<Object>();
            // values = parseJsonMessage(parser);
            else if (event == Event.START_ARRAY) {
                List<String> values = new ArrayList<String>();
                while(parser.next() != Event.END_ARRAY) {
                    try {
                        values.add(parser.getString());
                    }
                    catch (IllegalStateException error) {
                        invalidEventStateException(key, event);
                    }
                }
                content.put(key, values);
            }
            else {
                invalidEventStateException(key, event);
            }
            event = parser.next();
        }
        return content;
    }
    
    
    /**
     * {@link}https://javadoc.io/static/javax.json/javax.json-api/1.1.4/javax/json/stream/JsonParser.html
     * @param stream
     * @return
     */
    public static List<HashMap<String, Object>> parseJsonMessage(JsonParser parser) {
        List<HashMap<String, Object>> parsedMessage = new ArrayList<>();
        while (parser.next() != Event.START_ARRAY && parser.hasNext()) continue;
        while(parser.hasNext()) {
            Event event = parser.next();
            if (event == Event.START_OBJECT) {
                try {
                    parsedMessage.add(parseObject(parser));
                }
                catch (IOException error) {
                    System.out.println(error);
                    while(parser.next() != Event.END_OBJECT) continue;
                }
            }
        }
        return parsedMessage;
    }
    public static List<HashMap<String, Object>> parseJsonMessage(InputStream stream) throws IOException{
        JsonParser parser = Json.createParser(stream);
        List<HashMap<String, Object>> parsedMessage = parseJsonMessage(parser);
        return parsedMessage;
    }


    /**
     * Takes the JSON response from an http request and parses the InputStream
     * into a List of Hash Maps containing all data provided.
     * @param response to be consumed
     * @return List with all response data
     */
    public static List<HashMap<String, Object>> consumeHttpResponse(HttpResponse response) {
        List<HashMap<String, Object>> responseData = null;
        try {
            InputStream httpStream = response.getEntity().getContent();
            System.out.println(
                String.format("[INFO] Ready to start consuming %d bytes of data.", httpStream.available())
            );
            responseData = parseJsonMessage(httpStream);
            httpStream.close();
            return responseData;
        }
        catch (IOException error) {
            System.out.println("Could not consume HttpResponse");
            System.out.println(error);
        }
        return responseData;
    }

    public static class OauthHeaderGenerator {
        private String consumerKey;
        private String consumerSecret;
        private String signatureMethod = "HMAC-SHA1";
        private String token;
        private String tokenSecret;
        private String version = "1.0";

        public OauthHeaderGenerator(String consumerKey, String consumerSecret, String token, String tokenSecret) {
            this.consumerKey = consumerKey;
            this.consumerSecret = consumerSecret;
            this.token = token;
            this.tokenSecret = tokenSecret;
        }

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
}
