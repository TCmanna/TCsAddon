package com.tcmanna.tcsaddon.features.impl.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.PoseStack
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.addVec
import com.odtheking.odin.utils.render.drawText
import com.odtheking.odin.utils.renderPos
import com.odtheking.odin.utils.renderX
import com.odtheking.odin.utils.renderY
import com.odtheking.odin.utils.renderZ
import com.tcmanna.tcsaddon.utils.Utils
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.world.entity.player.Player
import kotlin.math.max

object NameTag : Module(
    name = "NameTag",
    description = "NameTags",
) {

    private val scaleSetting by NumberSetting("Scale", 0.8, 0.1, 2.0, 0.1, "")
    private val maxSetting by NumberSetting("MaxRender", 0.0, 0.0, 100.0, 1.0, "")
    private val rangeSetting by NumberSetting("Range", 0.0, 0.0, 512.0, 1.0, "")
    private val posAdjustment by NumberSetting("Pos Adjust", 0.0f, -5.0f, 5.0f, 0.1f, "")

    private val r by NumberSetting("Red", 20.0, 0.0, 255.0, 5.0, "")
    private val g by NumberSetting("Green", 20.0, 0.0, 255.0, 5.0, "")
    private val b by NumberSetting("Blue", 20.0, 0.0, 255.0, 5.0, "")
    private val a by NumberSetting("Alpha", 180.0, 0.0, 255.0, 5.0, "")

    private val distanceSetting by BooleanSetting("Distance", false, "")

    private val renderList = mutableListOf<Player>()

    init {
        on<TickEvent.End> {
            val player = mc.player ?: return@on
            val level = mc.level ?: return@on

            renderList.clear()

            for (p in level.players()) {
                if (shouldRender(p)) {
                    renderList.add(p)
                }
            }

            renderList.sortBy { it.distanceToSqr(player) }

            val max = maxSetting.toInt()
            if (max > 0 && renderList.size > max) {
                renderList.subList(max, renderList.size).clear()
            }
        }

        on<RenderEvent.Extract> {
            val scale = scaleSetting.toFloat()

            for (target in renderList) {
                val dist = mc.cameraEntity!!.distanceTo(target)
                val finalScale = max(1f, dist * 0.1f) * scale

                var name = target.displayName.string
                if (distanceSetting) {
                    val dist = mc.player!!.distanceTo(target)
                    name = "%.2fm $name".format(dist)
                }

                val health = target.health + target.absorptionAmount
                val healthStr = "%.1f".format(health)


                val renderText = "$name  ${getHealthColor(health)}$healthStr"

                drawText(renderText, target.renderPos.addVec(y = target.bbHeight + 0.5 + posAdjustment), finalScale, false)
            }

        }

        on<RenderEvent.Last>(priority = 10) {
            val player = mc.player ?: return@on
            val camera = mc.gameRenderer.mainCamera
            val pose = context.matrices()
            val buffer = context.consumers()
            val immediate = buffer as MultiBufferSource.BufferSource

            val scale = scaleSetting.toFloat()
            val color = ((a.toInt() shl 24) or
                    (r.toInt() shl 16) or
                    (g.toInt() shl 8) or
                    b.toInt())

            for (target in renderList) {
                renderBG(target, pose, buffer, camera, scale, color)
            }

            immediate.endBatch()
        }

    }

    @JvmStatic
    fun shouldRender(player: Player): Boolean {
        val self = mc.player ?: return false

        if (player == self) return false
        if (!player.isAlive) return false
        if (player.uuid.version() != 4) return false

        val range = rangeSetting
        if (range > 0 && self.distanceTo(player) > range) return false

        return true
    }

    private fun renderBG(
        player: Player,
        pose: PoseStack,
        buffer: MultiBufferSource,
        camera: Camera,
        scale: Float,
        color: Int
    ) {
        val camPos = camera.position()

        val x = player.renderX - camPos.x
        val y = player.renderY + player.bbHeight + 0.5 + posAdjustment - camPos.y
        val z = player.renderZ - camPos.z

        pose.pushPose()

        pose.translate(x, y, z)
        pose.mulPose(mc.entityRenderDispatcher.camera!!.rotation())

        val dist = mc.cameraEntity!!.distanceTo(player)
        val finalScale = max(1f, dist * 0.1f) * scale * 0.025f

        pose.scale(-finalScale, -finalScale, finalScale)

        val font = mc.font
        var name = player.displayName.string

        if (distanceSetting) {
            val dist = mc.player!!.distanceTo(player)
            name = "%.2fm $name".format(dist)
        }
        val health = player.health + player.absorptionAmount
        val healthStr = "%.1f".format(health)

        val renderText = "$name  $healthStr"

        val width = font.width(Utils.getTextWithoutFormattingCodes(renderText)) / 2

        drawBackground(pose, buffer, -width - 2, -2, width + 2, 10, color)
        pose.popPose()
    }

    private fun drawBackground(
        pose: PoseStack,
        buffer: MultiBufferSource,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        color: Int
    ) {
        val consumer = buffer.getBuffer(TestType)

        val a = (color shr 24 and 255) / 255f
        val r = (color shr 16 and 255) / 255f
        val g = (color shr 8 and 255) / 255f
        val b = (color and 255) / 255f

        val matrix = pose.last().pose()

        consumer.addVertex(matrix, left.toFloat(), bottom.toFloat(), 0f).setColor(r, g, b, a)
        consumer.addVertex(matrix, right.toFloat(), bottom.toFloat(), 0f).setColor(r, g, b, a)
        consumer.addVertex(matrix, right.toFloat(), top.toFloat(), 0f).setColor(r, g, b, a)
        consumer.addVertex(matrix, left.toFloat(), top.toFloat(), 0f).setColor(r, g, b, a)
    }
    val TestPipeline: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation("pipeline/debug_quads")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build()
    )
    val TestType: RenderType = RenderType.create(
        "testtype",
        RenderSetup.builder(TestPipeline)
            .sortOnUpload()
            .createRenderSetup()
    )

    private fun getHealthColor(h: Float): String {
        return when {
            h > 10f -> "§a"
            h > 5f -> "§c"
            else -> "§4"
        }
    }
}