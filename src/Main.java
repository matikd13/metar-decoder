import datafetcher.Metar;
import org.apache.logging.log4j.*;

import java.io.IOException;

public class Main {

    static Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws IOException {
        logger.info("Starting program");

        Metar testowy = new Metar("EPZG");

    }
}