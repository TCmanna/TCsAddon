package com.tcmanna.tcsaddon.features.impl.skyblock

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.texture
import com.tcmanna.tcsaddon.mixin.accessors.KeyMappingAccessor
import com.tcmanna.tcsaddon.utils.ControlSystem
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.KeyMapping
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3

object AutoBeachBall : Module(
    name = "Auto BeachBall",
    description = "Automatically bounces beach balls"
) {

    private val holdShift by BooleanSetting("Hold Shift", true, "")
    private val stopRange by NumberSetting("Stop Range", 0.2, 0.05, 1, 0.05, "")

    private enum class State {
        WAITING,
        PLACE,
        BOUNCE,
        RETURN
    }

    private var state = State.WAITING
    private var trackedBall: Entity? = null
    private var bounceCount = 0
    private var bounceTimer = 0L
    private var tickCounter = 0
    private var hasActiveRun = false
    private var startPos = Vec3.ZERO
    private var lastVelocityY = 0.0
    private var ballDescending = false
    private val trailHistory = mutableListOf<Vec3>()
    private val predictedPath = mutableListOf<Vec3>()
    private var landingPoint: Vec3? = null
    private val bounceRegex = Regex("Bounces: (\\d{1,3})")

    private const val TRAIL_MAX_POINTS = 30
    private const val PREDICTION_STEPS = 100

    private const val GRAVITY = 0.03
    private const val DRAG = 0.99

    private const val HEAD_HEIGHT_OFFSET = 1.8

    private const val SMALL_BEACHBALL_TEXTURE =
        "ewogICJ0aW1lc3RhbXAiIDogMTczNjQyNzQ4ODAwNCwKICAicHJvZmlsZUlkIiA6ICIzN2JhNjRkYzkxOTg0OGI4YjZhNDdiYTg0ZDgwNDM3MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTb3lLb3NhIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJhZGY5ZDcxMzY3Y2Q2ZTUwNWZiNDhjYWFhNWFjZGNkZmYyYTA5ZjY2YzQ4OGRhZjA0ZDA0NWVlMGJmNTI4ZTEiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="

    private const val LARGE_BEACHBALL_TEXTURE =
        "eyJ0aW1lc3RhbXAiOjE1ODY2NjcxNjgzNzksInByb2ZpbGVJZCI6ImJlY2RkYjI4YTJjODQ5YjRhOWIwOTIyYTU4MDUxNDIwIiwicHJvZmlsZU5hbWUiOiJTdFR2Iiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yOTllYTEyMGJkODNkMGM4MWEzYzQ2MjdmNWJjZTFiMTJmYjAzYmNiNTc3NzljNjNkY2M3N2UzZjRhZThhNzkzIn19fQ=="


    init {
        ClientReceiveMessageEvents.MODIFY_GAME.register { message, actionBar ->
            if (this.enabled) {
                val match = bounceRegex.find(message.string.noControlCodes)
                if (match != null) {
                    bounceCount = match.groupValues[1].toInt()
                    bounceTimer = System.currentTimeMillis()
                }
            }
            message
        }

        on<TickEvent.Start> {
            updateTrajectory()
            when (state) {
                State.PLACE -> handlePlace()
                State.BOUNCE -> handleBounce()
                State.RETURN -> handleReturn()
                else -> {}
            }
        }
    }

    override fun onEnable() {
        startPos = mc.player?.position()?: Vec3.ZERO
        trackedBall = null
        bounceCount = 0
        tickCounter = 0
        hasActiveRun = false
        trailHistory.clear()
        predictedPath.clear()
        landingPoint = null
        state = State.PLACE
        super.onEnable()
    }

    override fun onDisable() {
        trackedBall = null
        trailHistory.clear()
        predictedPath.clear()
        landingPoint = null
        state = State.WAITING
        ControlSystem.haltMovement()
        super.onDisable()
    }

    private fun handlePlace() {
        if (trackedBall == null) {
            trackedBall = findBeachBall()

            if (trackedBall != null) {
                bounceCount = 0
                hasActiveRun = false
                state = State.BOUNCE
                return
            }
        }

        val slot = findItem("Bouncy Beach Ball")

        if (slot == -1) {
            this.onKeybind()
            return
        }

        setHotbarSlot(slot)

        tickCounter++

        if (tickCounter % 10 == 0) {
            rightClick()
        }
    }

    private fun handleBounce() {
        val ball = trackedBall
        if (ball == null || ball.isRemoved) {
            tickCounter++

            if (tickCounter > 10) {
                trackedBall = null
                state = State.RETURN
            }

            return
        }

        tickCounter = 0

        if (bounceCount > 0) hasActiveRun = true

        if (bounceCount > 40) {
            trackedBall = null
            state = State.RETURN
            return
        }

        val vx = ball.x - ball.xOld
        val vz = ball.z - ball.zOld

        val targetX = ball.x + vx * 3
        val targetZ = ball.z + vz * 3

        val dx = targetX - mc.player!!.x
        val dz = targetZ - mc.player!!.z

        val flatDistanceSq = dx * dx + dz * dz

        mc.options.keyShift.isDown = holdShift

        if (flatDistanceSq > 0.25) {
            moveTo(Vec3(targetX, ball.y, targetZ))
        }

        if (flatDistanceSq < stopRange * stopRange) {
            ControlSystem.haltMovement()
        }
    }

    private fun handleReturn() {
        ControlSystem.fullRelease()
        val player = mc.player?: return

        val distance = player.distanceToSqr(startPos)

        if (distance < 4) {
            ControlSystem.haltMovement()
            tickCounter = -10
            state = State.PLACE
            return
        }

        moveTo(startPos)
    }

    private fun updateTrajectory() {

        val ball = trackedBall ?: return

        val currentPos = ball.position()

        val velocity = Vec3(
                ball.x - ball.xOld,
                ball.y - ball.yOld,
                ball.z - ball.zOld
            )

        trailHistory.add(currentPos)

        while (trailHistory.size > TRAIL_MAX_POINTS) {
            trailHistory.removeAt(0)
        }

        if (lastVelocityY > 0 && velocity.y <= 0) {
            ballDescending = true
        }

        if (velocity.y > 0.1) {
            ballDescending = false
        }

        lastVelocityY = velocity.y

        if (ballDescending) {

            val prediction = predictParabola(currentPos, velocity)

            predictedPath.clear()
            predictedPath.addAll(prediction.first)

            landingPoint = prediction.second

        } else {
            predictedPath.clear()
            landingPoint = null
        }
    }

    private fun predictParabola(
        pos: Vec3,
        velocity: Vec3
    ): Pair<List<Vec3>, Vec3?> {

        val path =
            mutableListOf<Vec3>()

        var x = pos.x
        var y = pos.y
        var z = pos.z

        var vx = velocity.x
        var vy = velocity.y
        var vz = velocity.z

        var landing: Vec3? = null

        val bounceY = mc.player!!.y + HEAD_HEIGHT_OFFSET

        repeat(PREDICTION_STEPS) {
            val prevY = y

            vy -= GRAVITY

            vx *= DRAG
            vy *= DRAG
            vz *= DRAG

            x += vx
            y += vy
            z += vz

            val point = Vec3(x, y, z)

            path.add(point)

            if (vy < 0 && prevY > bounceY && y <= bounceY) {
                landing = Vec3(x, bounceY, z)
                return@repeat
            }
        }

        return path to landing
    }

    private fun findBeachBall(): Entity? {

        return mc.level!!.entitiesForRendering().filterIsInstance<ArmorStand>()
            .firstOrNull {
                mc.player!!.distanceToSqr(it) < 100 && isBeachBall(it)
            }
    }

    private fun isBeachBall(stand: ArmorStand): Boolean {
        try {
            val helmet = stand.getItemBySlot(EquipmentSlot.HEAD)
            if (helmet.item == Items.AIR || helmet.texture == null) return false

            return helmet.texture!!.contains(SMALL_BEACHBALL_TEXTURE) ||
                    helmet.texture!!.contains(LARGE_BEACHBALL_TEXTURE)

        } catch (_: Exception) {
            return false
        }
    }

    private fun moveTo(vec: Vec3) {
        ControlSystem.setMovementToCoords(vec)
    }

    private fun rightClick() {
        val key = (mc.options.keyUse as KeyMappingAccessor).boundKey
        KeyMapping.set(key, true)
        KeyMapping.click(key)
        KeyMapping.set(key, false)
    }

    private fun findItem(name: String): Int {
        val player = mc.player?: return -1

        for (i in 0.. 8) {
            val stack = player.inventory.getItem(i)
            if (!stack.isEmpty && stack.hoverName.string.noControlCodes == name) {
                return i
            }
        }

        return -1
    }

    private fun setHotbarSlot(slot: Int) {
        mc.player?.inventory?.selectedSlot = slot
    }
}