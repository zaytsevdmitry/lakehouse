package lakehouse.api.dto;

public class DagEdgeDTO {
    private String  from;
    private String to;

    public DagEdgeDTO() {
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
