package com.tcmanna.tcsaddon.features.impl.rift

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.PacketEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils

object VampireSlayer : Module(
    name = "Vampire Slayer",
    description = "Automates Vampire Slayer mechanics",
    category = Category.SKYBLOCK
) {
    private val autoJump by BooleanSetting("Auto Jump", true, "")
    private val autoSneak by BooleanSetting("Auto Sneak", true, "")
    private val autoClick by BooleanSetting("Auto Click", true, "")
    val smoothRotate by BooleanSetting("Smooth Rotate", true, "").withDependency { autoClick }
    val rotateSpeed by NumberSetting("Rotate Speed", 140f, 50f, 200f, 10f, "", "ms").withDependency { autoClick && smoothRotate }

    private val autoHolyIce by BooleanSetting("Auto Holy Ice", true, "")
    private val holyIceDelay by NumberSetting("Holy Ice Delay", 18, 1, 40, 1, "").withDependency { autoHolyIce }

    private val autoMelon by BooleanSetting("Healing Melon", true, "")
    private val melonHealth by NumberSetting("Health", 7.0f, 1.0f, 20.0f, 0.5f, "").withDependency { autoMelon }

    private val queue = TickActionQueue()

    private val holyIce = HolyIceController(queue)
    private val melon = HealingMelonController(queue)
    private val subtitle = SubtitleController(queue)

    override fun onEnable() {
        queue.clear()
        holyIce.reset()
        melon.reset()
        subtitle.reset()
        super.onEnable()
    }

    override fun onDisable() {
        queue.clear()
        holyIce.reset()
        melon.reset()
        subtitle.reset()
        releaseKeys()
        super.onDisable()
    }

    init {
        on<TickEvent.Start> {
            if (mc.player == null || mc.level == null) return@on
            if (LocationUtils.currentArea != Island.Rift) return@on

            queue.tick()

            holyIce.tick(enabled = autoHolyIce, delayTicks = holyIceDelay)

            melon.tick(enabled = autoMelon, threshold = melonHealth)

            subtitle.tick(jump = autoJump, sneak = autoSneak, click = autoClick)
        }

        on<PacketEvent.Receive> {
            if (LocationUtils.currentArea != Island.Rift) return@on
            subtitle.onPacket(packet)
        }
    }

    private fun releaseKeys() {
        mc.options.keyJump.isDown = false
        mc.options.keyShift.isDown = false
    }
}

