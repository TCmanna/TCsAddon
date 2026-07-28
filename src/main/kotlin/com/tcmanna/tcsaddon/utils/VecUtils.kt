package com.tcmanna.tcsaddon.utils

import com.odtheking.odin.OdinMod.mc
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth.wrapDegrees
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.*

fun BlockPos.getHitResult(force: Boolean = false): BlockHitResult? {
    val player = mc.player ?: return null

    var shape = this.shape
    if (shape.isEmpty && force) shape = Shapes.block()
    if (shape.isEmpty) return null

    val eyes = player.eyePosition
    val centre = shape.bounds().center.add(x.toDouble(), y.toDouble(), z.toDouble())
    val dir = centre.subtract(eyes).normalize()
    val end = eyes.add(dir.scale(eyes.distanceTo(centre) + 1.5))
    return shape.clip(eyes, end, this)
}

fun getDirection(from: Vec3, to: Vec3): RotationUtils.Direction {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val dz = to.z - from.z

    val distXZ = sqrt(dx.sq + dz.sq)
    val dist = sqrt(distXZ.sq + dy.sq)

    val yaw = -atan2(dx, dz).deg
    val pitch = -atan2(dy, distXZ).deg

    return RotationUtils.Direction(wrapDegrees(yaw), wrapDegrees(pitch), dist)
}

fun getDirection(to: Vec3) = getDirection(mc.player!!.eyePosition, to)

inline val Number.deg get() = (toFloat() * 180 / Math.PI).toFloat()
inline val Int.sq get() = this * this
inline val Float.sq get() = this * this
inline val Double.sq get() = this * this

inline val BlockPos.shape: VoxelShape
    get() = mc.level?.let { it.getBlockState(this).getShape(it, this) } ?: Shapes.empty()