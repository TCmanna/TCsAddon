package com.tcmanna.tcsaddon.compat.rei

import com.odtheking.odin.utils.equalsOneOf
import me.shedaniel.rei.api.client.plugins.REIClientPlugin
import me.shedaniel.rei.api.client.registry.screen.OverlayDecider
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.InteractionResult

class ScreenREIPlugin : REIClientPlugin {

    override fun registerScreens(registry: ScreenRegistry) {
        registry.registerDecider(object : OverlayDecider {

            override fun <R : Screen?> isHandingScreen(p0: Class<R>): Boolean {
                return true
            }

            override fun <T : Screen> shouldScreenBeOverlaid(screen: T): InteractionResult {

                if (screen is AbstractContainerScreen<*>) {
                    val titleText = screen.title.string
                    if (titleText.equalsOneOf("Spirit Leap", "Teleport to Player")) {
                        return InteractionResult.FAIL
                    }
                }

                return InteractionResult.PASS
            }
        } )
    }
}