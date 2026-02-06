package fr.xyness.SCS.Types;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.CScoreboard;
import fr.xyness.SCS.SimpleClaimSystem;

/**
 * This class handles CPlayer object
 */
public class CPlayer {

    // ***************
    // *  Variables  *
    // ***************

    /** The player associated with this CPlayer instance */
    private volatile Player player;

    /** The uuid of player */
    private final UUID playerId;

    /** The name of the player */
    private volatile String playerName;

    /** The number of claims the player has */
    private volatile int claimsCount;

    /** Whether the player has claim chat enabled */
    private volatile boolean claimChat;

    /** Whether the player has claim automap enabled */
    private volatile boolean claimAutomap;

    /** Whether the player has claim autoaddchunk enabled */
    private volatile String claimAuto;

    /** The claim for autoaddchunk and autodelchunk */
    private volatile Claim claimChunk;

    /** Whether the player has claim autofly enabled */
    private volatile boolean claimAutofly;

    /** Whether the player has claim fly enabled */
    private volatile boolean claimFly;

    /** The current GUI page */
    private volatile int guiPage;

    /** The current claim for the GUI */
    private volatile Claim claim;

    /** A map of claims for the GUI, indexed by slot */
    private final Map<Integer, Claim> mapClaims = new ConcurrentHashMap<>();

    /** A map of locations for the GUI, indexed by slot */
    private final Map<Integer, Location> mapLoc = new ConcurrentHashMap<>();

    /** A map of strings for the GUI, indexed by slot */
    private final Map<Integer, String> mapString = new ConcurrentHashMap<>();

    /** The filter for the GUI */
    private volatile String filter;

    /** The owner for the GUI */
    private volatile String owner;

    /** The player's scoreboard */
    private volatile CScoreboard scoreboard;

    /** Instance of SimpleClaimSystem */
    private final SimpleClaimSystem instance;

    // ******************
    // *  Constructors  *
    // ******************

    /**
     * Constructor initializing all fields.
     *
     * @param player       The player associated with this CPlayer instance.
     * @param playerId     The UUID of the player.
     * @param claimsCount  The number of claims the player has.
     * @param instance     Instance of SimpleClaimSystem.
     */
    public CPlayer(Player player, UUID playerId, Integer claimsCount, SimpleClaimSystem instance) {
        this.player = player;
        this.playerId = playerId;
        this.playerName = player != null ? player.getName() : null;
        this.claimsCount = claimsCount == null ? 0 : claimsCount;

        this.guiPage = 0;
        this.claimChat = false;
        this.claimAutomap = false;
        this.claimAutofly = false;
        this.claimAuto = "";
        this.claimFly = false;

        this.filter = null;
        this.owner = null;
        this.scoreboard = null;

        this.instance = instance;
    }

    // ********************
    // *  Other methods   *
    // ********************

    // --------------------
    // Setters
    // --------------------

    public void setPlayer(Player player) {
        this.player = player;
        if (player != null) {
            this.playerName = player.getName();
        }
    }

    public void setName(String playerName) {
        this.playerName = playerName;
    }

    public void setClaimsCount(Integer claimsCount) {
        this.claimsCount = claimsCount == null ? 0 : claimsCount;
    }

    public void setGuiPage(Integer page) {
        this.guiPage = page == null ? 0 : page;
    }

    public void setClaimChat(Boolean setting) {
        this.claimChat = setting != null && setting;
    }

    public void setClaimAutomap(Boolean setting) {
        this.claimAutomap = setting != null && setting;
    }

    public void setClaimAuto(String setting) {
        this.claimAuto = setting == null ? "" : setting;
    }

    public void setTargetClaimChunk(Claim claim) {
        this.claimChunk = claim;
    }

    public void setClaim(Claim claim) {
        this.claim = claim;
    }

    public void addMapClaim(Integer slot, Claim claim) {
        if (slot == null) return;
        if (claim == null) {
            mapClaims.remove(slot);
            return;
        }
        mapClaims.put(slot, claim);
    }

