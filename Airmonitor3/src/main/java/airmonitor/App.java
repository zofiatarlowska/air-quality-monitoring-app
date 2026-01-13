package airmonitor;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import airmonitor.Api.WeatherApiClient;
import airmonitor.Model.AirQualityPoint;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import airmonitor.Model.ComparisonRow;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.control.ScrollPane;
import javafx.stage.StageStyle;


public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.initStyle(StageStyle.DECORATED);
        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #f4f4f4;");
        root.setFillWidth(true);

        Label titleLabel = new Label("MONITOR POGODY I POWIETRZA");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Pole wyboru miasta
        TextField cityField = new TextField();
        cityField.setFocusTraversable(false);
        cityField.setPromptText("Wpisz miasto...");
        cityField.setMaxWidth(200);

        // FUNKCJONALNOŚĆ 6: Wybór profilu użytkownika
        Label profileLabel = new Label("Twój profil:");
        ComboBox<String> profileBox = new ComboBox<>(FXCollections.observableArrayList(
                "Standardowy", "Alergik / Astmatyk", "Małe dziecko"
        ));
        profileBox.setValue("Standardowy"); // domyślna wartość

        Button checkButton = new Button("Sprawdź stan");
        checkButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");

        Button history24hButton = new Button("Wykres: ostatnie 24h (PM2.5)");
        history24hButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");

        Button history7dButton = new Button("Wykres: ostatnie 7 dni (PM2.5)");
        history7dButton.setStyle("-fx-background-color: #6A1B9A; -fx-text-fill: white; -fx-font-weight: bold;");

