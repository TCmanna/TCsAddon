package com.tcmanna.tcsaddon.features.impl.render

import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.addVec
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.render.drawTracer
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.renderPos
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.world.entity.player.Player

object LittlefootESP: Module(
    name = "Littlefoot ESP",
    description = "ESP for Mineshaft mob Littlefoot."
) {
    const val SKIN = "f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b"
    val littlefootList = ArrayList<Player>()

    init {
        on<TickEvent.End> {
            if (LocationUtils.currentArea != Island.Mineshaft) return@on

            val level = mc.level?: return@on
            val player = mc.player?: return@on

            littlefootList.clear()

            val filter = level.players().filter {
                it.getEntityTextureString()?.contains(SKIN) == true
            }

            littlefootList.addAll(filter)
        }

        on<RenderEvent.Extract> {
            if (LocationUtils.currentArea != Island.Mineshaft) return@on
            if (littlefootList.isEmpty()) return@on

            littlefootList.forEach {
                drawStyledBox(it.renderBoundingBox, Colors.MINECRAFT_GOLD, 2, false)
                drawTracer(it.renderPos.addVec(y = it.bbHeight.toDouble() / 2), Colors.MINECRAFT_GOLD, false)
            }
        }
    }

    //copy from https://github.com/TurtleOnFire2/kittycat-1.21.11/blob/master/src/client/kotlin/kitty/cat/features/visual/CustomESP.kt
    fun Player.getEntityTextureString(): String? {
        val encoded = this.gameProfile.properties["textures"].firstOrNull()?.value
        if (encoded != null) {
            val json = String(java.util.Base64.getDecoder().decode(encoded))
            val obj = com.google.gson.JsonParser.parseString(json).asJsonObject
            return obj["textures"]?.asJsonObject
                ?.get("SKIN")?.asJsonObject
                ?.get("url")?.asString
        }

        return null
    }
}