    public void addMapLoc(Integer slot, Location loc) {
        if (slot == null) return;
        if (loc == null) {
            mapLoc.remove(slot);
            return;
        }
        mapLoc.put(slot, loc);
    }

    public void addMapString(Integer slot, String s) {
        if (slot == null) return;
        if (s == null) {
            mapString.remove(slot);
            return;
        }
        mapString.put(slot, s);
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setClaimAutofly(Boolean setting) {
        this.claimAutofly = setting != null && setting;
    }

    public void setClaimFly(Boolean setting) {
        this.claimFly = setting != null && setting;
    }

    public void setScoreboard(CScoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    // --------------------
    // Getters
    // --------------------

    public Player getPlayer() {
        return this.player;
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public String getName() {
        return this.playerName;
    }

    public int getClaimsCount() {
        return this.claimsCount;
    }

    public int getGuiPage() {
        return this.guiPage;
    }

    public boolean getClaimChat() {
        return this.claimChat;
    }

    public boolean getClaimAutomap() {
        return this.claimAutomap;
    }

    public String getClaimAuto() {
        return this.claimAuto;
    }

    public Claim getTargetClaimChunk() {
        return this.claimChunk;
    }

    public Claim getClaim() {
        return this.claim;
    }

    public Claim getMapClaim(Integer slot) {
        if (slot == null) return null;
        return this.mapClaims.get(slot);
    }

    public Location getMapLoc(Integer slot) {
        if (slot == null) return null;
        return this.mapLoc.get(slot);
    }

    public String getMapString(Integer slot) {
        if (slot == null) return null;
        return this.mapString.get(slot);
    }

    public String getFilter() {
        return this.filter;
    }

    public String getOwner() {
        return this.owner;
    }

    public boolean getClaimAutofly() {
        return this.claimAutofly;
    }

    public boolean getClaimFly() {
        return this.claimFly;
    }

    public CScoreboard getScoreboard() {
        return this.scoreboard;
    }

    // --------------------
    // Permission-derived values
    // --------------------

    public Integer getMaxClaims() {
        Player p = this.player;
        if (p == null) return 0;
        if (p.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("max-claims")) {
            return (int) Math.round(playerConfig.get("max-claims"));
        }

        int n = p.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .map(CPlayerMain.CLAIM_PATTERN::matcher)
                .filter(Matcher::find)
                .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                .max().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();

            n = (int) Math.round(groupsSettings.get("default").get("max-claims"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                String perm = entry.getValue();
                if (perm != null && instance.getPlayerMain().checkPermPlayer(p, perm)) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-claims")));
                }
            }
        }

        return n;
    }

    public Integer getMaxRadiusClaims() {
        Player p = this.player;
        if (p == null) return 0;
        if (p.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("max-radius-claims")) {
            return (int) Math.round(playerConfig.get("max-radius-claims"));
        }

        int n = p.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .map(CPlayerMain.RADIUS_PATTERN::matcher)
                .filter(Matcher::find)
                .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                .max().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();

            n = (int) Math.round(groupsSettings.get("default").get("max-radius-claims"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                String perm = entry.getValue();
                if (perm != null && instance.getPlayerMain().checkPermPlayer(p, perm)) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-radius-claims")));
                }
            }
        }

        return n;
    }

    public int getDelay() {
        Player p = this.player;
        if (p == null) return 0;
        if (p.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("teleportation-delay")) {
            return (int) Math.round(playerConfig.get("teleportation-delay"));
        }

        int n = p.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .map(CPlayerMain.DELAY_PATTERN::matcher)
                .filter(Matcher::find)
                .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                .min().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();

            n = (int) Math.round(groupsSettings.get("default").get("teleportation-delay"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                String perm = entry.getValue();
                if (perm != null && instance.getPlayerMain().checkPermPlayer(p, perm)) {
                    n = Math.min(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("teleportation-delay")));
                }
            }
        }

        return n;
    }

