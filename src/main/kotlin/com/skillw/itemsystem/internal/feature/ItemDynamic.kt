package com.skillw.itemsystem.internal.feature


import com.skillw.itemsystem.internal.feature.ItemCache.cacheLore
import com.skillw.itemsystem.internal.feature.ItemCache.getTag
import com.skillw.pouvoir.api.PouvoirAPI.eval
import com.skillw.pouvoir.internal.core.function.context.SimpleContext
import com.skillw.pouvoir.util.ColorUtils.decolored
import com.skillw.pouvoir.util.StringUtils.toList
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import taboolib.module.chat.colored
import taboolib.module.chat.uncolored
import java.util.regex.Pattern

object ItemDynamic {
    private val dynamicPattern = Pattern.compile("\\{\\{_dynamic::(?<content>.*?)_}}")

    private fun String.dynamic(itemStack: ItemStack, entity: LivingEntity): String {
        val matcher = dynamicPattern.matcher(this.decolored())
        if (!matcher.find()) return this
        val buffer = StringBuffer()
        val context = SimpleContext().apply { put("item", itemStack); put("entity", entity) }
        do {
            val content = matcher.group("content").uncolored().replace("\\$", "&")
            matcher.appendReplacement(
                buffer,
                content.eval(
                    namespaces = arrayOf("item_system", "common"),
                    context = context
                )
                    .toString()
            )
        } while (matcher.find())
        return matcher.appendTail(buffer).toString().colored()
    }

    internal fun ItemStack.replaceDynamic(entity: LivingEntity) {
        if (!hasItemMeta()) return
        if (!getTag().containsKey("ITEM_SYSTEM")) return
        val meta = itemMeta
        var display = if (meta.hasDisplayName()) meta.displayName else null
        val originLore = cacheLore()
        val newLore = ArrayList<String>()
        display = display?.dynamic(this, entity)
        originLore.forEach { line ->
            newLore.addAll(line.dynamic(this, entity).toList())
        }
        meta.setDisplayName(display)
        meta.lore = newLore
        itemMeta = meta
    }

}