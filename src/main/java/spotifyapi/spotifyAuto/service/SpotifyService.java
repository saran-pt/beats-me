package spotifyapi.spotifyAuto.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class SpotifyService {

    public String authenticate(){
        String url = "https://accounts.spotify.com/authorize?";
        String clientId = "";
        String responseType = "code";
        String redirectUri = "http://localhost:8080/callback";
        String scope = "user-read-private user-top-read playlist-modify-public playlist-modify-private";
        boolean showDialog = true;

        String authUrl =  url + "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&response_type=" + URLEncoder.encode(responseType, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
                "&show_dialog=" + URLEncoder.encode(String.valueOf(showDialog), StandardCharsets.UTF_8);

        return authUrl;
    }

    public String getUserId(String accessToken) throws Exception{
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://api.spotify.com/v1/me"))
                .headers("Authorization", "Bearer "+accessToken)
                .GET().build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject jsObj = new JSONObject(response.body());

        return jsObj.getString("id");
    }


    public ArrayList<String> getSongUris(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://api.spotify.com/v1/me/top/tracks?time_range=short_term&limit=20"))
                    .headers("Authorization", "Bearer "+accessToken)
                    .GET().build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jsonResponse = new JSONObject(response.body());

            JSONArray itemsArray = jsonResponse.getJSONArray("items");

            List<String> songsUris = new ArrayList<>();

            for (int index=0; index<itemsArray.length(); index++) {
                JSONObject arr = itemsArray.getJSONObject(index);
                songsUris.add(arr.getString("uri"));
            }
            return new ArrayList<>(songsUris);
        } catch (Exception e) {
            System.out.println("Error occurred :"+ e.toString());
        }
        return new ArrayList<>();
    }


    public String createPlayList(String accessToken) throws Exception{
        ArrayList<String> songUris = getSongUris(accessToken);
        String userId = getUserId(accessToken);

        String url = "https://api.spotify.com/v1/users/"+userId+"/playlists";

        String playlistName = "Recent Fav Player!";
        String description = "Playlist created by java spring";
        String isPublic = "true";

        String requestBody = String.format("{\"name\":\"%s\",\"description\":\"%s\",\"public\":%s}",
                playlistName, description, isPublic);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .headers("Authorization", "Bearer " + accessToken)
                .headers("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject jsonObject = new JSONObject(response.body());
        String playListId = jsonObject.getString("id");

        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("uris", songUris);

        ObjectMapper mapper = new ObjectMapper();

        String uriRequestBody = mapper.writeValueAsString(jsonMap);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(new URI("https://api.spotify.com/v1/playlists/"+playListId+"/tracks"))
                .headers("Authorization", "Bearer "+accessToken)
                .headers("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(uriRequestBody))
                .build();

        HttpResponse<String> responseObj = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        JSONObject jsObj = new JSONObject(responseObj.body());

        return jsObj.toString();

    }


    public String callBack(String code) {
        String redirectUri = "http://localhost:8080/callback";
        String clientId = "";
        String clientSecret = "";

        try {
            String authorizationHeader = "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

            String requestBody = "code=" + code +
                    "&redirect_uri=" + redirectUri +
                    "&grant_type=authorization_code";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(new URI("https://accounts.spotify.com/api/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", authorizationHeader)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            JSONObject jsonResponse = new JSONObject(response.body());
            String accessToken = jsonResponse.getString("access_token");

            return createPlayList(accessToken);

        } catch (Exception e) {
            System.out.println("Error occurred "+ e.toString());
        }
        return "Playlist Not Created!";
    }
}
