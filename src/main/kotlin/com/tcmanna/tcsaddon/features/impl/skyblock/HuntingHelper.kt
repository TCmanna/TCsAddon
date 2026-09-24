package com.tcmanna.tcsaddon.features.impl.skyblock

import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.LocationChangeEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.addVec
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.render.drawText
import com.odtheking.odin.utils.render.drawTracer
import com.odtheking.odin.utils.render.drawWireFrameBox
import com.odtheking.odin.utils.render.getStringWidth
import com.odtheking.odin.utils.render.text
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.odtheking.odin.utils.startsWithOneOf
import com.tcmanna.tcsaddon.utils.Utils
import com.tcmanna.tcsaddon.utils.Utils.getEntityTextureString
import com.tcmanna.tcsaddon.utils.Utils.toPos
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.gui.screens.InBedChatScreen
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.bee.Bee
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Shulker
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.Optional
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

object HuntingHelper: Module(
    name = "Hunting Helper",
    description = "Automation tools for creature hunting."
) {
    private val safariUnique by HUD("Safari Unique Display", "") {
        if (LocationUtils.isCurrentArea(Island.Safari) || it) {
            if (currentUnique.isEmpty()) return@HUD 0 to 0
            val width = currentUnique.maxOf { (area, nameList) ->
                nameList.maxOfOrNull { name ->
                    getStringWidth(name)
                } ?: 0
            }
            var i = 0
            currentUnique.forEach { (area, nameList) ->
                val color = when (area) {
                    "Cavern" -> Colors.MINECRAFT_GOLD
                    "Forest" -> Colors.MINECRAFT_DARK_GREEN
                    "Haunted" -> Colors.MINECRAFT_LIGHT_PURPLE
                    "Icy" -> Colors.MINECRAFT_AQUA
                    else -> Colors.WHITE
                }
                text(area, 0, i * 9, Colors.WHITE)
                i++
                nameList.forEach { name ->
                    text(name, 0, i * 9, color)
                    i++
                }
            }
            return@HUD width to i * 9
        }
        0 to 0
    }

    private val esp by DropdownSetting("ESP")
    private val sparklingESP by BooleanSetting("Sparkling ESP", true, "").withDependency { esp }
    private val hideonsunEsp by BooleanSetting("Hideonsun ESP", false, "Highlight Hideonsun.").withDependency { esp }
    private val pangolinEsp by BooleanSetting("Pangolin ESP", false, "Highlight Pangolin.").withDependency { esp }
    private val beeHeemothEsp by BooleanSetting("BeeHeemoth ESP", false, "Highlight BeeHeemoth.").withDependency { esp }
    private val blueJayEsp by BooleanSetting("Blue Jay ESP", false, "Highlight Blue Jay.").withDependency { esp }
    private val hideonwallEsp by BooleanSetting("Hideonwall ESP", false, "Highlight Hideonwall  in Safari.").withDependency { esp }
    private val hideonfloorEsp by BooleanSetting("Hideonfloor ESP", false, "Highlight Hideonfloor in Safari.").withDependency { esp }
    private val bloodbatEsp by BooleanSetting("Bloodbat ESP", false, "Highlight Bloodbat in Safari.").withDependency { esp }
    private val floordropEsp by BooleanSetting("Floordrop ESP", false, "Highlight floordrop item in Safari.").withDependency { esp }
    private val hideyhoESP by BooleanSetting("Hideyho ESP", false, "Highlight and tracking Hideyho in Safari.").withDependency { esp }

    private val automation by DropdownSetting("Automation")
    private val autoReel by BooleanSetting("Auto Reel", false, "Auto right-click REEL armor stands when holding a Lasso.").withDependency { automation }
    private val autoGetup by BooleanSetting("Auto Getup", true, "").withDependency { automation }
    private val autoHideyho by BooleanSetting("Auto Hideyho", true, "").withDependency { automation }

    private val uniqueCavern = listOf(
        "Cavernfish", "Flitter", "Shyworm", "Driftling", "Chuckwalla", "Rockmite", "Scrappy", "Snoozle", "Gemzie"
    )
    private val uniqueForest = listOf(
        "Foxtrot", "Bluebird", "Honeybug", "Treefrog", "Woodchucker", "Fluffling", "Hideonfloor", "Parakeet", "Macaw"
    )
    private val uniqueHaunted = listOf(
        "Areita", "Bloodbat", "Duplico", "Gazer", "Litterbug", "Solsnatcher", "Gimmiegold", "Hideonwall", "Hideyho", "Doomspiral"
    )
    private val uniqueIcy = listOf(
        "Strongarm", "Tepid", "Polaris", "Shuddersquid", "Billygoat", "Mantis Shrimp", "Nozzlenose", "Troodon", "Wumpa"
    )

    private val currentUnique = mutableMapOf<String, MutableList<String>>()

    data class SparklingParticle(
        val pos: BlockPos,
        var lastSeen: Long
    )

    data class HuntingMob(
        val name: String,
        val type: EntityType<*>,
        val color: Color,
        val enabled: () -> Boolean,
        val area: Island? = null,
        val extra: (Entity) -> Boolean = { true },
    ) {
        val targets = mutableListOf<Entity>()
    }

    private val huntingMobs = listOf(
        HuntingMob("Hideonsun", EntityType.SHULKER, Colors.MINECRAFT_GOLD, { hideonsunEsp },
            area = Island.TorrhusCanyon,
            extra = { val c = (it as Shulker).color; c == DyeColor.YELLOW || c == DyeColor.BROWN }),
        HuntingMob("Hideonwall", EntityType.SHULKER, Colors.MINECRAFT_DARK_PURPLE, { hideonwallEsp },
            area = Island.Safari,
            extra = { (it as Shulker).color == DyeColor.PURPLE }),
        HuntingMob("Hideonfloor", EntityType.SHULKER, Colors.MINECRAFT_DARK_GREEN, { hideonfloorEsp },
            area = Island.Safari,
            extra = { (it as Shulker).color == DyeColor.GREEN }),
        HuntingMob("Bloodbat", EntityType.BAT, Colors.MINECRAFT_RED, { bloodbatEsp },
            area = Island.Safari),

        HuntingMob("Pangolin", EntityType.ARMADILLO, Color(0x9F5656), { pangolinEsp }),
        HuntingMob("BeeHeemoth", EntityType.BEE, Color(0xFECE3E), { beeHeemothEsp }, extra = { (it as Bee).scale == 9.0f }),
        HuntingMob("Blue Jay", EntityType.PARROT, Colors.MINECRAFT_BLUE, { blueJayEsp }, area = Island.TorrhusCanyon),
    )

    private val hideyhoSkin = "3504f1f2327a5110e643bb8667082512815fa434a29ed37f4ca83bb16d2db533"

    private val floordrops = mutableSetOf<BlockPos>()
    private val reelStands = mutableListOf<ArmorStand>()
    private val sparklingStands = mutableListOf<BlockPos>()
    private val sparklingParticles = mutableListOf<SparklingParticle>()
    private val hideyhoList = mutableListOf<Player>()

    private var lastReelTime = 0L
    private var reelingStandId = -1
    private var scanTick = 0

    init {
        on<ScreenEvent.Open> {
            if (!autoGetup || !LocationUtils.isCurrentArea(Island.Safari)) return@on
            if (screen is InBedChatScreen) {
                mc.player?.connection?.send(
                    ServerboundPlayerCommandPacket(
                        mc.player!!,
                        ServerboundPlayerCommandPacket.Action.STOP_SLEEPING
                    )
                )
            }
        }

        onReceive<ClientboundLevelParticlesPacket> {
            if (!LocationUtils.isCurrentArea(Island.Safari)) return@onReceive
            if (!sparklingESP) return@onReceive
            if (particle.type != ParticleTypes.WAX_ON) return@onReceive
            val particleVec = Vec3(x, y, z)
            val now = System.currentTimeMillis()

            val existing = sparklingParticles.firstOrNull {
                it.pos.distToCenterSqr(particleVec) < 25
            }

            if (existing != null) {
                existing.lastSeen = now
            }
            else {
                sparklingParticles += SparklingParticle(particleVec.toPos(), now)
            }
        }

        on<LocationChangeEvent> {
            if (!LocationUtils.isCurrentArea(Island.Safari)) return@on
            resetUnique()
        }

        on<MessageEvent.Chat> {
            if (!LocationUtils.isCurrentArea(Island.Safari)) return@on
            if (message.startsWithOneOf("LOOT SHARE!", "CAPTURE!")) {
                currentUnique.forEach { (area, nameList) ->
                    nameList.removeIf {
                        message.contains(it)
                    }
                }
            }
            if (autoHideyho && message == "Select an option: [Sure] [No thanks...] ") {
                val payload = CompoundTag().apply {
                    putString("npcId", "hideyho")
                    putString("responseKey", "r_4_1")
                }
                val packet = ServerboundCustomClickActionPacket(
                    Identifier.parse("skyblock:dialogue_response"),
                    Optional.of(payload)
                )
                mc.connection?.send(packet)
            }
        }

        on<TickEvent.End> {
            val now = System.currentTimeMillis()

            if (++scanTick >= 2) {
                scanTick = 0
                refreshTargets()
            }

            if (autoReel) {
                if (reelingStandId != -1) {
                    val stand = mc.level?.getEntity(reelingStandId)
                    reelingStandId = when {
                        stand == null || !stand.isAlive -> -1
                        now - lastReelTime >= 3000L -> -1
                        else -> return@on
                    }
                }

                if (now - lastReelTime < 250L) return@on
                val player = mc.player ?: return@on
                val held = player.mainHandItem
                if (!held.itemId.contains("LASSO", ignoreCase = true)) return@on

                val stand = reelStands.firstOrNull { it.isAlive && it.distanceToSqr(player) <= 225.0 }
                if (stand != null) {
                    lastReelTime = now
                    reelingStandId = stand.id
                    scope.launch {
                        delay(Random.nextLong(20, 100).milliseconds)
                        Utils.playerUseHeldItem(mc.player, false)
                    }
                }
            }
        }

        on<RenderEvent.Extract> {
            if (!isHuntingArea()) return@on

            renderMobEsp(this)
            renderHideyho(this)
            renderFloordropEsp(this)
            renderSparkling(this)
        }

        on<LevelEvent.Load> {
            sparklingParticles.clear()
            sparklingStands.clear()
        }
    }

    override fun onEnable() {
        resetUnique()
        super.onEnable()
    }

    private fun renderMobEsp(event: RenderEvent.Extract) {
        for (mob in huntingMobs) {
            if (!mob.enabled()) continue
            for (entity in mob.targets) {
                if (entity.isAlive) event.drawWireFrameBox(entity.renderBoundingBox, mob.color, 2f)
            }
        }
    }

    private fun renderHideyho(event: RenderEvent.Extract) {
        if (!hideyhoESP || hideyhoList.isEmpty()) return
        val area = SafariArea.getCurrentArea() ?: return
        if (area != SafariArea.Haunted) return
        for (hideyho in hideyhoList) {
            if (hideyho.isAlive) {
                event.drawWireFrameBox(hideyho.renderBoundingBox, Colors.WHITE, 2f)
                event.drawTracer(hideyho.renderBoundingBox.center, Colors.WHITE, false, 3f)
            }
        }
    }

    private fun renderFloordropEsp(event: RenderEvent.Extract) {
        if (!floordropEsp) return
        val area = SafariArea.getCurrentArea() ?: return
        if (area == SafariArea.Spawn) return
        for (pos in floordrops) {
            if (!isPositionInArea(area.corner1, area.corner2, pos)) continue
            event.drawWireFrameBox(floordropBox(pos), Colors.WHITE, 2f)
        }
    }

    private fun renderSparkling(event: RenderEvent.Extract) {
        if (!sparklingESP) return
        if (!LocationUtils.isCurrentArea(Island.Safari)) {
            sparklingParticles.clear()
            return
        }
        val sparklingPos = mutableListOf<BlockPos>()
        sparklingPos.addAll(sparklingStands)

        val now = System.currentTimeMillis()

        sparklingParticles.removeIf {
            now - it.lastSeen > 1000
        }

        sparklingParticles.forEach {
            if (!sparklingPos.any { pos -> pos.distSqr(it.pos) < 25 }) {
                sparklingPos.add(it.pos)
            }
        }
        sparklingPos.forEach {
            val aabb = AABB(
                it.x.toDouble(), it.y.toDouble(), it.z.toDouble(),
                it.x.toDouble()+1, it.y.toDouble()+1, it.z.toDouble()+1
            )
            event.drawText("§6§l!!!SPARKLING!!!", it.center.addVec(y = 1), 3f, false)
            event.drawWireFrameBox(aabb, Colors.MINECRAFT_GOLD, 3f)
            event.drawTracer(it.center, Colors.MINECRAFT_GOLD, false, 3f)
        }
    }

    private fun refreshTargets() {
        val level = mc.level ?: return
        huntingMobs.forEach { it.targets.clear() }
        reelStands.clear()
        sparklingStands.clear()
        floordrops.clear()

        hideyhoList.clear()
        val filter = level.players().filter {
            it.getEntityTextureString()?.contains(hideyhoSkin) == true
        }
        hideyhoList.addAll(filter)

        val activeMobs = huntingMobs.filter { it.area == null || LocationUtils.isCurrentArea(it.area) }
        for (entity in level.entitiesForRendering()) {
            if (!entity.isAlive) continue
            for (mob in activeMobs) {
                if (entity.type == mob.type && mob.extra(entity)) mob.targets.add(entity)
            }
            if (entity is ArmorStand) {
                val pureName = entity.displayName.string.noControlCodes
                if (pureName.contains("REEL", ignoreCase = true))
                    reelStands.add(entity)
                if (pureName.contains("SPARKLING", ignoreCase = true))
                    sparklingStands.add(entity.onPos)
            }
            if (floordropEsp && entity is Display.ItemDisplay &&
                entity.itemRenderState()?.itemStack()?.item == Items.STRING) {
                floordrops.add(entity.blockPosition())
            }
        }
    }

    private fun floordropBox(pos: BlockPos): AABB = AABB(
        pos.x.toDouble(), pos.y + 1.0, pos.z.toDouble(),
        pos.x + 1.0, pos.y + 1.125, pos.z + 1.0)

    private fun isHuntingArea(): Boolean =
        LocationUtils.isCurrentArea(Island.LotusAtoll, Island.MoongladeMarsh, Island.TorrhusCanyon, Island.Safari)


    enum class SafariArea(val corner1: BlockPos, val corner2: BlockPos) {
        Spawn(BlockPos(-74, 32, -24), BlockPos(-26, 128, 24)),
        Icy(BlockPos(-50, 32, -1), BlockPos(-181, 128, -120)),
        Haunted(BlockPos(-49, 32, 0), BlockPos(62, 128, -120)),
        Cavern(BlockPos(-51, 32, 0), BlockPos(-181, 128, 120)),
        Forest(BlockPos(-50, 32, 1), BlockPos(62, 128, 120));

        companion object {
            fun getCurrentArea(): SafariArea? {
                val player = mc.player ?: return null
                val playerPos = BlockPos(player.x.toInt(), player.y.toInt(), player.z.toInt())

                if (isPositionInArea(Spawn.corner1, Spawn.corner2, playerPos)) return Spawn

                return entries.firstOrNull { area ->
                    area != Spawn && isPositionInArea(area.corner1, area.corner2, playerPos)
                }
            }
        }
    }

    fun isPositionInArea(corner1: BlockPos, corner2: BlockPos, pos: BlockPos): Boolean {
        val minX = minOf(corner1.x, corner2.x)
        val maxX = maxOf(corner1.x, corner2.x)
        val minY = minOf(corner1.y, corner2.y)
        val maxY = maxOf(corner1.y, corner2.y)
        val minZ = minOf(corner1.z, corner2.z)
        val maxZ = maxOf(corner1.z, corner2.z)

        return pos.x in minX..maxX &&
                pos.y in minY..maxY &&
                pos.z in minZ..maxZ
    }

    private fun resetUnique() {
        currentUnique.clear()
        currentUnique["Cavern"] = mutableListOf()
        currentUnique["Cavern"]?.addAll(uniqueCavern)
        currentUnique["Forest"] = mutableListOf()
        currentUnique["Forest"]?.addAll(uniqueForest)
        currentUnique["Haunted"] = mutableListOf()
        currentUnique["Haunted"]?.addAll(uniqueHaunted)
        currentUnique["Icy"] = mutableListOf()
        currentUnique["Icy"]?.addAll(uniqueIcy)
    }
}