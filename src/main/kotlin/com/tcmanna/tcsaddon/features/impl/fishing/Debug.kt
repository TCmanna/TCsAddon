package com.tcmanna.tcsaddon.features.impl.fishing

import com.odtheking.odin.events.PacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket

//nan
object Debug : Module(
    name = "DEBUG",
    category = Category.custom("Fishing"),
    description = "Test."
) {
    private val bounceRegex = Regex("Bounces: (\\d{1,3})")

    init {
        on<PacketEvent.Receive> {
            if (packet is ClientReceiveMessageEvents.ModifyGame) {
                val text = (packet as ClientboundSetActionBarTextPacket).text.string.noControlCodes
                modMessage(text)

                val match = bounceRegex.find(text)

                if (match != null) modMessage("b: " + match.groupValues[1].toInt())
            }
        }
    }
}