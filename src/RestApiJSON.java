import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RestApiJSON {

    public List<List<String>> RestApiGet(String url, String fromDate, String endDate, String workshop) throws URISyntaxException, IOException, InterruptedException {
        Gson gson = new Gson();

        // Constructing valid api url
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(new URI(url + "?from=" + fromDate))
                .build();

        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());

        List<Transcript> transcripts = gson.fromJson(getResponse.body(), new TypeToken<List<Transcript>>(){}.getType());
        List<List<String>> availableTimes = new ArrayList<>();

        // Finding all available times within start and end dates
        for (Transcript lineJSON : transcripts) {
            if (lineJSON.isAvailable()) {
                String[] dateTime = lineJSON.getTime().split("T");
                LocalDate date1 = LocalDate.parse(endDate);
                LocalDate date2 = LocalDate.parse(dateTime[0]);

                if (date2.isBefore(date1)) {
                    List<String> availableTimeSlot = new ArrayList<>();
                    availableTimeSlot.add(workshop);
                    availableTimeSlot.add(lineJSON.getId());
                    availableTimeSlot.add(lineJSON.getTime());
                    availableTimes.add(availableTimeSlot);
                } else {
                    break;
                }
            }
        }

        return availableTimes;
    }

    public void RestApiPut(String apiURL, String id, String contactInfo) throws URISyntaxException, IOException, InterruptedException {
        Transcript transcript = new Transcript();
        Gson gson = new Gson();
        HttpClient httpClient = HttpClient.newHttpClient();

        transcript.setContactInformation(contactInfo);
        // Turning transcript into json format
        String jsonRequest = gson.toJson(transcript);

        // Constructing valid api url
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(new URI(apiURL + "/" + id + "/booking"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
    }
}
