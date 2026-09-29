package com.tcmanna.tcsaddon.features.impl.nether

import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils

object AutoFillTap: Module(
    name = "Auto Fill Tap",
    description = "Auto GFS toxic arrow when kuudra stun."
) {
    private val amount by NumberSetting("Amount", 16, 1, 64, 1, "")
    private const val STUN_MESSAGE = "destroyed one of Kuudra's pods!"
    init {
        on<MessageEvent.Chat> {
            if (!LocationUtils.isCurrentArea(Island.Kuudra)) return@on
            if (message.contains(STUN_MESSAGE)) {
                sendCommand("gfs TOXIC_ARROW_POISON $amount")
            }
        }
    }
}