package com.tcmanna.tcsaddon.features.impl.rift

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.ScreenCloseEvent
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.SetSlotEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.tcmanna.tcsaddon.events.TickEventStart
import com.tcmanna.tcsaddon.utils.Utils
import net.kite.api.Kite
import net.kite.api.skill.level.SkillLevel
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.Items

object Auto4InARaw: Module(
    name = "Auto 4 In A Raw",
    description = "",
    category = Category.SKYBLOCK
) {
    private val fullAuto by BooleanSetting("Full Auto", false, "")

    private val solver = Kite.createInstance()
    private var inGameScreen = false
    private var npcColor : PlayerColor? = null
    private val placedSet = mutableSetOf<Int>()
    private val clickQueue = ArrayDeque<() -> Unit>()

    init {
        on<MessageEvent.Chat> {
            if (!fullAuto || !LocationUtils.isCurrentArea(Island.Rift)) return@on

            if (message == "Select an option: [Let's play again.] [I would like to do another trial.] ")
                sendCommand("selectnpcoption wizardman r_1_1")
            if (message == "[NPC] Wizardman: Put on your new armor and speak to me.")
                Utils.realRightClick()
        }

        on<ScreenEvent.Open> {
            if (!LocationUtils.isCurrentArea(Island.Rift)) return@on
            if (screen !is AbstractContainerScreen<*>) return@on
            val titleText = screen.title.string.noControlCodes
            if (titleText.contains("Quad Link Legacy")) {
                inGameScreen = true
            }
            if (fullAuto) {
                if (titleText.startsWith("Tight Pants")) {
                    clickQueue.add { mc.player?.closeContainer() }
                }
                if (titleText.startsWith("Wizardman Trials")) {
                    clickQueue.add {
                        if (mc.screen is AbstractContainerScreen<*>) {
                            mc.player?.clickSlot(15, 0, ContainerInput.PICKUP)
                        }
                    }
                }
            }
        }

        on<ScreenCloseEvent> {
            if (!LocationUtils.isCurrentArea(Island.Rift)) return@on
            if (inGameScreen) {
                inGameScreen = false
                solver.clearBoard()
                placedSet.clear()
                npcColor = null
            }
        }

        on<SetSlotEvent> {
            if (!LocationUtils.isCurrentArea(Island.Rift)) return@on
            if (inGameScreen) {
                if (npcColor == null)
                    npcColor = PlayerColor.getColor(slots[18].item.displayName.string)?: return@on

                if (slotIndex > 0 && slotIndex != 18 && itemStack.item == Items.PLAYER_HEAD &&
                    PlayerColor.getColor(itemStack.displayName.string) == npcColor) {
                    if (!placedSet.contains(slotIndex)) {
                        placedSet.add(slotIndex)
                    } else return@on
                    val npcMove = slotToMove(slotIndex) ?: return@on
                    solver.playMove(npcMove)

                    val solverMove = solver.skilledMove(SkillLevel.PERFECT)
                    solver.playMove(solverMove)
                    clickQueue.add {
                        if (mc.screen is AbstractContainerScreen<*>) {
                            mc.player?.clickSlot(solverMove, 0, ContainerInput.PICKUP)
                        }
                    }
                }
            }
        }

        on<TickEventStart> {
            if (!LocationUtils.isCurrentArea(Island.Rift)) return@on
            val player = mc.player?: return@on

            if (clickQueue.isNotEmpty()) {
                clickQueue.removeFirst().invoke()
                return@on
            }
        }
    }

    private fun slotToMove(slot: Int): Int? {
        if (slot in 1..7) return slot
        if (slot in 10..16) return slot - 9
        if (slot in 19..25) return slot - 18
        if (slot in 28..34) return slot - 27
        if (slot in 37..43) return slot - 36
        if (slot in 46..52) return slot - 45
        return null
    }

    enum class PlayerColor {
        Black,
        Orange,
        Green,
        Red,
        Yellow,
        Blue;

        companion object {
            fun getColor(name: String): PlayerColor? {
                for (value in entries) {
                    if (name.contains(value.name)) return value
                }
                return null
            }
        }
    }
}