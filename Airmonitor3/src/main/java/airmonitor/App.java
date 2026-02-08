package airmonitor;

import airmonitor.Api.WeatherApiClient;
import airmonitor.Model.AirQualityPoint;
import airmonitor.Model.ComparisonRow;
import airmonitor.service.CityHistoryService;
import airmonitor.service.ExportService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javafx.scene.layout.StackPane;
import javafx.beans.value.ChangeListener;

public class App extends Application {
    private List<airmonitor.Model.AirQualityPoint> lastHistory7d = java.util.List.of();
    private String lastHistoryCity = "";
    private List<AirQualityPoint> lastPlottedPoints = List.of();

    private final String PRIMARY = "#2c3e50";
    private final String ACCENT = "#2196F3";
    private final String SUCCESS = "#4CAF50";
    private final String INFO = "#2980b9";

    private TableColumn<ComparisonRow, Double> colTemp;
    private final ImageView weatherIconView = new ImageView();
    private final HBox forecastContainer = new HBox(15);
    private Timeline autoRefresh;
    private static final int HISTORY_LIMIT = 6;
    private Popup toastPopup;
    private String lastToastKey = "";
    private Stage primaryStageRef;
    private TextField cityField;
    private ComboBox<String> profileBox;
    private ComboBox<String> unitBox;
    private Label tempLabel, detailsLabel, airLabel, recommendationLabel;
    private NumberAxis xAxis;
    private LineChart<Number, Number> pmChart;
    private CityHistoryService cityHistoryService;

    private enum TempUnit {
        C("Celsius (°C)", "°C"),
        K("Kelvin (K)", "K"),
        F("Fahrenheit (°F)", "°F");

        final String label;
        final String symbol;

        TempUnit(String label, String symbol) {
            this.label = label;
            this.symbol = symbol;
        }

        static TempUnit fromLabel(String label) {
            for (TempUnit u : values()) {
                if (u.label.equals(label)) return u;
            }
            return C;
        }
    }
    private boolean isSensitiveProfile(String profile) {
        return "Alergik / Astmatyk".equals(profile) || "Małe dziecko".equals(profile);
    }

    private record Thresholds(double safe, double warning, double danger) {}

    private Thresholds thresholdsFor(String profile) {
        boolean s = isSensitiveProfile(profile);
        return new Thresholds(
                s ? 12.0 : 20.0,
                s ? 20.0 : 35.0,
                s ? 35.0 : 55.0
        );
    }

    private record Advice(String text, String color, String level) {}

    private Advice buildAdvice(String profile, double pm25) {
        Thresholds t = thresholdsFor(profile);
        boolean sensitive = isSensitiveProfile(profile);

        if (pm25 < t.safe()) {
            return new Advice(
                    "🌿 Jest super! Idealna pora na spacer lub bieganie. Powietrze dla grupy '" + profile + "' jest dziś wzorowe.",
                    "#2e7d32",
                    "info"
            );
        } else if (pm25 < t.warning()) {
            String msg = sensitive
                    ? "☁️ Nie jest idealnie. Jeśli jesteś wrażliwy, lepiej skróć dzisiejszy spacer."
                    : "🏃 Można wychodzić, ale bez bicia rekordów. Powietrze jest takie sobie, poczujesz to przy mocniejszym treningu.";
            return new Advice(msg, "#f57f17", "warn");
        } else {
            String msg = sensitive
                    ? "😷 Uwaga! Fatalna jakość powietrza. Jako " + profile + " lepiej dziś odpuść wyjścia i nie otwieraj okien."
                    : "🚫 Smog daje o sobie znać. Lepiej zostać w domu, wietrzenie mieszkania to teraz słaby pomysł.";
            return new Advice(msg, "#c62828", "error");
        }
    }


