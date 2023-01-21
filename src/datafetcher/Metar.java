package datafetcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;


public class Metar {
    static Logger logger = LogManager.getLogger();
    private String icao_code = "";
    private final List<String> metarData = new ArrayList<>();

    private boolean auto = false;
    private boolean cor= false;
    private boolean nil = false;

    private String time;
    private String day;

    private String wind_dir;
    private String wind_speed;
    private boolean wind_gusting =false;
    private String wind_gusting_speed;
    private boolean wind_var = false;
    private final String[] wind_var_vals = new String[2];

    private String visibility;
    private String vertical_visibility;

    private final List<String> cloud_types = Arrays.asList("FEW", "SCT", "BKN", "OVC");
    private final List<Cloud> clouds = new ArrayList<>();
    private boolean nsc = false;
    private boolean cavok = false;
    private boolean nosig = false;

    private String pressure;

    public Metar(String icao) throws IOException {

        icao_code = icao.toUpperCase();
        logger.info("Creating Metar Object, airport: "+icao_code);
        updateData();
    }

    void updateData() throws IOException {
        fetch();
        parse();
    }

    void parse()
    {
        List<String> tempMetar = metarData;
        tempMetar.remove(0);//remove METAR
        tempMetar.remove(0);//remove ICAO code
        if(tempMetar.contains("COR"))
        {
            cor = true;
            tempMetar.removeIf(s -> s.equals("COR"));
            logger.info("cor");
        } else logger.info("No cor");

        String timeRaw = tempMetar.get(0); //getting time

        time = timeRaw.substring(2,4) + ":" + timeRaw.substring(4,6);
        day = timeRaw.substring(0,2);
        tempMetar.remove(0); // removing time

        if(tempMetar.contains("NIL"))
        {
            nil = true;
            logger.info("NIL");
            return;
        }

        if(tempMetar.contains("AUTO"))
        {
            auto = true;
            tempMetar.removeIf(s -> s.equals("AUTO"));
            logger.info("auto");
        } else logger.info("No auto");

        //wiatr
        if(tempMetar.get(0).endsWith("KT"))
        {
            String wind = tempMetar.get(0);
            wind_gusting = wind.contains("G");
            wind_dir = wind.substring(0,3);
            wind_speed = wind.substring(3,5);
            if(wind_gusting)
                wind_gusting_speed = wind.substring(6,8);
            tempMetar.remove(0);

            logger.info("wind " + wind);

        } else logger.error("Wind not found");

        if(tempMetar.get(0).length() == 7 && tempMetar.get(0).charAt(3) == 'V')
        {
            wind_var_vals[0] = tempMetar.get(0).substring(0,3);
            wind_var_vals[1] = tempMetar.get(0).substring(4);
            wind_var = true;
            tempMetar.remove(0);
            logger.info("wind var "+ Arrays.toString(wind_var_vals));

        }else logger.info("No wind variation");

        if(isNumeric(tempMetar.get(0)) && tempMetar.get(0).length() == 4)
        {
            visibility = tempMetar.get(0);
            tempMetar.remove(0);
            logger.info("Vis "+visibility);
        }
        else logger.error("Visibility not found");

        if((tempMetar.get(0).length() > 4) && isNumeric(tempMetar.get(0).substring(0, 5)))
        {
            //minimal vis
            tempMetar.remove(0);
        } else logger.info("No minimal vis");

        boolean cloud_found = false;
        //clouds
        while(true)
        {
            for (String cloud: cloud_types)
            {
                int index = -1;
                for (String metarElement: tempMetar)
                {
                    if(metarElement.contains(cloud))
                    {
                        index = tempMetar.indexOf(metarElement);
                        break;
                    }
                }

                if(index != -1)
                {
                    cloud_found = true;

                    String cloudStr = tempMetar.get(index);

                    Cloud tempCloud = new Cloud(cloudStr.substring(0,3), cloudStr.substring(3,6), cloudStr.contains("CB"));

                    clouds.add(tempCloud);

                    tempMetar.remove(index);
                }
            }
            if(!cloud_found)
                break;

            cloud_found = false;
        }
        if(tempMetar.contains("NSC"))
        {
            logger.info("No clouds");
            nsc = true;
            tempMetar.remove("NSC");
        } else if (tempMetar.contains("CAVOK")) {
            logger.info("Celing and visability ok");
            cavok=true;
            tempMetar.remove("CAVOK");
        }
        else
            logger.info(clouds);

        for (String metarElement: tempMetar)
        {
            if(metarElement.startsWith("VV"))
            {
                vertical_visibility = metarElement.substring(2,5);
                logger.info(metarElement);
                tempMetar.remove(metarElement);
                break;
            }
        }


        //ciśnienie
        for (String metarElement: tempMetar)
        {
            if(metarElement.startsWith("Q"))
            {
                pressure = metarElement.substring(1,5);
                logger.info("QNH "+pressure);
                tempMetar.remove(metarElement);
                break;
            }
        }

        for (String metarElement: tempMetar)
        {
            if(metarElement.startsWith("NOSIG"))
            {
                logger.info("NOSIG");
                nosig=true;
                tempMetar.remove(metarElement);
                break;
            }
        }



        logger.info(tempMetar.toString());

//        logger.info(wind_var_vals);

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
            if(linkText.equals("METAR " + icao_code))
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
            if(linkText.equals("METAR " + icao_code))
            {
                assert Objects.requireNonNull(link.parent()).parent() != null;
                metar00DataString = link.parent().parent().text();
                logger.info("airport metar00 finded, data: "+metar00DataString);
                break;
            }
        }

        String[] metar00Data = metar00DataString.split("\s");
        String[] metar30Data = metar30DataString.split("\s");

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
}
