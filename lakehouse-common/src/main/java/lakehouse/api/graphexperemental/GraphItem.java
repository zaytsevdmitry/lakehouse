package lakehouse.api.graphexperemental;

public class GraphItem {
    private String current;
    private String next;
    private int iteration;
    private int level;

    public GraphItem(String current, String next, int iteration, int level) {
        this.current = current;
        this.next = next;
        this.iteration = iteration;
        this.level = level;
    }

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
