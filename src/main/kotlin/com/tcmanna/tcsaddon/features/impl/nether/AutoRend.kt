package com.tcmanna.tcsaddon.features.impl.nether

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.sendCommand
import com.tcmanna.tcsaddon.events.TickEventStart
import com.tcmanna.tcsaddon.mixin.accessors.KeyMappingAccessor
import com.tcmanna.tcsaddon.utils.TickAction
import com.tcmanna.tcsaddon.utils.Utils
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.player.LocalPlayer

object AutoRend: Module(
    name = "Auto Rend",
    description = ""
) {
    private val tuxeduSlot by NumberSetting("Tuxedu Slot", 1, 1, 12, 1, "")
    private val actionDelay by NumberSetting("Action Delay", 2, 1, 10, 1, "", "tick")
    private val pullDelay by NumberSetting("Pull Delay", 10, 5, 20, 5, "", "tick")
    private val throwRod by BooleanSetting("Throw Rod", true, "Should use rod swap.")
    private val gyroClick by BooleanSetting("Gyro Click", false, "Ice spray will be replaced.")
    private val enableMsg by BooleanSetting("Enable Message", false, "")

    private var action: TickAction? = null
    private var tickDelay = 0
    private var startTime = 0L

    private const val HOTS = "AXE_OF_THE_SHREDDED"
    private const val BONE = "BONE_BOOMERANG"
    private const val ROD = "ROD"
    private const val RAG = "RAGNAROCK_AXE"
    private const val ICE = "ICE_SPRAY_WAND"
    private const val GYRO = "GYROKINETIC_WAND"

    private const val PREFIX = "§3Auto Rend §8»§r "

    init {
        on<TickEventStart> {
            if (System.currentTimeMillis() - startTime > 5000) {
                toggle()
                return@on
            }
            if (tickDelay > 0) {
                tickDelay--
                return@on
            }

            action?.onTickStart()
        }

        on<ScreenEvent.Open> {
            if (screen !is AbstractContainerScreen<*>) return@on

            action?.onScreenOpen(screen as AbstractContainerScreen<*>)
        }
    }

    override fun onEnable() {
        super.onEnable()
        val player = mc.player
        if (player == null || !checkItem(player)) {
            this.toggle()
            return
        }
        startTime = System.currentTimeMillis()
        tickDelay = 0
        action = RendAction()

    }

    override fun onKeybind() {
        if (enableMsg) super.onKeybind()
        else toggle()
    }

    private fun checkItem(player: LocalPlayer): Boolean {
        val reqItem = mutableListOf(HOTS, BONE, RAG)
        if (throwRod) reqItem.add(ROD)
        if (gyroClick) reqItem.add(GYRO) else reqItem.add(ICE)

        for (i in 0 until 9) {
            val itemStack = player.inventory.getItem(i)
            reqItem.removeIf { itemStack.itemId.contains(it) }
        }

        if (reqItem.isNotEmpty()) {
            modMessage("Missing Item: $reqItem", PREFIX)
            return false
        }
        return true
    }

    private fun selectItem(item: String) {
        val player = mc.player?: return
        for (i in 0 until 9) {
            val itemStack = player.inventory.getItem(i)
            if (itemStack.itemId.contains(item)) {
                player.inventory.selectedSlot = i
                return
            }
        }
    }

    private class RendAction: TickAction {
        private enum class Step {
            SELECT_ROD,
            RC_ROD,
            SELECT_BONE,
            RC_BONE,
            OPEN_LOADOUT,
            SELECT_HOTS,
            WAIT_LOADOUT,
            CLICK_TUXEDU,
            CLOSE_LOADOUT,

            SELECT_ICE,
            RC_ICE,
            SELECT_BONE2,
            LC_BONE,
            DONE
        }

        private var step = if (throwRod) Step.SELECT_ROD else Step.SELECT_BONE

        override fun onTickStart() {
            when (step) {
                Step.SELECT_ROD -> {
                    tickDelay = actionDelay
                    selectItem(ROD)
                    step = Step.RC_ROD
                }

                Step.RC_ROD -> {
                    tickDelay = actionDelay
                    Utils.realRightClick()
                    step = Step.SELECT_BONE
                }

                Step.SELECT_BONE -> {
                    tickDelay = actionDelay
                    selectItem(BONE)
                    step = Step.RC_BONE
                }

                Step.RC_BONE -> {
                    tickDelay = actionDelay
                    Utils.realRightClick()
                    step = Step.SELECT_HOTS
                }

                Step.SELECT_HOTS -> {
                    selectItem(HOTS)
                    step = Step.OPEN_LOADOUT
                }

                Step.OPEN_LOADOUT -> {
                    sendCommand("loadouts")
                    step = Step.WAIT_LOADOUT
                }

                Step.CLICK_TUXEDU -> {
                    tickDelay = actionDelay
                    mc.player?.clickSlot(Utils.loadoutIndexToSlotId(tuxeduSlot))
                    step = Step.CLOSE_LOADOUT
                }

                Step.CLOSE_LOADOUT -> {
                    if (mc.screen is AbstractContainerScreen<*>) {
                        tickDelay = pullDelay
                        mc.player?.closeContainer()
                        step = Step.SELECT_ICE
                    }
                }

                Step.SELECT_ICE -> {
                    tickDelay = actionDelay
                    if (gyroClick) selectItem(GYRO) else selectItem(ICE)
                    step = Step.RC_ICE
                }

                Step.RC_ICE -> {
                    tickDelay = actionDelay
                    Utils.realRightClick()
                    step = Step.SELECT_BONE2
                }

                Step.SELECT_BONE2 -> {
                    tickDelay = actionDelay
                    selectItem(BONE)
                    step = Step.LC_BONE
                }

                Step.LC_BONE -> {
                    tickDelay = 2
                    mc.options.keyAttack.isDown = true
                    KeyMapping.click((mc.options.keyAttack as KeyMappingAccessor).boundKey)
                    step = Step.DONE
                }

                Step.DONE -> {
                    mc.options.keyAttack.isDown = false
                    toggle()
                }

                else -> Unit
            }
        }

        override fun onScreenOpen(screen: AbstractContainerScreen<*>) {
            val title = screen.title.string.noControlCodes

            when (step) {
                Step.WAIT_LOADOUT -> {
                    if (title.contains("Loadouts")) {
                        tickDelay = actionDelay
                        step = Step.CLICK_TUXEDU
                    }
                }

                else -> Unit
            }
        }
    }

}