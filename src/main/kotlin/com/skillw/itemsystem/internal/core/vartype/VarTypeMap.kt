package com.skillw.itemsystem.internal.core.vartype

import com.skillw.itemsystem.ItemSystem
import com.skillw.itemsystem.api.meta.data.Memory
import com.skillw.itemsystem.api.vartype.VariableType
import com.skillw.itemsystem.internal.core.builder.ProcessData
import com.skillw.itemsystem.internal.core.meta.data.MetaData
import com.skillw.itemsystem.internal.core.meta.define.MetaDefine
import com.skillw.pouvoir.api.annotation.AutoRegister
import com.skillw.pouvoir.internal.core.function.context.SimpleContext
import java.util.function.Supplier


@AutoRegister
object VarTypeMap : VariableType("map") {
    private val ignoreKeys = arrayOf("var", "cache", "save", "key")

    @Suppress("UNCHECKED_CAST")
    override fun createVar(memory: Memory): Any {
        with(memory) {
            val context = SimpleContext()
            val mapData = ProcessData(entity, context)
            context.apply {
                putAll(memory.metaData.map
                    .filterKeys { it !in ignoreKeys }.mapValues {
                        val value = it.value
                        if (value !is Map<*, *>) return@mapValues value
                        value as Map<String, Any>
                        if (!value.containsKey("type")) return@mapValues value
                        val metaData = MetaData(MetaDefine).apply { putAll(value);put("key", it.key) }
                        ItemSystem.varTypeManager.createVar(Memory(metaData, mapData)) ?: value
                    })
            }
            return object : MutableMap<String, Any> by mapData {
                override fun get(key: String): Any? {
                    return context[key]?.let {
                        when (it) {
                            is Supplier<*> -> it.get().analysis()
                            else -> it.analysis()
                        }
                    }
                }
            }
        }
    }
}