    @Override
    public void start(Stage primaryStage) {
        this.primaryStageRef = primaryStage;
        VBox mainContainer = new VBox(25);
        mainContainer.getStyleClass().add("app-background");
        mainContainer.setPadding(new Insets(30));
        mainContainer.setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label("MONITOR POGODY I POWIETRZA");
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
        titleLabel.getStyleClass().add("app-title");
        CheckBox darkToggle = new CheckBox("🌙 Ciemny tryb");
        darkToggle.getStyleClass().add("mode-toggle");

        // PANEL WYSZUKIWANIA
        VBox searchCard = new VBox(15);
        searchCard.getStyleClass().add("card");
        searchCard.setPadding(new Insets(20));
        searchCard.setAlignment(Pos.CENTER);
        searchCard.setMaxWidth(700);

        cityField = new TextField();
        cityHistoryService = new CityHistoryService(App.class, HISTORY_LIMIT);
        ListView<String> historyList = new ListView<>(cityHistoryService.items());
        historyList.visibleProperty().bind(Bindings.isEmpty(historyList.getItems()).not());
        historyList.managedProperty().bind(Bindings.isEmpty(historyList.getItems()).not());
        int visibleRows = 3;

        historyList.setFixedCellSize(32);
        historyList.setPrefHeight(visibleRows * historyList.getFixedCellSize() + 2);
        historyList.setMaxHeight(visibleRows * historyList.getFixedCellSize() + 2);

        historyList.setMaxWidth(350);

        historyList.setStyle("-fx-background-radius: 12; -fx-border-radius: 12;");

        cityField.setPromptText("Wpisz miasto...");
        cityField.setStyle("-fx-background-radius: 20; -fx-padding: 10 15; -fx-border-color: #dcdde1; -fx-border-radius: 20;");
        cityField.setMaxWidth(350);


        Button locBtn = new Button("📍 Moja lokalizacja");
        locBtn.setStyle("-fx-background-radius: 20; -fx-background-color: #ecf0f1; -fx-cursor: hand;");
        locBtn.setOnAction(e -> {
            Task<String> locTask = new Task<>() {
                @Override protected String call() throws Exception {
                    return new WeatherApiClient().fetchCityByIp();
                }
            };
            locTask.setOnSucceeded(ev -> {
                cityField.setText(locTask.getValue());
                handleCheck();
            });
            new Thread(locTask).start();
        });

        HBox settingsBox = new HBox(20);
        settingsBox.setAlignment(Pos.CENTER);
        profileBox = new ComboBox<>(FXCollections.observableArrayList("Standardowy", "Alergik / Astmatyk", "Małe dziecko"));
        profileBox.setValue("Standardowy");
        unitBox = new ComboBox<>(FXCollections.observableArrayList("Celsius (°C)", "Kelvin (K)", "Fahrenheit (°F)"));
        unitBox.setValue("Celsius (°C)");
        settingsBox.getChildren().addAll(new Label("Profil:"), profileBox, new Label("Jednostka:"), unitBox);

        Button checkButton = new Button("SPRAWDŹ STAN");
        styleButton(checkButton, ACCENT);
        checkButton.setMinWidth(220);
        historyList.setOnMouseClicked(e -> {
            String selected = historyList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                cityField.setText(selected);
                checkButton.fire();
            }
        });


        VBox cityBox = new VBox(8, new HBox(10, cityField, locBtn), historyList);
        cityBox.setAlignment(Pos.CENTER);

        searchCard.getChildren().addAll(cityBox, settingsBox, checkButton);
        ((HBox)cityBox.getChildren().get(0)).setAlignment(Pos.CENTER);


        VBox resultCard = new VBox(15);
        resultCard.getStyleClass().add("card");
        resultCard.setPadding(new Insets(25));
        resultCard.setAlignment(Pos.CENTER);
        resultCard.setMaxWidth(700);
        resultCard.setManaged(false);
        resultCard.setVisible(false);

        weatherIconView.setFitHeight(80);
        weatherIconView.setFitWidth(80);

        tempLabel = new Label();
        tempLabel.setStyle("-fx-font-size: 52px; -fx-font-weight: bold;");
        tempLabel.getStyleClass().add("temp-value");

