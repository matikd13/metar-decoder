package pl.jakubowskii.metardecoder.datafetcher;

import javafx.fxml.Initializable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;


public class Metar {
    static Logger logger = LogManager.getLogger();
    private String icaoCode = "";
    private boolean unsupportedAirport = false;
    private final List<String> metarData = new ArrayList<>();

    private boolean auto = false;
    private boolean cor= false;
    private boolean nil = false;

    private String time;
    private String day;

    private String windDir;
    private String windSpeed;
    private boolean windGusting =false;
    private String windGustingSpeed;
    private boolean windVar = false;
    private final String[] windVarVals = new String[2];

    private String visibility;
    private String verticalVisibility="";
    private final List<String> cloudTypes = Arrays.asList("FEW", "SCT", "BKN", "OVC");
    private final List<Cloud> clouds = new ArrayList<>();
    private boolean nsc = false;
    private boolean cavok = false;
    private boolean nosig = false;

    private String pressure;

    private int temp;
    private int dewPoint;

    public Metar(String icao) throws IOException {

        icaoCode = icao.toUpperCase();
        logger.info("Creating Metar Object, airport: "+ icaoCode);
        updateData();
    }

    public void updateData() throws IOException {
        fetch();
        parse();
    }

    void parse()
    {
        try {
            List<String> tempMetar = new ArrayList<>(metarData);
            tempMetar.remove(0);//remove METAR
            tempMetar.remove(0);//remove ICAO code
            if (tempMetar.contains("COR")) {
                cor = true;
                tempMetar.removeIf(s -> s.equals("COR"));
                logger.info("cor");
            } else logger.info("No cor");

            String timeRaw = tempMetar.get(0); //getting time

            time = timeRaw.substring(2, 4) + ":" + timeRaw.substring(4, 6);
            day = timeRaw.substring(0, 2);
            tempMetar.remove(0); // removing time

            if (tempMetar.contains("NIL")) {
                nil = true;
                logger.info("NIL");
                return;
            }

            if(tempMetar.contains("TEMPO"))
            {
                for(int i = tempMetar.indexOf("TEMPO"); i <= tempMetar.size(); i++)
                {
                    tempMetar.remove(tempMetar.size()-1);
                }
            }
            if(tempMetar.contains("BECMG"))
            {
                for(int i = tempMetar.indexOf("BECMG"); i <= tempMetar.size(); i++)
                {
                    tempMetar.remove(tempMetar.size()-1);
                }
            }

            if (tempMetar.contains("AUTO")) {
                auto = true;
                tempMetar.removeIf(s -> s.equals("AUTO"));
                logger.info("auto");
            } else logger.info("No auto");

            //wiatr
            if (tempMetar.get(0).endsWith("KT")) {
                String wind = tempMetar.get(0);
                windGusting = wind.contains("G");
                windDir = wind.substring(0, 3);
                windSpeed = wind.substring(3, 5);
                if (windGusting)
                    windGustingSpeed = wind.substring(6, 8);
                tempMetar.remove(0);

                logger.info("wind " + wind);

            } else logger.error("Wind not found");

            if (tempMetar.get(0).length() == 7 && tempMetar.get(0).charAt(3) == 'V') {
                windVarVals[0] = tempMetar.get(0).substring(0, 3);
                windVarVals[1] = tempMetar.get(0).substring(4);
                windVar = true;
                tempMetar.remove(0);
                logger.info("wind var " + Arrays.toString(windVarVals));

            } else logger.info("No wind variation");

            if (isNumeric(tempMetar.get(0)) && tempMetar.get(0).length() == 4) {
                visibility = tempMetar.get(0);
                tempMetar.remove(0);
                logger.info("Vis " + visibility);
            } else logger.warn("Visibility not found");

            if ((tempMetar.get(0).length() > 4) && isNumeric(tempMetar.get(0).substring(0, 5))) {
                //minimal vis
                tempMetar.remove(0);
            } else logger.info("No minimal vis");

            boolean cloud_found = false;
            //clouds
            while (true) {
                for (String cloud : cloudTypes) {
                    int index = -1;
                    for (String metarElement : tempMetar) {
                        if (metarElement.contains(cloud)) {
                            index = tempMetar.indexOf(metarElement);
                            break;
                        }
                    }

                    if (index != -1) {
                        cloud_found = true;

                        String cloudStr = tempMetar.get(index);

                        Cloud tempCloud = new Cloud(cloudStr.substring(0, 3), cloudStr.substring(3, 6), cloudStr.contains("CB"));

                        clouds.add(tempCloud);

                        tempMetar.remove(index);
                    }
                }
                if (!cloud_found)
                    break;

                cloud_found = false;
            }
            if (tempMetar.contains("NSC")) {
                logger.info("No clouds");
                nsc = true;
                tempMetar.remove("NSC");
            }
            if (tempMetar.contains("CAVOK")) {
                logger.info("Celing and visability ok");
                cavok = true;
                tempMetar.remove("CAVOK");
                visibility = "9999";
            }
            logger.info(clouds);

            for (String metarElement : tempMetar) {
                if (metarElement.startsWith("VV")) {
                    verticalVisibility = metarElement.substring(2, 5);
                    logger.info(metarElement);
                    tempMetar.remove(metarElement);
                    break;
                }
            }


            //ciśnienie
            for (String metarElement : tempMetar) {
                if (metarElement.startsWith("Q")) {
                    pressure = metarElement.substring(1, 5);
                    logger.info("QNH " + pressure);
                    tempMetar.remove(metarElement);
                    break;
                }
            }

            for (String metarElement : tempMetar) {
                if (metarElement.startsWith("NOSIG")) {
                    logger.info("NOSIG");
                    nosig = true;
                    tempMetar.remove(metarElement);
                    break;
                }
            }

            //temperatura

            for (String metarElement : tempMetar) {
                try {
                    if ((isNumeric(metarElement.substring(0, 2)) && (metarElement.charAt(2) == '/')) || (isNumeric(metarElement.substring(1, 3)) && (metarElement.charAt(3) == '/') && metarElement.charAt(0) == 'M')) {
                        if (metarElement.charAt(0) == 'M') {
                            temp = Integer.parseInt(metarElement.substring(1, 3));
                            temp *= -1;

                            if (metarElement.charAt(4) == 'M') {
                                dewPoint = Integer.parseInt(metarElement.substring(5, 7));
                                dewPoint *= -1;
                            } else {
                                dewPoint = Integer.parseInt(metarElement.substring(4, 6));
                            }

                        } else {
                            temp = Integer.parseInt(metarElement.substring(0, 2));
                            if (metarElement.charAt(3) == 'M') {
                                dewPoint = Integer.parseInt(metarElement.substring(4, 6));
                                dewPoint *= -1;
                            } else {
                                dewPoint = Integer.parseInt(metarElement.substring(3, 5));
                            }
                        }
                        logger.info("temp: " + temp + " dew point: " + dewPoint);
                        tempMetar.remove(metarElement);
                        break;
                    }
                } catch (StringIndexOutOfBoundsException ignored) {

                }
            }

            logger.info(tempMetar.toString());

//        logger.info(wind_var_vals);
        } catch (Exception ignored)
        {
            unsupportedAirport = true;
        }
    }

