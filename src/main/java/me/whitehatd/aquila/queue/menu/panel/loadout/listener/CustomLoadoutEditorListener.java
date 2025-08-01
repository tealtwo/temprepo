package me.whitehatd.aquila.queue.menu.panel.loadout.listener;

import gg.supervisor.core.annotation.Component;
import me.whitehatd.aquila.queue.menu.panel.loadout.LoadoutEditorMenu;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

@Component
public class CustomLoadoutEditorListener implements Listener {

    // Define the allowed actions for allowed slots.
    // These include standard pickup/placement operations.
    private static final Set<InventoryAction> ALLOWED_LOADOUT_ACTIONS = EnumSet.of(
            InventoryAction.PICKUP_ONE,
            InventoryAction.PICKUP_SOME,
            InventoryAction.PICKUP_ALL,
            InventoryAction.PLACE_ONE,
            InventoryAction.PLACE_SOME,
            InventoryAction.PLACE_ALL,
            InventoryAction.SWAP_WITH_CURSOR  // allows swapping between cursor and slot
    );

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoadoutEditorClick(InventoryClickEvent event) {
        // Check if the inventory belongs to a LoadoutEditorMenu.
        if (!(event.getWhoClicked().getOpenInventory().getTopInventory().getHolder() instanceof LoadoutEditorMenu))
            return;
        LoadoutEditorMenu menu = (LoadoutEditorMenu) event.getWhoClicked().getOpenInventory().getTopInventory().getHolder();

        int slot = event.getRawSlot();

        // Rule 1: If the clicked slot is not allowed, cancel the event.
        if (!menu.getEditableSlots().contains(slot)) {
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
            return;
        }

        // Rule 2: If the action is not one of the allowed pickup/placement actions, cancel.
        if (!ALLOWED_LOADOUT_ACTIONS.contains(event.getAction())) {
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoadoutEditorClose(InventoryCloseEvent event) {
        // Check if the closed inventory belongs to a LoadoutEditorMenu.
        if (!(event.getView().getTopInventory().getHolder() instanceof LoadoutEditorMenu)) return;

        event.getPlayer().setItemOnCursor(new ItemStack(Material.AIR));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoadoutEditorDrag(InventoryDragEvent event) {
        // Check if the dragged inventory belongs to a LoadoutEditorMenu.
        if (!(event.getWhoClicked().getOpenInventory().getTopInventory().getHolder()
                instanceof LoadoutEditorMenu)) return;
        LoadoutEditorMenu menu = (LoadoutEditorMenu) event.getWhoClicked().getOpenInventory().getTopInventory().getHolder();
        int topInvSize = event.getView().getTopInventory().getSize();

        // For drag events, ensure that every slot that is in the top inventory is allowed.
        for (Integer slot : event.getRawSlots()) {
            if (slot < topInvSize && !menu.getEditableSlots().contains(slot)) {
                event.setCancelled(true);
                event.setResult(org.bukkit.event.Event.Result.DENY);
                return;
            }
        }
    }
}
