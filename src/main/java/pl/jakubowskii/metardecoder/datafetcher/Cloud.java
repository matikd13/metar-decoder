package pl.jakubowskii.metardecoder.datafetcher;

public class Cloud {
    public String type;
    public int alt;
    public Boolean cb;

    public Cloud(String type_, String alt_, boolean cb_)
    {
        type=type_;
        alt=Integer.parseInt(alt_);
        cb=cb_;
    }

    public String getSkyCover()
    {
        return switch (type) {
            case "FEW" -> "1-2/8";
            case "SCT" -> "3-4/8";
            case "BKN" -> "5-7/8";
            case "OVC" -> "8/8";
            default -> null;
        };
    }

    @Override
    public String toString() {
        return "Cloud{" +
                "type='" + type + '\'' +
                ", alt='" + alt + '\'' +
                ", cb=" + cb +
                '}';
    }
}
