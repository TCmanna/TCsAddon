package com.tcmanna.tcsaddon.features.impl.skyblock

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.sendCommand
import com.tcmanna.tcsaddon.events.TickEventStart
import com.tcmanna.tcsaddon.utils.Animation
import com.tcmanna.tcsaddon.utils.ControlSystem
import com.tcmanna.tcsaddon.utils.RotationUtils.rotateSmoothly
import com.tcmanna.tcsaddon.utils.Utils
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RedstoneLampBlock
import net.minecraft.world.phys.Vec3
import kotlin.math.atan
import kotlin.math.sqrt

object AutoCarnivalZombie : Module(
    name = "Auto Carnival Zombie",
    description = "Auto Shooting."
) {
    private val autoRestart by BooleanSetting("Auto Restart", false, "")
    private val lowPriorityZombie by BooleanSetting("Low Priority", true, "Set diamond baby zombie to lower priority.")

    private val tpPos = Vec3(-96.5, 70.0, 37.5)
    private val walkPos1 = Vec3(-99.5, 70.0, 40.5)
    private val walkPos2 = Vec3(-102.5, 70.0, 39.5)
    private val npcPos = Vec3(-103.5, 71.5, 38.5)

    private var walking = 0
    private var lastClick = 0L
    private var aimDelay = 0
    private var sendCommand = false

    private val lampCoords = listOf(
        BlockPos(-96, 76, 61),
        BlockPos(-99, 77, 62),
        BlockPos(-102, 75, 62),
        BlockPos(-106, 77, 61),
        BlockPos(-109, 75, 60),
        BlockPos(-112, 76, 58),
        BlockPos(-115, 77, 55),
        BlockPos(-117, 76, 52),
        BlockPos(-118, 76, 49),
        BlockPos(-119, 75, 45),
        BlockPos(-119, 77, 42),
        BlockPos(-118, 76, 39)
    )

    init {
        on<TickEvent.End> {
            val now = System.currentTimeMillis()
            if (now - lastClick < 200) return@on

            val player = mc.player ?: return@on
            val item = player.mainHandItem
            val name = Utils.getTextWithoutFormattingCodes(item.displayName.string)
            if (!name.contains("Dart")) return@on

            val targets = getTarget() ?: return@on
            if (targets.isEmpty()) return@on

            val currentTarget = targets.removeAt(0)

            val (yaw, pitch) = calcYawPitch(currentTarget)?: return@on
            snapTo(yaw, pitch)

            mc.execute { Utils.playerUseHeldItem(player, true) }
            lastClick = now
        }

        onReceive<ClientboundPlayerPositionPacket> {
            if (!autoRestart) return@onReceive
            if (change.position == tpPos) walking = 2
        }

        on<TickEventStart> {
            if (aimDelay > 0 && --aimDelay == 0) aimNPC()

            if (!autoRestart || walking == 0) return@on
            walkToNPC()
        }

        on<MessageEvent.Chat> {
            if (!autoRestart) return@on
            if (message == "[NPC] Carnival Cowboy: Wouldja like to play Zombie Shootout?") {
                sendCommand = true
            }
            if (message.contains("[Sure thing, partner!]") && sendCommand) {
                sendCommand("selectnpcoption carnival_cowboy r_2_1")
                sendCommand = false
            }
        }
    }

    private fun walkToNPC() {
        ControlSystem.fullRelease()
        val player = mc.player?: return

        if (walking == 2) {
            val distance = player.distanceToSqr(walkPos1)
            if (distance < 1.5) {
                ControlSystem.haltMovement()
                walking--
                aimDelay = 10
                return
            }
            ControlSystem.setMovementToCoords(walkPos1)
        } else if (walking == 1) {
            val distance = player.distanceToSqr(walkPos2)
            if (distance < 1.5) {
                ControlSystem.haltMovement()
                walking--
                return
            }
            ControlSystem.setMovementToCoords(walkPos2)
        }
    }

    private fun aimNPC() {
        val player = mc.player?: return

        val calcYawPitch = calcYawPitch(npcPos)?: return

        player.rotateSmoothly(
            calcYawPitch.first,
            calcYawPitch.second,
            200f,
            Animation.Style.Linear
        ) {
            Utils.realRightClick()
        }
    }

    private fun getTarget(): MutableList<Vec3>? {
        val level = mc.level ?: return null
        val player = mc.player ?: return null

        val zombies = level.entitiesForRendering().filterIsInstance<Zombie>()
        if (zombies.isEmpty()) return null

        val itemLists = mutableMapOf(
            Items.DIAMOND_CHESTPLATE to mutableListOf(),
            Items.GOLDEN_CHESTPLATE to mutableListOf(),
            Items.IRON_CHESTPLATE to mutableListOf(),
            Items.LEATHER_CHESTPLATE to mutableListOf<Vec3>()
        )

        for (zombie in zombies) {
            val chestplate = zombie.getItemBySlot(EquipmentSlot.CHEST)
            if (chestplate.isEmpty) continue

            if (player.distanceTo(zombie) > 40f) continue

            val predicted = Vec3(
                zombie.x + zombie.deltaMovement.x * 8,
                zombie.y + zombie.eyeHeight,
                zombie.z + zombie.deltaMovement.z * 8
            )

            if (lowPriorityZombie && zombie.isBaby && chestplate.item == Items.DIAMOND_CHESTPLATE) {
                itemLists[Items.LEATHER_CHESTPLATE]!!.add(predicted)
            } else {
                for ((key, list) in itemLists) {
                    if (chestplate.item == key) {
                        list.add(predicted)
                        break
                    }
                }
            }
        }

        val lampList = mutableListOf<Vec3>()

        for (blockPos in lampCoords) {
            val block = level.getBlockState(blockPos)

            if (block.block == Blocks.REDSTONE_LAMP && block.getValue(RedstoneLampBlock.LIT)) {
                lampList.add(
                    Vec3(blockPos.x + 0.5, blockPos.y + 0.6, blockPos.z + 0.5)
                )
            }
        }

        val result = mutableListOf<Vec3>()
        result += itemLists[Items.DIAMOND_CHESTPLATE]!!
        result += lampList
        result += itemLists[Items.GOLDEN_CHESTPLATE]!!
        result += itemLists[Items.IRON_CHESTPLATE]!!
        result += itemLists[Items.LEATHER_CHESTPLATE]!!

        return result
    }

    fun snapTo(yaw: Float, pitch: Float) {
        val player = mc.player ?: return

        player.yRot = yaw
        player.xRot = pitch

        // also update previous rotation to prevent visual snapping glitches
        player.yRotO = yaw
        player.xRotO = pitch
    }

    fun calcYawPitch(target: Vec3, playerPos: Vec3? = null): Pair<Float, Float>? {
        val plr = playerPos ?: getEyePos() ?: return null

        val dx = target.x - plr.x
        val dy = target.y - plr.y
        val dz = target.z - plr.z

        var yaw: Double
        var pitch: Double

        if (dx != 0.0) {
            yaw = if (dx < 0) 1.5 * Math.PI else 0.5 * Math.PI
            yaw -= atan(dz / dx)
        } else {
            yaw = if (dz < 0) Math.PI else 0.0
        }

        val xz = sqrt(dx * dx + dz * dz)
        pitch = -atan(dy / xz)

        yaw = -yaw * 180.0 / Math.PI
        pitch *= 180.0 / Math.PI

        if (pitch < -90 || pitch > 90 || yaw.isNaN() || pitch.isNaN()) {
            return null
        }

        return Pair(yaw.toFloat(), pitch.toFloat())
    }

    fun getEyePos(): Vec3? {
        val player = mc.player ?: return null
        return Vec3(
            player.x,
            player.y + player.eyeHeight,
            player.z
        )
    }
}