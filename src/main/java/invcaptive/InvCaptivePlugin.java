package invcaptive;

import invcaptive.inventory.SharedInventoryBridge;
import invcaptive.objective.ObjectiveBlockListener;
import invcaptive.objective.ObjectiveManager;
import invcaptive.slot.LockedSlotListener;
import invcaptive.slot.SlotLockManager;
import invcaptive.storage.GameStateStorage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class InvCaptivePlugin
        extends JavaPlugin
        implements Listener {

    private SharedInventoryBridge
            sharedInventoryBridge;

    private SlotLockManager
            slotLockManager;

    private ObjectiveManager
            objectiveManager;

    private ObjectiveBlockListener
            objectiveBlockListener;

    private GameStateStorage
            gameStateStorage;


    /*
     * 서버가 켜질 때 플레이어가 아직 없으므로,
     * 저장된 인벤토리는 잠깐 여기 보관했다가
     * 첫 플레이어가 들어오면 복원한다.
     */
    private ItemStack[]
            pendingInventoryRestore;


    @Override
    public void onEnable() {

        sharedInventoryBridge =
                new SharedInventoryBridge();


        slotLockManager =
                new SlotLockManager(this);


        objectiveManager =
                new ObjectiveManager();


        gameStateStorage =
                new GameStateStorage(this);


        /*
         * =========================
         * 저장 데이터 불러오기
         * =========================
         */
        GameStateStorage.SavedState savedState =
                gameStateStorage.load();


        if (savedState != null) {

            slotLockManager.restoreState(
                    savedState.active(),
                    savedState.lockedSlots()
            );


            objectiveManager.restore(
                    savedState.seed(),
                    savedState.targets()
            );


            pendingInventoryRestore =
                    savedState.inventory();


            getLogger().info(
                    "Saved InvCaptive state loaded."
            );
        }


        /*
         * =========================
         * 이벤트 등록
         * =========================
         */
        Bukkit.getPluginManager()
                .registerEvents(
                        this,
                        this
                );


        Bukkit.getPluginManager()
                .registerEvents(
                        new LockedSlotListener(
                                slotLockManager
                        ),
                        this
                );


        objectiveBlockListener =
                new ObjectiveBlockListener(
                        slotLockManager,
                        objectiveManager,
                        this
                );


        Bukkit.getPluginManager()
                .registerEvents(
                        objectiveBlockListener,
                        this
                );


        /*
         * reload 등으로 이미 접속 중인
         * 플레이어가 있을 경우 처리
         */
        for (Player player
                : Bukkit.getOnlinePlayers()) {

            attachPlayer(
                    player
            );
        }


        /*
         * =========================
         * 장벽 자동 복구
         * =========================
         *
         * 20틱 = 약 1초
         */
        Bukkit.getScheduler()
                .runTaskTimer(
                        this,

                        () -> {

                            if (!slotLockManager.isActive()) {
                                return;
                            }


                            Player player =
                                    Bukkit.getOnlinePlayers()
                                            .stream()
                                            .findFirst()
                                            .orElse(null);


                            if (player == null) {
                                return;
                            }


                            slotLockManager
                                    .repairLockedSlots(
                                            player
                                    );
                        },

                        20L,
                        20L
                );


        /*
         * =========================
         * 자동 저장
         * =========================
         *
         * 600틱 = 약 30초
         */
        Bukkit.getScheduler()
                .runTaskTimer(
                        this,
                        this::saveState,
                        600L,
                        600L
                );


        getLogger().info(
                "InvCaptive enabled."
        );
    }


    /*
     * =========================
     * 서버 종료
     * =========================
     */
    @Override
    public void onDisable() {

        /*
         * 서버가 정상 종료될 때
         * 마지막 상태를 저장한다.
         */
        saveState();


        getLogger().info(
                "InvCaptive disabled."
        );
    }


    /*
     * =========================
     * 플레이어 접속
     * =========================
     */
    @EventHandler
    public void onPlayerJoin(
            PlayerJoinEvent event
    ) {

        attachPlayer(
                event.getPlayer()
        );
    }


    /*
     * =========================
     * 플레이어 퇴장
     * =========================
     */
    @EventHandler
    public void onPlayerQuit(
            PlayerQuitEvent event
    ) {

        /*
         * 마지막 사람이 나가더라도
         * 인벤토리 상태가 남도록
         * 나가기 직전에 저장.
         */
        gameStateStorage.save(
                slotLockManager,
                objectiveManager,
                event.getPlayer()
        );
    }


    /*
     * =========================
     * 공유 인벤토리 연결
     * + 저장 데이터 복구
     * =========================
     */
    private void attachPlayer(
            Player player
    ) {

        sharedInventoryBridge.attach(
                player
        );


        /*
         * 서버 재시작 후
         * 첫 플레이어가 들어온 경우
         */
        if (pendingInventoryRestore != null) {

            for (int slot = 0;
                 slot < SlotLockManager.SLOT_COUNT;
                 slot++) {

                ItemStack item =
                        pendingInventoryRestore[slot];


                if (item == null) {

                    player.getInventory()
                            .setItem(
                                    slot,
                                    null
                            );

                } else {

                    player.getInventory()
                            .setItem(
                                    slot,
                                    item.clone()
                            );
                }
            }


            /*
             * 한 번 복구했으면
             * 이제 공용 인벤토리 자체에 저장돼 있으므로
             * 다시 할 필요 없음.
             */
            pendingInventoryRestore = null;
        }


        /*
         * 혹시 저장 당시 장벽이 사라져 있었다면
         * 여기서도 다시 복구한다.
         */
        if (slotLockManager.isActive()) {

            slotLockManager
                    .repairLockedSlots(
                            player
                    );
        }


        player.updateInventory();
    }


    /*
     * =========================
     * 현재 상태 저장
     * =========================
     */
    private void saveState() {

        Player player =
                Bukkit.getOnlinePlayers()
                        .stream()
                        .findFirst()
                        .orElse(null);


        gameStateStorage.save(
                slotLockManager,
                objectiveManager,
                player
        );
    }


    /*
     * =========================
     * 명령어
     * =========================
     */
    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!command.getName()
                .equalsIgnoreCase(
                        "invcaptive"
                )) {

            return false;
        }


        /*
         * 도움말
         */
        if (args.length == 0) {

            sender.sendMessage(
                    "사용법: /invcaptive start | stop | list | unlock <슬롯번호|all>"
            );

            return true;
        }


        /*
         * =========================
         * START
         * =========================
         */
        if (args[0]
                .equalsIgnoreCase(
                        "start"
                )) {

            if (slotLockManager.isActive()) {

                sender.sendMessage(
                        "InvCaptive가 이미 실행 중입니다."
                );

                return true;
            }


            if (!slotLockManager.start()) {

                sender.sendMessage(
                        "플레이어가 한 명 이상 접속해 있어야 합니다."
                );

                return true;
            }


            objectiveManager.generate(
                    slotLockManager
            );


            saveState();


            sender.sendMessage(
                    "InvCaptive game started."
            );


            sender.sendMessage(
                    "목표 블록 "
                            + objectiveManager.size()
                            + "개가 생성되었습니다."
            );


            return true;
        }


        /*
         * =========================
         * STOP
         * =========================
         */
        if (args[0]
                .equalsIgnoreCase(
                        "stop"
                )) {

            if (!slotLockManager.stop()) {

                sender.sendMessage(
                        "InvCaptive가 실행 중이 아닙니다."
                );

                return true;
            }


            saveState();


            sender.sendMessage(
                    "InvCaptive game stopped."
            );


            sender.sendMessage(
                    "현재 인벤토리 상태는 그대로 유지됩니다."
            );


            return true;
        }


        /*
         * =========================
         * LIST
         * =========================
         */
        if (args[0]
                .equalsIgnoreCase(
                        "list"
                )) {

            if (!objectiveManager.hasTargets()) {

                sender.sendMessage(
                        "아직 목표 블록이 생성되지 않았습니다."
                );

                return true;
            }


            sender.sendMessage(
                    "----- InvCaptive 목표 블록 -----"
            );


            for (int slot = 0;
                 slot < SlotLockManager.SLOT_COUNT;
                 slot++) {

                Material target =
                        objectiveManager
                                .getTarget(slot);


                if (target == null) {
                    continue;
                }


                BlockType blockType =
                        target.asBlockType();


                if (blockType != null) {

                    sender.sendMessage(

                            Component
                                    .text(
                                            "["
                                                    + slot
                                                    + "] "
                                    )

                                    .append(
                                            Component.translatable(
                                                    blockType
                                            )
                                    )
                    );

                } else {

                    sender.sendMessage(
                            "["
                                    + slot
                                    + "] "
                                    + target.name()
                    );
                }
            }


            sender.sendMessage(
                    "Seed: "
                            + objectiveManager.getSeed()
            );


            return true;
        }


        /*
         * =========================
         * UNLOCK
         * =========================
         */
        if (args[0]
                .equalsIgnoreCase(
                        "unlock"
                )) {

            if (!slotLockManager.isActive()) {

                sender.sendMessage(
                        "InvCaptive가 실행 중이 아닙니다."
                );

                return true;
            }


            if (args.length < 2) {

                sender.sendMessage(
                        "사용법: /invcaptive unlock <슬롯번호|all>"
                );

                return true;
            }


            Player player =
                    Bukkit.getOnlinePlayers()
                            .stream()
                            .findFirst()
                            .orElse(null);


            if (player == null) {

                sender.sendMessage(
                        "접속 중인 플레이어가 없습니다."
                );

                return true;
            }


            /*
             * =========================
             * UNLOCK ALL
             * =========================
             */
            if (args[1]
                    .equalsIgnoreCase(
                            "all"
                    )) {

                int count =
                        slotLockManager
                                .forceUnlockAll(
                                        player
                                );


                sender.sendMessage(
                        count
                                + "개의 슬롯을 강제로 해금했습니다."
                );


                if (slotLockManager
                        .getLockedCount()
                        == 0) {

                    objectiveBlockListener
                            .playClearCelebration();


                    slotLockManager.stop();


                    sender.sendMessage(
                            "모든 슬롯이 열려 InvCaptive를 자동 종료했습니다."
                    );
                }


                saveState();

                return true;
            }


            /*
             * =========================
             * UNLOCK <번호>
             * =========================
             */
            int slot;


            try {

                slot =
                        Integer.parseInt(
                                args[1]
                        );

            } catch (NumberFormatException e) {

                sender.sendMessage(
                        "슬롯 번호는 숫자 또는 all이어야 합니다."
                );

                return true;
            }


            if (slot < 0
                    || slot
                    >= SlotLockManager.SLOT_COUNT) {

                sender.sendMessage(
                        "슬롯 번호는 0~40 사이여야 합니다."
                );

                return true;
            }


            if (!slotLockManager
                    .forceUnlock(
                            slot,
                            player
                    )) {

                sender.sendMessage(
                        slot
                                + "번 슬롯은 이미 열려 있거나 해금할 수 없습니다."
                );

                return true;
            }


            sender.sendMessage(
                    slot
                            + "번 슬롯을 강제로 해금했습니다."
            );


            if (slotLockManager
                    .getLockedCount()
                    == 0) {

                objectiveBlockListener
                        .playClearCelebration();


                slotLockManager.stop();


                sender.sendMessage(
                        "모든 슬롯이 열려 InvCaptive를 자동 종료했습니다."
                );
            }


            saveState();

            return true;
        }


        sender.sendMessage(
                "사용법: /invcaptive start | stop | list | unlock <슬롯번호|all>"
        );

        return true;
    }
}