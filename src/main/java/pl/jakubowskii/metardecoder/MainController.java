package pl.jakubowskii.metardecoder;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.jakubowskii.metardecoder.datafetcher.Cloud;
import pl.jakubowskii.metardecoder.datafetcher.Metar;
import java.lang.Math;
import java.io.IOException;
import java.util.Objects;

public class MainController {
    static Logger logger = LogManager.getLogger();

    @FXML
    private TextField icaoCode;
    @FXML
    private Text rawMetar;
    @FXML
    private Line windLine;
    @FXML
    private Line windLineLeft;
    @FXML
    private Line windLineRight;
    @FXML
    private Line windVarRight;
    @FXML
    private Line windVarLeft;
    @FXML
    private Text windInfo;
    @FXML
    private Text date;
    @FXML
    private Text vis;
    @FXML
    private Text temp;
    @FXML
    private Text clouds;

    private void hideAll()
    {
        date.setOpacity(0);
        windInfo.setOpacity(0);
        windVarRight.setOpacity(0);
        windVarLeft.setOpacity(0);
        windLine.setOpacity(0);
        windLineLeft.setOpacity(0);
        windLineRight.setOpacity(0);
        temp.setOpacity(0);
        vis.setOpacity(0);
        clouds.setOpacity(0);
    }

    public void getMetar(ActionEvent e) throws IOException {
        hideAll();
        String icao = icaoCode.getText();
        logger.info("Fetching metar "+ icao);
        rawMetar.setFill(Color.BLACK);

        if(icao.length() == 0)
        {
            rawMetar.setText("Please provide ICAO code of airport");
            rawMetar.setFill(Color.RED);
            hideAll();
            return;
        }

        Metar metar = new Metar(icao);

        if(metar.isUnsupportedAirport())
        {
            rawMetar.setText("Unsupported airport or parse error");
            rawMetar.setFill(Color.RED);
            hideAll();
            return;
        }

        rawMetar.setText(metar.toString());

        String dateString = "Informacja pogodowa z godziny " + metar.getTime() + ", " +
                "dnia " + metar.getDay();
        date.setText(dateString);
        date.setOpacity(1);

        String windInfoStr = "";
        if(metar.getWindDir() != -1.0)
        {
            windInfoStr= "Wiatr z kierunku " + (int) metar.getWindDir() + "* o sile " + metar.getWindSpeed() + " węzłów";
            if (metar.isWindGusting())
                windInfoStr += ", w porywach do " + metar.getWindGustingSpeed() + " węzłów";

            if(metar.isWindVar())
                windInfoStr += ", zmienny od "+Integer.parseInt(metar.getWindVarVals()[0])+"* do "+Integer.parseInt(metar.getWindVarVals()[1])+"*";

            windInfoStr+=".";

            setWindLine(metar);
        }
        else
        {
            windInfoStr= "Wiatr zmienny o sile " + metar.getWindSpeed() + " węzłów.";
        }
        windInfo.setOpacity(1);
        windInfo.setText(windInfoStr);

        String visStr = "Widzialność " + (Objects.equals(metar.getVisibility(), "9999") ? ">" : "") + metar.getVisibility() + " metrów";
        if(metar.getVerticalVisibility() != -1)
            visStr += ", widzialność pionowa "+metar.getVerticalVisibility()+" stóp";

        visStr += ".";

        vis.setText(visStr);
        vis.setOpacity(1);

        String tempStr = "Temperatura " + metar.getTemp() + "*C, punkt rosy " + metar.getDewPoint()+ "*C. Ciśnienie QNH "+metar.getPressure()+" hPa.";

        temp.setText(tempStr);
        temp.setOpacity(1);
        clouds.setOpacity(1);
        if(metar.isCavok() || metar.isNsc() || metar.getClouds().size() == 0)
        {
            clouds.setText("Brak znaczących chmur");
        }
        else
        {
            StringBuilder cloudsSb = new StringBuilder();
            for (Cloud cloud : metar.getClouds())
            {
                cloudsSb.append("Chmury ");
                if(cloud.cb) cloudsSb.append("konwekcyjne ");
                cloudsSb.append("zakrywające ").append(cloud.getSkyCover()).append(" nieba o podstawie ").append(cloud.alt*100).append(" stóp.\n");
            }

            clouds.setText(cloudsSb.toString());
        }

    }

    private void setWindLine(Metar metar)
    {
        double x = 90 * Math.cos(Math.toRadians(metar.getWindDir()-90));
        double y = 90 * Math.sin(Math.toRadians(metar.getWindDir()-90));

        double x_left = 10 * Math.cos(Math.toRadians(metar.getWindDir()-90+30));
        double y_left = 10 * Math.sin(Math.toRadians(metar.getWindDir()-90+30));

        double x_right = 10 * Math.cos(Math.toRadians(metar.getWindDir()-90-30));
        double y_right = 10 * Math.sin(Math.toRadians(metar.getWindDir()-90-30));

        windLine.setEndX(x);
        windLine.setEndY(y);

        windLineRight.setEndX(x_right);
        windLineRight.setEndY(y_right);

        windLineLeft.setEndX(x_left);
        windLineLeft.setEndY(y_left);

        windLine.setOpacity(1);
        windLineLeft.setOpacity(1);
        windLineRight.setOpacity(1);

        if (metar.isWindVar())
        {

            x_left = 90 * Math.cos(Math.toRadians(Double.parseDouble(metar.getWindVarVals()[0])-90));
            y_left = 90 * Math.sin(Math.toRadians(Double.parseDouble(metar.getWindVarVals()[0])-90));

            x_right = 90 * Math.cos(Math.toRadians(Double.parseDouble(metar.getWindVarVals()[1])-90));
            y_right = 90 * Math.sin(Math.toRadians(Double.parseDouble(metar.getWindVarVals()[1])-90));

            windVarRight.setEndX(x_right);
            windVarRight.setEndY(y_right);

            windVarLeft.setEndX(x_left);
            windVarLeft.setEndY(y_left);

            windVarRight.setOpacity(1);
            windVarLeft.setOpacity(1);
        }
        else
        {
            windVarRight.setOpacity(0);
            windVarLeft.setOpacity(0);
        }

    }

}
