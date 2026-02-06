package fr.xyness.SCS;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.CustomSet;

/**
 * This class handles CPlayer management and methods
 */
public class CPlayerMain {

    /** A map of player uuid to CPlayer instances */
    private final Map<UUID, CPlayer> players = new ConcurrentHashMap<>();

    /** A map of player uuid to players name instances */
    private final Map<UUID, String> playersName = new ConcurrentHashMap<>();

    /**
     * A case-insensitive map of player name (lowercase) to uuid.
     * Avoids O(n) scan and prevents concurrent iteration issues.
     */
    private final Map<String, UUID> playersUUIDLower = new ConcurrentHashMap<>();

    /** A set of players in DB */
    private final Set<UUID> playersRegistered = ConcurrentHashMap.newKeySet();

    /** A map of player names to their configuration settings */
    private final Map<UUID, Map<String, Double>> playersConfigSettings = new ConcurrentHashMap<>();

    /** Map of ItemStacks for players head */
    private final ConcurrentHashMap<String, ItemStack> playersHead = new ConcurrentHashMap<>();

    /** Map of players head hashed texture */
    private final ConcurrentHashMap<String, String> playersHashedTexture = new ConcurrentHashMap<>();

    /** Instance of SimpleClaimSystem */
    private final SimpleClaimSystem instance;

    /** Link of the mojang API */
    private final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";

    /** Link of the mojang profile API */
    private final String MOJANG_PROFILE_API_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";

    /** Defines the rate limit for requests in milliseconds */
    private static final int RATE_LIMIT = 50;

    /** Schedules tasks to run after a specified delay using a single-threaded executor */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /** Tracks the number of requests sent to calculate the scheduling delay for the next request */
    private final AtomicInteger requestCount = new AtomicInteger(0);

    /** Pattern for matching claim permissions */
    public static final Pattern CLAIM_PATTERN = Pattern.compile("scs\\.claim\\.(\\d+)");

    /** Pattern for matching radius permissions */
    public static final Pattern RADIUS_PATTERN = Pattern.compile("scs\\.radius\\.(\\d+)");

    /** Pattern for matching delay permissions */
    public static final Pattern DELAY_PATTERN = Pattern.compile("scs\\.delay\\.(\\d+)");

    /** Pattern for matching cost permissions */
    public static final Pattern COST_PATTERN = Pattern.compile("scs\\.claim-cost\\.(\\d+(\\.\\d+)?)");

    /** Pattern for matching cost permissions */
    public static final Pattern CHUNK_COST_PATTERN = Pattern.compile("scs\\.chunk-cost\\.(\\d+(\\.\\d+)?)");

    /** Pattern for matching multiplier permissions */
    public static final Pattern MULTIPLIER_PATTERN = Pattern.compile("scs\\.claim-cost-multiplier\\.(\\d+(\\.\\d+)?)");

    /** Pattern for matching chunk multiplier permissions */
    public static final Pattern CHUNK_MULTIPLIER_PATTERN = Pattern.compile("scs\\.chunk-cost-multiplier\\.(\\d+(\\.\\d+)?)");

    /** Pattern for matching member permissions */
    public static final Pattern MEMBERS_PATTERN = Pattern.compile("scs\\.members\\.(\\d+)");

    /** Pattern for matching chunks permissions */
    public static final Pattern CHUNKS_PATTERN = Pattern.compile("scs\\.chunks\\.(\\d+)");

    /** Pattern for matching distance permissions */
    public static final Pattern DISTANCE_PATTERN = Pattern.compile("scs\\.distance\\.(\\d+)");

    /** Pattern for matching chunks total permissions */
    public static final Pattern CHUNKS_TOTAL_PATTERN = Pattern.compile("scs\\.chunks-total\\.(\\d+)");

    /**
     * Constructor for CPlayerMain
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public CPlayerMain(SimpleClaimSystem instance) {
        this.instance = instance;
    }

    /**
     * Clears all maps and variables.
     */
    public void clearAll() {
        players.clear();
        playersName.clear();
        playersUUIDLower.clear();
        playersRegistered.clear();
        playersConfigSettings.clear();
        playersHead.clear();
        playersHashedTexture.clear();
    }

