package com.tcmanna.tcsaddon.features.impl.nether

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.tcmanna.tcsaddon.utils.Animation
import com.tcmanna.tcsaddon.utils.RotationUtils.rotate
import com.tcmanna.tcsaddon.utils.RotationUtils.rotateSmoothly
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.monster.MagmaCube


object AutoRotate: Module(
    name = "Auto Rotate",
    description = ""
) {
    private val smooth by BooleanSetting("Smoothly", true, "")
    private val smoothSpeed by NumberSetting("Speed", 100f, 50f, 200f, 10f, "").withDependency { smooth }

    private var enable = true
    init {
        onReceive<ClientboundPlayerPositionPacket> {
            if (!LocationUtils.isCurrentArea(Island.Kuudra)) return@onReceive
            if (!enable) return@onReceive

            val player = mc.player ?: return@onReceive
            val level = mc.level ?: return@onReceive

            if (change.position.y == 6.0) {
                schedule(2) {
                    findKuudraBoss(level)?.let {
                        val yaw = player.yRot + getDirection(it)

                        if (smooth) player.rotateSmoothly(yaw, player.xRot, smoothSpeed, Animation.Style.EaseInOutQuint)
                        else player.rotate(yaw, player.xRot)

                        enable = false
                    }
                }
            }
        }

        on<LevelEvent.Load> {
            enable = true
        }
    }

    override fun onDisable() {
        super.onDisable()
        enable = true
    }

    fun getDirection(entity: Entity): Float {
        val x: Double = entity.x
        val z: Double = entity.z

        if (x < -128) return 90f
        if (z > -84) return 0f
        if (x > -72) return -90f
        if (z < -132) return 180f

        return 0f
    }

    private fun findKuudraBoss(world: ClientLevel): MagmaCube? {
        var kuudra: MagmaCube? = null
        var maxY = 0.0
        var cubesFound = 0
        for (entity in world.entitiesForRendering()) {
            if (entity !is MagmaCube) continue
            if (entity.size != 30) continue

            val y = entity.y
            cubesFound++
            if (y > maxY) {
                kuudra = entity
                maxY = y
            }
        }

        if (kuudra == null || cubesFound == 0) return null
        if (kuudra.health <= 0) return null
        if (cubesFound == 2) {
            return kuudra
        }

        return kuudra
    }
}