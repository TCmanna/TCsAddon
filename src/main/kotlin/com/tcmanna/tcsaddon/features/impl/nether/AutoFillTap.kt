package com.tcmanna.tcsaddon.features.impl.nether

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
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
    private val twilightFill by BooleanSetting("Twilight Fill", false, "Auto GFS Twilight Arrow Poison after build.")
    private val preFireFill by BooleanSetting("PreFire Fill", true, "Auto GFS Toxic Arrow Poison after build.").withDependency { !twilightFill }
    private val stunFill by BooleanSetting("Stun Fill", true, "Auto GFS Toxic Arrow Poison when stunned.")

    private const val FIX_MESSAGE = "[NPC] Elle: Phew! The Ballista is finally ready!"
    private const val STUN_MESSAGE = "destroyed one of Kuudra's pods!"

    init {
        on<MessageEvent.Chat> {
            if (!LocationUtils.isCurrentArea(Island.Kuudra)) return@on
            if (message.contains(FIX_MESSAGE)) {
                if (twilightFill) {
                    sendCommand("gfs TWILIGHT_ARROW_POISON 3")
                }
                else if (preFireFill) {
                    sendCommand("gfs TOXIC_ARROW_POISON $amount")
                }
            }
            if ((message.contains(STUN_MESSAGE) && stunFill)) {
                sendCommand("gfs TOXIC_ARROW_POISON $amount")
            }
        }
    }
}