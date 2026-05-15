package com.tcmanna.tcsaddon.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.AirItem
import net.minecraft.world.item.Items
import java.util.regex.Pattern

object Utils {
    val mc = Minecraft.getInstance()
    val formattingCodePattern: Pattern = Pattern.compile("(?i)" + '\u00a7'.toString() + "[0-9A-FK-OR]")


    fun playerHoldFishRod(player: LocalPlayer?) : Boolean {
        if (player == null) return false
        return player.mainHandItem.item == Items.FISHING_ROD
    }

    fun playerUseHeldItem(player: LocalPlayer?) {
        if (player == null) return
        if (player.mainHandItem.item is AirItem || mc.gameMode == null) return

        mc.execute({
            mc.gameMode!!.useItem(player, InteractionHand.MAIN_HAND)
            if (player.mainHandItem.item == Items.FISHING_ROD) player.swing(InteractionHand.MAIN_HAND)
        })
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
}