        StackPane iconBadge = new StackPane(weatherIconView);
        iconBadge.setMinSize(90, 90);
        iconBadge.setPrefSize(90, 90);
        iconBadge.setMaxSize(90, 90);
        iconBadge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.85);" +
                        "-fx-background-radius: 45;" +
                        "-fx-border-color: rgba(0,0,0,0.08);" +
                        "-fx-border-radius: 45;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.20), 10, 0, 0, 3);"
        );

        HBox tempIconBox = new HBox(iconBadge, tempLabel);
        tempIconBox.setSpacing(15);
        tempIconBox.setAlignment(Pos.CENTER);



        detailsLabel = new Label();
        detailsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
        airLabel = new Label();
        recommendationLabel = new Label();
        recommendationLabel.setWrapText(true);
        recommendationLabel.setStyle("-fx-font-style: italic; -fx-text-alignment: center;");

        // prognoza pogody
        Label forecastTitle = new Label("Prognoza na najbliższe dni:");
        forecastTitle.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");
        forecastContainer.setAlignment(Pos.CENTER);

        resultCard.getChildren().addAll(tempIconBox, detailsLabel, airLabel, recommendationLabel, new Separator(), forecastTitle, forecastContainer);

        // wykresy
        VBox chartCard = new VBox(15);
        chartCard.getStyleClass().add("card");
        chartCard.setPadding(new Insets(20));
        chartCard.setMaxWidth(950);

        HBox chartButtons = new HBox(15);
        chartButtons.setAlignment(Pos.CENTER);
        Button h24Btn = new Button("Wykres: ostatnie 24h");
        Button h7dBtn = new Button("Wykres: ostatnie 7 dni");
        styleButton(h24Btn, SUCCESS);
        styleButton(h7dBtn, INFO);
        chartButtons.getChildren().addAll(h24Btn, h7dBtn);

        xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("PM2.5 (µg/m³)");
        pmChart = new LineChart<>(xAxis, yAxis);
        pmChart.setCreateSymbols(true);
        pmChart.setPrefHeight(350);
        pmChart.setAnimated(false);
        chartCard.getChildren().addAll(chartButtons, pmChart);

        // PANEL PORÓWNANIA
        VBox compareCard = new VBox(15);
        compareCard.getStyleClass().add("card");
        compareCard.setPadding(new Insets(20));
        compareCard.setMaxWidth(950);

        HBox compInputs = new HBox(10);
        compInputs.setAlignment(Pos.CENTER);
        TextField c1 = new TextField(); c1.setPromptText("Miasto 1");
        TextField c2 = new TextField(); c2.setPromptText("Miasto 2");
        TextField c3 = new TextField(); c3.setPromptText("Miasto 3");
        compInputs.getChildren().addAll(c1, c2, c3);

        Button compareBtn = new Button("PORÓWNAJ MIASTA");
        styleButton(compareBtn, PRIMARY);

        TableView<ComparisonRow> compareTable = new TableView<>();
        setupFullTable(compareTable);

        compareCard.getChildren().addAll(compInputs, compareBtn, compareTable);

        // eksport
        HBox exportBox = new HBox(20);
        exportBox.setAlignment(Pos.CENTER);
        Button csvBtn = new Button("Eksport CSV (7 dni)");
        Button pngBtn = new Button("Eksport PNG (Wykres)");
        Button pdfBtn = new Button("Eksport PDF (Raport)");
        styleButton(csvBtn, "#455A64");
        styleButton(pngBtn, "#00897B");
        styleButton(pdfBtn, "#5D4037");
        exportBox.getChildren().addAll(csvBtn, pngBtn, pdfBtn);

        checkButton.setOnAction(e -> {
            resultCard.setVisible(true);
            resultCard.setManaged(true);

            String city = cityField.getText();
            cityHistoryService.addCity(city);
            handleCheck();
        });

        autoRefresh = new Timeline(new KeyFrame(Duration.minutes(10), e -> {
            if (!cityField.getText().isEmpty()) checkButton.fire();
        }));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();

        h24Btn.setOnAction(e -> handleHistory(24, false));
        h7dBtn.setOnAction(e -> handleHistory(168, true));
        compareBtn.setOnAction(e -> handleComparison(c1, c2, c3, unitBox.getValue(), compareTable));
        csvBtn.setOnAction(e -> handleCsvExport(primaryStage));
        pngBtn.setOnAction(e -> handlePngExport(pmChart, primaryStage));
        pdfBtn.setOnAction(e -> handlePdfExport(tempLabel.getText(), primaryStage));

        StackPane topBar = new StackPane();
        topBar.setMaxWidth(700);
        topBar.setPadding(new Insets(0, 10, 0, 10));
        topBar.getChildren().add(titleLabel);
        StackPane.setAlignment(titleLabel, Pos.CENTER);
        topBar.getChildren().add(darkToggle);
        StackPane.setAlignment(darkToggle, Pos.CENTER_RIGHT);


        mainContainer.getChildren().addAll(
                topBar,
                searchCard,
                resultCard,
                chartCard,
                compareCard,
                exportBox
        );


        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #f0f2f5;");
        Rectangle2D vb = Screen.getPrimary().getVisualBounds();

        double w = vb.getWidth() * 0.80;
        double h = vb.getHeight() * 0.85;

        w = Math.max(900, w);
        h = Math.max(700, h);

        Scene scene = new Scene(scrollPane, w, h);
        scene.getStylesheets().add(
                getClass().getResource("/css/light.css").toExternalForm()
        );
        darkToggle.selectedProperty().addListener((obs, oldV, isDark) -> {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(
                    getClass().getResource(
                            isDark ? "/css/dark.css" : "/css/light.css"
                    ).toExternalForm()
            );
        });
        primaryStage.setTitle("Monitor Pogody i Powietrza");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    private void styleButton(Button btn, String color) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 25; -fx-padding: 12 25; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: derive(" + color + ", 15%); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 25; -fx-padding: 12 25; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 25; -fx-padding: 12 25; -fx-cursor: hand;"));
    }

    private void setupFullTable(TableView<ComparisonRow> table) {
        TableColumn<ComparisonRow, String> colCity = new TableColumn<>("Miasto");
        colCity.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getCity()));
        colTemp = new TableColumn<>("Temp");
        colTemp.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTemperature()));
        TableColumn<ComparisonRow, Integer> colHum = new TableColumn<>("Wilg (%)");
        colHum.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getHumidity()));
        TableColumn<ComparisonRow, Integer> colAqi = new TableColumn<>("AQI (1–5)");
        colAqi.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getAqi()));
        TableColumn<ComparisonRow, Double> colPm25 = new TableColumn<>("PM2.5");
        colPm25.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getPm25()));

        colPm25.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(String.format("%.1f", item));
                    if (item < 20) setStyle("-fx-background-color: #c8e6c9; -fx-text-fill: #2e7d32;");
                    else if (item < 50) setStyle("-fx-background-color: #fff9c4; -fx-text-fill: #f57f17;");
                    else setStyle("-fx-background-color: #ffcdd2; -fx-text-fill: #c62828;");
                }
            }
        });

        TableColumn<ComparisonRow, Double> colPm10 = new TableColumn<>("PM10");
        colPm10.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getPm10()));

        table.getColumns().addAll(colCity, colTemp, colHum, colAqi, colPm25, colPm10);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(40.0);
        double totalHeight = (40.0 * 3) + 45.0;
        table.setPrefHeight(totalHeight); table.setMinHeight(totalHeight); table.setMaxHeight(totalHeight);
        table.setStyle("-fx-background-insets: 0; -fx-padding: 0; -fx-table-cell-border-color: transparent; -fx-background-color: white;");
        table.getStylesheets().add("data:text/css,.table-view .scroll-bar:vertical { -fx-opacity: 0; -fx-pref-width: 0; }");
    }

    private void handleCheck() {
        String city = cityField.getText();
        if (city == null || city.isBlank()) return;
        String selectedUnit = unitBox.getValue();
        String profile = profileBox.getValue();

        WeatherApiClient client = new WeatherApiClient();

        Task<Void> combinedTask = new Task<>() {
            @Override protected Void call() throws Exception {
                var weather = client.fetchCurrentWeather(city);
                var air = client.fetchAirQuality(weather.getLat(), weather.getLon());
                var forecast = client.fetchForecast(city);

                Platform.runLater(() -> {
                    weatherIconView.setImage(new Image(weather.getIconUrl()));

                    double displayTemp = weather.getTemperatureInUnit(selectedUnit);
                    String unitSymbol = TempUnit.fromLabel(selectedUnit).symbol;

                    tempLabel.setText(String.format("%.1f %s", displayTemp, unitSymbol));
                    detailsLabel.setText(String.format("Wilgotność: %d%% | Miasto: %s",
                            weather.getHumidity(), weather.getCityName()));

                    double pm25 = air.getPm25();
                    Advice advice = buildAdvice(profile, pm25);

                    maybeShowAirToast(primaryStageRef, weather.getCityName(), profile, pm25);

                    airLabel.setText(String.format("PM2.5: %.1f | PM10: %.1f", air.getPm25(), air.getPm10()));
                    airLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + advice.color() + ";");

                    recommendationLabel.setText(advice.text());
                    recommendationLabel.setStyle("-fx-font-style: italic; -fx-font-weight: bold; -fx-text-fill: " + advice.color() + ";");

                    // prognoza 5-dniowa
                    forecastContainer.getChildren().clear();
                    for (int i = 0; i < forecast.getForecastList().size(); i += 8) {
                        var item = forecast.getForecastList().get(i);

                        VBox dayBox = new VBox(5);
                        dayBox.setAlignment(Pos.CENTER);

                        String day = Instant.ofEpochSecond(item.getDt())
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("dd.MM"));

                        Label dLabel = new Label(day);
                        ImageView iv = new ImageView(new Image(item.getIconUrl()));
                        iv.setFitWidth(40);
                        iv.setFitHeight(40);

                        Label tLabel = new Label(String.format("%.0f°C", item.getTemp()));
                        dayBox.getChildren().addAll(dLabel, iv, tLabel);

                        forecastContainer.getChildren().add(dayBox);
                    }
                });
                return null;
            }
        };

        combinedTask.setOnFailed(e ->
                Platform.runLater(() -> showError(combinedTask.getException()))
        );

        new Thread(combinedTask).start();
    }


    private void handleHistory(int hours, boolean is7d) {
        String city = cityField.getText();
        if (city == null || city.isBlank()) return;

        WeatherApiClient client = new WeatherApiClient();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                var w = client.fetchCurrentWeather(city);

                long now = Instant.now().getEpochSecond();
                long start = now - (hours * 3600L);

                List<AirQualityPoint> points =
                        client.fetchAirQualityHistory(w.getLat(), w.getLon(), start, now);

                Platform.runLater(() -> {
                    if (is7d) {
                        lastHistory7d = points;
                        lastHistoryCity = w.getCityName();
                        drawPm25ChartDailyAverage(xAxis, pmChart, points, "Trend 7-dniowy");
                    } else {
                        drawPm25ChartSamples(xAxis, pmChart, points, "Ostatnie 24 godziny");
                    }
                });
                return null;
            }
        };

        task.setOnFailed(e ->
                Platform.runLater(() -> showError(task.getException()))
        );

        new Thread(task).start();
    }


    private void handleComparison(TextField c1, TextField c2, TextField c3, String unit, TableView<ComparisonRow> table) {
        List<String> cities = new ArrayList<>();
        if(!c1.getText().isBlank()) cities.add(c1.getText().trim());
        if(!c2.getText().isBlank()) cities.add(c2.getText().trim());
        if(!c3.getText().isBlank()) cities.add(c3.getText().trim());
        if (cities.size() < 2) return;

        String unitSymbol = TempUnit.fromLabel(unit).symbol;
        colTemp.setText("Temp (" + unitSymbol + ")");

        WeatherApiClient client = new WeatherApiClient();
        Task<List<ComparisonRow>> task = new Task<>() {
            @Override protected List<ComparisonRow> call() throws Exception {
                List<ComparisonRow> rows = new ArrayList<>();
                for(String n : cities) {
                    var w = client.fetchCurrentWeather(n);
                    var a = client.fetchAirQuality(w.getLat(), w.getLon());
                    rows.add(new ComparisonRow(w.getCityName(), w.getTemperatureInUnit(unit), w.getHumidity(), a.getAqi(), a.getPm25(), a.getPm10()));
                }
                return rows;
            }
        };
        task.setOnSucceeded(e -> table.setItems(FXCollections.observableArrayList(task.getValue())));
        new Thread(task).start();
    }

    private void drawPm25ChartSamples(NumberAxis xAxis, LineChart<Number, Number> chart, List<AirQualityPoint> points, String title) {
        chart.getData().clear();
        chart.setTitle(title);
        lastPlottedPoints = points;
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0); xAxis.setUpperBound(points.size() - 1);
        xAxis.setTickUnit(Math.max(1, points.size() / 8));
        xAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override public String toString(Number v) {
                int i = v.intValue();
                if (i < 0 || i >= lastPlottedPoints.size()) return "";
                return Instant.ofEpochSecond(lastPlottedPoints.get(i).getTimestamp()).atZone(ZoneId.systemDefault()).toLocalTime().toString().substring(0, 5);
            }
            @Override public Number fromString(String s) { return 0; }
        });
        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        for (int i = 0; i < points.size(); i++) s.getData().add(new XYChart.Data<>(i, points.get(i).getPm25()));
        chart.getData().add(s);
    }

    private void drawPm25ChartDailyAverage(NumberAxis xAxis, LineChart<Number, Number> chart, List<AirQualityPoint> points, String title) {
        Map<LocalDate, double[]> acc = new TreeMap<>();
        for (AirQualityPoint p : points) {
            LocalDate d = Instant.ofEpochSecond(p.getTimestamp()).atZone(ZoneId.systemDefault()).toLocalDate();
            acc.putIfAbsent(d, new double[]{0, 0});
            acc.get(d)[0] += p.getPm25(); acc.get(d)[1]++;
        }
        List<LocalDate> days = new ArrayList<>(acc.keySet());
        chart.getData().clear(); chart.setTitle(title);
        xAxis.setAutoRanging(false); xAxis.setLowerBound(0); xAxis.setUpperBound(days.size() - 1);
        xAxis.setTickUnit(1);
        xAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override public String toString(Number v) {
                int i = v.intValue();
                return (i >= 0 && i < days.size()) ? days.get(i).format(DateTimeFormatter.ofPattern("dd.MM")) : "";
            }
            @Override public Number fromString(String s) { return 0; }
        });
        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        for (int i = 0; i < days.size(); i++) {
            double[] val = acc.get(days.get(i));
            s.getData().add(new XYChart.Data<>(i, val[0] / val[1]));
        }
        chart.getData().add(s);
    }

    private void handleCsvExport(Stage s) {
        if(lastHistory7d.isEmpty()) return;
        FileChooser fc = new FileChooser(); fc.setInitialFileName("historia_7dni.csv");
        File f = fc.showSaveDialog(s);
        if(f != null) { try { ExportService.exportAirQualityCsv(lastHistory7d, f.toPath()); } catch(Exception ex) { ex.printStackTrace(); } }
    }

    private void handlePngExport(LineChart n, Stage s) {
        FileChooser fc = new FileChooser(); fc.setInitialFileName("wykres.png");
        File f = fc.showSaveDialog(s);
        if(f != null) { try { ExportService.exportNodeToPng(n, f); } catch(Exception ex) { ex.printStackTrace(); } }
    }

    private void handlePdfExport(String temp, Stage s) {
        if(lastHistory7d.isEmpty()) return;
        FileChooser fc = new FileChooser(); fc.setInitialFileName("raport.pdf");
        File f = fc.showSaveDialog(s);
        if(f != null) { try { ExportService.exportWeeklyPdfReport(lastHistoryCity, lastHistory7d, temp, f); } catch(Exception ex) { ex.printStackTrace(); } }
    }

    private void maybeShowAirToast(Stage owner, String city, String profile, double pm25) {
        Thresholds t = thresholdsFor(profile);
        double warning = t.warning();
        double danger  = t.danger();

        String level;
        String title;
        String msg;
        if (pm25 >= danger) {
            level = "error";
            title = "❌ ALERT: bardzo zła jakość powietrza";
            msg = "Miasto: " + city +
                    "\nPM2.5: " + String.format("%.1f", pm25) + " µg/m³" +
                    "\nSprawdź zalecenia w aplikacji.";
        } else if (pm25 >= warning) {
            level = "warn";
            title = "⚠️ Uwaga: podwyższone PM2.5";
            msg = "Miasto: " + city +
                    "\nPM2.5: " + String.format("%.1f", pm25) + " µg/m³";
        } else {
            return;
        }

        String key = city.toLowerCase() + "|" + profile + "|" + (pm25 >= danger ? "danger" : "warning");
        if (key.equals(lastToastKey)) return;
        lastToastKey = key;

        showToast(owner, title, msg, level);
    }

    private void showToast(Stage owner, String title, String message, String level) {
        if (toastPopup == null) toastPopup = new Popup();
        toastPopup.getContent().clear();

        Label t = new Label(title);
        t.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label m = new Label(message);
        m.setWrapText(true);
        m.setMaxWidth(320);

        VBox box = new VBox(6, t, m);
        box.setPadding(new Insets(12));
        box.setMaxWidth(340);

        String bg;
        String border;
        String text;

        switch (level) {
            case "error" -> { bg = "#3b1f1f"; border = "#ff6b6b"; text = "#ffecec"; }
            case "warn"  -> { bg = "#3a2f14"; border = "#ffd166"; text = "#fff3d6"; }
            default      -> { bg = "#1f2a3b"; border = "#6aa9ff"; text = "#eaf2ff"; }
        }

        box.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + border + ";" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.35), 12, 0, 0, 6);"
        );

        t.setStyle(t.getStyle() + "-fx-text-fill: " + text + ";");
        m.setStyle("-fx-text-fill: " + text + ";");

        toastPopup.getContent().add(box);
        toastPopup.setAutoFix(false);
        toastPopup.setAutoHide(false);
        toastPopup.setHideOnEscape(true);

        toastPopup.show(owner);

        double x = owner.getX() + owner.getWidth() - 360;
        double y = owner.getY() + owner.getHeight() - 140;
        toastPopup.setX(Math.max(owner.getX() + 10, x));
        toastPopup.setY(Math.max(owner.getY() + 10, y));
        ChangeListener<Number> reposition = (obs, o, n) -> {
            toastPopup.setX(owner.getX() + owner.getWidth() - 360);
            toastPopup.setY(owner.getY() + owner.getHeight() - 140);
        };

        owner.xProperty().addListener(reposition);
        owner.yProperty().addListener(reposition);
        owner.widthProperty().addListener(reposition);
        owner.heightProperty().addListener(reposition);


        Timeline hide = new Timeline(new KeyFrame(Duration.seconds(10), e -> toastPopup.hide()));
        hide.play();
    }
    private void showError(Throwable ex) {
        ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Błąd");
        a.setHeaderText("Nie udało się pobrać danych");
        a.setContentText(ex.getMessage() != null ? ex.getMessage() : ex.toString());
        a.showAndWait();
    }



    public static void main(String[] args) { launch(args); }
}