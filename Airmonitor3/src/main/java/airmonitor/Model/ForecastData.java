package airmonitor.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastData {

    @JsonProperty("list")
    private List<ForecastItem> forecastList;

    public List<ForecastItem> getForecastList() {
        return forecastList;
    }

    public void setForecastList(List<ForecastItem> forecastList) {
        this.forecastList = forecastList;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForecastItem {
        private long dt;
        private double temp;
        private String icon;

        @JsonProperty("dt")
        public long getDt() { return dt; }
        public void setDt(long dt) { this.dt = dt; }

        @JsonProperty("main")
        private void unpackMain(Map<String, Object> main) {
            this.temp = ((Number) main.get("temp")).doubleValue();
        }

        @JsonProperty("weather")
        private void unpackWeather(List<Map<String, Object>> weather) {
            if (weather != null && !weather.isEmpty()) {
                this.icon = (String) weather.get(0).get("icon");
            }
        }

        public double getTemp() { return temp; }
        public String getIcon() { return icon; }

        public String getIconUrl() {
            return "https://openweathermap.org/img/wn/" + icon + ".png";
        }
    }
}
