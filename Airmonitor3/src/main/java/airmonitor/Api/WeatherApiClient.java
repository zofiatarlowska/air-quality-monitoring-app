package airmonitor.Api;

import airmonitor.Model.AirQualityData;
import airmonitor.Model.ForecastData;
import com.fasterxml.jackson.databind.ObjectMapper;
import airmonitor.Model.WeatherData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import airmonitor.Model.AirQualityPoint;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

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

    public String fetchCityByIp() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://ip-api.com/json"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode node = mapper.readTree(response.body());
        return node.get("city").asText();
    }

    public ForecastData fetchForecast(String city) throws Exception {
        String encodedCity = java.net.URLEncoder.encode(city, java.nio.charset.StandardCharsets.UTF_8);
        String url = String.format("https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=metric", encodedCity, API_KEY);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return mapper.readValue(response.body(), ForecastData.class);
        } else {
            throw new RuntimeException("Błąd prognozy: " + response.statusCode());
        }
    }

    public List<AirQualityPoint> fetchAirQualityHistory(double lat, double lon, long startEpochSec, long endEpochSec) throws Exception {
        String url = String.format(
                "https://api.openweathermap.org/data/2.5/air_pollution/history?lat=%s&lon=%s&start=%s&end=%s&appid=%s",
                lat, lon, startEpochSec, endEpochSec, API_KEY
        );

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Błąd pobierania historii smogu: " + response.statusCode() + " | " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode list = root.get("list");

        List<AirQualityPoint> points = new ArrayList<>();
        if (list != null && list.isArray()) {
            for (JsonNode item : list) {
                long dt = item.get("dt").asLong();

                int aqi = item.get("main").get("aqi").asInt();

                JsonNode comp = item.get("components");
                double pm25 = comp.get("pm2_5").asDouble();
                double pm10 = comp.get("pm10").asDouble();

                points.add(new AirQualityPoint(dt, pm25, pm10, aqi));
            }
        }
        return points;
    }

}