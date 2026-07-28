package com.tcmanna.tcsaddon.features.impl.skyblock

import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.noControlCodes
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.world.inventory.ContainerInput

object AutoFusion: Module(
    name = "Auto Fusion",
    description = "Auto Fusion"
) {
    val delay by NumberSetting("Click Delay", 200L, 100L, 500L, 10L, unit = "ms", desc = "")

    val fusionBox = "Fusion Box"
    val confirm = "Confirm Fusion"

    var clickDelay = 0L
    var shouldClick = false

    init {
        onReceive<ClientboundOpenScreenPacket> {
            val windowName = title.string.noControlCodes
            if (windowName.contains(fusionBox) || windowName.contains(confirm)) {
                clickDelay = System.currentTimeMillis() + delay
                shouldClick = true
            }
        }

        on<TickEvent.Start> {
            if (!shouldClick) return@on
            if (clickDelay > System.currentTimeMillis()) return@on

            if (mc.screen is AbstractContainerScreen<*>) {
                val screen = mc.screen as AbstractContainerScreen<*>
                val windowName = screen.title.string.noControlCodes

                if (windowName.contains(fusionBox)) {
                    mc.player?.clickSlot(screen.menu.containerId, 47, 2, ContainerInput.CLONE)
                    shouldClick = false
                }

                if (windowName.contains(confirm)) {
                    mc.player?.clickSlot(screen.menu.containerId, 33, 2, ContainerInput.CLONE)
                    shouldClick = false
                }
            }
        }
    }
}