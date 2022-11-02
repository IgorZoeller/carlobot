package com.zoeller.carlobot;

import com.zoeller.carlobot.Configuration;

import org.apache.http.HttpResponse;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;

public class TwitterApi {

    private Configuration config = new Configuration();
    private HttpClient client = new DefaultHttpClient();

    public TwitterApi() {
    }

    private URI buildURIWithRequestParameters(String URL, List<BasicNameValuePair> parameters)
            throws URISyntaxException {
        URIBuilder builder = new URIBuilder(URL);
        parameters.forEach(param -> builder.addParameter(param.getName(), param.getValue()));
        return builder.build();
    }

    private Header[] httpOAuthHeaders() {
        String auth = String.format("Bearer %s", this.config.getBearerToken());
        String user = "carlobot";
        Header[] headers = {
                new BasicHeader("Authorization", auth),
                new BasicHeader("User-Agent", user)
        };
        return headers;
    }

    private HttpResponse requestGET(String endpoint, Header[] headers, List<BasicNameValuePair> parameters)
            throws IOException, URISyntaxException {
        HttpGet request = new HttpGet(endpoint);
        request.setHeaders(headers);
        request.setURI(buildURIWithRequestParameters(endpoint, parameters));
        return this.client.execute(request);
    }

    public HttpResponse getLikedTweetsFromUserId(String id) {
        String endpointURL = String.format("https://api.twitter.com/2/users/%s/liked_tweets", id);
        List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        parameters.add(new BasicNameValuePair("tweet.fields", "created_at,author_id"));
        try {
            HttpResponse response = requestGET(endpointURL, httpOAuthHeaders(), parameters);
            return response;
        }
        catch (IOException e) {
        }
        catch (URISyntaxException e) {
        }
        return null;
    }
}