    /**
     * Checks if the player's data has changed.
     *
     * @param player The player to check and update claims for.
     */
    public void checkPlayer(Player player) {
        if (player == null) return;

        instance.executeAsync(() -> {
            final UUID uuid = player.getUniqueId();
            final String playerName = player.getName();
            final String oldName = playersName.get(uuid);

            final boolean needRegister = !playersRegistered.contains(uuid) || oldName == null;
            final boolean nameChanged = !needRegister && !oldName.equals(playerName);

            // Mojang API는 async에서 준비 (느림/외부 호출)
            String uuidMojang = null;
            String textures = null;

            // 등록이거나, 스킨/이름 변경 확인을 위해 조회 시도
            uuidMojang = getUUIDFromMojang(playerName);
            if (uuidMojang != null) {
                textures = getSkinURLWithoutDelay(uuidMojang);
            }

            final String uuidMojangFinal = (uuidMojang == null ? "none" : uuidMojang);
            final String texturesFinal = (textures == null ? "none" : textures);

            // 1) 메모리 캐시/Claim/Bukkit API는 반드시 sync에서
            instance.executeSync(() -> {
                if (needRegister) {
                    playersRegistered.add(uuid);
                    playersName.put(uuid, playerName);
                    playersUUIDLower.put(playerName.toLowerCase(Locale.ROOT), uuid);

                    ItemStack head;
                    if (!"none".equals(uuidMojangFinal) && !"none".equals(texturesFinal)) {
                        head = createPlayerHeadWithTexture(uuidMojangFinal, texturesFinal);
                    } else {
                        head = new ItemStack(Material.PLAYER_HEAD);
                    }

                    playersHead.put(playerName, head);
                    playersHashedTexture.put(playerName, texturesFinal);

                    instance.getLogger().info(playerName + " is now registered (" + uuid + ").");
                }

                if (nameChanged) {
                    // 이름 캐시 갱신
                    playersUUIDLower.remove(oldName.toLowerCase(Locale.ROOT));
                    playersName.put(uuid, playerName);
                    playersUUIDLower.put(playerName.toLowerCase(Locale.ROOT), uuid);

                    // head/texture 키도 이전 이름에서 새 이름으로 이동
                    ItemStack oldHead = playersHead.remove(oldName);
                    String oldTex = playersHashedTexture.remove(oldName);
                    if (oldHead != null) playersHead.put(playerName, oldHead);
                    if (oldTex != null) playersHashedTexture.put(playerName, oldTex);

                    // ✅ Claim 객체 변경 + BossBar 갱신은 sync에서만
                    CustomSet<Claim> claims = instance.getMain().getPlayerClaims(uuid);
                    if (claims != null && !claims.isEmpty()) {
                        claims.forEach(c -> {
                            c.setOwner(playerName);
                            instance.getBossBars().activateBossBar(c.getChunks());
                        });
                        instance.getMain().setPlayerClaims(uuid, claims);
                    }

                    instance.getLogger().info(oldName + " changed their name to " + playerName + " (" + uuid + "), new name saved.");
                }

                // 스킨 갱신: textures가 유효하고, 기존과 다르면 교체
                if (!"none".equals(uuidMojangFinal) && !"none".equals(texturesFinal)) {
                    String currentHash = playersHashedTexture.getOrDefault(playerName, "none");
                    if (!texturesFinal.equals(currentHash)) {
                        ItemStack head = createPlayerHeadWithTexture(uuidMojangFinal, texturesFinal);
                        playersHead.put(playerName, head);
                        playersHashedTexture.put(playerName, texturesFinal);

                        instance.getLogger().info(playerName + " changed their skin (" + uuid + "), new textures saved.");
                    }
                } else {
                    // fallback head 보장
                    playersHead.putIfAbsent(playerName, new ItemStack(Material.PLAYER_HEAD));
                    playersHashedTexture.putIfAbsent(playerName, "none");
                }
            });

            // 2) DB 갱신은 async에서
            if (needRegister) {
                try (Connection connection = instance.getDataSource().getConnection()) {
                    String dbProductName = connection.getMetaData().getDatabaseProductName().toLowerCase();
                    String updateQuery;

                    if (dbProductName.contains("sqlite")) {
                        updateQuery = "INSERT INTO scs_players(uuid_server, uuid_mojang, player_name, player_head, player_textures) " +
                                "VALUES(?, ?, ?, ?, ?) ON CONFLICT(uuid_server) DO UPDATE SET player_name = excluded.player_name";
                    } else if (dbProductName.contains("mysql")) {
                        updateQuery = "INSERT INTO scs_players(uuid_server, uuid_mojang, player_name, player_head, player_textures) " +
                                "VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name = VALUES(player_name)";
                    } else {
                        throw new UnsupportedOperationException("Unsupported database: " + dbProductName);
                    }

                    try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                        preparedStatement.setString(1, uuid.toString());
                        preparedStatement.setString(2, "none".equals(uuidMojangFinal) ? "none" : uuidMojangFinal);
                        preparedStatement.setString(3, playerName);
                        preparedStatement.setString(4, "");
                        preparedStatement.setString(5, texturesFinal);
                        preparedStatement.executeUpdate();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (nameChanged) {
                try (Connection connection = instance.getDataSource().getConnection()) {
                    String updateQuery = "UPDATE scs_players SET player_name = ? WHERE uuid_server = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                        preparedStatement.setString(1, playerName);
                        preparedStatement.setString(2, uuid.toString());
                        preparedStatement.executeUpdate();
                    }

                    updateQuery = "UPDATE scs_claims_1 SET owner_name = ? WHERE owner_uuid = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                        preparedStatement.setString(1, playerName);
                        preparedStatement.setString(2, uuid.toString());
                        preparedStatement.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                // 스킨만 변경되었을 가능성: texturesFinal이 유효할 때만 업데이트
                if (!"none".equals(texturesFinal)) {
                    try (Connection connection = instance.getDataSource().getConnection()) {
                        String updateQuery = "UPDATE scs_players SET player_textures = ? WHERE uuid_server = ?";
                        try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                            preparedStatement.setString(1, texturesFinal);
                            preparedStatement.setString(2, uuid.toString());
                            preparedStatement.executeUpdate();
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Loads player data from Bukkit and inserts it into the database.
     * If a player already exists in the database, their name is updated.
     */
    public void loadPlayers() {
        instance.info(" ");
        instance.info(net.md_5.bungee.api.ChatColor.DARK_GREEN + "Loading players..");
        int i = 0;

        try (Connection connection = instance.getDataSource().getConnection()) {
            String getQuery = "SELECT * FROM scs_players";
            try (PreparedStatement preparedStatement = connection.prepareStatement(getQuery);
                 ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    UUID uuid = UUID.fromString(resultSet.getString("uuid_server"));
                    String uuid_mojang = resultSet.getString("uuid_mojang");
                    String playerName = resultSet.getString("player_name");
                    String textures = resultSet.getString("player_textures");

                    // 캐시 갱신은 sync에서 (Bukkit profile 생성 포함)
                    final int[] inc = {0};
                    instance.executeSync(() -> {
                        ItemStack playerHead = createPlayerHeadWithTexture(uuid_mojang, textures);
                        playersHead.put(playerName, playerHead);
                        playersHashedTexture.put(playerName, textures == null ? "none" : textures);
                        playersName.put(uuid, playerName);
                        playersUUIDLower.put(playerName.toLowerCase(Locale.ROOT), uuid);
                        playersRegistered.add(uuid);
                        inc[0] = 1;
                    });
                    i += inc[0];
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        instance.info(instance.getMain().getNumberSeparate(String.valueOf(i)) + " players loaded.");
    }

    /**
     * Get or create a player head with the correct texture.
     *
     * @param playerName The player name.
     * @return The ItemStack representing the player's head.
     */
    public ItemStack getPlayerHead(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        ItemStack cached = playersHead.get(playerName);
        if (cached != null) return cached;

        // computeIfAbsent는 thread-safe이나, Bukkit profile 생성은 sync에서
        final CompletableFuture<ItemStack> future = new CompletableFuture<>();
        instance.executeSync(() -> {
            ItemStack head = playersHead.computeIfAbsent(playerName, p -> {
                ItemStack h = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) h.getItemMeta();
                if (meta != null) {
                    PlayerProfile profile = Bukkit.createPlayerProfile(playerName);
                    if (profile != null) meta.setOwnerProfile(profile);
                    h.setItemMeta(meta);
                }
                return h;
            });
            future.complete(head == null ? new ItemStack(Material.PLAYER_HEAD) : head);
        });
        return future.join();
    }

    /**
     * Adds dashes to a UUID string if they are missing.
     *
     * @param uuid The UUID string without dashes.
     * @return The UUID string with dashes.
     */
    private String addDashesToUUID(String uuid) {
        if (uuid != null && uuid.length() == 32) {
            return uuid.replaceFirst(
                    "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
                    "$1-$2-$3-$4-$5"
            );
        }
        return uuid;
    }

    /**
     * Creates an ItemStack of a player head with the specified texture.
     *
     * @param uuid The UUID of the player.
     * @param texture The texture URL of the player's head
     * @return An ItemStack representing the player's head with the applied texture.
     */
    public ItemStack createPlayerHeadWithTexture(String uuid, String texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null
                && uuid != null && !uuid.isBlank() && !"none".equals(uuid)
                && texture != null && !texture.isBlank() && !"none".equals(texture)) {
            try {
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.fromString(uuid));
                URI uri = URI.create(texture);
                URL url = uri.toURL();
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(url);
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
                head.setItemMeta(meta);
            } catch (Exception ignore) {
                return head;
            }
        }
        return head;
    }

    /**
     * Creates an ItemStack of a custom skull with the specified texture id.
     *
     * @param texture The texture id of the player's head (not full URL)
     * @return An ItemStack representing the head with the applied texture.
     */
    public ItemStack createPlayerHeadWithTexture(String texture, String title, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null && texture != null && !texture.isBlank()) {
            try {
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                URI uri = URI.create("http://textures.minecraft.net/texture/" + texture);
                URL url = uri.toURL();
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(url);
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
                if (title != null) meta.setDisplayName(title);
                if (lore != null && !lore.isEmpty()) meta.setLore(lore);
                head.setItemMeta(meta);
            } catch (MalformedURLException ignore) {
                return head;
            }
        }
        return head;
    }

    /**
     * Retrieves the URL of a Minecraft player's skin texture from Mojang using the player's UUID.
     *
     * @param uuid The UUID of the player whose skin texture URL is to be retrieved.
     * @return A CompletableFuture that resolves to a string representing the URL of the player's skin texture, or null if an error occurs.
     */
    public CompletableFuture<String> getSkinURL(String uuid) {
        CompletableFuture<String> future = new CompletableFuture<>();
        int delayMs = requestCount.getAndIncrement() * RATE_LIMIT;

        scheduler.schedule(() -> {
            try {
                URI uri = URI.create(MOJANG_PROFILE_API_URL + uuid);
                URL url = uri.toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject properties = response.getAsJsonArray("properties").get(0).getAsJsonObject();
                    String value = properties.get("value").getAsString();
                    String decodedValue = new String(Base64.getDecoder().decode(value));
                    JsonObject textureProperty = JsonParser.parseString(decodedValue).getAsJsonObject();
                    future.complete(textureProperty.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString());
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            future.complete(null);
        }, delayMs, TimeUnit.MILLISECONDS);

        return future;
    }

    /**
     * Retrieves the URL of a Minecraft player's skin texture from Mojang using the player's UUID.
     *
     * @param uuid The UUID of the player whose skin texture URL is to be retrieved.
     * @return A String representing the URL of the player's skin texture, or null if an error occurs.
     */
    public String getSkinURLWithoutDelay(String uuid) {
        try {
            URI uri = URI.create(MOJANG_PROFILE_API_URL + uuid);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject properties = response.getAsJsonArray("properties").get(0).getAsJsonObject();
                String value = properties.get("value").getAsString();
                String decodedValue = new String(Base64.getDecoder().decode(value));
                JsonObject textureProperty = JsonParser.parseString(decodedValue).getAsJsonObject();
                return textureProperty.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves the UUID of a player from Mojang's API using the player's name.
     *
     * @param playerName The name of the player.
     * @return The UUID of the player as a string, or null if an error occurs.
     */
    private String getUUIDFromMojang(String playerName) {
        try {
            URI uri = URI.create(MOJANG_API_URL + playerName);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                    JsonObject responseJson = JsonParser.parseReader(reader).getAsJsonObject();
                    return addDashesToUUID(responseJson.get("id").getAsString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Removes the CPlayer instance associated with the given player uuid.
     *
     * @param targetUUID The uuid of the player
     */
    public void removeCPlayer(UUID targetUUID) {
        if (targetUUID == null) return;
        players.remove(targetUUID);
    }

    /**
     * Gets the CPlayer instance associated with the given player uuid.
     *
     * @param targetUUID The uuid of the player
     * @return The CPlayer instance, or null if not found
     */
    public CPlayer getCPlayer(UUID targetUUID) {
        if (targetUUID == null) return null;
        return players.get(targetUUID);
    }

    /**
     * Gets the player name associated with the given player uuid.
     *
     * @param targetUUID The uuid of the player
     * @return The player name
     */
    public String getPlayerName(UUID targetUUID) {
        if (targetUUID == null) return null;
        String name = playersName.get(targetUUID);
        return name == null ? Bukkit.getOfflinePlayer(targetUUID).getName() : name;
    }

    /**
     * Gets the player UUID associated with the given player name, case-insensitively.
     *
     * @param targetName The name of the player.
     * @return The player's UUID, or null if not found.
     */
    public UUID getPlayerUUID(String targetName) {
        if (targetName == null || targetName.isBlank()) return null;

        UUID uuid = playersUUIDLower.get(targetName.toLowerCase(Locale.ROOT));
        if (uuid != null) return uuid;

        return Bukkit.getOfflinePlayer(targetName).getUniqueId();
    }

    /**
     * Sets the configuration settings for all players.
     *
     * @param p A map of player uuids to their configuration settings
     */
    public void setPlayersConfigSettings(Map<UUID, Map<String, Double>> p) {
        playersConfigSettings.clear();
        if (p == null || p.isEmpty()) return;

        // 내부 map도 Concurrent로 감싸서 이후 update에서 안전하게
        for (Map.Entry<UUID, Map<String, Double>> e : p.entrySet()) {
            UUID uuid = e.getKey();
            Map<String, Double> settings = e.getValue();
            if (uuid == null || settings == null) continue;
            playersConfigSettings.put(uuid, new ConcurrentHashMap<>(settings));
        }
    }

    /**
     * Update a player setting ("players" in config.yml)
     *
     * @param playerId The UUID of player
     * @param key The key of the setting
     * @param value The value of the setting
     */
    public void updatePlayerConfigSettings(UUID playerId, String key, Double value) {
        if (playerId == null || key == null || value == null) return;
        playersConfigSettings.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    /**
     * Checks if a player can add a member to their claim.
     *
     * @param player The player
     * @param claim The claim
     * @return True if the player can add a member, false otherwise
     */
    public boolean canAddMember(Player player, Claim claim) {
        if (player == null || claim == null) return false;
        if (player.hasPermission("scs.admin")) return true;

        CPlayer cPlayer = players.get(player.getUniqueId());
        if (cPlayer == null) return false;

        int i = claim.getMembers().size();
        int nb_members = cPlayer.getMaxMembers();
        return nb_members == 0 || nb_members > i;
    }

    /**
     * Checks if a player has a specific permission.
     *
     * @param player The player
     * @param perm The permission to check
     * @return True if the player has the permission, false otherwise
     */
    public boolean checkPermPlayer(Player player, String perm) {
        if (player == null || perm == null) return false;
        return player.hasPermission("scs.admin") || player.hasPermission(perm);
    }

    /**
     * Activates fly mode for the player.
     *
     * @param player The player
     */
    public void activePlayerFly(Player player) {
        if (player == null) return;
        CPlayer cPlayer = players.get(player.getUniqueId());
        if (cPlayer == null) return;

        if (!player.getAllowFlight()) {
            player.setAllowFlight(true);
        }
        player.setFlying(true);
        cPlayer.setClaimFly(true);
    }

    /**
     * Removes fly mode from the player.
     *
     * @param player The player
     */
    public void removePlayerFly(Player player) {
        if (player == null) return;
        CPlayer cPlayer = players.get(player.getUniqueId());
        if (cPlayer == null) return;

        if (cPlayer.getClaimFly()) {
            GameMode pMode = player.getGameMode();
            if (pMode.equals(GameMode.ADVENTURE) || pMode.equals(GameMode.SURVIVAL)) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
            cPlayer.setClaimFly(false);
        }
    }

    /**
     * Returns the player config from "players" section in config.yml
     *
     * @param uuid The target uuid
     * @return The player config
     */
    public Map<String, Double> getPlayerConfig(UUID uuid) {
        if (uuid == null) return null;
        return playersConfigSettings.get(uuid);
    }

    /**
     * Sets the permissions of a player when he joins the server.
     *
     * @param player The player
     */
    public void addPlayerPermSetting(Player player) {
        if (player == null) return;

        // CPlayer 생성은 Bukkit 객체를 포함하므로 sync에서 저장이 안전
        instance.executeSync(() -> {
            UUID playerId = player.getUniqueId();
            int count = instance.getMain().getPlayerClaimsCount(playerId);
            players.put(playerId, new CPlayer(player, playerId, count, instance));
        });
    }
}