// Wykres PM2.5
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Czas");
        xAxis.setAutoRanging(false);       // my ustawimy ręcznie zakres
        xAxis.setForceZeroInRange(false);  // nie wciskaj zera jeśli nie trzeba
        xAxis.setMinorTickVisible(false);  // usuń te drobne kreseczki
        xAxis.setTickMarkVisible(true);


        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("PM2.5 (µg/m³)");

        LineChart<Number, Number> pmChart = new LineChart<>(xAxis, yAxis);
        pmChart.setCreateSymbols(false);
        pmChart.setLegendVisible(false);
        pmChart.setAnimated(false);
        pmChart.setHorizontalGridLinesVisible(true);
        pmChart.setVerticalGridLinesVisible(false);


        // Sekcja wyników
        Label tempLabel = new Label();
        tempLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");

        // POWRÓT WILGOTNOŚCI
        Label detailsLabel = new Label();
        detailsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        Label airLabel = new Label();
        airLabel.setStyle("-fx-font-size: 14px; -fx-padding: 10 0 0 0;");

        Label recommendationLabel = new Label();
        recommendationLabel.setWrapText(true);
        recommendationLabel.setStyle("-fx-font-style: italic; -fx-text-alignment: center; -fx-padding: 10;");

        checkButton.setOnAction(event -> {
            String city = cityField.getText();
            String profile = profileBox.getValue();
            WeatherApiClient client = new WeatherApiClient();

            try {
                // Pobieranie danych (używamy lat/lon w tle do smogu)
                airmonitor.Model.WeatherData weather = client.fetchCurrentWeather(city);
                airmonitor.Model.AirQualityData air = client.fetchAirQuality(weather.getLat(), weather.getLon());

                // 1. Wyświetlanie temperatury
                tempLabel.setText(String.format("%.1f°C", weather.getTemperature()));

                // 2. Wyświetlanie wilgotności (Przywrócone!)
                detailsLabel.setText(String.format("Wilgotność: %d%% | Miasto: %s",
                        weather.getHumidity(), weather.getCityName()));

                // 3. Wyświetlanie pyłów
                airLabel.setText(String.format("PM2.5: %.1f | PM10: %.1f", air.getPm25(), air.getPm10()));

                // 4. LOGIKA REKOMENDACJI (Zależna od profilu)
                double pmLimit = profile.equals("Standardowy") ? 25.0 : 15.0;

                if (air.getPm25() < pmLimit) {
                    airLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    recommendationLabel.setText("✅ Warunki dobre dla profilu: " + profile + ". Możesz wyjść na zewnątrz.");
                } else if (air.getPm25() < pmLimit * 2) {
                    airLabel.setStyle("-fx-text-fill: #F57C00; -fx-font-weight: bold;");
                    recommendationLabel.setText("⚠️ Powietrze umiarkowane. " + (profile.equals("Standardowy") ? "Dla Ciebie OK." : "Zalecana ostrożność!"));
                } else {
                    airLabel.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                    recommendationLabel.setText("❌ UWAGA! Wysokie stężenie pyłów. Nie zaleca się wychodzenia.");
                }

            } catch (Exception e) {
                tempLabel.setText("Błąd!");
                detailsLabel.setText("");
                airLabel.setText("Nie znaleziono miejscowości.");
                recommendationLabel.setText("");
            }
        });

        history24hButton.setOnAction(e -> {
            String city = cityField.getText();
            WeatherApiClient client = new WeatherApiClient();

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    // 1) Najpierw pobierz współrzędne (z current weather)
                    airmonitor.Model.WeatherData weather = client.fetchCurrentWeather(city);

                    long now = Instant.now().getEpochSecond();
                    long start = now - 24 * 3600;

                    // 2) Historia smogu 24h
                    List<AirQualityPoint> points = client.fetchAirQualityHistory(weather.getLat(), weather.getLon(), start, now);

                    Platform.runLater(() -> drawPm25ChartSamples(xAxis, pmChart, points, "Ostatnie 24h"));

                    return null;
                }
            };

            task.setOnFailed(ev -> Platform.runLater(() -> {
                pmChart.getData().clear();
                pmChart.setTitle("Błąd pobierania historii 24h");
            }));

            new Thread(task).start();
        });

        history7dButton.setOnAction(e -> {
            String city = cityField.getText();
            WeatherApiClient client = new WeatherApiClient();

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    airmonitor.Model.WeatherData weather = client.fetchCurrentWeather(city);

                    long now = Instant.now().getEpochSecond();
                    long start = now - 7L * 24 * 3600;

                    // Historia smogu 7 dni (godzinowa), potem zrobimy średnią dzienną
                    List<AirQualityPoint> points = client.fetchAirQualityHistory(weather.getLat(), weather.getLon(), start, now);

                    Platform.runLater(() -> drawPm25ChartDailyAverage(xAxis, pmChart, points, "Ostatnie 7 dni (średnia dzienna)"));
                    return null;
                }
            };

            task.setOnFailed(ev -> Platform.runLater(() -> {
                pmChart.getData().clear();
                pmChart.setTitle("Błąd pobierania historii 7 dni");
            }));

            new Thread(task).start();
        });
        // ===== FUNKCJONALNOŚĆ 5: PORÓWNANIE MIAST (2-3) =====
        Label compareTitle = new Label("Porównanie miast (2–3):");
        compareTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        TextField cityField1 = new TextField();
        cityField1.setPromptText("Miasto 1");
        cityField1.setMaxWidth(160);

        TextField cityField2 = new TextField();
        cityField2.setPromptText("Miasto 2");
        cityField2.setMaxWidth(160);

        TextField cityField3 = new TextField();
        cityField3.setPromptText("Miasto 3 (opcjonalnie)");
        cityField3.setMaxWidth(160);

        HBox citiesBox = new HBox(10, cityField1, cityField2, cityField3);
        citiesBox.setAlignment(Pos.CENTER);

        Button compareButton = new Button("Porównaj");
        compareButton.setStyle("-fx-background-color: #111; -fx-text-fill: white; -fx-font-weight: bold;");

