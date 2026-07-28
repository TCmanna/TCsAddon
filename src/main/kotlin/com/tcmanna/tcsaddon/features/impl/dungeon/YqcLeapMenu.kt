package com.tcmanna.tcsaddon.features.impl.dungeon

import com.mojang.blaze3d.opengl.GlTexture
import com.odtheking.odin.OdinMod
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
import com.odtheking.odin.utils.ui.HoverHandler
import com.odtheking.odin.utils.ui.mouseX
import com.odtheking.odin.utils.ui.mouseY
import com.odtheking.odin.utils.ui.rendering.NVGPIPRenderer
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import org.lwjgl.glfw.GLFW
import java.util.ArrayList
import kotlin.math.min

//Form https://github.com/Necron-Dev/yqc-leap-menu
object YqcLeapMenu : Module(
    name = "[YQ] Leap Menu",
    description = "Renders a custom leap menu when in the Spirit Leap gui.",
    key = GLFW.GLFW_KEY_UNKNOWN
) {
    private val colorStyle by BooleanSetting("Color Style", false, desc = "Which color style to use.")

    private val backgroundColor by ColorSetting(
        "Background Color",
        Colors.gray38.withAlpha(0.75f),
        true,
        desc = "Color of the background of the leap menu."
    ).withDependency { !colorStyle }

    private val deadColor by ColorSetting(
        "Dead Color",
        Colors.MINECRAFT_DARK_RED.withAlpha(0.75f),
        true,
        desc = "Color of the background of the leap menu. (dead)"
    ).withDependency { !colorStyle }

    private val scale by NumberSetting("Scale", 0.5f, 0.1f, 2f, 0.1f, desc = "Scale of the leap menu.", unit = "x")

    private val renderLine by BooleanSetting("Render Line", true, "")

    private val leapAnnounce by BooleanSetting("Leap Announce", false, desc = "Announces when you leap to a player.")

    private val leapMessage by StringSetting(
        "Announce Message",
        "pc Leaped to %player%!",
        desc = "%player% %class% %shortclass%"
    )

//    private val debugButton by ActionSetting("set debug", "") {
//        leapTeammates = listOf(
//            DungeonPlayer("Empty", DungeonClass.Archer, 0, null),
//            DungeonPlayer("Empty", DungeonClass.Berserk, 0, null),
//            DungeonPlayer("Empty", DungeonClass.Mage, 0, null),
//            DungeonPlayer("Empty", DungeonClass.Healer, 0, null),
//            DungeonPlayer("Empty", DungeonClass.Tank, 0, null)
//        )
//    }

    private val hoverHandler = List(5) { HoverHandler(200L) }
    private val EMPTY = DungeonPlayer("Empty", DungeonClass.EMPTY, 0, null)
    private val leapedRegex = Regex("You have teleported to (\\w{1,16})!")
    private val imageCacheMap = mutableMapOf<String, Int>()

    const val CW = 300f
    const val CH = 200f
    const val BW = 600f
    const val BH = 600f
    const val GAP = 30f

    val b = min(BW, BH)

    data class Box(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val tx: Float,
        val ty: Float,
    )

    val boxes = listOf(
        Box(-CW-BW-GAP, -CH-BH-GAP, -GAP/2, -CH-GAP, -CW/2, -CH + 50f), // top left
        Box(GAP/2, -CH-BH-GAP, CW+BW+GAP, -CH-GAP, CW/2, -CH + 50f), // top right
        Box(CW+GAP, -CH, CW+BW+GAP, CH+BH+GAP, CW - 30f, 20f), // bottom right
        Box(-CW, CH+GAP, CW, CH+BH+GAP, 0f, CH - 50f), // bottom middle
        Box(-CW-BW-GAP, -CH, -CW-GAP, CH+BH+GAP, -CW + 30f, 20f), // bottom left
    )

    var leapTeammates: List<DungeonPlayer> = ArrayList(0)

    init {
        on<ScreenEvent.Render> {
            val chest = (screen as? AbstractContainerScreen<*>) ?: return@on
            if (!chest.title.string.equalsOneOf("Spirit Leap", "Teleport to Player")) return@on
            if (leapTeammates.isEmpty() || leapTeammates.all { it == EMPTY }) return@on

            drawYqloss()
            cancel()
            return@on
        }

        on<ScreenEvent.MouseClick> {
            debug("mouseclickbegin")
            mouseTrigger()
            debug("mouseclickend")
        }

        on<ChatPacketEvent> {
            if (!leapAnnounce || !DungeonUtils.inDungeons) return@on
            leapedRegex.find(value)?.groupValues?.get(1)?.let {  name ->
                val teammate = DungeonListener.dungeonTeammatesNoSelf.firstOrNull { it.name == name }
                val cls = teammate?.clazz?.name ?: "???"
                val shortCls = cls[0].toString()
                sendCommand(
                    leapMessage
                        .replace("%player%", name)
                        .replace("%class%", cls)
                        .replace("%shortclass%", shortCls)
                )
            }
        }

        onReceive<ClientboundPlayerInfoUpdatePacket>(-100) {
            updateDungeonTeammates()
        }
    }

    private fun updateDungeonTeammates() = OdinMod.mc.execute {
        leapTeammates = yqlossSorting(DungeonListener.dungeonTeammatesNoSelf)
    }

    fun ScreenEvent.Render.drawYqloss() {
        val hw = mc.window.screenWidth / 2f
        val hh = mc.window.screenHeight / 2f

        hoverHandler.forEachIndexed { index, handler ->
            val (left, top, right, bottom) = boxes[index]
            handler.handle(
                hw + left * scale,
                hh + top * scale,
                (right - left) * scale,
                (bottom - top) * scale
            )
        }

        NVGPIPRenderer.draw(guiGraphics, 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight()) {
            NVGRenderer.translate(hw, hh)
            NVGRenderer.scale(scale, scale)

            boxes.forEachIndexed { index, box ->
                val player = leapTeammates.getOrNull(index) ?: return@forEachIndexed
                if (player == EMPTY) return@forEachIndexed

                val midx = (box.left + box.right) / 2
                val midy = (box.top + box.bottom) / 2
                val isHovered = hoverHandler[index].isHovered
                val expand = hoverHandler[index].anim.get(0f, 15f, !isHovered) * 2f

                NVGRenderer.push()
                NVGRenderer.translate(midx, midy)
                NVGRenderer.scale((expand + b) / b, (expand + b) / b)

                NVGRenderer.rect(
                    box.left - midx,
                    box.top - midy,
                    (box.right - box.left),
                    (box.bottom - box.top),
                    if (player.isDead) deadColor.rgba else (if (colorStyle) player.clazz.color else backgroundColor).rgba,
                    12f
                )

                val p = 0.3f

                (player.playerSkin?.body?.id() ?: mc.player?.skin?.body?.id())?.let { locationSkin ->
                    imageCacheMap.getOrPut(locationSkin.path) {
                        NVGRenderer.createNVGImage(
                            (mc.textureManager.getTexture(locationSkin).texture as? GlTexture)?.glId() ?: 0,
                            64,
                            64
                        )
                    }.let { glTextureId ->
                        val x = b * -p
                        val y = b * -p
                        val size = b * p * 2f

                        NVGRenderer.image(
                            glTextureId,
                            64,
                            64,
                            8,
                            8,
                            8,
                            8,
                            x,
                            y,
                            size,
                            size,
                            9f
                        )

                        NVGRenderer.image(
                            glTextureId,
                            64,
                            64,
                            40,
                            8,
                            8,
                            8,
                            x,
                            y,
                            size,
                            size,
                            9f
                        )

                        if (player.isDead) {
                            NVGRenderer.rect(b * -p, b * -p, b * p * 2f, b * p * 2f, Colors.gray26.withAlpha(0.8f).rgba, 9f)
                        }
                    }
                }

                val fontSize = 75f
                val up = -1.2f
                val down = 0.3f

                run {
                    val className = player.clazz.name

                    val width = NVGRenderer.textWidth(className, fontSize, NVGRenderer.defaultFont)

                    NVGRenderer.textShadow(
                        className,
                        - width / 2,
                        -b * p + fontSize * up,
                        fontSize,
                        if (player.isDead) Colors.gray38.rgba else if (!colorStyle) player.clazz.color.rgba else Colors.WHITE.rgba,
                        NVGRenderer.defaultFont
                    )
                }

                run {
                    val playerName = shrinkString(player.name, box.right - box.left - 50f, fontSize)

                    val width = NVGRenderer.textWidth(playerName, fontSize, NVGRenderer.defaultFont)

                    NVGRenderer.textShadow(
                        playerName,
                        - width / 2,
                        b * p + fontSize * down,
                        fontSize,
                        if (player.isDead) Colors.gray38.rgba else Colors.WHITE.rgba,
                        NVGRenderer.defaultFont
                    )
                }

                NVGRenderer.pop()
            }

            if (renderLine) {
                val sx = mc.window.screenWidth.toFloat() / guiGraphics.guiWidth()
                val sy = mc.window.screenHeight.toFloat() / guiGraphics.guiHeight()

                val mx = (mouseX * sx - hw) / scale
                val my = (mouseY * sy - hh) / scale

                NVGRenderer.line(
                    0f,
                    0f,
                    mx,
                    my,
                    4f,
                    Colors.WHITE.rgba
                )
            }

            val selectedPlayer = leapTeammates.getOrElse(getArea() - 1) { EMPTY }
            if (selectedPlayer != EMPTY) {
                val width = NVGRenderer.textWidth(selectedPlayer.clazz.name, 75f, NVGRenderer.defaultFont)
                NVGRenderer.textShadow(
                    selectedPlayer.clazz.name,
                    - width / 2,
                    0f,
                    75f,
                    Colors.WHITE.rgba,
                    NVGRenderer.defaultFont
                )
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
        return boxes.indexOfFirst { cx in it.left..it.right && cy in it.top..it.bottom } + 1
    }

    fun ScreenEvent.mouseTrigger() {
        debug("begin")
        val chest = (screen as? AbstractContainerScreen<*>).debug("screen") ?: return
        if (
            chest
                .title
                .debug("title")
                ?.string
                .debug("title.string")
                ?.equalsOneOf("Spirit Leap", "Teleport to Player")
                .debug("equalsOneOf") == false ||
            leapTeammates.isEmpty().debug("teammates.empty") ||
            leapTeammates.all { it == EMPTY }.debug("teammates.allempty")
        ) return

        val quadrant = getArea().debug("area")

        val playerToLeap = leapTeammates.getOrElse(quadrant - 1) { EMPTY }.debug("playertoleap")
        if (playerToLeap == EMPTY) return
        if (playerToLeap.isDead) return modMessage("This player is dead, can't leap.")

        leapTo(playerToLeap.name, chest)
        cancel()

        debug("end")
    }

    fun leapTo(name: String, screenHandler: AbstractContainerScreen<*>) {
        debug("beginleapto")
        val index = screenHandler
            .menu
            .debug("menu")
            .slots
            .debug("slots")
            .subList(11, 16)
            .debug("subList")
            .firstOrNull {
                it
                    .debug("it")
                    .item
                    .debug("item")
                    ?.hoverName
                    .debug("hoverName")
                    ?.string
                    .debug("hoverNameString")
                    ?.substringAfter(' ')
                    .debug("hoverNameStringAfterSpace")
                    .equals(name.noControlCodes, ignoreCase = true)
                    .debug("equals")
            }
            .debug("firstOrNull")
            ?.index
            .debug("index")
            ?: return
        mc
            .player
            ?.clickSlot(screenHandler.menu.containerId, index)
            .debug("clickSlot")
        modMessage("Teleporting to $name.")
        debug("endleapto")
    }

    fun <T> T.debug(name: String) = also {
        println("[YQCLEAP] $name: $it")
    }

    fun yqlossSorting(players: List<DungeonPlayer>): List<DungeonPlayer> {
        val players = players.toMutableList()
        players.sortBy { it.name }

        return listOf(
            DungeonClass.ARCHER,
            DungeonClass.BERSERK,
            DungeonClass.MAGE,
            DungeonClass.HEALER,
            DungeonClass.TANK,
        )
            .map { clazz -> players.firstOrNull { it.clazz == clazz }?.also(players::remove) }
            .map { it ?: players.removeFirstOrNull() ?: EMPTY }
    }
}