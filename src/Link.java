public class Link {
    public final String a;
    public final String b;

    public Link(String a, String b) {
        this.a = a;
        this.b = b;
    }

    public boolean touches(String id) {
        return a.equals(id) || b.equals(id);
    }

    public String other(String id) {
        if (a.equals(id)) return b;
        if (b.equals(id)) return a;
        throw new IllegalArgumentException("Link does not include id: " + id);
    }
}