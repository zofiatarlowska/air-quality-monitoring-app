package airmonitor.Model;

public class ComparisonRow {
    private final String city;
    private final double temperature;
    private final int humidity;
    private final int aqi;
    private final double pm25;
    private final double pm10;

    public ComparisonRow(String city, double temperature, int humidity, int aqi, double pm25, double pm10) {
        this.city = city;
        this.temperature = temperature;
        this.humidity = humidity;
        this.aqi = aqi;
        this.pm25 = pm25;
        this.pm10 = pm10;
    }

    public String getCity() { return city; }
    public double getTemperature() { return temperature; }
    public int getHumidity() { return humidity; }
    public int getAqi() { return aqi; }
    public double getPm25() { return pm25; }
    public double getPm10() { return pm10; }
}
