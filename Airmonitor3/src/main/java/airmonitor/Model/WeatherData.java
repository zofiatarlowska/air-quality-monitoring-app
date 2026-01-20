package airmonitor.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherData {
    private String cityName;
    private double temperature;
    private int humidity;
    private double lat;
    private double lon;
    private String iconCode;

    @JsonProperty("name")
    public void setCityName(String cityName) { this.cityName = cityName; }

    @JsonProperty("main")
    private void unpackMain(Map<String, Object> main) {

        this.temperature = ((Number) main.get("temp")).doubleValue();
        this.humidity = ((Number) main.get("humidity")).intValue();
    }


    @JsonProperty("weather")
    private void unpackWeather(List<Map<String, Object>> weather) {
        if (weather != null && !weather.isEmpty()) {
            this.iconCode = (String) weather.get(0).get("icon");
        }
    }

    // Metoda generująca link do obrazka ikony z serwerów OpenWeatherMap
    public String getIconUrl() {
        return "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
    }

    public String getCityName() { return cityName; }
    public double getTemperature() { return temperature; }
    public int getHumidity() { return humidity; }


    @JsonProperty("coord")
    private void unpackCoord(Map<String, Double> coord) {
        this.lat = coord.get("lat");
        this.lon = coord.get("lon");
    }

    public double getTemperatureInUnit(String unit) {
        switch (unit) {
            case "Kelvin (K)":
                return temperature + 273.15;
            case "Fahrenheit (°F)":
                return (temperature * 9/5) + 32;
            default: // Celsius
                return temperature;
        }
    }

    public double getLat() { return lat; }
    public double getLon() { return lon; }
}