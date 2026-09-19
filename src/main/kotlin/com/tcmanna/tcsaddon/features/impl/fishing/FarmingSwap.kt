package com.tcmanna.tcsaddon.features.impl.fishing

import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.sendCommand
import com.tcmanna.tcsaddon.events.ClientModMessage
import com.tcmanna.tcsaddon.events.TickEventStart
import com.tcmanna.tcsaddon.features.impl.fishing.FarmingSwap.State.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

object FarmingSwap: Module(
    name = "Farming Swap",
    description = "",
    category = Category.custom("Fishing"),
) {
    private val pestSetSlot by NumberSetting("Pest Set Slot", 1, 1, 12, 1, "")
    private val farmingSetSlot by NumberSetting("Farming Set Slot", 4, 1, 12, 1, "")
    private val actionDelay by NumberSetting("Action Delay", 2, 1, 20, 1, "", "tick")

    private var state = Waiting
    private var tickDelay = 0

    const val YUCK = "YUCK! "
    const val EXPIRES = "[SkyHanni] Pest spawn cooldown expires in"

    init {
        on<ClientModMessage> {
            if (!Farming.enabled) return@on
            if (message.startsWith(YUCK)) {
                swapFarmingSet()
            }
            else if (message.startsWith(EXPIRES)) {
                swapPestSet()
            }
        }

        on<TickEventStart> {
            if (tickDelay > 0) {
                tickDelay--
                return@on
            }
            when (state) {
                Click1 -> {
                    tickDelay = actionDelay
                    mc.player?.clickSlot(loadoutIndexToSlotId(farmingSetSlot))
                    state = CloseDone
                }
                Click2 -> {
                    tickDelay = actionDelay
                    mc.player?.clickSlot(loadoutIndexToSlotId(pestSetSlot))
                    state = Close
                }
                Close -> {
                    if (mc.screen is AbstractContainerScreen<*>) {
                        tickDelay = actionDelay
                        mc.player?.closeContainer()
                        state = BackFarming
                    }
                }
                CloseDone -> {
                    if (mc.screen is AbstractContainerScreen<*>) {
                        mc.player?.closeContainer()
                        state = Waiting
                    }
                }
                BackFarming -> {
                    if (mc.screen == null) {
                        Farming.onKeybind()
                        state = Waiting
                    }
                }
                else -> return@on
            }
        }

        on<ScreenEvent.Open> {
            if (screen !is AbstractContainerScreen<*>) return@on
            val titleText = screen.title.string.noControlCodes
            if (titleText.contains("Loadouts")) {
                state = when (state) {
                    WaitingOpen1 -> Click1
                    WaitingOpen2 -> Click2
                    else -> return@on
                }
            }
        }
    }

    private fun swapFarmingSet() {
        sendCommand("loadouts")
        tickDelay = actionDelay
        state = WaitingOpen1
    }

    private fun swapPestSet() {
        sendCommand("loadouts")
        tickDelay = actionDelay
        state = WaitingOpen2
    }

    private fun loadoutIndexToSlotId(loadoutIndex: Int): Int {
        return when (loadoutIndex) {
            1 -> 14
            2 -> 15
            3 -> 16
            4 -> 23
            5 -> 24
            6 -> 25
            7 -> 32
            8 -> 33
            9 -> 34
            10 -> 41
            11 -> 42
            12 -> 43
            else -> throw IllegalArgumentException("Illegal loadoutIndex $loadoutIndex")
        }
    }

    private enum class State {
        Waiting,
        WaitingOpen1,
        Click1,
        WaitingOpen2,
        Click2,
        Close,
        CloseDone,
        BackFarming
    }

}