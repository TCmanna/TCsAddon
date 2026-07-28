package com.tcmanna.tcsaddon.utils

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.tcmanna.tcsaddon.events.MouseEvent
import net.minecraft.client.player.LocalPlayer
import net.minecraft.util.Mth.wrapDegrees
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

object RotationUtils {

    private var rotationTask: (LocalPlayer.() -> Boolean)? = null

    init {
        on<RenderEvent.Last> {
            val player = mc.player ?: return@on

            while (rotationTask != null) {
                val current = rotationTask!!
                if (player.current()) {
                    if (rotationTask === current) {
                        rotationTask = null
                        break
                    }
                } else break
            }
        }

        on<MouseEvent.Move> {
            if (rotationTask != null) cancel()
        }

        on<LevelEvent.Load> {
            rotationTask = null
        }
    }

    fun rotationTask(task: (LocalPlayer.() -> Boolean)?): Boolean {
        val player = mc.player ?: return false
        rotationTask = task
        return if (task != null) player.task() else true
    }

    fun cancelRotationTask() {
        rotationTask = null
    }

    var LocalPlayer.yaw
        get() = wrapDegrees(this.yRot)
        set(v) {
            this.yRot = v
            this.yHeadRot = v
        }

    var LocalPlayer.pitch
        get() = this.xRot
        set(v) {
            this.xRot = v
        }

    fun LocalPlayer.rotate(yaw: Number = this.yaw, pitch: Number = this.pitch) {
        this.yaw = yaw.toFloat()
        this.pitch = pitch.toFloat()
    }

    fun LocalPlayer.rotate(dir: Direction) = this.rotate(dir.yaw, dir.pitch)

    fun LocalPlayer.rotateSmoothly(
        yaw: Float,
        pitch: Float,
        duration: Float,
        style: Animation.Style = Animation.Style.EaseOutQuint,
        onFinish: (() -> Unit)? = null
    ) {
        var initialised = false
        var startYaw = 0f
        var startPitch = 0f
        var deltaYaw = 0f
        var deltaPitch = 0f
        lateinit var anim: Animation

        rotationTask {
            if (!initialised) {
                startYaw = this.yaw
                startPitch = this.pitch
                deltaYaw = wrapDegrees(yaw - startYaw)
                deltaPitch = pitch - startPitch
                anim = Animation(duration, style).onFinish { onFinish?.invoke() }
                initialised = true
            }

            val progress = anim.get()

            this.rotate(
                startYaw + (deltaYaw * progress),
                startPitch + (deltaPitch * progress)
            )

            anim.finished
        }
    }

    fun LocalPlayer.rotateSmoothly(
        dir: Direction,
        duration: Float,
        style: Animation.Style = Animation.Style.EaseOutQuint,
        onFinish: (() -> Unit)? = null
    ) = this.rotateSmoothly(dir.yaw, dir.pitch, duration, style, onFinish)

    fun getLook(yaw: Float, pitch: Float): Vec3 {
        val f2 = -cos(-pitch * 0.017453292f).toDouble()
        return Vec3(
            sin(-yaw * 0.017453292f - 3.1415927f) * f2,
            sin(-pitch * 0.017453292f).toDouble(),
            cos(-yaw * 0.017453292f - 3.1415927f) * f2
        )
    }

    data class Direction(val yaw: Float, val pitch: Float, val distance: Double = 0.0) {
        fun getLook() = getLook(yaw, pitch)
    }
}