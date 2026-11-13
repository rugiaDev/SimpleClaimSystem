package fr.xyness.SCS.Support;

import net.playavalon.mythicdungeons.MythicDungeons;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * MythicDungeons 던전 안인지 알아보는 곳.
 *
 * <p>언제 쓰나: 던전 인스턴스 월드에서는 점유 기능을 통째로 끄기 위해
 * (config.yml {@code mythicdungeons-disable: true}) 월드 모드를 고를 때 물어본다.</p>
 *
 * <p>MythicDungeons 가 없거나 꺼져 있으면 언제나 "던전 아님" 으로 답한다.</p>
 */
public final class ClaimMythicDungeons {

    private ClaimMythicDungeons() {}

    /**
     * 이 이름의 월드가 지금 돌아가는 MythicDungeons 던전(인스턴스) 월드인지.
     *
     * @param worldName 월드 이름
     * @return 던전 월드면 true
     */
    public static boolean isDungeonWorld(String worldName) {
        if (worldName == null) return false;
        Plugin mythicDungeonsPlugin = Bukkit.getPluginManager().getPlugin("MythicDungeons");
        if (mythicDungeonsPlugin == null || !mythicDungeonsPlugin.isEnabled()) return false;
        return lookUpInstance(worldName);
    }

    /** 실제 조회 — 플러그인이 있을 때만 불러야 클래스를 못 찾는 오류가 나지 않는다. */
    private static boolean lookUpInstance(String worldName) {
        MythicDungeons mythicDungeons = MythicDungeons.inst();
        if (mythicDungeons == null) return false;
        return mythicDungeons.getDungeonInstance(worldName) != null;
    }
}
