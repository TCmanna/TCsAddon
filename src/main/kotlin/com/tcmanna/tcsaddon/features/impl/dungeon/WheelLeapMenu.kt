package com.tcmanna.tcsaddon.features.impl.dungeon

import com.mojang.blaze3d.opengl.GlTexture
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color.Companion.withAlpha
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonListener
import com.odtheking.odin.utils.skyblock.dungeon.DungeonPlayer
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.ui.animations.EaseOutAnimation
import com.odtheking.odin.utils.ui.mouseX
import com.odtheking.odin.utils.ui.mouseY
import com.odtheking.odin.utils.ui.rendering.NVGPIPRenderer
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import org.lwjgl.glfw.GLFW
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object WheelLeapMenu : Module(
    name = "Wheel Leap Menu",
    description = "Renders a custom wheel leap menu when in the Spirit Leap gui.",
    key = GLFW.GLFW_KEY_UNKNOWN
) {
    private val colorStyle by BooleanSetting("Color Style", false, desc = "Which color style to use.")
    private val backgroundColor by ColorSetting("Background Color", Colors.gray38.withAlpha(0.75f), true, desc = "Color of the background of the leap menu.").withDependency { !colorStyle }
    private val deadColor by ColorSetting("Dead Color", Colors.MINECRAFT_DARK_RED.withAlpha(0.75f), true, desc = "Color of the background of the leap menu. (dead)").withDependency { !colorStyle }
    private val scale by NumberSetting("Scale", 0.5f, 0.1f, 2f, 0.1f, desc = "Scale of the leap menu.", unit = "x")
    private val sectorMode by BooleanSetting("Sector Selection Mode", true, "")
    private val renderLine by BooleanSetting("Render Line", true, "")
    private val leapAnnounce by BooleanSetting("Leap Announce", false, desc = "Announces when you leap to a player.")
    private val leapMessage by StringSetting("Announce Message", "pc Leaped to %player%!", length = 99999, desc = "%player% %class% %shortclass%")

    private val EMPTY = DungeonPlayer("Empty", DungeonClass.EMPTY, 0, null)
    private val leapedRegex = Regex("You have teleported to (\\w{1,16})!")
    private val imageCacheMap = mutableMapOf<String, Int>()

    private const val PLAYER_COUNT = 5
    private const val SECTOR_ANGLE = 72f
    private const val START_ANGLE = -90f
    private const val INNER_RADIUS = 340f
    private const val OUTER_RADIUS = 750f
    private const val CARD_RADIUS = 520f
    private const val CARD_SIZE = 220f

    private var openAnim = EaseOutAnimation(200)
    private var opened = false

    var leapTeammates: List<DungeonPlayer> = emptyList()

    init {
        on<ScreenEvent.Close> {
            updateOpened(false)
        }

        on<ScreenEvent.Render> {
            val chest = (screen as? AbstractContainerScreen<*>) ?: return@on

            val valid = chest.title.string.equalsOneOf("Spirit Leap", "Teleport to Player")
            if (!valid || leapTeammates.isEmpty() || leapTeammates.all { it == EMPTY }) return@on
            updateOpened(true)
            drawWheel()
            cancel()
        }

        on<ScreenEvent.MouseClick> {
            mouseTrigger()
        }

        on<ChatPacketEvent> {
            if (!leapAnnounce || !DungeonUtils.inDungeons) return@on
            leapedRegex.find(value)?.groupValues?.get(1)?.let { name ->
                val teammate = DungeonListener.dungeonTeammatesNoSelf.firstOrNull { it.name == name }
                val cls = teammate?.clazz?.name ?: "???"
                val shortCls = cls.firstOrNull()?.toString() ?: "?"
                sendCommand(leapMessage.replace("%player%", name).replace("%class%", cls).replace("%shortclass%", shortCls))
            }
        }

        onReceive<ClientboundPlayerInfoUpdatePacket>(-100) {
            updateDungeonTeammates()
        }
    }

    private fun updateDungeonTeammates() = mc.execute {
        leapTeammates = yqlossSorting(DungeonListener.dungeonTeammatesNoSelf)
    }

    fun ScreenEvent.Render.drawWheel() {
        val hw = mc.window.screenWidth / 2f
        val hh = mc.window.screenHeight / 2f
        val selectedArea = getArea()

        NVGPIPRenderer.draw(guiGraphics, 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight()) {
            var moveScale = 1f
            if (openAnim.isAnimating()) {
                moveScale = openAnim.get(0f, 1f)

            }
            NVGRenderer.translate(hw, hh)
            NVGRenderer.scale(scale * moveScale, scale * moveScale)


            for (index in 0 until PLAYER_COUNT) {
                val startAngle = Math.toRadians((START_ANGLE + index * SECTOR_ANGLE).toDouble()).toFloat()
                val endAngle = Math.toRadians((START_ANGLE + (index + 1) * SECTOR_ANGLE).toDouble()).toFloat()
                val player = leapTeammates.getOrNull(index) ?: EMPTY
                val selected = selectedArea == index + 1

                val baseColor = when {
                    player == EMPTY -> Colors.gray26.withAlpha(0.35f).rgba
                    player.isDead -> deadColor.rgba
                    colorStyle -> player.clazz.color.withAlpha(0.75f).rgba
                    else -> backgroundColor.rgba
                }

                val sectorColor = if (selected) Colors.WHITE.withAlpha(0.30f).rgba else baseColor
                NVGRenderer.ringSector(0f, 0f, INNER_RADIUS, OUTER_RADIUS, startAngle, endAngle, sectorColor)
            }

            for (index in 0 until PLAYER_COUNT) {
                val angle = Math.toRadians((START_ANGLE + index * SECTOR_ANGLE + SECTOR_ANGLE / 2f).toDouble())
                val x = cos(angle).toFloat() * CARD_RADIUS
                val y = sin(angle).toFloat() * CARD_RADIUS
                val player = leapTeammates.getOrNull(index) ?: EMPTY

                if (player == EMPTY) continue

                val selected = selectedArea == index + 1
                val imageSize = if (selected) CARD_SIZE * 0.68f else CARD_SIZE * 0.6f

                NVGRenderer.push()
                NVGRenderer.translate(x, y)

                (player.playerSkin?.body?.id() ?: mc.player?.skin?.body?.id())?.let { locationSkin ->
                    imageCacheMap.getOrPut(locationSkin.path) {
                        NVGRenderer.createNVGImage((mc.textureManager.getTexture(locationSkin).texture as? GlTexture)?.glId() ?: 0, 64, 64)
                    }.let { glTextureId ->
                        NVGRenderer.image(glTextureId, 64, 64, 8, 8, 8, 8, -imageSize / 2f, -imageSize / 2f, imageSize, imageSize, 12f)
                        NVGRenderer.image(glTextureId, 64, 64, 40, 8, 8, 8, -imageSize / 2f, -imageSize / 2f, imageSize, imageSize, 12f)
                        if (player.isDead) NVGRenderer.circle(0f, 0f, imageSize / 2f, Colors.gray26.withAlpha(0.8f).rgba)
                    }
                }

                val fontSize = 55f
                val className = player.clazz.name
                val classWidth = NVGRenderer.textWidth(className, fontSize, NVGRenderer.defaultFont)

                NVGRenderer.textShadow(
                    className,
                    -classWidth / 2f,
                    -imageSize / 2f - fontSize * 1.1f,
                    fontSize,
                    if (player.isDead) Colors.gray38.rgba else if (!colorStyle) player.clazz.color.rgba else Colors.WHITE.rgba,
                    NVGRenderer.defaultFont
                )

                val playerName = shrinkString(player.name, CARD_SIZE - 40f, fontSize)
                val playerWidth = NVGRenderer.textWidth(playerName, fontSize, NVGRenderer.defaultFont)

                NVGRenderer.textShadow(
                    playerName,
                    -playerWidth / 2f,
                    imageSize / 2f + fontSize * 0.2f,
                    fontSize,
                    if (player.isDead) Colors.gray38.rgba else Colors.WHITE.rgba,
                    NVGRenderer.defaultFont
                )

                NVGRenderer.pop()
            }

            NVGRenderer.circle(0f, 0f, INNER_RADIUS - 4f, Colors.gray26.withAlpha(0.2f).rgba)

            for (index in 0 until PLAYER_COUNT) {
                val angle = Math.toRadians((START_ANGLE + index * SECTOR_ANGLE).toDouble())
                val x1 = cos(angle).toFloat() * INNER_RADIUS
                val y1 = sin(angle).toFloat() * INNER_RADIUS
                val x2 = cos(angle).toFloat() * OUTER_RADIUS
                val y2 = sin(angle).toFloat() * OUTER_RADIUS
                NVGRenderer.line(x1, y1, x2, y2, 4f, Colors.WHITE.withAlpha(0.25f).rgba)
            }

            if (renderLine) {
                val sx = mc.window.screenWidth.toFloat() / guiGraphics.guiWidth()
                val sy = mc.window.screenHeight.toFloat() / guiGraphics.guiHeight()
                val mx = (mouseX * sx - hw) / scale
                val my = (mouseY * sy - hh) / scale
                NVGRenderer.line(0f, 0f, mx, my, 4f, Colors.WHITE.rgba)
            }

            val selectedPlayer = leapTeammates.getOrElse(selectedArea - 1) { EMPTY }

            if (selectedPlayer != EMPTY) {
                val width = NVGRenderer.textWidth(selectedPlayer.clazz.name, 65f, NVGRenderer.defaultFont)
                NVGRenderer.textShadow(selectedPlayer.clazz.name, -width / 2f, 0f, 65f, selectedPlayer.clazz.color.rgba, NVGRenderer.defaultFont)
            }
        }
    }

    fun shrinkString(string: String, width: Float, fontSize: Float): String {
        if (NVGRenderer.textWidth(string, fontSize, NVGRenderer.defaultFont) < width) return string
        repeat(string.length) {
            val omitted = string.substring(0, string.length - it) + "..."
            if (NVGRenderer.textWidth(omitted, fontSize, NVGRenderer.defaultFont) < width) return omitted
        }
        return "..."
    }

    fun getArea(): Int {
        val cx = (mouseX - mc.window.screenWidth / 2f) / scale
        val cy = (mouseY - mc.window.screenHeight / 2f) / scale
        val distance = hypot(cx.toDouble(), cy.toDouble()).toFloat()

        if (!sectorMode) {
            if (distance !in INNER_RADIUS..OUTER_RADIUS) return 0
        } else if (distance !in 10f..2000f) return 0

        var angle = Math.toDegrees(atan2(cy.toDouble(), cx.toDouble())).toFloat() - START_ANGLE
        angle %= 360f
        if (angle < 0f) angle += 360f

        return (angle / SECTOR_ANGLE).toInt() + 1
    }

    fun ScreenEvent.mouseTrigger() {
        val chest = (screen as? AbstractContainerScreen<*>) ?: return
        if (!chest.title.string.equalsOneOf("Spirit Leap", "Teleport to Player")) return
        if (leapTeammates.isEmpty() || leapTeammates.all { it == EMPTY }) return

        val area = getArea()
        if (area <= 0) return

        val playerToLeap = leapTeammates.getOrElse(area - 1) { EMPTY }
        if (playerToLeap == EMPTY) return
        if (playerToLeap.isDead) return modMessage("This player is dead, can't leap.")

        leapTo(playerToLeap.name, chest)
        cancel()
    }

    fun leapTo(name: String, screenHandler: AbstractContainerScreen<*>) {
        val index = screenHandler.menu.slots.subList(11, 16).firstOrNull {
            it.item.hoverName.string.substringAfter(' ')
                .equals(name.noControlCodes, ignoreCase = true)
        }?.index ?: return
        mc.player?.clickSlot(screenHandler.menu.containerId, index)
        modMessage("Teleporting to $name.")
    }

    private fun yqlossSorting(players: List<DungeonPlayer>): List<DungeonPlayer> {
        val players = players.toMutableList()
        players.sortBy { it.name }

        return listOf(
            DungeonClass.BERSERK,
            DungeonClass.MAGE,
            DungeonClass.HEALER,
            DungeonClass.TANK,
            DungeonClass.ARCHER,
        )
            .map { clazz -> players.firstOrNull { it.clazz == clazz }?.also(players::remove) }
            .map { it ?: players.removeFirstOrNull() ?: EMPTY }
    }

    fun NVGRenderer.ringSector(cx: Float, cy: Float, innerRadius: Float, outerRadius: Float, startAngle: Float, endAngle: Float, color: Int) {
        (this as NVGRendererAccessor).`tcsaddon$ringSector`(cx, cy, innerRadius, outerRadius, startAngle, endAngle, color)
    }

    private fun updateOpened(value: Boolean) {
        if (opened != value) {
            opened = value
            if (opened) {
                openAnim = EaseOutAnimation(300)
                openAnim.start()
            }
        }
    }
}