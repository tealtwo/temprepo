package me.whitehatd.aquila.queue.bridge.match;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public enum MatchType {

    ONE_VS_ONE("1v1", Material.IRON_SWORD, 1),
    TWO_VS_TWO("2v2",Material.GOLDEN_SWORD, 2),
    THREE_VS_THREE("3v3", Material.DIAMOND_SWORD, 3),
    FOUR_VS_FOUR("4v4", Material.NETHERITE_SWORD, 4),
    PARTY_VS_PARTY("Party vs Party", Material.NETHER_STAR, -1);

    private final Material material;
    private final String name;
    private final int size;

    MatchType(String name, Material material, int size) {
        this.material = material;
        this.name = name;
        this.size = size;
    }
}
