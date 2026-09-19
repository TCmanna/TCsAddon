package com.tcmanna.tcsaddon.features.impl.rift

import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.LocationChangeEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.tcmanna.tcsaddon.events.TickEventStart
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag

object AutoUbiksCube: Module(
    name = "Auto Ubiks Cube",
    description = "Auto Steal",
    category = Category.SKYBLOCK
) {
    private val clickDelay by NumberSetting("Click Delay", 5, 2, 20, 1, "", "tick")

    private const val TITLE_NAME = "Split or Steal"
    private const val STEAL_NPC = "Playstyle: 100% SPLIT"
    private val closeText = Regex("ROUND \\d+ \\(FINAL\\):.*")

    private var delay = clickDelay

    init {
        on<MessageEvent.Chat> {
            if (!LocationUtils.isCurrentArea(Island.Rift)) return@on
            if (mc.screen !is AbstractContainerScreen<*>) return@on
            if (closeText.matches(message)) {
                mc.player?.closeContainer()
            }
        }

        on<TickEventStart> {
            if (!LocationUtils.isCurrentArea(Island.Rift)) return@on
            if (mc.screen !is AbstractContainerScreen<*>) return@on
            val screen = mc.screen as AbstractContainerScreen<*>
            if (screen.title.string.noControlCodes != TITLE_NAME) return@on

            val npcHead = screen.menu.slots[43].item
            if (npcHead.isEmpty) return@on
            val splitPane = screen.menu.slots[15].item.item == Items.RED_STAINED_GLASS_PANE
            val matchedNPC = npcHead.getTooltipLines(Item.TooltipContext.EMPTY, mc.player, TooltipFlag.NORMAL).any {
                it.string.noControlCodes == STEAL_NPC
            }
            if (splitPane && matchedNPC) {
                if (delay > 0) {
                    delay--
                    return@on
                }

                mc.player?.clickSlot(15)
                delay = clickDelay
            }
        }

        on<LocationChangeEvent> {
            if (!LocationUtils.isCurrentArea(Island.Rift)) return@on
            delay = clickDelay
        }
    }
}