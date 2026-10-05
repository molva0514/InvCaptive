package invcaptive.objective;

import invcaptive.slot.SlotLockManager;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class ObjectiveManager {

    /*
     * 아이템 형태는 없지만,
     * 실제 마인크래프트에서는 별개의 블록으로 존재하며
     * 목표로 넣을 가치가 있는 블록들.
     */
    private static final List<Material> EXTRA_BLOCKS = List.of(

            // =========================
            // 화분
            // =========================

            Material.POTTED_ACACIA_SAPLING,
            Material.POTTED_ALLIUM,
            Material.POTTED_AZALEA_BUSH,
            Material.POTTED_AZURE_BLUET,
            Material.POTTED_BAMBOO,
            Material.POTTED_BIRCH_SAPLING,
            Material.POTTED_BLUE_ORCHID,
            Material.POTTED_BROWN_MUSHROOM,
            Material.POTTED_CACTUS,
            Material.POTTED_CHERRY_SAPLING,
            Material.POTTED_CLOSED_EYEBLOSSOM,
            Material.POTTED_CORNFLOWER,
            Material.POTTED_CRIMSON_FUNGUS,
            Material.POTTED_CRIMSON_ROOTS,
            Material.POTTED_DANDELION,
            Material.POTTED_DARK_OAK_SAPLING,
            Material.POTTED_DEAD_BUSH,
            Material.POTTED_FERN,
            Material.POTTED_FLOWERING_AZALEA_BUSH,
            Material.POTTED_GOLDEN_DANDELION,
            Material.POTTED_JUNGLE_SAPLING,
            Material.POTTED_LILY_OF_THE_VALLEY,
            Material.POTTED_MANGROVE_PROPAGULE,
            Material.POTTED_OAK_SAPLING,
            Material.POTTED_OPEN_EYEBLOSSOM,
            Material.POTTED_ORANGE_TULIP,
            Material.POTTED_OXEYE_DAISY,
            Material.POTTED_PALE_OAK_SAPLING,
            Material.POTTED_PINK_TULIP,
            Material.POTTED_POPLAR_SAPLING,
            Material.POTTED_POPPY,
            Material.POTTED_RED_MUSHROOM,
            Material.POTTED_RED_TULIP,
            Material.POTTED_SPRUCE_SAPLING,
            Material.POTTED_TORCHFLOWER,
            Material.POTTED_WARPED_FUNGUS,
            Material.POTTED_WARPED_ROOTS,
            Material.POTTED_WHITE_TULIP,
            Material.POTTED_WITHER_ROSE,


            // =========================
            // 초가 꽂힌 케이크
            // =========================

            Material.CANDLE_CAKE,
            Material.WHITE_CANDLE_CAKE,
            Material.ORANGE_CANDLE_CAKE,
            Material.MAGENTA_CANDLE_CAKE,
            Material.LIGHT_BLUE_CANDLE_CAKE,
            Material.YELLOW_CANDLE_CAKE,
            Material.LIME_CANDLE_CAKE,
            Material.PINK_CANDLE_CAKE,
            Material.GRAY_CANDLE_CAKE,
            Material.LIGHT_GRAY_CANDLE_CAKE,
            Material.CYAN_CANDLE_CAKE,
            Material.PURPLE_CANDLE_CAKE,
            Material.BLUE_CANDLE_CAKE,
            Material.BROWN_CANDLE_CAKE,
            Material.GREEN_CANDLE_CAKE,
            Material.RED_CANDLE_CAKE,
            Material.BLACK_CANDLE_CAKE,


            // =========================
            // 내용물이 든 가마솥
            // =========================

            Material.WATER_CAULDRON,
            Material.LAVA_CAULDRON,
            Material.POWDER_SNOW_CAULDRON,


            // =========================
            // 지형 변화 블록
            // =========================

            Material.FARMLAND,
            Material.DIRT_PATH,

            // 서리걸음으로만 생성되는 그 새끼
            Material.FROSTED_ICE,


            // =========================
            // 줄기 / 작물
            // 성장 단계는 따로 세지 않는다.
            // =========================

            Material.MELON_STEM,
            Material.ATTACHED_MELON_STEM,

            Material.PUMPKIN_STEM,
            Material.ATTACHED_PUMPKIN_STEM,

            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,

            Material.COCOA,
            Material.SWEET_BERRY_BUSH,

            Material.TORCHFLOWER_CROP,
            Material.PITCHER_CROP,


            // =========================
            // 식물이 성장하며 생기는 별도 블록
            // =========================

            Material.BAMBOO_SAPLING,

            Material.BIG_DRIPLEAF_STEM,

            Material.CAVE_VINES,
            Material.CAVE_VINES_PLANT,

            Material.KELP_PLANT,

            Material.TWISTING_VINES_PLANT,
            Material.WEEPING_VINES_PLANT,

            Material.CHORUS_PLANT,


            // =========================
            // 설치하면 별도 블록으로 변하는 것
            // =========================

            Material.REDSTONE_WIRE,
            Material.TRIPWIRE
    );


    private final Material[] targetBySlot =
            new Material[SlotLockManager.SLOT_COUNT];

    private final Map<Material, Integer> slotByTarget =
            new EnumMap<>(Material.class);

    private long seed;


    public void generate(SlotLockManager slotLockManager) {

        Arrays.fill(targetBySlot, null);
        slotByTarget.clear();


        List<Integer> lockedSlots =
                new ArrayList<>();

        for (int slot = 0;
             slot < SlotLockManager.SLOT_COUNT;
             slot++) {

            if (slotLockManager.isLocked(slot)) {
                lockedSlots.add(slot);
            }
        }


        /*
         * 기본 후보.
         */
        Set<Material> candidateSet =
                Arrays.stream(Material.values())

                        .filter(Material::isBlock)
                        .filter(Material::isItem)
                        .filter(material -> !material.isAir())

                        // 베드락, 방벽 등 파괴 불가능 블록 제외
                        .filter(material ->
                                material.getHardness() >= 0.0F
                        )

                        .collect(
                                LinkedHashSet::new,
                                LinkedHashSet::add,
                                LinkedHashSet::addAll
                        );


        /*
         * 아이템은 아니지만
         * 실제 목표 블록으로 넣을 놈들 추가.
         */
        candidateSet.addAll(EXTRA_BLOCKS);


        List<Material> candidates =
                new ArrayList<>(candidateSet);


        if (candidates.size() < lockedSlots.size()) {

            throw new IllegalStateException(
                    "목표 블록 후보가 부족합니다."
            );
        }


        seed =
                ThreadLocalRandom.current().nextLong();


        Collections.shuffle(
                candidates,
                new Random(seed)
        );


        for (int i = 0;
             i < lockedSlots.size();
             i++) {

            int slot =
                    lockedSlots.get(i);

            Material material =
                    candidates.get(i);


            targetBySlot[slot] =
                    material;

            slotByTarget.put(
                    material,
                    slot
            );
        }
    }


    public Material getTarget(int slot) {

        if (slot < 0
                || slot >= targetBySlot.length) {

            return null;
        }

        return targetBySlot[slot];
    }


    public Integer getSlot(Material material) {

        return slotByTarget.get(material);
    }


    public boolean hasTargets() {

        return !slotByTarget.isEmpty();
    }


    public int size() {

        return slotByTarget.size();
    }


    public long getSeed() {

        return seed;
    }
    public void restore(
            long savedSeed,
            Material[] savedTargets
    ) {

        Arrays.fill(
                targetBySlot,
                null
        );

        slotByTarget.clear();

        seed =
                savedSeed;


        int length =
                Math.min(
                        savedTargets.length,
                        targetBySlot.length
                );


        for (int slot = 0;
             slot < length;
             slot++) {

            Material target =
                    savedTargets[slot];


            if (target == null) {
                continue;
            }


            targetBySlot[slot] =
                    target;


            slotByTarget.put(
                    target,
                    slot
            );
        }
    }
}