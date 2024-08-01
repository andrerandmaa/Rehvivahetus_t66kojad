import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class RestApiXML {

    public List<List<String>> RestApiGet(String url, String fromDate, String toDate, String workshop) throws URISyntaxException, IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();

        // Constructing valid api url
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(new URI(url + "/available?from=" + fromDate + "&until=" + toDate))
                .build();

        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());

        String[] splittedResponse = getResponse.body().split("><");
        List<List<String>> availableTimes = new ArrayList<>();

        // Finding all available times within start and end dates
        if (splittedResponse[0].equals("<tireChangeTimesResponse")) {
            if (splittedResponse[1].equals("availableTime")) {

                List<String> availableTimeSlot = new ArrayList<>();
                for (String xmlLine : splittedResponse) {
                    if (xmlLine.contains("uuid>") && xmlLine.contains("</uuid")) {
                        String uuid = xmlLine.replaceAll("uuid>", "").replaceAll("</uuid", "");
                        availableTimeSlot.add(workshop);
                        availableTimeSlot.add(uuid);                        
                    } else if (xmlLine.contains("time>") && xmlLine.contains("</time")) {
                        String time = xmlLine.replaceAll("time>", "").replaceAll("</time", "");
                        availableTimeSlot.add(time);
                        availableTimes.add(availableTimeSlot);
                        availableTimeSlot = new ArrayList<>();
                    }
                }
            }
        } else {
            throw new RuntimeException("Invalid filter inputs");
        }

        // Returns available times in format [[workshop, uuid, time], ... ]
        return availableTimes;
    }

    public void RestApiPost(String apiURL, String uuid, String contactInfo) throws URISyntaxException, IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();

        // Constructing valid request body
        String xmlFormatRequestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<london.tireChangeBookingRequest>\n" +
                "\t<contactInformation>" + contactInfo + "</contactInformation>\n" +
                "</london.tireChangeBookingRequest>";

        // Constructing valid api url
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(new URI(apiURL + "/" + uuid + "/booking"))
                .header("Content-Type", "application/xml")
                .PUT(HttpRequest.BodyPublishers.ofString(xmlFormatRequestBody))
                .build();

        HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
    }
}
