package airmonitor.Model;

public class AirQualityPoint {
    private final long timestamp;
    private final double pm25;
    private final double pm10;
    private final int aqi;

    public AirQualityPoint(long timestamp, double pm25, double pm10, int aqi) {
        this.timestamp = timestamp;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.aqi = aqi;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getPm25() {
        return pm25;
    }

    public double getPm10() {
        return pm10;
    }

    public int getAqi() {
        return aqi;
    }
}

