package airmonitor;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import airmonitor.Api.WeatherApiClient;
import airmonitor.Model.AirQualityPoint;
import airmonitor.Model.ForecastData;
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
import airmonitor.service.ExportService;
import java.io.File;
import javafx.stage.FileChooser;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class App extends Application {
    private List<airmonitor.Model.AirQualityPoint> lastHistory7d = java.util.List.of();
    private String lastHistoryCity = "";
    private List<AirQualityPoint> lastPlottedPoints = List.of();

    private final String PRIMARY = "#2c3e50";
    private final String ACCENT = "#2196F3";
    private final String SUCCESS = "#4CAF50";
    private final String INFO = "#2980b9";
    private final String CARD_BG = "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);";

    private TableColumn<ComparisonRow, Double> colTemp;
    private final ImageView weatherIconView = new ImageView();
    private final HBox forecastContainer = new HBox(15); // Kontener na prognozę 5-dniową
    private Timeline autoRefresh;

    @Override
    public void start(Stage primaryStage) {
        VBox mainContainer = new VBox(25);
        mainContainer.setPadding(new Insets(30));
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setStyle("-fx-background-color: #f0f2f5;");

        Label titleLabel = new Label("MONITOR POGODY I POWIETRZA");
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY + ";");

        // --- PANEL WYSZUKIWANIA ---
        VBox searchCard = new VBox(15);
        searchCard.setStyle(CARD_BG);
        searchCard.setPadding(new Insets(20));
        searchCard.setAlignment(Pos.CENTER);
        searchCard.setMaxWidth(700);

        TextField cityField = new TextField();
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
                handleCheck(cityField, null, null, null, null, null, null); // Wyzwalacz sprawdzania
            });
            new Thread(locTask).start();
        });

        HBox settingsBox = new HBox(20);
        settingsBox.setAlignment(Pos.CENTER);
        ComboBox<String> profileBox = new ComboBox<>(FXCollections.observableArrayList("Standardowy", "Alergik / Astmatyk", "Małe dziecko"));
        profileBox.setValue("Standardowy");
        ComboBox<String> unitBox = new ComboBox<>(FXCollections.observableArrayList("Celsius (°C)", "Kelvin (K)", "Fahrenheit (°F)"));
        unitBox.setValue("Celsius (°C)");
        settingsBox.getChildren().addAll(new Label("Profil:"), profileBox, new Label("Jednostka:"), unitBox);

        Button checkButton = new Button("SPRAWDŹ STAN");
        styleButton(checkButton, ACCENT);
        checkButton.setMinWidth(220);

        searchCard.getChildren().addAll(new HBox(10, cityField, locBtn), settingsBox, checkButton);
        ((HBox)searchCard.getChildren().get(0)).setAlignment(Pos.CENTER);


        VBox resultCard = new VBox(15);
        resultCard.setStyle(CARD_BG);
        resultCard.setPadding(new Insets(25));
        resultCard.setAlignment(Pos.CENTER);
        resultCard.setMaxWidth(700);
        resultCard.setManaged(false);
        resultCard.setVisible(false);

        weatherIconView.setFitHeight(80);
        weatherIconView.setFitWidth(80);

        Label tempLabel = new Label();
        tempLabel.setStyle("-fx-font-size: 52px; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY + ";");

        HBox tempIconBox = new HBox(15, weatherIconView, tempLabel);
        tempIconBox.setAlignment(Pos.CENTER);

        Label detailsLabel = new Label();
        detailsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
        Label airLabel = new Label();
        Label recommendationLabel = new Label();
        recommendationLabel.setWrapText(true);
        recommendationLabel.setStyle("-fx-font-style: italic; -fx-text-alignment: center;");

        // prognoza pogody
        Label forecastTitle = new Label("Prognoza na najbliższe dni:");
        forecastTitle.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");
        forecastContainer.setAlignment(Pos.CENTER);

        resultCard.getChildren().addAll(tempIconBox, detailsLabel, airLabel, recommendationLabel, new Separator(), forecastTitle, forecastContainer);

        // wykresy
        VBox chartCard = new VBox(15);
        chartCard.setStyle(CARD_BG);
        chartCard.setPadding(new Insets(20));
        chartCard.setMaxWidth(950);

        HBox chartButtons = new HBox(15);
        chartButtons.setAlignment(Pos.CENTER);
        Button h24Btn = new Button("Wykres: ostatnie 24h");
        Button h7dBtn = new Button("Wykres: ostatnie 7 dni");
        styleButton(h24Btn, SUCCESS);
        styleButton(h7dBtn, INFO);
        chartButtons.getChildren().addAll(h24Btn, h7dBtn);

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("PM2.5 (µg/m³)");
        LineChart<Number, Number> pmChart = new LineChart<>(xAxis, yAxis);
        pmChart.setCreateSymbols(true);
        pmChart.setPrefHeight(350);
        pmChart.setAnimated(false);
        chartCard.getChildren().addAll(chartButtons, pmChart);

        // --- PANEL PORÓWNANIA ---
        VBox compareCard = new VBox(15);
        compareCard.setStyle(CARD_BG);
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
            handleCheck(cityField, profileBox, unitBox, tempLabel, detailsLabel, airLabel, recommendationLabel);
        });

        autoRefresh = new Timeline(new KeyFrame(Duration.minutes(10), e -> {
            if (!cityField.getText().isEmpty()) checkButton.fire();
        }));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();

        h24Btn.setOnAction(e -> handleHistory(cityField.getText(), 24, xAxis, pmChart, false));
        h7dBtn.setOnAction(e -> handleHistory(cityField.getText(), 168, xAxis, pmChart, true));
        compareBtn.setOnAction(e -> handleComparison(c1, c2, c3, unitBox.getValue(), compareTable));
        csvBtn.setOnAction(e -> handleCsvExport(primaryStage));
        pngBtn.setOnAction(e -> handlePngExport(pmChart, primaryStage));
        pdfBtn.setOnAction(e -> handlePdfExport(tempLabel.getText(), primaryStage));

        mainContainer.getChildren().addAll(titleLabel, searchCard, resultCard, chartCard, compareCard, exportBox);

        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #f0f2f5;");

        Scene scene = new Scene(scrollPane, 1100, 950);
        primaryStage.setTitle("Monitor Pogody i Powietrza");
        primaryStage.setScene(scene);
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

    private void handleCheck(TextField cityF, ComboBox<String> profB, ComboBox<String> unitB, Label tL, Label dL, Label aL, Label rL) {
        String city = cityF.getText();
        String selectedUnit = (unitB == null) ? "Celsius (°C)" : unitB.getValue();
        WeatherApiClient client = new WeatherApiClient();

        Task<Void> combinedTask = new Task<>() {
            @Override protected Void call() throws Exception {
                var weather = client.fetchCurrentWeather(city);
                var air = client.fetchAirQuality(weather.getLat(), weather.getLon());
                var forecast = client.fetchForecast(city);

                Platform.runLater(() -> {
                    weatherIconView.setImage(new Image(weather.getIconUrl()));
                    double displayTemp = weather.getTemperatureInUnit(selectedUnit);
                    String unitSymbol = selectedUnit.substring(selectedUnit.indexOf("(") + 1, selectedUnit.indexOf(")"));

                    if(tL != null) tL.setText(String.format("%.1f %s", displayTemp, unitSymbol));
                    if(dL != null) dL.setText(String.format("Wilgotność: %d%% | Miasto: %s", weather.getHumidity(), weather.getCityName()));
                    if(aL != null) aL.setText(String.format("PM2.5: %.1f | PM10: %.1f", air.getPm25(), air.getPm10()));

                    // Obsługa prognozy
                    forecastContainer.getChildren().clear();
                    for (int i = 0; i < forecast.getForecastList().size(); i += 8) {
                        var item = forecast.getForecastList().get(i);
                        VBox dayBox = new VBox(5); dayBox.setAlignment(Pos.CENTER);
                        String day = Instant.ofEpochSecond(item.getDt()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM"));
                        Label dLabel = new Label(day);
                        ImageView iv = new ImageView(new Image(item.getIconUrl())); iv.setFitWidth(40); iv.setFitHeight(40);
                        Label tLabel = new Label(String.format("%.0f°C", item.getTemp()));
                        dayBox.getChildren().addAll(dLabel, iv, tLabel);
                        forecastContainer.getChildren().add(dayBox);
                    }
                });
                return null;
            }
        };
        new Thread(combinedTask).start();
    }

    private void handleHistory(String city, int hours, NumberAxis xAxis, LineChart<Number, Number> chart, boolean is7d) {
        WeatherApiClient client = new WeatherApiClient();
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                var w = client.fetchCurrentWeather(city);
                long now = Instant.now().getEpochSecond();
                long start = now - (hours * 3600L);
                List<airmonitor.Model.AirQualityPoint> points = client.fetchAirQualityHistory(w.getLat(), w.getLon(), start, now);
                Platform.runLater(() -> {
                    if (is7d) {
                        lastHistory7d = points; lastHistoryCity = w.getCityName();
                        drawPm25ChartDailyAverage(xAxis, chart, points, "Trend 7-dniowy");
                    } else {
                        drawPm25ChartSamples(xAxis, chart, points, "Ostatnie 24 godziny");
                    }
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    private void handleComparison(TextField c1, TextField c2, TextField c3, String unit, TableView<ComparisonRow> table) {
        List<String> cities = new ArrayList<>();
        if(!c1.getText().isBlank()) cities.add(c1.getText().trim());
        if(!c2.getText().isBlank()) cities.add(c2.getText().trim());
        if(!c3.getText().isBlank()) cities.add(c3.getText().trim());
        if (cities.size() < 2) return;

        String unitSymbol = unit.substring(unit.indexOf("(") + 1, unit.indexOf(")"));
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

    public static void main(String[] args) { launch(args); }
}