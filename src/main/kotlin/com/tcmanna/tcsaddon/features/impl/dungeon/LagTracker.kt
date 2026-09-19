package com.tcmanna.tcsaddon.features.impl.dungeon

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.PersonalBest
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import java.util.regex.Pattern


object LagTracker: Module(
    name = "Lag Tracker",
    description = ""
) {
    private val sendLagToParty by BooleanSetting("Send Msg", true, "")
    private val sendPBToParty by BooleanSetting("Send PB", true, "")
    private val customText by DropdownSetting("Custom Text")
    private val customMsg by StringSetting("Custom Msg", "{time}s lost to lag.", desc = "{time} will be replace.").withDependency { customText }
    private val customMinPbMsg by StringSetting("Custom Min PBMsg", " +MinPB {current}s -> {old}s", desc = "{current} and {old} will be replace.").withDependency { customText }
    private val customMaxPbMsg by StringSetting("Custom Max PBMsg", " +MaxPB {current}s -> {old}s", desc = "{current} and {old} will be replace.").withDependency { customText }

    private val lagPBs = PersonalBest(this, "DungeonLag")

    private const val RUN_START_MSG = "[NPC] Mort: Here, I found this map when I first entered the dungeon."
    private val terminalCompleteRegex = Regex("^\\s*☠ Defeated (.+) in 0?([\\dhms ]+?)\\s*(\\(NEW RECORD!\\))?$")
    private var active = false
    private var startMs = 0L
    private var ticks = 0L

    init {
        on<MessageEvent.Chat> {
            if (!DungeonUtils.inDungeons) return@on
            if (!active && message == RUN_START_MSG) {
                startMs = System.currentTimeMillis()
                ticks = 0L
                active = true
            }
            else if (active && terminalCompleteRegex.matches(message)) {
                val wallSec = (System.currentTimeMillis() - startMs).toDouble() / 1000.0
                val tickSec = ticks.toDouble() * 0.05
                val lag = wallSec - tickSec
                active = false
                if (lag >= 0.1) {
                    val time = "%.2f".format(lag)
                    val msg = customMsg.replace("{time}", time)

                    modMessage(msg, "§3Lag Tracker §8»§r ")
                    val pbText = updateLagPB(lag.toFloat())
                    if (sendLagToParty) sendCommand("pc ${msg.noControlCodes}${if (sendPBToParty) pbText else ""}")
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

    private fun updateLagPB(time: Float): String {
        var pbString = ""
        val oldMinPB = lagPBs.get("LagMin") ?: 10.0f
        val oldMaxPB = lagPBs.get("LagMax") ?: 30.0f

        if (oldMinPB > time) {
            lagPBs.set("LagMin", time)
            pbString += customMinPbMsg
                .replace("{current}", "%.2f".format(time))
                .replace("{old}", "%.2f".format(oldMinPB))

            modMessage("§7(§a§lNew Min PB§r§7) §a$time §r§7-> §8$oldMinPB", "§3Lag Tracker §8»§r ")
        }

        if (oldMaxPB < time) {
            lagPBs.set("LagMax", time)
            pbString += customMaxPbMsg
                .replace("{current}", "%.2f".format(time))
                .replace("{old}", "%.2f".format(oldMaxPB))
            modMessage("§7(§c§lNew Max PB§r§7) §c$time §r§7-> §8$oldMaxPB", "§3Lag Tracker §8»§r ")
        }

        val newMinPB = minOf(oldMinPB, time)
        val newMaxPB = maxOf(oldMaxPB, time)

        modMessage("§8(§7${"%.2f".format(newMinPB)} / ${"%.2f".format(newMaxPB)}§8)", "§3Lag Tracker §8»§r ")
        return pbString
    }
}