package invcaptive.objective;

import invcaptive.slot.SlotLockManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

public final class ObjectiveBlockListener
        implements Listener {

    private final SlotLockManager slotLockManager;
    private final ObjectiveManager objectiveManager;

    private final NamespacedKey celebrationFireworkKey;


    public ObjectiveBlockListener(
            SlotLockManager slotLockManager,
            ObjectiveManager objectiveManager,
            JavaPlugin plugin
    ) {

        this.slotLockManager =
                slotLockManager;

        this.objectiveManager =
                objectiveManager;

        this.celebrationFireworkKey =
                new NamespacedKey(
                        plugin,
                        "celebration_firework"
                );
    }


    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(
            BlockBreakEvent event
    ) {

        if (!slotLockManager.isActive()) {
            return;
        }


        Material brokenBlock =
                event.getBlock()
                        .getType();


        Integer slot =
                objectiveManager
                        .getSlot(
                                brokenBlock
                        );


        if (slot == null) {
            return;
        }


        Player player =
                event.getPlayer();


        if (!slotLockManager.unlock(
                slot,
                player
        )) {
            return;
        }


        BlockType blockType =
                brokenBlock.asBlockType();


        Component blockName;

        if (blockType != null) {

            blockName =
                    Component.translatable(
                            blockType
                    );

        } else {

            blockName =
                    Component.text(
                            brokenBlock.name()
                    );
        }


        Component message =
                Component.text(
                                player.getName()
                                        + "님이 목표 블록 ["
                        )

                        .append(blockName)

                        .append(
                                Component.text(
                                        "] 발견! "
                                                + slot
                                                + "번 슬롯이 해금되었습니다!"
                                )
                        );


        for (Player onlinePlayer
                : Bukkit.getOnlinePlayers()) {

            onlinePlayer.sendMessage(
                    message
            );
        }


        /*
         * =========================
         * 게임 클리어
         * =========================
         */

        if (slotLockManager.getLockedCount() == 0) {

            playClearCelebration();

            slotLockManager.stop();
        }
    }


    /*
     * =========================
     * 클리어 연출
     * =========================
     */
    public void playClearCelebration() {

        Title clearTitle =
                Title.title(

                        Component.text(
                                        "클리어!",
                                        NamedTextColor.GREEN
                                )
                                .decorate(
                                        TextDecoration.BOLD
                                ),

                        Component.text(
                                "클리어를 축하합니다!",
                                NamedTextColor.GOLD
                        ),

                        Title.Times.times(
                                Duration.ofMillis(500),
                                Duration.ofSeconds(3),
                                Duration.ofSeconds(1)
                        )
                );


        for (Player player
                : Bukkit.getOnlinePlayers()) {

            /*
             * 타이틀
             */
            player.showTitle(
                    clearTitle
            );


            /*
             * 도전과제 완료 사운드
             */
            player.playSound(
                    player.getLocation(),
                    "minecraft:ui.toast.challenge_complete",
                    1.0F,
                    1.0F
            );


            /*
             * 플레이어 머리 위에
             * 별 모양 폭죽 생성
             */
            Firework firework =
                    player.getWorld()
                            .spawn(
                                    player.getLocation()
                                            .clone()
                                            .add(
                                                    0,
                                                    1.5,
                                                    0
                                            ),
                                    Firework.class
                            );


            FireworkMeta meta =
                    firework.getFireworkMeta();


            meta.addEffect(

                    FireworkEffect
                            .builder()

                            .with(
                                    FireworkEffect.Type.STAR
                            )

                            .withColor(
                                    Color.LIME,
                                    Color.GREEN
                            )

                            .withFade(
                                    Color.WHITE
                            )

                            .flicker(true)

                            .trail(true)

                            .build()
            );


            /*
             * 멀리 날아가지 않게
             * 최소 비행력
             */
            meta.setPower(0);


            firework.setFireworkMeta(
                    meta
            );


            /*
             * 클리어용 폭죽임을 표시
             */
            firework
                    .getPersistentDataContainer()
                    .set(
                            celebrationFireworkKey,
                            PersistentDataType.BYTE,
                            (byte) 1
                    );


            /*
             * 가능한 즉시 폭발
             */
            firework.detonate();
        }
    }


    /*
     * =========================
     * 클리어 폭죽 데미지 방지
     * =========================
     */
    @EventHandler(ignoreCancelled = true)
    public void onFireworkDamage(
            EntityDamageByEntityEvent event
    ) {

        if (!(event.getDamager()
                instanceof Firework firework)) {

            return;
        }


        if (firework
                .getPersistentDataContainer()
                .has(
                        celebrationFireworkKey,
                        PersistentDataType.BYTE
                )) {

            event.setCancelled(true);
        }
    }
}