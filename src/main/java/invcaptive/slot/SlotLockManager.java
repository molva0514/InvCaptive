package invcaptive.slot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class SlotLockManager {

    public static final int SLOT_COUNT = 41;

    private final boolean[] lockedSlots =
            new boolean[SLOT_COUNT];

    private final NamespacedKey lockedKey;

    private boolean active = false;


    public SlotLockManager(JavaPlugin plugin) {

        this.lockedKey =
                new NamespacedKey(
                        plugin,
                        "locked_slot"
                );
    }


    /*
     * =========================
     * 게임 시작
     * =========================
     */
    public boolean start() {

        Player player =
                Bukkit.getOnlinePlayers()
                        .stream()
                        .findFirst()
                        .orElse(null);

        if (player == null) {
            return false;
        }


        Arrays.fill(
                lockedSlots,
                true
        );

        // 첫 번째 슬롯만 개방
        lockedSlots[0] = false;


        PlayerInventory inventory =
                player.getInventory();


        for (int slot = 0;
             slot < SLOT_COUNT;
             slot++) {

            if (lockedSlots[slot]) {

                inventory.setItem(
                        slot,
                        createLockedBarrier(slot)
                );

            } else {

                inventory.setItem(
                        slot,
                        null
                );
            }
        }


        active = true;

        updateAllPlayers();

        return true;
    }


    /*
     * =========================
     * 게임 정지
     * =========================
     */
    public boolean stop() {

        if (!active) {
            return false;
        }

        // 인벤토리는 건드리지 않고
        // 잠금 기능만 끈다.
        active = false;

        updateAllPlayers();

        return true;
    }


    /*
     * =========================
     * 실제 게임 해금
     * =========================
     */
    public boolean unlock(
            int slot,
            Player player
    ) {

        if (!active) {
            return false;
        }

        if (!isLocked(slot)) {
            return false;
        }


        lockedSlots[slot] = false;


        // 장벽 대신 랜덤 보상 지급
        player.getInventory()
                .setItem(
                        slot,
                        createUnlockReward()
                );


        updateAllPlayers();

        return true;
    }


    /*
     * =========================
     * 관리자 강제 해금
     * =========================
     */
    public boolean forceUnlock(
            int slot,
            Player player
    ) {

        if (!active) {
            return false;
        }

        if (!isLocked(slot)) {
            return false;
        }


        lockedSlots[slot] = false;


        // 테스트용이므로 보상 없음
        player.getInventory()
                .setItem(
                        slot,
                        null
                );


        updateAllPlayers();

        return true;
    }


    /*
     * =========================
     * 관리자 전체 강제 해금
     * =========================
     */
    public int forceUnlockAll(
            Player player
    ) {

        if (!active) {
            return 0;
        }


        int unlockedCount = 0;


        for (int slot = 0;
             slot < SLOT_COUNT;
             slot++) {

            if (!lockedSlots[slot]) {
                continue;
            }


            lockedSlots[slot] = false;


            player.getInventory()
                    .setItem(
                            slot,
                            null
                    );


            unlockedCount++;
        }


        updateAllPlayers();

        return unlockedCount;
    }


    /*
     * =========================
     * 남은 잠금 수
     * =========================
     */
    public int getLockedCount() {

        int count = 0;


        for (boolean locked
                : lockedSlots) {

            if (locked) {
                count++;
            }
        }


        return count;
    }


    /*
     * =========================
     * 저장 데이터 복구
     * =========================
     */
    public void restoreState(
            boolean active,
            boolean[] savedLockedSlots
    ) {

        Arrays.fill(
                lockedSlots,
                false
        );


        int length =
                Math.min(
                        savedLockedSlots.length,
                        SLOT_COUNT
                );


        System.arraycopy(
                savedLockedSlots,
                0,
                lockedSlots,
                0,
                length
        );


        this.active = active;
    }


    /*
     * =========================
     * 잠긴 장벽 자동 복구
     * =========================
     *
     * /clear나 다른 관리자 명령으로
     * 잠긴 장벽이 사라져도 다시 만든다.
     */
    public void repairLockedSlots(
            Player player
    ) {

        if (!active) {
            return;
        }


        PlayerInventory inventory =
                player.getInventory();


        boolean changed = false;


        for (int slot = 0;
             slot < SLOT_COUNT;
             slot++) {

            if (!lockedSlots[slot]) {
                continue;
            }


            ItemStack current =
                    inventory.getItem(slot);


            if (isLockedBarrier(current)) {
                continue;
            }


            inventory.setItem(
                    slot,
                    createLockedBarrier(slot)
            );


            changed = true;
        }


        if (changed) {
            updateAllPlayers();
        }
    }


    public boolean isActive() {

        return active;
    }


    public boolean isLocked(
            int slot
    ) {

        return slot >= 0
                && slot < SLOT_COUNT
                && lockedSlots[slot];
    }


    public boolean isLockedBarrier(
            ItemStack item
    ) {

        if (item == null
                || item.getType()
                != Material.BARRIER) {

            return false;
        }


        ItemMeta meta =
                item.getItemMeta();


        if (meta == null) {
            return false;
        }


        return meta
                .getPersistentDataContainer()
                .has(
                        lockedKey,
                        PersistentDataType.INTEGER
                );
    }


    /*
     * =========================
     * 잠긴 슬롯 장벽 생성
     * =========================
     */
    private ItemStack createLockedBarrier(
            int slot
    ) {

        ItemStack barrier =
                new ItemStack(
                        Material.BARRIER
                );


        ItemMeta meta =
                barrier.getItemMeta();


        meta.displayName(
                Component
                        .text(
                                "잠긴 슬롯",
                                NamedTextColor.RED
                        )
                        .decoration(
                                TextDecoration.ITALIC,
                                false
                        )
        );


        meta.getPersistentDataContainer()
                .set(
                        lockedKey,
                        PersistentDataType.INTEGER,
                        slot
                );


        barrier.setItemMeta(meta);

        return barrier;
    }


    /*
     * =========================
     * 해금 보상
     * =========================
     */
    private ItemStack createUnlockReward() {

        /*
         * 0 ~ 999
         *
         * 0~799   = 80%
         * 800~989 = 19%
         * 990~996 = 0.7%
         * 997~999 = 0.3%
         */
        int roll =
                ThreadLocalRandom
                        .current()
                        .nextInt(1000);


        /*
         * 80%
         * 키위신의 은총
         */
        if (roll < 800) {

            ItemStack reward =
                    new ItemStack(
                            Material.ENCHANTED_GOLDEN_APPLE
                    );


            ItemMeta meta =
                    reward.getItemMeta();


            meta.displayName(
                    Component.text(
                                    "키위신의 은총",
                                    NamedTextColor.GOLD
                            )
                            .decoration(
                                    TextDecoration.ITALIC,
                                    false
                            )
            );


            meta.lore(
                    List.of(

                            Component.text(
                                            "뭐, 신의 안배, 그런거야",
                                            NamedTextColor.GRAY
                                    )
                                    .decoration(
                                            TextDecoration.ITALIC,
                                            false
                                    )
                    )
            );


            reward.setItemMeta(meta);

            return reward;
        }


        /*
         * 19%
         * 똥 닦은 휴지
         */
        if (roll < 990) {

            ItemStack reward =
                    new ItemStack(
                            Material.BROWN_DYE
                    );


            ItemMeta meta =
                    reward.getItemMeta();


            meta.displayName(
                    Component.text(
                                    "똥 닦은 휴지",
                                    NamedTextColor.DARK_RED
                            )
                            .decoration(
                                    TextDecoration.ITALIC,
                                    false
                            )
            );


            meta.lore(
                    List.of(

                            Component.text(
                                            "친환경 갈색 휴지로 닦은 똥덩어리이다.",
                                            NamedTextColor.GRAY
                                    )
                                    .decoration(
                                            TextDecoration.ITALIC,
                                            false
                                    ),

                            Component.text(
                                            "믿음이 간절하지 않구나!!",
                                            NamedTextColor.RED
                                    )
                                    .decoration(
                                            TextDecoration.ITALIC,
                                            false
                                    )
                    )
            );


            reward.setItemMeta(meta);

            return reward;
        }


        /*
         * 0.7%
         * 네더라이트 강화 형판
         */
        if (roll < 997) {

            return new ItemStack(
                    Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE
            );
        }


        /*
         * 0.3%
         * 네더라이트 블록
         */
        return new ItemStack(
                Material.NETHERITE_BLOCK
        );
    }


    /*
     * =========================
     * 모든 플레이어 화면 갱신
     * =========================
     */
    public void updateAllPlayers() {

        for (Player player
                : Bukkit.getOnlinePlayers()) {

            player.updateInventory();
        }
    }
}