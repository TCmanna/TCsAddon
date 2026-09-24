package com.tcmanna.tcsaddon.features.impl.fishing

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

object FarmingSwap : Module(
    name = "Farming Swap",
    description = "",
    category = Category.custom("Fishing"),
) {
    private val timeSwap by BooleanSetting("Time Swap", false, "")
    private val pestSetSlot by NumberSetting("Pest Set Slot", 1, 1, 12, 1, "")
    private val farmingSetSlot by NumberSetting("Farming Set Slot", 4, 1, 12, 1, "")
    private val actionDelay by NumberSetting("Action Delay", 2, 1, 20, 1, "", "tick")

    private var action: Action = WaitingAction
    private var tickDelay = 0

    const val YUCK = "YUCK! "
    const val EXPIRES = "[SkyHanni] Pest spawn cooldown expires in"

    init {
        on<ClientModMessage> {
            if (!Farming.enabled) return@on
            action.onModMessage(message)
        }

        on<TickEventStart> {
            if (tickDelay > 0) {
                tickDelay--
                return@on
            }

            action.onTickStart()
        }

        on<ScreenEvent.Open> {
            if (screen !is AbstractContainerScreen<*>) return@on

            action.onScreenOpen(screen as AbstractContainerScreen<*>)
        }
    }

    private interface Action {
        fun onModMessage(message: String) {}
        fun onTickStart() {}
        fun onScreenOpen(screen: AbstractContainerScreen<*>) {}
        fun start() {}
    }

    private object WaitingAction : Action {
        override fun onModMessage(message: String) {
            when {
                message.startsWith(YUCK) -> {
                    action = FarmingAction()
                    action.start()
                }

                message.startsWith(EXPIRES) -> {
                    action = PestAction()
                    action.start()
                }
            }
        }
    }

    private class FarmingAction : Action {
        private enum class Step {
            WAIT_LOADOUTS,
            CLICK_LOADOUT,
            CLOSE,
            DESK,
            WAIT_DESK,
            CLICK_DESK,
            WAIT_GARDEN_TIME,
            CLICK_GARDEN_TIME,
            CLOSE_TIME,
            DONE
        }

        private var step = Step.WAIT_LOADOUTS

        override fun start() {
            sendCommand("loadouts")
            tickDelay = actionDelay
        }

        override fun onScreenOpen(screen: AbstractContainerScreen<*>) {
            val title = screen.title.string.noControlCodes

            when (step) {
                Step.WAIT_LOADOUTS -> {
                    if (title.contains("Loadouts")) {
                        step = Step.CLICK_LOADOUT
                    }
                }

                Step.WAIT_DESK -> {
                    if (title.contains("Desk")) {
                        step = Step.CLICK_DESK
                    }
                }

                Step.WAIT_GARDEN_TIME -> {
                    if (title.contains("Garden Time")) {
                        step = Step.CLICK_GARDEN_TIME
                    }
                }

                else -> Unit
            }
        }

        override fun onTickStart() {
            when (step) {
                Step.CLICK_LOADOUT -> {
                    tickDelay = actionDelay

                    mc.player?.clickSlot(
                        loadoutIndexToSlotId(farmingSetSlot)
                    )

                    if (timeSwap) {
                        step = Step.DESK
                    } else {
                        step = Step.CLOSE
                    }
                }

                Step.CLOSE -> {
                    if (mc.screen is AbstractContainerScreen<*>) {
                        tickDelay = actionDelay
                        mc.player?.closeContainer()
                        step = Step.DONE
                    }
                }

                Step.DESK -> {
                    tickDelay = actionDelay
                    sendCommand("desk")
                    step = Step.WAIT_DESK
                }

                Step.CLICK_DESK -> {
                    tickDelay = actionDelay
                    mc.player?.clickSlot(50)
                    step = Step.WAIT_GARDEN_TIME
                }

                Step.CLICK_GARDEN_TIME -> {
                    tickDelay = actionDelay
                    mc.player?.clickSlot(11)
                    step = Step.CLOSE_TIME
                }

                Step.CLOSE_TIME -> {
                    mc.player?.closeContainer()
                    step = Step.DONE
                }

                Step.WAIT_LOADOUTS,
                Step.WAIT_DESK,
                Step.WAIT_GARDEN_TIME,
                Step.DONE -> Unit
            }

            if (step == Step.DONE) {
                action = WaitingAction
            }
        }
    }

    private class PestAction : Action {
        private enum class Step {
            WAIT_LOADOUTS,
            CLICK_LOADOUT,
            CLOSE,
            BACK_FARMING,
            DESK,
            WAIT_DESK,
            CLICK_DESK,
            WAIT_GARDEN_TIME,
            CLICK_GARDEN_TIME,
            CLOSE_TIME,
            DONE
        }

        private var step = Step.WAIT_LOADOUTS

        override fun start() {
            sendCommand("loadouts")
            tickDelay = actionDelay
        }

        override fun onScreenOpen(screen: AbstractContainerScreen<*>) {
            val title = screen.title.string.noControlCodes

            when (step) {
                Step.WAIT_LOADOUTS -> {
                    if (title.contains("Loadouts")) {
                        step = Step.CLICK_LOADOUT
                    }
                }

                Step.WAIT_DESK -> {
                    if (title.contains("Desk")) {
                        step = Step.CLICK_DESK
                    }
                }

                Step.WAIT_GARDEN_TIME -> {
                    if (title.contains("Garden Time")) {
                        step = Step.CLICK_GARDEN_TIME
                    }
                }

                else -> Unit
            }
        }

        override fun onTickStart() {
            when (step) {
                Step.CLICK_LOADOUT -> {
                    tickDelay = actionDelay

                    mc.player?.clickSlot(
                        loadoutIndexToSlotId(pestSetSlot)
                    )

                    if (timeSwap) {
                        step = Step.DESK
                    } else {
                        step = Step.CLOSE
                    }
                }

                Step.CLOSE -> {
                    if (mc.screen is AbstractContainerScreen<*>) {
                        tickDelay = actionDelay
                        mc.player?.closeContainer()
                        step = Step.BACK_FARMING
                    }
                }

                Step.BACK_FARMING -> {
                    if (mc.screen == null) {
                        Farming.onKeybind()
                        step = Step.DONE
                    }
                }

                Step.DESK -> {
                    tickDelay = actionDelay
                    sendCommand("desk")
                    step = Step.WAIT_DESK
                }

                Step.CLICK_DESK -> {
                    tickDelay = actionDelay
                    mc.player?.clickSlot(50)
                    step = Step.WAIT_GARDEN_TIME
                }

                Step.CLICK_GARDEN_TIME -> {
                    tickDelay = actionDelay
                    mc.player?.clickSlot(13)
                    step = Step.CLOSE_TIME
                }

                Step.CLOSE_TIME -> {
                    tickDelay = actionDelay
                    mc.player?.closeContainer()
                    step = Step.BACK_FARMING
                }

                Step.WAIT_LOADOUTS,
                Step.WAIT_DESK,
                Step.WAIT_GARDEN_TIME,
                Step.DONE -> Unit
            }

            if (step == Step.DONE) {
                action = WaitingAction
            }
        }
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
            else -> throw IllegalArgumentException(
                "Illegal loadoutIndex $loadoutIndex"
            )
        }
    }
}