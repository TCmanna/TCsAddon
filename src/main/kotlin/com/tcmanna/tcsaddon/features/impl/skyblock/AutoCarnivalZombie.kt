package com.tcmanna.tcsaddon.features.impl.skyblock

import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.tcmanna.tcsaddon.utils.Utils
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RedstoneLampBlock
import net.minecraft.world.phys.Vec3
import kotlin.math.atan
import kotlin.math.sqrt

object AutoCarnivalZombie : Module(
    name = "Auto Carnival Zombie",
    description = "Auto Shooting."
) {
    private var lastClick = 0L

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
            val name = Utils.getTextWithoutFormattingCodes(item.displayName.string) ?: return@on
            if (!name.contains("Dart")) return@on

            val targets = getTarget() ?: return@on
            if (targets.isEmpty()) return@on

            val currentTarget = targets.removeAt(0)

            val (yaw, pitch) = calcYawPitch(currentTarget)?: return@on
            snapTo(yaw, pitch)

            mc.execute { Utils.playerUseHeldItem(player, true) }
            lastClick = now
        }
    }

    private fun getTarget(): MutableList<Vec3>? {
        val level = mc.level ?: return null
        val player = mc.player ?: return null

        val zombies = level.entitiesForRendering().filterIsInstance<Zombie>()
        if (zombies.isEmpty()) return null

        val itemLists = mutableMapOf(
            "Diamond" to mutableListOf(),
            "Golden" to mutableListOf(),
            "Iron" to mutableListOf(),
            "Leather" to mutableListOf<Vec3>()
        )

        for (zombie in zombies) {
            val chestplate = zombie.getItemBySlot(EquipmentSlot.CHEST)
            if (chestplate.isEmpty) continue

            val name = chestplate.hoverName.string

            if (player.distanceTo(zombie) > 40f) continue

            val predicted = Vec3(
                zombie.x + zombie.deltaMovement.x * 8,
                zombie.y + zombie.eyeHeight,
                zombie.z + zombie.deltaMovement.z * 8
            )

            for ((key, list) in itemLists) {
                if (name.contains(key)) {
                    list.add(predicted)
                    break
                }
            }
        }

        val lampList = mutableListOf<Vec3>()

        for (blockPos in lampCoords) {
            val block = level.getBlockState(blockPos)

            // 124 = Redstone Lamp (lit) in old versions
            if (block.block == Blocks.REDSTONE_LAMP && block.getValue(RedstoneLampBlock.LIT)) {
                lampList.add(
                    Vec3(blockPos.x + 0.5, blockPos.y + 0.6, blockPos.z + 0.5)
                )
            }
        }

        val result = mutableListOf<Vec3>()
        result += itemLists["Diamond"]!!
        result += lampList
        result += itemLists["Golden"]!!
        result += itemLists["Iron"]!!
        result += itemLists["Leather"]!!

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