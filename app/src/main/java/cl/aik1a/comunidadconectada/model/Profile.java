package cl.aik1a.comunidadconectada.model;
public class Profile {
    public String name, home, communityId, role;
    public Profile() {}
    public Profile(String name, String home) {
        this.name=name; this.home=home; communityId="comunidad01"; role="neighbor";
    }
    @com.google.firebase.database.Exclude
    public boolean isAdmin() { return "admin".equals(role); }
    public boolean valid() { return name!=null && home!=null && communityId!=null && (isAdmin() || "neighbor".equals(role)); }
    public String identity() { return name+" · #"+home; }
}
