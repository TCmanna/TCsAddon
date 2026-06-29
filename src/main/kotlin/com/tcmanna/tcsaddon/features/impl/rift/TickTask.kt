package com.tcmanna.tcsaddon.features.impl.rift

import com.tcmanna.tcsaddon.mixin.accessors.KeyMappingAccessor
import com.tcmanna.tcsaddon.utils.Animation
import com.tcmanna.tcsaddon.utils.RotationUtils.rotateSmoothly
import com.tcmanna.tcsaddon.utils.Utils.mc
import net.minecraft.client.KeyMapping
import kotlin.reflect.KClass

interface TickTask {
    fun tick(): Boolean
    fun cancel() {}
}

class TickActionQueue {
    private val queue = ArrayDeque<TickTask>()
    private val muti = ArrayList<TickTask>()
    private var current: TickTask? = null

    fun tick() {
        muti.removeIf { it.tick() }

        if (current == null) {
            if (queue.isEmpty()) return
            current = queue.removeFirst()
        }

        val finished = current!!.tick()
        if (finished) current = null
    }

    fun enqueue(task: TickTask) {
        queue.addLast(task)
    }

    fun enMuti(task: TickTask) {
        muti.addLast(task)
    }

    fun enqueueFirst(task: TickTask) {
        val list = ArrayDeque<TickTask>()
        list.add(task)
        list.addAll(queue)
        queue.clear()
        queue.addAll(list)
    }

    fun isIdle(): Boolean {
        return current == null && queue.isEmpty()
    }

    fun hasTask(): Boolean {
        return current != null || queue.isNotEmpty()
    }

    fun <T : TickTask> contains(clazz: KClass<T>): Boolean {
        if (current != null && clazz.isInstance(current)) return true
        return queue.any { clazz.isInstance(it) }
    }

    fun clear() {
        current?.cancel()
        current = null
        queue.forEach { it.cancel() }
        queue.clear()
    }

    fun delay(ticks: Int, block: () -> Unit) {
        enqueue(DelayTask(ticks, block))
    }

    fun execute(block: () -> Unit) {
        enqueue(InstantTask(block))
    }

    fun enqueueAll(vararg tasks: TickTask) {
        tasks.forEach { enqueue(it) }
    }
}

class DelayTask(private var ticks: Int, private val block: () -> Unit) : TickTask {

    override fun tick(): Boolean {
        if (ticks > 0) {
            ticks--
            return false
        }

        block()
        return true
    }
}

class InstantTask(private val block: () -> Unit) : TickTask {
    override fun tick(): Boolean {
        block()
        return true
    }
}

class KeyHoldTask private constructor(private val key: KeyMapping, private var ticks: Int) : TickTask {
    companion object {

        fun jump(ticks: Int = 2): KeyHoldTask {
            return KeyHoldTask(mc.options.keyJump, ticks)
        }

        fun sneak(ticks: Int = 2): KeyHoldTask {
            return KeyHoldTask(mc.options.keyShift, ticks)
        }
    }

    override fun tick(): Boolean {
        key.isDown = true
        ticks--
        if (ticks <= 0) {
            key.isDown = false
            return true
        }
        return false
    }

    override fun cancel() {
        key.isDown = false
    }
}

class RotationSequence private constructor(private val targetPitch: Float) : TickTask {
    companion object {

        fun up(): RotationSequence {
            return RotationSequence(-90f)
        }

        fun down(): RotationSequence {
            return RotationSequence(90f)
        }
    }

    private enum class State {
        ROTATE,
        WAIT,
        CLICK_DOWN,
        CLICK_UP,
        RESTORE,
        DONE
    }

    private var state = State.ROTATE

    private var timer = 0

    private var lastPitch = 0f

    override fun tick(): Boolean {
        val player = mc.player?: return true
        when (state) {

            State.ROTATE -> {
                lastPitch = player.xRot
                if (VampireSlayer.smoothRotate) {
                    player.rotateSmoothly(player.yRot, targetPitch, VampireSlayer.rotateSpeed, Animation.Style.Linear)
                } else {
                    player.xRot = targetPitch
                }
                timer = 2
                state = State.WAIT
            }

            State.WAIT -> {
                if (--timer <= 0) state = State.CLICK_DOWN
            }

            State.CLICK_DOWN -> {
                mc.options.keyAttack.isDown = true
                KeyMapping.click((mc.options.keyAttack as KeyMappingAccessor).boundKey)
                timer = 2
                state = State.CLICK_UP
            }

            State.CLICK_UP -> {
                if (--timer <= 0) {
                    mc.options.keyAttack.isDown = false
                    timer = 6 // 原Java约350ms
                    state = State.RESTORE
                }
            }

            State.RESTORE -> {
                if (--timer <= 0) {
                    if (VampireSlayer.smoothRotate) {
                        player.rotateSmoothly(player.yRot, lastPitch, VampireSlayer.rotateSpeed, Animation.Style.Linear)
                    } else {
                        player.xRot = lastPitch
                    }
                    state = State.DONE
                }
            }

            State.DONE -> return true
        }

        return false
    }

    override fun cancel() {
        mc.player?.xRot = lastPitch
    }
}