    void fetch() throws IOException {
        Document metar00 = Jsoup.connect("https://awiacja.imgw.pl/en/metar-g00/").get();
        Document metar30 = Jsoup.connect("https://awiacja.imgw.pl/en/metar-gg30-2/").get();

        String metar30DataString = "";
        String metar00DataString = "";

        Element metarTable30 = metar30.getElementById("metartable");
        assert metarTable30 != null;
        Elements links30 = metarTable30.getElementsByTag("a");

        for (Element link : links30) {
            String linkText = link.text();
            if(linkText.equals("METAR " + icaoCode))
            {
                assert Objects.requireNonNull(link.parent()).parent() != null;
                metar30DataString = link.parent().parent().text();
                logger.info("airport metar30 finded, data: "+metar30DataString);
                break;
            }
        }

        Element metarTable00 = metar00.getElementById("metartable");
        assert metarTable00 != null;
        Elements links00 = metarTable00.getElementsByTag("a");

        for (Element link : links00) {
            String linkText = link.text();
            if(linkText.equals("METAR " + icaoCode))
            {
                assert Objects.requireNonNull(link.parent()).parent() != null;
                metar00DataString = link.parent().parent().text();
                logger.info("airport metar00 finded, data: "+metar00DataString);
                break;
            }
        }

        String[] metar00Data = metar00DataString.split(" ");
        String[] metar30Data = metar30DataString.split(" ");

        int time00 = 0;
        int time30 = 0;

        for (String element : metar30Data) {
            if (element.length() == 7 && element.substring(6).equals("Z")) {
                time30 = Integer.parseInt(element.substring(0, 6));
                logger.info("metar30 time: " + element);
                break;
            }
        }

        for (String element : metar00Data) {
            if (element.length() == 7 && element.substring(6).equals("Z")) {
                logger.info("metar00 time: " + element);
                time00 = Integer.parseInt(element.substring(0, 6));
            }
        }

        String[] goodMetar;
        if((time30>time00))
            goodMetar = metar30Data;
        else
            goodMetar = metar00Data;

        Collections.addAll(metarData, goodMetar);

        logger.info("actual metar: " + metarData.toString());

    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public String getTime() {
        return time+" UTC";
    }

    public String getDay() {
        Calendar cal = Calendar.getInstance();
        return day+"."+(cal.get(Calendar.MONTH)+1)+"."+cal.get(Calendar.YEAR)+"r.";
    }

    @Override
    public String toString() {
        return String.join(" ", metarData);
    }

    public boolean isUnsupportedAirport() {
        return unsupportedAirport;
    }

    public double getWindDir() {
        if(Objects.equals(windDir, "VRB"))
        {
            return -1.0;
        }
        return Double.parseDouble(windDir);
    }

    public Integer getWindSpeed() {
        return Integer.parseInt(windSpeed);
    }

    public boolean isWindGusting() {
        return windGusting;
    }

    public Integer getWindGustingSpeed() {
        return Integer.parseInt(windGustingSpeed);
    }

    public boolean isWindVar() {
        return windVar;
    }

    public String[] getWindVarVals() {
        return windVarVals;
    }

    public String getVisibility() {
        return visibility;
    }

    public Integer getVerticalVisibility() {
        if(Objects.equals(verticalVisibility, ""))
            return -1;
        return Integer.parseInt(verticalVisibility) * 100;
    }

    public int getTemp() {
        return temp;
    }

    public int getDewPoint() {
        return dewPoint;
    }

    public Integer getPressure() {
        return Integer.parseInt(pressure);
    }

    public List<Cloud> getClouds() {
        return clouds;
    }

    public boolean isCavok() {
        return cavok;
    }

    public boolean isNil() {
        return nil;
    }

    public boolean isNsc() {
        return nsc;
    }

}
