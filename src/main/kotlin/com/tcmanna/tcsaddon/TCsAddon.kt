package com.tcmanna.tcsaddon

import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.utils.ui.rendering.Font
import com.tcmanna.tcsaddon.commands.odinAddonCommand
import com.tcmanna.tcsaddon.events.FishingEventDispatcher
import com.tcmanna.tcsaddon.events.core.CustomEventDispatcher
import com.tcmanna.tcsaddon.features.impl.boss.AutoSS
import com.tcmanna.tcsaddon.features.impl.boss.Icant4
import com.tcmanna.tcsaddon.features.impl.boss.LeapMid
import com.tcmanna.tcsaddon.features.impl.dungeon.LagTracker
import com.tcmanna.tcsaddon.features.impl.dungeon.YqcLeapMenu
import com.tcmanna.tcsaddon.features.impl.fishing.*
import com.tcmanna.tcsaddon.features.impl.render.ChestESP
import com.tcmanna.tcsaddon.features.impl.render.CorpseESP
import com.tcmanna.tcsaddon.features.impl.render.DisablePranks
import com.tcmanna.tcsaddon.features.impl.render.HideEntity
import com.tcmanna.tcsaddon.features.impl.render.LittlefootESP
import com.tcmanna.tcsaddon.features.impl.render.NameTag
import com.tcmanna.tcsaddon.features.impl.rift.VampireSlayer
import com.tcmanna.tcsaddon.features.impl.skyblock.AntiNick
import com.tcmanna.tcsaddon.features.impl.skyblock.AutoBeachBall
import com.tcmanna.tcsaddon.features.impl.skyblock.AutoCarnivalZombie
import com.tcmanna.tcsaddon.features.impl.skyblock.AutoFusion
import com.tcmanna.tcsaddon.features.impl.skyblock.LeftClicker
import com.tcmanna.tcsaddon.utils.RotationUtils
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import java.io.File

object TCsAddon : ClientModInitializer {

    //from polar
    val sound = Identifier.fromNamespaceAndPath("tcsaddon", "tave_check")
    val soundEvent = SoundEvent.createVariableRangeEvent(sound)
    @JvmStatic
    val systemFont = loadMicrosoftYaHei()

    override fun onInitializeClient() {
        println("Tcs Addon initialized!")
        // Register commands by adding to the array
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            arrayOf(odinAddonCommand).forEach { commodore -> commodore.register(dispatcher) }
        }

        // Register objects to event bus by adding to the list
        listOf(
            this, FishingEventDispatcher, RotationUtils, CustomEventDispatcher, StaffActionController
        ).forEach { EventBus.subscribe(it) }

        // Register modules by adding to the list
        val moduleConfig = ModuleConfig("TCsAddon.json")
        ModuleManager.registerModules(
            moduleConfig,
            LeftClicker, AutoFish, KillWorm, SpecProtect,
            Icant4, AutoCHPass, RandomMove, AutoSS, AntiNick,
            AutoCarnivalZombie, NameTag, HideEntity, AutoFusion,
            ChestESP, CorpseESP, GoldenFishNotif, LittlefootESP,
            AutoBeachBall, VampireSlayer, LagTracker, DisablePranks,
            YqcLeapMenu, LeapMid
        )
        val debugUser = listOf("Paper_Flany", "MC_tianci", "TCmanna")
        if (debugUser.contains(Minecraft.getInstance().gameProfile.name))
            ModuleManager.registerModules(moduleConfig, Debug)
    }

    @JvmStatic
    fun loadMicrosoftYaHei(): Font {
        val fontFile = File(System.getenv("WINDIR"), "Fonts/msyh.ttc")
        return Font("Microsoft YaHei", fontFile.inputStream())
    }
}