// Tabela porównawcza
        TableView<ComparisonRow> compareTable = new TableView<>();
        compareTable.setMaxWidth(Double.MAX_VALUE);
        compareTable.setPrefHeight(160);

        TableColumn<ComparisonRow, String> colCity = new TableColumn<>("Miasto");
        colCity.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCity()));
        colCity.setPrefWidth(140);

        TableColumn<ComparisonRow, Double> colTemp = new TableColumn<>("Temp (°C)");
        colTemp.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getTemperature()));
        colTemp.setPrefWidth(90);

        TableColumn<ComparisonRow, Integer> colHum = new TableColumn<>("Wilg (%)");
        colHum.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getHumidity()));
        colHum.setPrefWidth(80);

        TableColumn<ComparisonRow, Integer> colAqi = new TableColumn<>("AQI (1–5)");
        colAqi.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getAqi()));
        colAqi.setPrefWidth(90);

        TableColumn<ComparisonRow, Double> colPm25 = new TableColumn<>("PM2.5");
        colPm25.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getPm25()));
        colPm25.setPrefWidth(80);

        TableColumn<ComparisonRow, Double> colPm10 = new TableColumn<>("PM10");
        colPm10.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getPm10()));
        colPm10.setPrefWidth(80);

        compareTable.getColumns().addAll(colCity, colTemp, colHum, colAqi, colPm25, colPm10);

// Wykres (BarChart) PM2.5
        CategoryAxis bx = new CategoryAxis();
        bx.setLabel("Miasto");

        NumberAxis by = new NumberAxis();
        by.setLabel("PM2.5 (µg/m³)");

        BarChart<String, Number> pm25Bar = new BarChart<>(bx, by);
        pm25Bar.setTitle("Porównanie PM2.5");
        pm25Bar.setLegendVisible(false);
        pm25Bar.setAnimated(false);
        pm25Bar.setPrefHeight(260);
        pm25Bar.setMaxWidth(Double.MAX_VALUE);

