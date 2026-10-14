package com.tcmanna.tcsaddon.utils

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

interface TickAction {
    fun onModMessage(message: String) {}
    fun onTickStart() {}
    fun onScreenOpen(screen: AbstractContainerScreen<*>) {}
    fun start() {}
}