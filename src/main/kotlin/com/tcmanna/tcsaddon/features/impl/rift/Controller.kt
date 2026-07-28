package com.tcmanna.tcsaddon.features.impl.rift

import com.tcmanna.tcsaddon.utils.Utils.mc
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player

class HolyIceController(private val queue: TickActionQueue) {
    private var cooldown = 0
    private var delay = 0

    private var inDelay = false

    fun reset() {
        cooldown = 0
    }

    fun tick(enabled: Boolean, delayTicks: Int) {
        if (!enabled) return

        if (mc.player == null || mc.level == null)
            return

        if (cooldown > 0) {
            cooldown--
            return
        }

        val bloodfiend = findBloodfiend() ?: return
        if (!hasTwinClaws(bloodfiend)) return
        if (!spawnedByPlayer(bloodfiend)) return

        if (!inDelay) {
            delay = delayTicks
            inDelay = true
            return
        }

        if (delay-- > 0) return

        queue.enqueue(UseItemSequence("Holy Ice"))

        inDelay = false
        cooldown = 60
    }

    /**
     * 查找Bloodfiend
     */
    private fun findBloodfiend(): Player? {

        val player = mc.player ?: return null

        return mc.level!!.players().firstOrNull {
                it != player && it.name.string == "Bloodfiend " && it.distanceTo(player) < 15f
            }
    }

    /**
     * 是否存在Twinclaws
     */
    private fun hasTwinClaws(target: Player): Boolean {
        return getNearbyArmorStands(target).any {
                it.customName?.string?.contains("TWINCLAWS") ?: false
            }
    }

    /**
     * 是否是自己召唤
     */
    private fun spawnedByPlayer(target: Player): Boolean {
        val player = mc.player ?: return false
        return getNearbyArmorStands(target).any {
                val name = it.customName?.string ?: return@any false
                name.contains("Spawned by") && name.contains(player.name.string)
            }
    }

    /**
     * 获取附近ArmorStand
     */
    private fun getNearbyArmorStands(player: Player): List<ArmorStand> {

        val box = player.boundingBox.inflate(
            3.0,
            6.0,
            3.0
        )

        return mc.level!!.getEntitiesOfClass(ArmorStand::class.java, box)
    }
}

class HealingMelonController(private val queue: TickActionQueue) {
    private var cooldown = 0
    private var lastHealth = 20f

    fun reset() {
        lastHealth = 20f
    }

    fun tick(enabled: Boolean, threshold: Float) {
        if (!enabled) return

        if (cooldown > 0) {
            cooldown--
            return
        }

        val player = mc.player ?: return
        val health = player.health
        val crossed = threshold in health..<lastHealth

        if (crossed && health < player.maxHealth) {
            queue.enqueue(UseItemSequence("Healing Melon"))
        }

        lastHealth = health
        cooldown = 20
    }
}

class SubtitleController(private val queue: TickActionQueue) {

    private var subtitle = ""
    private var cooldown = 0

    fun reset() {
        subtitle = ""
        cooldown = 0
    }

    fun onPacket(packet: Any) {
        if (packet !is ClientboundSetSubtitleTextPacket) return
        if (cooldown > 0) return
        subtitle = packet.text.string
    }

    fun tick(jump: Boolean, sneak: Boolean, click: Boolean) {

        if (cooldown > 0) {
            cooldown--
            return
        }

        when {
            subtitle.contains("SNEAK", true) && sneak -> {
                queue.enMuti(KeyHoldTask.sneak(2))
                consume()
            }

            subtitle.contains("JUMP", true) && jump -> {
                queue.enMuti(KeyHoldTask.jump(2))
                consume()
            }

            subtitle.contains("CLICK UP", true) && click -> {
                queue.enMuti(RotationSequence.up())
                consume()
            }

            subtitle.contains("CLICK DOWN", true) && click -> {
                queue.enMuti(RotationSequence.down())
                consume()
            }
        }
    }

    private fun consume() {
        subtitle = ""
        cooldown = 40
    }
}