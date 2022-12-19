package com.skillw.itemsystem.api

import com.skillw.itemsystem.ItemSystem
import com.skillw.itemsystem.ItemSystem.itemBuilderManager
import com.skillw.itemsystem.api.action.ActionType
import com.skillw.itemsystem.internal.core.builder.ProcessData
import com.skillw.itemsystem.internal.feature.ItemCache.getTag
import com.skillw.itemsystem.internal.feature.ItemDrop
import com.skillw.itemsystem.internal.feature.ItemDrop.drop
import com.skillw.itemsystem.internal.feature.ItemDynamic.replaceDynamic
import com.skillw.itemsystem.internal.feature.ItemUpdater.updateItem
import com.skillw.pouvoir.api.annotation.ScriptTopLevel
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import taboolib.common.util.random
import taboolib.module.nms.getItemTag
import java.util.function.Consumer
/*
主要API
 */
@ScriptTopLevel
object ItemAPI {

    /**
     * 生成一个ItemSystem的物品
     *
     * @param item String 物品构建器id
     * @param entity LivingEntity? 物品所在的实体
     * @param consumer Consumer<ProcessData> 操作过程数据
     * @return ItemStack? 结果物品
     */
    @JvmStatic
    fun productItem(
        item: String,
        entity: LivingEntity? = null,
        consumer: Consumer<ProcessData>? = null,
    ): ItemStack? {
        return itemBuilderManager[item]?.build(entity, ProcessData(entity).apply { consumer?.accept(this) })
    }

    /**
     * 将物品的展示名,描述中的 dynamic 替换成实时内联函数运算的返回值(会直接改变物品本身)
     *
     * @param entity LivingEntity 实体
     * @receiver ItemStack 物品
     */
    @JvmStatic
    @ScriptTopLevel
    fun ItemStack.dynamic(entity: LivingEntity) {
        this.replaceDynamic(entity)
    }

    /**
     * 更新物品
     *
     * @param entity LivingEntity 实体
     * @param variables Set<String> 只更新的变量名，如果含 ’all‘ 则更新所有
     * @param productData Map<String, Any> 构造数据(就是变量名和变量值)
     * @param force 是否强制更新物品
     * @return ItemStack 更新后的物品
     * @receiver ItemStack 待更新的物品
     */
    @JvmStatic
    @ScriptTopLevel
    fun ItemStack.update(
        entity: LivingEntity,
        variables: Set<String> = emptySet(),
        productData: Map<String, Any> = emptyMap(),
    ): ItemStack {
        return this.updateItem(entity, variables, productData)
    }

    /**
     * 判斷物品是否來自ItemSystem
     *
     * @return Boolean 是否來自ItemSystem
     * @receiver ItemStack 物品
     */
    @JvmStatic
    @ScriptTopLevel
    fun ItemStack.fromIS(): Boolean {
        return getTag().containsKey("ITEM_SYSTEM")
    }

    /**
     * 注册物品动作
     *
     * @param key String 物品动作id
     * @return ActionType 物品动作
     */
    @JvmStatic
    @ScriptTopLevel
    fun registerAction(key: String): ActionType {
        return ActionType(key).apply { register() }
    }

    /**
     * 注册物品动作
     *
     * @param key String 物品动作id
     * @return ActionType 物品动作
     */
    @JvmStatic
    @ScriptTopLevel
    @Deprecated(
        "过时，请用registerAction",
        ReplaceWith("registerAction")
    )
    fun registerAsyncAction(key: String): ActionType {
        return ActionType(key).apply { register() }
    }

    /**
     * 注册同步物品动作 与当前线程同步
     *
     * 现在同步动作需要在 物品元 Action 里将 sync 调为 true
     *
     * @param key String 物品动作id
     * @return ActionType 物品动作
     */
    @JvmStatic
    @ScriptTopLevel
    @Deprecated(
        "过时，请用registerAction",
        ReplaceWith("registerAction")
    )
    fun registerSyncAction(key: String): ActionType {
        return ActionType(key).apply { register() }
    }

    /**
     * 触发物品动作
     *
     * @param key String 物品动作id
     * @param entity LivingEntity 玩家
     * @param itemStack ItemStack 物品
     * @param receiver Consumer<MutableMap<String, Any>> 操作变量池
     */
    @JvmStatic
    @ScriptTopLevel
    fun callAction(
        key: String,
        entity: LivingEntity,
        itemStack: ItemStack,
        receiver: Consumer<MutableMap<String, Any>>,
    ) {
        ItemSystem.actionTypeManager.call(key, entity, itemStack) { receiver.accept(this) }
    }

    /**
     * 掉落，如果物品有掉落技能则会触发技能
     *
     * @param location Location 位置
     * @param entity LivingEntity 实体
     * @receiver ItemStack 物品
     */
    @JvmStatic
    @ScriptTopLevel
    fun ItemStack.dropAt(location: Location, entity: LivingEntity): Item {
        return drop(location, ItemDrop.DropData(entity))
    }

    /**
     * 更新物品NBT 用于刷新玩家界面下物品上的 sync NBT / dynamic
     *
     * @receiver ItemStack
     */
    @JvmStatic
    @ScriptTopLevel
    fun ItemStack.refresh() {
        getItemTag().apply {
            putDeep("ITEM_SYSTEM.dynamic", random())
        }.saveTo(this)
    }

}