package invcaptive.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.EnumMap;

public final class SharedInventoryBridge {

    // 일반 인벤토리 36칸
    private final NonNullList<ItemStack> sharedItems;

    // 갑옷 + 보조손이 들어가는 공용 장비 저장소
    private final EnumMap<EquipmentSlot, ItemStack> sharedEquipmentItems;

    private final Field inventoryItemsField;
    private final Field inventoryEquipmentField;
    private final Field equipmentItemsField;

    @SuppressWarnings("unchecked")
    public SharedInventoryBridge() {

        try {
            // 일반 인벤토리 36칸
            inventoryItemsField =
                    Inventory.class.getDeclaredField("items");
            inventoryItemsField.setAccessible(true);

            // 각 플레이어 인벤토리가 사용하는 장비 저장소
            inventoryEquipmentField =
                    Inventory.class.getDeclaredField("equipment");
            inventoryEquipmentField.setAccessible(true);

            // 장비 저장소 내부의 실제 아이템 Map
            equipmentItemsField =
                    EntityEquipment.class.getDeclaredField("items");
            equipmentItemsField.setAccessible(true);

            // 공용 일반 인벤토리 생성
            sharedItems =
                    NonNullList.withSize(36, ItemStack.EMPTY);

            // 빈 장비 저장소 하나를 만든 뒤
            // 그 안의 Map 자체를 공용 저장소로 사용
            EntityEquipment emptyEquipment =
                    new EntityEquipment();

            sharedEquipmentItems =
                    (EnumMap<EquipmentSlot, ItemStack>)
                            equipmentItemsField.get(emptyEquipment);

        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Minecraft 26.3 인벤토리 구조를 초기화하지 못했습니다.",
                    e
            );
        }
    }

    public void attach(Player player) {

        Inventory inventory =
                ((CraftPlayer) player)
                        .getHandle()
                        .getInventory();

        try {
            // 일반 36칸 공유
            inventoryItemsField.set(
                    inventory,
                    sharedItems
            );

            // 이 플레이어가 가지고 있는 장비 저장소를 가져옴
            EntityEquipment equipment =
                    (EntityEquipment)
                            inventoryEquipmentField.get(inventory);

            // 장비 저장소 내부 Map을 공용 Map으로 교체
            equipmentItemsField.set(
                    equipment,
                    sharedEquipmentItems
            );

            // 클라이언트 화면 갱신
            player.updateInventory();

        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "플레이어를 공유 인벤토리에 연결하지 못했습니다.",
                    e
            );
        }
    }
}