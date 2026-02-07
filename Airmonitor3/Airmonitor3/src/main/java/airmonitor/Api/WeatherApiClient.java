package airmonitor.Api;

import airmonitor.Model.AirQualityData;
import com.fasterxml.jackson.databind.ObjectMapper;
import airmonitor.Model.WeatherData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WeatherApiClient {
    private final String API_KEY = "f33573643419fc120e247e24bab2cbcc";
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public WeatherData fetchCurrentWeather(String city) throws Exception {

        String encodedCity = java.net.URLEncoder.encode(city, java.nio.charset.StandardCharsets.UTF_8);
        String url = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric", encodedCity, API_KEY);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return mapper.readValue(response.body(), WeatherData.class);
        } else {
            throw new RuntimeException("Błąd pobierania danych: " + response.statusCode());
        }
    }

    public AirQualityData fetchAirQuality(double lat, double lon) throws Exception {
        String url = String.format("https://api.openweathermap.org/data/2.5/air_pollution?lat=%s&lon=%s&appid=%s", lat, lon, API_KEY);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return mapper.readValue(response.body(), AirQualityData.class);
        } else {
            throw new RuntimeException("Błąd pobierania smogu");
        }
    }
}
