package com.skillw.itemsystem.internal.core.meta.bukkit

import com.skillw.itemsystem.api.builder.ItemData
import com.skillw.itemsystem.api.meta.BaseMeta
import com.skillw.itemsystem.api.meta.data.Memory
import com.skillw.pouvoir.api.annotation.AutoRegister
import com.skillw.pouvoir.util.ColorUtils.decolored
import taboolib.module.chat.colored

@AutoRegister
object MetaLore : BaseMeta("lore") {

    override val priority = 6
    override val default = emptyList<String>()

    private fun Memory.addLore(str: String) {
        if (str.contains("\n")) {
            str.split("\n").forEach { addLore(it) }
        } else {
            if (!str.contains("_delete_"))
                builder.lore.add(str.colored())
        }
    }

    override fun invoke(memory: Memory) {
        with(memory) {
            //                                        气死，拓展函数不能函数引用
            getList("lore").map { it.toString() }.forEach { addLore(it) }
        }
    }

    override fun loadData(data: ItemData): Any {
        return data.builder.lore.decolored()
    }
}