package me.whitehatd.aquila.queue.bridge.match;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detailed statistics for a player during a single match.
 */
@Data
public class PlayerMatchStats {

    private final UUID playerId;

    public PlayerMatchStats(UUID playerId) {
        this.playerId = playerId;
    }
    
    // Health statistics
    private double finalHealth = 0;        // Health at the end of the match
    private double maxHealth = 0;          // Maximum health (e.g., starting or peak health)
    private double healthRegenerated = 0;  // Total health regained via regeneration, potions, etc.
    
    // Combat statistics
    private double damageDealt = 0;             // Total damage dealt to opponents
    private double damageTaken = 0;            // Total damage received
    private int hitsLanded = 0;             // Number of successful hits landed
    private int hitsReceived = 0;            // Number of times hit by opponents
    private int criticalHits = 0;            // Count of critical hits landed
    private int maxCombo = 0;               // Maximum combo (consecutive hits) achieved
    private double longestDistanceHit = 0;     // Longest distance from which a hit was registered
    
    // Bow statistics
    private int arrowsShot = 0;              // Total arrows fired
    private int arrowsHit = 0;               // Total arrows that hit targets
    /**
     * Bow accuracy percentage, calculated as: 
     * (arrowsHit / arrowsShot * 100)
     */
    private double bowAccuracy = 0;

    // Movement statistics
    private int blocksTraversed = 0;         // Total blocks walked/traversed
    private int jumps = 0;                  // Number of jumps
    private double sprintDistance = 0;         // Total sprint distance in blocks
    
    // ELO statistics (for ranked matches)
    private int previousElo = 0;             // ELO before the match
    private int newElo = 0;                 // ELO after the match
    private int eloChange = 0;              // Difference (newElo - previousElo)
    
    // Inventory statistics
    /**
     * Serialized representation (e.g. JSON) of the player's final inventory.
     */
    private String finalInventoryJson = "";
    private int potionsUsed = 0;            // Total potions consumed during the match
    private int foodConsumed = 0;           // Total food items consumed


    private Map<ArmorSlot, Double> armorDurability = new HashMap<>();
    
    // Additional statistics
    private int blocksPlaced = 0;            // Total blocks placed
    private int blocksBroken = 0;            // Total blocks broken
    private int itemsPickedUp = 0;           // Total items picked up
    private int itemsDropped = 0;            // Total items dropped

    private boolean winner;
}
