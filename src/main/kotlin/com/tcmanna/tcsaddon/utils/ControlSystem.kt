package com.tcmanna.tcsaddon.utils

import com.tcmanna.tcsaddon.utils.Utils.mc
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2

object ControlSystem {

    private enum class Direction(
        val keys: Set<(net.minecraft.client.Options) -> KeyMapping>
    ) {
        FORWARD(setOf({ it.keyUp })),
        FORWARD_RIGHT(setOf({ it.keyUp }, { it.keyRight })),
        RIGHT(setOf({ it.keyRight })),
        BACK_RIGHT(setOf({ it.keyDown }, { it.keyRight })),
        BACK(setOf({ it.keyDown })),
        BACK_LEFT(setOf({ it.keyDown }, { it.keyLeft })),
        LEFT(setOf({ it.keyLeft })),
        FORWARD_LEFT(setOf({ it.keyUp }, { it.keyLeft }))
    }

    private var currentDirection: Direction? = null

    /**
     * 防止角度在边界附近来回抖动
     */
    private const val HYSTERESIS = 8.0

    fun setMovementToCoords(vec: Vec3) {
        setMovementToCoords(vec.x, vec.z)
    }

    fun setMovementToCoords(x: Double, z: Double) {

        if (isGuiOpen()) return

        val player = mc.player ?: return

        val dx = x - player.x
        val dz = z - player.z

        if (dx * dx + dz * dz < 0.001) {
            haltMovement()
            return
        }

        var angle =
            -(atan2(dx, dz) * 180.0 / Math.PI) -
                    player.yRot

        while (angle <= -180) angle += 360
        while (angle > 180) angle -= 360

        setMovementAngle(angle)
    }

    private fun setMovementAngle(angle: Double) {

        val newDirection = angleToDirection(angle)

        if (
            currentDirection != null &&
            currentDirection != newDirection
        ) {

            val currentCenter =
                directionCenter(currentDirection!!)

            val delta =
                angleDifference(angle, currentCenter)

            if (delta < HYSTERESIS) {
                return
            }
        }

        applyDirection(newDirection)
    }

    private fun angleToDirection(angle: Double): Direction {

        val normalized =
            ((angle + 22.5 + 360.0) % 360.0)

        return when ((normalized / 45.0).toInt()) {
            0 -> Direction.FORWARD
            1 -> Direction.FORWARD_RIGHT
            2 -> Direction.RIGHT
            3 -> Direction.BACK_RIGHT
            4 -> Direction.BACK
            5 -> Direction.BACK_LEFT
            6 -> Direction.LEFT
            else -> Direction.FORWARD_LEFT
        }
    }

    private fun applyDirection(direction: Direction) {

        if (direction == currentDirection)
            return

        val options = mc.options

        val wanted =
            direction.keys
                .map { it(options) }
                .toSet()

        val all = setOf(
            options.keyUp,
            options.keyDown,
            options.keyLeft,
            options.keyRight
        )

        all.forEach {
            val shouldPress = it in wanted

            if (it.isDown != shouldPress) {
                it.isDown = shouldPress
            }
        }

        currentDirection = direction
    }

    fun haltMovement() {

        val options = mc.options

        listOf(
            options.keyUp,
            options.keyDown,
            options.keyLeft,
            options.keyRight
        ).forEach {
            if (it.isDown) {
                it.isDown = false
            }
        }

        currentDirection = null
    }

    fun fullRelease() {

        haltMovement()

        mc.options.keyShift.isDown = false
        mc.options.keyAttack.isDown = false
    }

    private fun directionCenter(direction: Direction): Double =
        when (direction) {
            Direction.FORWARD -> 0.0
            Direction.FORWARD_RIGHT -> 45.0
            Direction.RIGHT -> 90.0
            Direction.BACK_RIGHT -> 135.0
            Direction.BACK -> 180.0
            Direction.BACK_LEFT -> -135.0
            Direction.LEFT -> -90.0
            Direction.FORWARD_LEFT -> -45.0
        }

    private fun angleDifference(a: Double, b: Double): Double {

        var diff = a - b

        while (diff > 180) diff -= 360
        while (diff < -180) diff += 360

        return kotlin.math.abs(diff)
    }

    fun isGuiOpen(): Boolean {
        return mc.screen != null &&
                mc.screen !is ChatScreen
    }
}