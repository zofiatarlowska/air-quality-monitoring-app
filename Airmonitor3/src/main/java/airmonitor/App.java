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

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #f4f4f4;");

        Label titleLabel = new Label("MONITOR POGODY I POWIETRZA");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Pole wyboru miasta
        TextField cityField = new TextField();
        cityField.setPromptText("Wpisz miasto...");
        cityField.setMaxWidth(200);

        //  Wybór profilu użytkownika
        Label profileLabel = new Label("Twój profil:");
        ComboBox<String> profileBox = new ComboBox<>(FXCollections.observableArrayList(
                "Standardowy", "Alergik / Astmatyk", "Małe dziecko"
        ));
        profileBox.setValue("Standardowy");

        Button checkButton = new Button("Sprawdź stan");
        checkButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");

        Label tempLabel = new Label();
        tempLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");

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

                airmonitor.Model.WeatherData weather = client.fetchCurrentWeather(city);
                airmonitor.Model.AirQualityData air = client.fetchAirQuality(weather.getLat(), weather.getLon());

                tempLabel.setText(String.format("%.1f°C", weather.getTemperature()));

                detailsLabel.setText(String.format("Wilgotność: %d%% | Miasto: %s",
                        weather.getHumidity(), weather.getCityName()));

                airLabel.setText(String.format("PM2.5: %.1f | PM10: %.1f", air.getPm25(), air.getPm10()));

                double pmLimit = profile.equals("Standardowy") ? 25.0 : 15.0;

                if (air.getPm25() < pmLimit) {
                    airLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    recommendationLabel.setText("Warunki dobre dla profilu: " + profile + ". Możesz wyjść na zewnątrz.");
                } else if (air.getPm25() < pmLimit * 2) {
                    airLabel.setStyle("-fx-text-fill: #F57C00; -fx-font-weight: bold;");
                    recommendationLabel.setText("Powietrze umiarkowane. " + (profile.equals("Standardowy") ? "Dla Ciebie OK." : "Zalecana ostrożność!"));
                } else {
                    airLabel.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                    recommendationLabel.setText("UWAGA! Wysokie stężenie pyłów. Nie zaleca się wychodzenia.");
                }

            } catch (Exception e) {
                tempLabel.setText("Błąd!");
                detailsLabel.setText("");
                airLabel.setText("Nie znaleziono miejscowości.");
                recommendationLabel.setText("");
            }
        });

        root.getChildren().addAll(
                titleLabel,
                cityField,
                profileLabel, profileBox,
                checkButton,
                tempLabel, detailsLabel,
                airLabel, recommendationLabel
        );

        Scene scene = new Scene(root, 380, 500);
        primaryStage.setTitle("AirMonitor Pro");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}