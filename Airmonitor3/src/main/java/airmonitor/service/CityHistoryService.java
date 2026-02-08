package airmonitor.service;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.Arrays;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class CityHistoryService {
    private final Preferences prefs;
    private final int limit;
    private final ObservableList<String> cityHistory = FXCollections.observableArrayList();

    public CityHistoryService(Class<?> prefsOwner, int limit) {
        this.prefs = Preferences.userNodeForPackage(prefsOwner);
        this.limit = limit;
        load();
    }

    public ObservableList<String> items() {
        return cityHistory;
    }

    public void addCity(String city) {
        if (city == null) return;
        String c = city.trim();
        if (c.isBlank()) return;

        cityHistory.removeIf(x -> x.equalsIgnoreCase(c));
        cityHistory.add(0, c);

        while (cityHistory.size() > limit) {
            cityHistory.remove(cityHistory.size() - 1);
        }
        save();
    }

    private void load() {
        String raw = prefs.get("cityHistory", "");
        cityHistory.setAll(
                Arrays.stream(raw.split("\\|"))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .limit(limit)
                        .collect(Collectors.toList())
        );
    }

    private void save() {
        String raw = cityHistory.stream()
                .limit(limit)
                .collect(Collectors.joining("|"));
        prefs.put("cityHistory", raw);
    }
}
