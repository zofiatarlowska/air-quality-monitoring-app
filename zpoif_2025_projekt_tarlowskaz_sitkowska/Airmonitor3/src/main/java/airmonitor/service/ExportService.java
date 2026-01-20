package airmonitor.service;

import airmonitor.Model.AirQualityPoint;
import airmonitor.Model.ComparisonRow;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class ExportService {

    private static final DateTimeFormatter DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public static void exportAirQualityCsv(List<AirQualityPoint> points, Path file) throws Exception {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("timestamp,datetime,pm25,pm10,aqi");
            w.newLine();
            for (AirQualityPoint p : points) {
                w.write(p.getTimestamp() + "," +
                        DT.format(Instant.ofEpochSecond(p.getTimestamp())) + "," +
                        p.getPm25() + "," +
                        p.getPm10() + "," +
                        p.getAqi());
                w.newLine();
            }
        }
    }

    public static void exportComparisonCsv(List<ComparisonRow> rows, Path file) throws Exception {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("city,temperature,humidity,aqi,pm25,pm10");
            w.newLine();
            for (ComparisonRow r : rows) {
                w.write(r.getCity() + "," +
                        r.getTemperature() + "," +
                        r.getHumidity() + "," +
                        r.getAqi() + "," +
                        r.getPm25() + "," +
                        r.getPm10());
                w.newLine();
            }
        }
    }

    public static void exportNodeToPng(Node node, File file) throws Exception {
        WritableImage image = node.snapshot(new SnapshotParameters(), null);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
    }

    public static void exportWeeklyPdfReport(String city, List<AirQualityPoint> points, String tempInfo, File file) throws Exception {
        double avgPm25 = points.stream().mapToDouble(AirQualityPoint::getPm25).average().orElse(0);
        double maxPm25 = points.stream().mapToDouble(AirQualityPoint::getPm25).max().orElse(0);
        double avgPm10 = points.stream().mapToDouble(AirQualityPoint::getPm10).average().orElse(0);
        double maxPm10 = points.stream().mapToDouble(AirQualityPoint::getPm10).max().orElse(0);

        String from = points.isEmpty() ? "-" : DT.format(Instant.ofEpochSecond(points.get(0).getTimestamp()));
        String to = points.isEmpty() ? "-" : DT.format(Instant.ofEpochSecond(points.get(points.size() - 1).getTimestamp()));

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.newLineAtOffset(50, 780);
                cs.showText("Raport jakosci powietrza i pogody");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, 740);

                cs.showText("Miasto: " + city); cs.newLineAtOffset(0, -18);
                cs.showText("Aktualna temperatura: " + tempInfo); cs.newLineAtOffset(0, -18); // NOWA LINIA
                cs.showText("Zakres danych: " + from + "  ->  " + to); cs.newLineAtOffset(0, -18);
                cs.showText(String.format("PM2.5: srednia = %.2f, max = %.2f", avgPm25, maxPm25)); cs.newLineAtOffset(0, -18);
                cs.showText(String.format("PM10 : srednia = %.2f, max = %.2f", avgPm10, maxPm10)); cs.newLineAtOffset(0, -18);

                cs.newLineAtOffset(0, -18);
                cs.showText("Wygenerowano z aplikacji AirMonitor.");
                cs.endText();
            }
            doc.save(file);
        }
    }
}
