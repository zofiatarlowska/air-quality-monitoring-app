package airmonitor.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AirQualityData {
    private double pm25;
    private double pm10;
    private int aqi; // Indeks od 1 do 5

    @JsonProperty("list")
    private void unpackList(List<Map<String, Object>> list) {
        Map<String, Object> firstItem = list.get(0);
        Map<String, Object> main = (Map<String, Object>) firstItem.get("main");
        this.aqi = (int) main.get("aqi");

        Map<String, Object> components = (Map<String, Object>) firstItem.get("components");
        this.pm25 = ((Number) components.get("pm2_5")).doubleValue();
        this.pm10 = ((Number) components.get("pm10")).doubleValue();
    }

    public double getPm25() { return pm25; }
    public double getPm10() { return pm10; }
    public int getAqi() { return aqi; }
}