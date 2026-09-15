package com.tcmanna.tcsaddon.utils

import com.tcmanna.tcsaddon.mixin.accessors.KeyMappingAccessor
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.AirItem
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import java.util.regex.Pattern
import kotlin.collections.firstOrNull
import kotlin.math.floor

object Utils {
    val mc = Minecraft.getInstance()
    val formattingCodePattern: Pattern = Pattern.compile("(?i)" + '\u00a7'.toString() + "[0-9A-FK-OR]")


    fun playerHoldFishRod(player: LocalPlayer?) : Boolean {
        if (player == null) return false
        return player.mainHandItem.item == Items.FISHING_ROD
    }

    fun playerUseHeldItem(player: LocalPlayer?, packetClick: Boolean) {
        if (player == null) return
        if (player.mainHandItem.item is AirItem || mc.gameMode == null) return

        mc.execute {
            if (packetClick) {
                mc.gameMode!!.useItem(player, InteractionHand.MAIN_HAND)
                if (player.mainHandItem.item == Items.FISHING_ROD) player.swing(InteractionHand.MAIN_HAND)
            } else {
                realRightClick()
            }
        }
    }

    fun getTextWithoutFormattingCodes(text: String): String {
        return if (text.isEmpty()) "" else formattingCodePattern.matcher(text)
            .replaceAll("")
    }

    fun closeCurrentScreen() {
        val player = mc.player?: return

        if (player.containerMenu !== player.inventoryMenu) player.closeContainer()

        else mc.setScreen(null)
    }

    fun realRightClick() {
        val options = mc.options
        val key = (options.keyUse as KeyMappingAccessor).boundKey
        KeyMapping.set(key, true)
        KeyMapping.click(key)
        KeyMapping.set(key, false)
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

    fun Vec3.toPos() = BlockPos(floor(x).toInt(), floor(y).toInt(), floor(z).toInt())
}