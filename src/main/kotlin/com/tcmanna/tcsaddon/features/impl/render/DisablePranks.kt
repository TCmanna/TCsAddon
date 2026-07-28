package com.tcmanna.tcsaddon.features.impl.render

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import net.fabricmc.loader.api.FabricLoader

object DisablePranks: Module(
    name = "Disable Pranks",
    description = ":)"
) {
    val odinDungeon by BooleanSetting("Odin Dungeon", false, "")
    val starredDonators by BooleanSetting("Starred Donators", true, "").withDependency { starredLoaded }
    val naRat by BooleanSetting("NA RAT", true, "").withDependency { naLoaded }

    private var starredLoaded = false
    private var naLoaded = false

    init {
        starredLoaded = FabricLoader.getInstance().isModLoaded("aerii-library")
        naLoaded = FabricLoader.getInstance().isModLoaded("noammaddons")
    }
}