// Obsługa przycisku "Porównaj"
        compareButton.setOnAction(ev -> {
            List<String> cities = new ArrayList<>();
            if (!cityField1.getText().isBlank()) cities.add(cityField1.getText().trim());
            if (!cityField2.getText().isBlank()) cities.add(cityField2.getText().trim());
            if (!cityField3.getText().isBlank()) cities.add(cityField3.getText().trim());

            if (cities.size() < 2) {
                // prosta informacja w tabeli
                compareTable.setItems(FXCollections.observableArrayList(
                        new ComparisonRow("Wpisz min. 2 miasta", 0, 0, 0, 0, 0)
                ));
                pm25Bar.getData().clear();
                return;
            }

            WeatherApiClient client = new WeatherApiClient();

            Task<List<ComparisonRow>> task = new Task<>() {
                @Override
                protected List<ComparisonRow> call() throws Exception {
                    List<ComparisonRow> rows = new ArrayList<>();
                    for (String c : cities) {
                        airmonitor.Model.WeatherData w = client.fetchCurrentWeather(c);
                        airmonitor.Model.AirQualityData a = client.fetchAirQuality(w.getLat(), w.getLon());
                        rows.add(new ComparisonRow(
                                w.getCityName(),
                                w.getTemperature(),
                                w.getHumidity(),
                                a.getAqi(),
                                a.getPm25(),
                                a.getPm10()
                        ));
                    }
                    return rows;
                }
            };

            task.setOnSucceeded(done -> {
                List<ComparisonRow> rows = task.getValue();
                ObservableList<ComparisonRow> data = FXCollections.observableArrayList(rows);
                compareTable.setItems(data);

                // wykres PM2.5
                pm25Bar.getData().clear();
                XYChart.Series<String, Number> s = new XYChart.Series<>();
                for (ComparisonRow r : rows) {
                    s.getData().add(new XYChart.Data<>(r.getCity(), r.getPm25()));
                }
                pm25Bar.getData().add(s);
            });

            task.setOnFailed(f -> {
                compareTable.setItems(FXCollections.observableArrayList(
                        new ComparisonRow("Błąd pobierania danych", 0, 0, 0, 0, 0)
                ));
                pm25Bar.getData().clear();
            });

            new Thread(task).start();
        });


        // Dodawanie wszystkiego do widoku
        root.getChildren().addAll(
                titleLabel,
                cityField,
                profileLabel, profileBox,
                checkButton,
                tempLabel, detailsLabel,
                airLabel, recommendationLabel,
                history24hButton,
                history7dButton,
                pmChart,
                compareTitle,
                citiesBox,
                compareButton,
                compareTable,
                pm25Bar
        );

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setPadding(new Insets(60, 0, 0, 0));
        scrollPane.setVvalue(0.0);
        scrollPane.setHvalue(0.0);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Scene scene = new Scene(scrollPane, 1000, 900);
        primaryStage.setScene(scene);
        Platform.runLater(() -> scrollPane.setVvalue(0.0));
        primaryStage.show();
        Platform.runLater(() -> {
            scrollPane.setVvalue(0.0);
            scrollPane.setHvalue(0.0);
            root.requestFocus(); // zabiera focus z TextField
        });

    }

    public static void main(String[] args) {
        launch(args);
    }

    private static final DateTimeFormatter HOUR_FMT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private void drawPm25ChartSamples(NumberAxis xAxis, LineChart<Number, Number> chart,
                                      List<AirQualityPoint> points, String title) {
        chart.getData().clear();
        chart.setTitle("PM2.5 – " + title);

        lastPlottedPoints = points;

        int n = points.size();
        if (n == 0) return;

        // 1) Zakres osi X: dokładnie 0..n-1
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(n - 1);

        // 2) Chcemy ~8 etykiet na osi
        int labelCount = 8;
        int step = Math.max(1, (n - 1) / labelCount);

        // 3) TickUnit = step (czyli skoki co kilka punktów)
        xAxis.setTickUnit(step);

        // 4) Format etykiet: pokaż HH:mm tylko na tickach, reszta pusta
        xAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number value) {
                int idx = (int) Math.round(value.doubleValue());
                if (idx < 0 || idx >= lastPlottedPoints.size()) return "";

                long ts = lastPlottedPoints.get(idx).getTimestamp();
                return java.time.Instant.ofEpochSecond(ts)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime()
                        .toString()
                        .substring(0, 5); // HH:mm
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });

        // Dane
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (int i = 0; i < n; i++) {
            series.getData().add(new XYChart.Data<>(i, points.get(i).getPm25()));
        }

        chart.getData().add(series);
    }




    private void drawPm25ChartDailyAverage(NumberAxis xAxis, LineChart<Number, Number> chart,
                                           List<AirQualityPoint> points, String title) {
        // grupowanie po dacie i liczenie średniej
        Map<LocalDate, double[]> acc = new TreeMap<>();
        ZoneId zone = ZoneId.systemDefault();

        for (AirQualityPoint p : points) {
            LocalDate date = Instant.ofEpochSecond(p.getTimestamp()).atZone(zone).toLocalDate();
            acc.putIfAbsent(date, new double[]{0.0, 0.0}); // sum, count
            acc.get(date)[0] += p.getPm25();
            acc.get(date)[1] += 1.0;
        }

        // przygotuj listę dni + wartości
        List<LocalDate> days = new ArrayList<>(acc.keySet());
        List<Double> values = new ArrayList<>(days.size());
        for (LocalDate d : days) {
            double[] sc = acc.get(d);
            values.add(sc[0] / sc[1]);
        }

        chart.getData().clear();
        chart.setTitle("PM2.5 – " + title);

        int n = days.size();
        if (n == 0) return;

        // oś X: dokładnie 0..n-1, równo rozłożone ticki
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(n - 1);

        int labelCount = Math.min(7, n);          // max 7 etykiet (bo 7 dni)
        int step = Math.max(1, (n - 1) / labelCount);
        xAxis.setTickUnit(step);

        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd.MM");

        xAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number value) {
                int idx = (int) Math.round(value.doubleValue());
                if (idx < 0 || idx >= days.size()) return "";
                return dayFmt.format(days.get(idx));
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });

        // dane do wykresu
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (int i = 0; i < n; i++) {
            series.getData().add(new XYChart.Data<>(i, values.get(i)));
        }

        chart.getData().add(series);
    }

    private List<AirQualityPoint> lastPlottedPoints = List.of();

}
