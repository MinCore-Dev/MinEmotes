package dev.minemotes.core;

public final class Config {
  public Core core = new Core();
  public Emotes emotes = new Emotes();
  public Permissions permissions = new Permissions();

  public static final class Core {
    public boolean enabled = true;
    public int cooldownS = 2;
    public Cancel cancelOnDamage = new Cancel();
    public Cancel cancelOnMove = new Cancel();
    public AllowInWater allowInWater = new AllowInWater();
    public String[] disableInWorlds = new String[0];
    public static final class Cancel { public boolean sit=true, lay=true, belly=true, crawl=false; }
    public static final class AllowInWater { public boolean sit=false, lay=false, belly=false; }
  }
  public static final class Emotes {
    public Toggle crawl = new Toggle(true);
    public Sit sit = new Sit();
    public Lay lay = new Lay();
    public Lay belly = new Lay(-0.92, -90);
  }
  public static final class Permissions {
    public String crawl="minemotes.crawl", sit="minemotes.sit", lay="minemotes.lay", belly="minemotes.belly", admin="minemotes.admin";
  }
  public static final class Toggle { public boolean enabled; public Toggle(boolean e){enabled=e;} public Toggle(){} }
  public static final class Sit extends Toggle { public double offsetY = -0.45; public Sit(){super(true);} }
  public static final class Lay extends Toggle { public double offsetY = -0.90; public int pitchDegrees = 90; public Lay(){super(true);} public Lay(double y,int p){super(true); offsetY=y; pitchDegrees=p;} }
}
