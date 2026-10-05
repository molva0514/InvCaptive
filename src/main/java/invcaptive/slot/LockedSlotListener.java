package invcaptive.slot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;

public final class LockedSlotListener implements Listener {

    private final SlotLockManager slotLockManager;

    public LockedSlotListener(SlotLockManager slotLockManager) {
        this.slotLockManager = slotLockManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {

        if (!slotLockManager.isActive()) {
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory instanceof PlayerInventory) {

            int slot = event.getSlot();

            if (slotLockManager.isLocked(slot)) {
                event.setCancelled(true);
                return;
            }
        }

        if (slotLockManager.isLockedBarrier(event.getCurrentItem())
                || slotLockManager.isLockedBarrier(event.getCursor())) {

            event.setCancelled(true);
            return;
        }

        int hotbarButton = event.getHotbarButton();

        if (hotbarButton >= 0
                && slotLockManager.isLocked(hotbarButton)) {

            event.setCancelled(true);
            return;
        }

        if (event.getClick() == ClickType.SWAP_OFFHAND
                && slotLockManager.isLocked(40)) {

            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {

        if (!slotLockManager.isActive()) {
            return;
        }

        for (int rawSlot : event.getRawSlots()) {

            Inventory inventory =
                    event.getView().getInventory(rawSlot);

            if (!(inventory instanceof PlayerInventory)) {
                continue;
            }

            int slot =
                    event.getView().convertSlot(rawSlot);

            if (slotLockManager.isLocked(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {

        if (!slotLockManager.isActive()) {
            return;
        }

        if (slotLockManager.isLockedBarrier(
                event.getItemDrop().getItemStack()
        )) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {

        if (!slotLockManager.isActive()) {
            return;
        }

        int selectedSlot =
                event.getPlayer()
                        .getInventory()
                        .getHeldItemSlot();

        if (slotLockManager.isLocked(40)
                || slotLockManager.isLocked(selectedSlot)) {

            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {

        if (!slotLockManager.isActive()) {
            return;
        }

        if (slotLockManager.isLockedBarrier(event.getItem())) {
            event.setCancelled(true);
        }
    }
}