    public int getMaxMembers() {
        Player p = this.player;
        if (p == null) return 0;
        if (p.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("max-members")) {
            return (int) Math.round(playerConfig.get("max-members"));
        }

        int n = p.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .map(CPlayerMain.MEMBERS_PATTERN::matcher)
                .filter(Matcher::find)
                .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                .max().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();

            n = (int) Math.round(groupsSettings.get("default").get("max-members"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                String perm = entry.getValue();
                if (perm != null && instance.getPlayerMain().checkPermPlayer(p, perm)) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-members")));
                }
            }
        }

        return n;
    }

    public double getCost() {
        Player p = this.player;
        if (p == null) return 0;
        if (p.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("claim-cost")) {
            return playerConfig.get("claim-cost");
        }

        double n = p.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .map(CPlayerMain.COST_PATTERN::matcher)
                .filter(Matcher::find)
                .mapToDouble(matcher -> Double.parseDouble(matcher.group(1)))
                .min().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();

            n = groupsSettings.get("default").get("claim-cost");
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                String perm = entry.getValue();
                if (perm != null && instance.getPlayerMain().checkPermPlayer(p, perm)) {
                    n = Math.min(n, groupsSettings.get(entry.getKey()).get("claim-cost"));
                }
            }
        }

        return n;
    }

    public double getChunkCost() {
        Player p = this.player;
        if (p == null) return 0;
        if (p.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("chunk-cost")) {
            return playerConfig.get("chunk-cost");
        }

        double n = p.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .map(CPlayerMain.CHUNK_COST_PATTERN::matcher)
                .filter(Matcher::find)
                .mapToDouble(matcher -> Double.parseDouble(matcher.group(1)))
                .min().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();

            n = groupsSettings.get("default").get("chunk-cost");
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                String perm = entry.getValue();
                if (perm != null && instance.getPlayerMain().checkPermPlayer(p, perm)) {
                    n = Math.min(n, groupsSettings.get(entry.getKey()).get("chunk-cost"));
                }
            }
        }

        return n;
    }

    public double getMultiplier() {
        Player p = this.player;
        if (p == null) return 0;
        if (p.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("claim-cost-multiplier")) {
            return playerConfig.get("claim-cost-multiplier");
        }

        double n = p.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .map(CPlayerMain.MULTIPLIER_PATTERN::matcher)
                .filter(Matcher::find)
                .mapToDouble(matcher -> Double.parseDouble(matcher.group(1)))
                .min().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();

            n = groupsSettings.get("default").get("claim-cost-multiplier");
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                String perm = entry.getValue();
                if (perm != null && instance.getPlayerMain().checkPermPlayer(p, perm)) {
                    n = Math.min(n, groupsSettings.get(entry.getKey()).get("claim-cost-multiplier"));
                }
            }
        }

        return n;
    }

    public double getChunkMultiplier() {
        Player p = this.player;
        if (p == null) return 0;
        if (p.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("chunk-cost-multiplier")) {
            return playerConfig.get("chunk-cost-multiplier");
        }

        double n = p.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .map(CPlayerMain.CHUNK_MULTIPLIER_PATTERN::matcher)
                .filter(Matcher::find)
                .mapToDouble(matcher -> Double.parseDouble(matcher.group(1)))
                .min().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();

            n = groupsSettings.get("default").get("chunk-cost-multiplier");
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                String perm = entry.getValue();
                if (perm != null && instance.getPlayerMain().checkPermPlayer(p, perm)) {
                    n = Math.min(n, groupsSettings.get(entry.getKey()).get("chunk-cost-multiplier"));
                }
            }
        }

        return n;
    }

    public int getMaxChunksPerClaim() {
        Player p = this.player;
        if (p == null) return 0;
        if (p.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("max-chunks-per-claim")) {
            return (int) Math.round(playerConfig.get("max-chunks-per-claim"));
        }

        int n = p.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .map(CPlayerMain.CHUNKS_PATTERN::matcher)
                .filter(Matcher::find)
                .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                .max().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();

            n = (int) Math.round(groupsSettings.get("default").get("max-chunks-per-claim"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                String perm = entry.getValue();
                if (perm != null && instance.getPlayerMain().checkPermPlayer(p, perm)) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-chunks-per-claim")));
                }
            }
        }

        return n;
    }

    public int getClaimDistance() {
        Player p = this.player;
        if (p == null) return 0;
        if (p.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("claim-distance")) {
            return (int) Math.round(playerConfig.get("claim-distance"));
        }

        int n = p.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .map(CPlayerMain.DISTANCE_PATTERN::matcher)
                .filter(Matcher::find)
                .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                .min().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();

            n = (int) Math.round(groupsSettings.get("default").get("claim-distance"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                String perm = entry.getValue();
                if (perm != null && instance.getPlayerMain().checkPermPlayer(p, perm)) {
                    n = Math.min(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("claim-distance")));
                }
            }
        }

        return n;
    }

    public int getMaxChunksTotal() {
        Player p = this.player;
        if (p == null) return 0;
        if (p.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("max-chunks-total")) {
            return (int) Math.round(playerConfig.get("max-chunks-total"));
        }

        int n = p.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .map(CPlayerMain.CHUNKS_TOTAL_PATTERN::matcher)
                .filter(Matcher::find)
                .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                .max().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();

            n = (int) Math.round(groupsSettings.get("default").get("max-chunks-total"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                String perm = entry.getValue();
                if (perm != null && instance.getPlayerMain().checkPermPlayer(p, perm)) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-chunks-total")));
                }
            }
        }

        return n;
    }

    // --------------------
    // Simple helpers
    // --------------------

    public boolean canClaim() {
        Player p = this.player;
        if (p == null) return false;
        if (p.hasPermission("scs.admin")) return true;

        int maxClaims = getMaxClaims();
        return maxClaims == 0 || maxClaims > claimsCount;
    }

    public boolean canClaimX(int n) {
        Player p = this.player;
        if (p == null) return false;
        if (p.hasPermission("scs.admin")) return true;

        int maxClaims = getMaxClaims();
        return maxClaims == 0 || maxClaims > (claimsCount + n);
    }

    public boolean canClaimWithNumber(int n) {
        Player p = this.player;
        if (p == null) return false;
        if (p.hasPermission("scs.admin")) return true;

        int maxChunks = getMaxChunksPerClaim();
        return maxChunks == 0 || maxChunks >= n;
    }

    public boolean canClaimTotalWithNumber(int total) {
        Player p = this.player;
        if (p == null) return false;
        if (p.hasPermission("scs.admin")) return true;

        int maxChunks = getMaxChunksTotal();
        return maxChunks == 0 || maxChunks >= total;
    }

    public boolean canRadiusClaim(int r) {
        Player p = this.player;
        if (p == null) return false;
        if (p.hasPermission("scs.admin")) return true;

        int radius = getMaxRadiusClaims();
        return radius == 0 || radius >= r;
    }

    public Double getMultipliedCost() {
        Player p = this.player;
        if (p == null) return 0.0;
        if (p.hasPermission("scs.admin")) return 0.0;

        double cost = getCost();
        double multiplier = getMultiplier();
        double result = cost * Math.pow(multiplier, claimsCount);
        return Math.round(result * 100.0) / 100.0;
    }

    public Double getChunkMultipliedCost(int nbChunks) {
        Player p = this.player;
        if (p == null) return 0.0;
        if (p.hasPermission("scs.admin")) return 0.0;

        double cost = getChunkCost();
        double multiplier = getChunkMultiplier();
        double result = cost * Math.pow(multiplier, (nbChunks - 1));
        return Math.round(result * 100.0) / 100.0;
    }

    public Double getRadiusMultipliedCost(int r) {
        Player p = this.player;
        if (p == null) return 0.0;
        if (p.hasPermission("scs.admin")) return 0.0;

        int n = claimsCount;
        double price = 0.0;
        double cost = getCost();
        double multiplier = getMultiplier();

        for (int i = 0; i < r; i++) {
            price += cost * Math.pow(multiplier, (n - 1));
            n++;
        }
        return Math.round(price * 100.0) / 100.0;
    }

    // --------------------
    // GUI maps
    // --------------------

    public void clearMapClaim() { mapClaims.clear(); }
    public void clearMapLoc() { mapLoc.clear(); }
    public void clearMapString() { mapString.clear(); }
}
