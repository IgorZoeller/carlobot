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

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;

public final class HttpHandler {
    public HttpHandler() {

    }

    public static HashMap<String, Object> parseJsonMessage(InputStream stream) throws IOException {
        JsonParser parser = Json.createParser(stream);
        HashMap<String, Object> parsedMessage = new HashMap<String, Object>();
        while (parser.hasNext()) {
            if (parser.next() == Event.KEY_NAME) {
                String key = parser.getString();
                parsedMessage.put(key, parseObject(parser));
            }
        }
        parser.close();
        return parsedMessage;
    }

    public static Object parseObject(JsonParser parser) {
        Object object = null;
        Event event = parser.next();
        if (event == Event.START_ARRAY) {
            object = parseJsonArray(parser);
        } else if (event == Event.START_OBJECT) {
            object = parseJson(parser);
        }
        return object;
    }

    public static List<HashMap<String, Object>> parseJsonArray(JsonParser parser) {
        List<HashMap<String, Object>> jsonArray = new ArrayList<HashMap<String, Object>>();
        Event jEvent = parser.next();
        while (jEvent != Event.END_ARRAY) {
            if (jEvent == Event.START_OBJECT) {
                HashMap<String, Object> object = parseJson(parser);
                jsonArray.add(object);
            }
            jEvent = parser.next();
        }
        return jsonArray;
    }

    public static HashMap<String, Object> parseJson(JsonParser parser) {
        HashMap<String, Object> json = new HashMap<String, Object>();
        Event jEvent = parser.next();
        String key = null;
        Object value = null;
        while (jEvent != Event.END_OBJECT) {
            if (jEvent == Event.KEY_NAME) {
                key = parser.getString();
            }
            else if (jEvent.toString().startsWith("VALUE_")) {
                value = parser.getString();
                json.put(key, value);
            }
            else if (jEvent == Event.START_ARRAY) {
                value = parseJsonArray(parser);
                json.put(key, value);
            }
            jEvent = parser.next();
        }
        return json;
    }
    /**
     * Parses the JSON contained in a response from an HTTP request into a HashMap.
     * @param response of type {@link HttpResponse}
     * @return responseJson of type HashMap corresponding to the consumed JSON
     */
    public static HashMap<String, Object> consumeHttpResponse(HttpResponse response) {
        HashMap<String, Object> responseJson = null;
        try {
            InputStream httpStream = response.getEntity().getContent();
            System.out.println(
                String.format("[INFO] Ready to start consuming %d bytes of data.", httpStream.available())
            );
            responseJson = parseJsonMessage(httpStream);
            httpStream.close();
            return responseJson;
        }
        catch (IOException error) {
            System.out.println("Could not consume HttpResponse");
            System.out.println(error);
        }
        return responseJson;
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
