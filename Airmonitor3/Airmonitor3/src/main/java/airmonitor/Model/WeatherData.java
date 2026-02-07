package airmonitor.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherData {
    private String cityName;
    private double temperature;
    private int humidity;
    private double lat;
    private double lon;

    @JsonProperty("name")
    public void setCityName(String cityName) { this.cityName = cityName; }

    @JsonProperty("main")
    private void unpackMain(Map<String, Object> main) {
        this.temperature = (double) main.get("temp");
        this.humidity = (int) main.get("humidity");
    }

    public String getCityName() { return cityName; }
    public double getTemperature() { return temperature; }
    public int getHumidity() { return humidity; }


    @JsonProperty("coord")
    private void unpackCoord(Map<String, Double> coord) {
        this.lat = coord.get("lat");
        this.lon = coord.get("lon");
    }

    public double getLat() { return lat; }
    public double getLon() { return lon; }
}