package com.tcmanna.tcsaddon.features.impl.boss

import com.odtheking.odin.events.InputEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.tcmanna.tcsaddon.features.impl.dungeon.YqcLeapMenu
import com.tcmanna.tcsaddon.mixin.accessors.KeyMappingAccessor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

object LeapMid: Module(
    name = "Leap Mid",
    description = "Auto leap selected leap menu when you riding."
) {
    var leaped = false
    private var cooldown = 0

    init {
        on<TickEvent.Start> {
            val player = mc.player ?: return@on
            if (LocationUtils.currentArea != Island.Dungeon) return@on

            if (cooldown > 0) cooldown--
            if (leaped) {
                if (cooldown == 0 && !player.isPassenger) {
                    leaped = false
                    if (mc.screen == null) mc.mouseHandler.grabMouse()
                }
            } else {
                val screen =
                    if (mc.screen is AbstractContainerScreen<*>) mc.screen as AbstractContainerScreen<*> else return@on

                if (player.isPassenger && YqcLeapMenu.enabled && YqcLeapMenu.leapTeammates.isNotEmpty()) {
                    YqcLeapMenu.leapTeammates.getOrNull(YqcLeapMenu.getArea() - 1)?.let {
                        YqcLeapMenu.leapTo(it.name, screen)
                        leaped = true
                        cooldown = 40
                    }
                }
            }
        }

        on<WorldEvent.Load> {
            leaped = false
        }

        on<InputEvent> {
            if (key.value == (mc.options.keyShift as KeyMappingAccessor).boundKey.value) {
                leaped = false
            }
        }
    }
}