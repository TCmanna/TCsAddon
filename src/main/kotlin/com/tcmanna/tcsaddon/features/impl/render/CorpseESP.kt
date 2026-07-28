package com.tcmanna.tcsaddon.features.impl.render

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.render.drawTracer
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import java.awt.Color

object CorpseESP: Module(
    name = "Corpse ESP",
    description = "With Mineshaft Corpse."
) {
    val lapis by BooleanSetting("Lapis", true, "")
    val umber by BooleanSetting("Umber", false, "")
    val tungsten by BooleanSetting("Tungsten", false, "")
    val vanguard by BooleanSetting("Vanguard", false, "")

    val armorStands = ArrayList<ArmorStand>()
    val hidePos = ArrayList<BlockPos>()

    init {
        on<TickEvent.End> {
            if (LocationUtils.currentArea != Island.Mineshaft) return@on
            val player = mc.player?: return@on
            val level = mc.level?: return@on

            armorStands.clear()
            armorStands.addAll(
                level.entitiesForRendering()
                .filter { it is ArmorStand && it.showArms() && !it.showBasePlate() && !it.isInvisible }
                .map { it as ArmorStand }
                .filter { !it.getItemBySlot(EquipmentSlot.HEAD).isEmpty }
            )

            armorStands.forEach {
                if (it.distanceTo(player) < 5) {
                    if(!hidePos.contains(it.blockPosition())) hidePos.add(it.blockPosition())
                }
            }
        }

        on<RenderEvent.Extract> {
            if (LocationUtils.currentArea != Island.Mineshaft) return@on
            val player = mc.player ?: return@on
            val level = mc.level ?: return@on
            if (armorStands.isEmpty()) return@on

            armorStands.forEach {
                val corpse = Corpse.anyMathName(
                    it.getItemBySlot(EquipmentSlot.HEAD).displayName.string.noControlCodes.replace(Regex("[\\[\\]]"), "")
                )
                if (corpse != null && corpse.canShow() && !hidePos.any { hide -> hide == it.blockPosition() }) {
                    val color = com.odtheking.odin.utils.Color(corpse.color.rgb)
                    drawStyledBox(it.renderBoundingBox, color, 2, false)
                    drawTracer(it.position(), color, false)
                }
            }
        }

        on<LevelEvent.Load> {
            armorStands.clear()
            hidePos.clear()
        }
    }

    enum class Corpse(helmetName: String, color: Color) {
        LAPIS("LAPIS ARMOR HELMET", Color.BLUE),
        UMBER("YOG HELMET", Color.orange),
        TUNGSTEN("MINERAL HELMET", Color.GRAY),
        VANGUARD("VANGUARD HELMET", Color.CYAN);

        val helmetName: String
        val color: Color

        init {
            this.helmetName = helmetName
            this.color = color
        }

        fun canShow(): Boolean {
            if (lapis && this == LAPIS) return true
            if (umber && this == UMBER) return true
            if (tungsten && this == TUNGSTEN) return true
            if (vanguard && this == VANGUARD) return true
            return false
        }

        companion object {
            fun anyMathName(helmetName: String): Corpse? {
                for (value in entries) {
                    if (helmetName.equals(value.helmetName, ignoreCase = true)) return value
                }
                return null
            }
        }
    }
}