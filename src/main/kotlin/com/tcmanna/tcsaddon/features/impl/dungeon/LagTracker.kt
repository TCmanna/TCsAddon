package com.tcmanna.tcsaddon.features.impl.dungeon

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.sendCommand
import java.util.regex.Pattern


object LagTracker: Module(
    name = "Lag Tracker",
    description = ""
) {
    private val sendLagToParty by BooleanSetting("Send Msg", true, "")
    private val customMsg by StringSetting("Custom Msg", "{time} lost to lag.", desc = "{time} will be replace.")

    private const val RUN_START_MSG = "\u00a7e[NPC] \u00a7bMort\u00a7f: Here, I found this map when I first entered the dungeon."
    private val RUN_END_PATTERN = Pattern.compile("^\\s*\u2620 Defeated (.+) in 0?([\\dhms ]+)\\s*(\\(NEW RECORD!\\))?$")
    private var active = false
    private var startMs = 0L
    private var ticks = 0L

    init {
        on<ChatPacketEvent> {
            val s = component.string
            if (!active && s == RUN_START_MSG) {
                startMs = System.currentTimeMillis()
                ticks = 0L
                active = true
            }
            else if (active && RUN_END_PATTERN.matcher(s).find()) {
                val wallSec = (System.currentTimeMillis() - startMs).toDouble() / 1000.0
                val tickSec = ticks.toDouble() * 0.05
                val lag = wallSec - tickSec
                active = false
                if (lag >= 0.1) {
                    val time = String.format("%.2f", lag) + "s"
                    val msg = customMsg.replace("{time}", time)
                    modMessage(msg, "§3Lag Tracker §8»§r ")
                    if (sendLagToParty) sendCommand("pc $msg".noControlCodes)
                }
            }
        }

        on<TickEvent.Server> {
            if (active) ++ticks
        }

        on<LevelEvent.Load> {
            active = false
            startMs = 0L
            ticks = 0L
        }
    }
}