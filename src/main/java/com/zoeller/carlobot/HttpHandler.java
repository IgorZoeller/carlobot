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
            else if (event == Event.START_ARRAY) {
                List<String> values = new ArrayList<String>();
                while(parser.next() != Event.END_ARRAY) {
                    values.add(parser.getString());
                }
                content.put(key, values);
            }
            else {
                System.out.println(key);
                System.out.print(event + " : ");
                System.out.println(parser.getString());
                throw new IOException("JSON_PARSER_EXCEPTION - Unable to parse Json, invalid EVENT state");
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
    public static List<HashMap<String, Object>> parseJsonMessage(InputStream stream) throws IOException{
        List<HashMap<String, Object>> parsedMessage = new ArrayList<>();
        JsonParser parser = Json.createParser(stream);
        while (parser.next() != Event.START_ARRAY) continue;
        while(parser.hasNext()) {
            Event event = parser.next();
            if (event == Event.START_OBJECT) {
                parsedMessage.add(parseObject(parser));
            }
        }
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
