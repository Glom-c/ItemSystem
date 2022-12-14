package com.skillw.itemsystem.internal.feature


import com.skillw.itemsystem.ItemSystem
import com.skillw.itemsystem.api.event.ItemBuildEvent
import com.skillw.itemsystem.internal.core.builder.ProcessData
import com.skillw.itemsystem.internal.core.option.OptionAutoUpdate.autoUpdate
import com.skillw.itemsystem.internal.core.option.OptionLockedLore.lockedLore
import com.skillw.itemsystem.internal.core.option.OptionLockedNBTKeys.lockedNBT
import com.skillw.itemsystem.internal.feature.ItemCache.cacheLore
import com.skillw.itemsystem.internal.feature.ItemCache.getTag
import com.skillw.itemsystem.util.NBTUtils.toMutableMap
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import taboolib.module.nms.ItemTag
import taboolib.module.nms.ItemTagData
import taboolib.module.nms.ItemTagType
import taboolib.module.nms.getItemTag
import taboolib.platform.util.isAir
import taboolib.platform.util.modifyLore

object ItemUpdater {
    /**
     * 物品是否该更新
     *
     * @return Boolean 是否该更新
     * @receiver ItemStack 待更新的物品
     */
    @JvmStatic
    fun ItemStack.isOutDated(): Boolean {
        if (isAir()) return false
        val tag = getTag()["ITEM_SYSTEM"]?.asCompound() ?: return false
        val key = tag["key"]?.asString() ?: return false
        val new = ItemSystem.itemBuilderManager[key] ?: return false
        return new.autoUpdate && tag["hash"]?.asInt() != new.hashCode()
    }

    @JvmStatic
    fun ItemStack.updateIfNeed(
        entity: LivingEntity,
        variables: Set<String> = emptySet(),
        productData: Map<String, Any> = emptyMap(),
    ): ItemStack {
        return if (isOutDated()) {
            updateItem(entity, variables, productData)
        } else this
    }

    /**
     * 更新物品
     *
     * @param entity LivingEntity 实体
     * @param variables Set<String> 只更新的变量名，如果含 ’all‘ 则更新所有
     * @param productData Map<String, Any> 构造数据(就是变量名和变量值)
     * @return ItemStack 更新后的物品
     * @receiver ItemStack 待更新的物品
     */

    @JvmStatic
    fun ItemStack.updateItem(
        entity: LivingEntity,
        variables: Set<String> = emptySet(),
        productData: Map<String, Any> = emptyMap(),
    ): ItemStack {
        val tag = getTag()["ITEM_SYSTEM"]?.asCompound() ?: return this
        val key = tag["key"]?.asString() ?: return this
        val data = tag["data"]?.run {
            asCompound()
                .toMutableMap().run {
                    when {
                        "all" in variables -> emptyMap()
                        variables.isNotEmpty() -> {
                            filterKeys { it !in variables }
                        }

                        else -> this
                    }
                }
                .toMutableMap()
                .apply { putAll(productData) }
        } ?: return this
        val originLore = cacheLore()
        val item = ItemSystem.itemBuilderManager[key] ?: return this
        val processData = ProcessData(entity).apply { putAll(data); savingKeys.addAll(data.keys) }
        val update = ItemBuildEvent.Update(item, processData, this, entity)
        update.call()
        val itemTag = update.itemStack.getTag().apply {
            removeDeep("ITEM_SYSTEM")
        }

        return item.build(entity, processData).apply item@{
            amount = this@updateItem.amount
            getItemTag().apply {
                item.lockedNBT.forEach(::removeDeep)
                itemTag.forEach { key, value ->
                    merge(key, value)
                }
                saveTo(this@item)
            }
            if (item.lockedLore) {
                modifyLore {
                    clear()
                    addAll(originLore)
                }
            }
        }
    }

    private fun ItemTag.merge(key: String, value: ItemTagData) {
        get(key)?.let {
            if (it.type == ItemTagType.COMPOUND && value.type == ItemTagType.COMPOUND) {
                val compound = get(key)?.asCompound()
                value.asCompound().forEach { key, value ->
                    compound?.merge(key, value)
                }
            }
        }
        putIfAbsent(key, value)
    }

    /**
     * 更新背包里的物品
     *
     * @param entity LivingEntity 实体
     * @receiver Inventory 背包
     */
    fun Inventory.updateItems(entity: LivingEntity) {
        for (i in 0 until size) {
            getItem(i)?.apply {
                setItem(i, updateIfNeed(entity))
            }
        }
    }

    /**
     * 更新玩家背包里的物品
     *
     * @receiver Player 玩家
     */
    fun Player.updateItems() {
        inventory.updateItems(this)
    }
}
