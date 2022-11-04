package com.zoeller.carlobot;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        while (parser.next() != Event.START_ARRAY) continue;
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
}
