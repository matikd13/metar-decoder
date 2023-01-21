package datafetcher;

public class Cloud {
    public String type;
    public String alt;
    public Boolean cb;

    public Cloud(String type_, String alt_, boolean cb_)
    {
        type=type_;
        alt=alt_;
        cb=cb_;
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
