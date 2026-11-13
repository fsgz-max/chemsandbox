package com.example.chemsandbox

import android.graphics.Color
import kotlin.random.Random

class Simulation(val width: Int, val height: Int) {

    private val size = width * height
    private val grid  = IntArray(size) { Element.EMPTY.ordinal }
    private val life  = IntArray(size)
    private val moved = BooleanArray(size)

    var gravity: Float = 1.0f
    var reactionRate: Float = 1.0f
    var paused = false

    val temp = FloatArray(size) { 20f }
    private val newTemp = FloatArray(size)
    var ambient: Float = 20f
    var showTemperature = false

    private val conduct = FloatArray(Element.values().size)
    private val ignitePt = FloatArray(Element.values().size)

    private val rnd = Random.Default

    init {
        Element.values().forEach { e ->
            conduct[e.ordinal] = e.conductivity
            ignitePt[e.ordinal] = e.ignitePoint
        }
    }

    private fun idx(x: Int, y: Int) = y * width + x
    private fun inBounds(x: Int, y: Int) = x in 0 until width && y in 0 until height

    private fun get(x: Int, y: Int): Element =
        if (inBounds(x, y)) Element.values()[grid[idx(x, y)]] else Element.STONE

    private fun set(x: Int, y: Int, e: Element) {
        if (!inBounds(x, y)) return
        val i = idx(x, y)
        grid[i] = e.ordinal
        life[i] = e.baseLife
        moved[i] = true
    }

    private fun setByIndex(i: Int, e: Element) {
        grid[i] = e.ordinal
        life[i] = e.baseLife
    }

    fun clear() {
        grid.fill(Element.EMPTY.ordinal)
        life.fill(0)
        temp.fill(ambient)
    }

    fun paint(cx: Int, cy: Int, radius: Int, e: Element) {
        for (dy in -radius..radius) for (dx in -radius..radius) {
            if (dx * dx + dy * dy > radius * radius) continue
            val x = cx + dx; val y = cy + dy
            if (!inBounds(x, y)) continue
            if (e == Element.EMPTY || get(x, y) == Element.EMPTY || rnd.nextFloat() < 0.6f)
                set(x, y, e)
        }
    }

    fun step() {
        if (paused) return
        moved.fill(false)
        for (y in height - 1 downTo 0) {
            val ltr = rnd.nextBoolean()
            for (i in 0 until width) {
                val x = if (ltr) i else width - 1 - i
                val index = idx(x, y)
                if (moved[index]) continue
                when (Element.values()[grid[index]].state) {
                    Element.State.POWDER -> updatePowder(x, y)
                    Element.State.LIQUID -> updateLiquid(x, y)
                    Element.State.GAS    -> updateGas(x, y)
                    Element.State.SOLID  -> {}
                }
            }
        }
        updateTemperature()
    }

    private fun swap(x1: Int, y1: Int, x2: Int, y2: Int) {
        val a = idx(x1, y1); val b = idx(x2, y2)
        val g = grid[a]; grid[a] = grid[b]; grid[b] = g
        val l = life[a]; life[a] = life[b]; life[b] = l
        moved[a] = true; moved[b] = true
    }

    private fun canDisplace(mover: Element, target: Element): Boolean {
        if (target == Element.EMPTY) return true
        return (mover.state == Element.State.LIQUID || mover.state == Element.State.POWDER) &&
                target.state == Element.State.LIQUID && mover.density > target.density
    }

    private fun tryMove(x: Int, y: Int, nx: Int, ny: Int, e: Element): Boolean {
        if (!inBounds(nx, ny)) return false
        val target = get(nx, ny)
        if (react(x, y, nx, ny, e, target)) return true
        if (canDisplace(e, target)) { swap(x, y, nx, ny); return true }
        return false
    }

    private fun react(x: Int, y: Int, nx: Int, ny: Int, e: Element, t: Element): Boolean {
        val r = rnd.nextFloat(); val rate = reactionRate
        when {
            (e == Element.FIRE && t == Element.WATER) || (e == Element.WATER && t == Element.FIRE) -> {
                set(x, y, Element.STEAM); set(nx, ny, Element.STEAM); return true
            }
            e == Element.LAVA && t == Element.WATER -> { set(x, y, Element.STONE); set(nx, ny, Element.STEAM); return true }
            e == Element.WATER && t == Element.LAVA -> { set(x, y, Element.STEAM); set(nx, ny, Element.STONE); return true }
            e == Element.FIRE && t.flammable && r < 0.2f * rate -> set(nx, ny, Element.FIRE)
            e == Element.LAVA && t.flammable && r < 0.3f * rate -> set(nx, ny, Element.FIRE)
            e == Element.ACID && t != Element.EMPTY && t != Element.STONE && t != Element.ACID && r < 0.15f * rate -> {
                set(x, y, if (rnd.nextFloat() < 0.5f) Element.ACID else Element.EMPTY)
                set(nx, ny, Element.EMPTY); return true
            }
            e == Element.PLANT && t == Element.WATER && r < 0.05f * rate -> set(nx, ny, Element.PLANT)
            else -> {}
        }
        return false
    }

    private fun updatePowder(x: Int, y: Int) {
        val e = get(x, y)
        if (rnd.nextFloat() > gravity) return
        if (tryMove(x, y, x, y + 1, e)) return
        val d = if (rnd.nextBoolean()) 1 else -1
        if (tryMove(x, y, x + d, y + 1, e)) return
        tryMove(x, y, x - d, y + 1, e)
    }

    private fun updateLiquid(x: Int, y: Int) {
        val e = get(x, y)
        if (rnd.nextFloat() < gravity) {
            if (tryMove(x, y, x, y + 1, e)) return
            val d = if (rnd.nextBoolean()) 1 else -1
            if (tryMove(x, y, x + d, y + 1, e)) return
            if (tryMove(x, y, x - d, y + 1, e)) return
        }
        val d = if (rnd.nextBoolean()) 1 else -1
        if (tryMove(x, y, x + d, y, e)) return
        tryMove(x, y, x - d, y, e)
    }

    private fun updateGas(x: Int, y: Int) {
        val e = get(x, y); val i = idx(x, y)
        if (life[i] > 0) {
            life[i]--
            if (life[i] == 0) {
                when (e) {
                    Element.FIRE  -> set(x, y, if (rnd.nextFloat() < 0.3f) Element.SMOKE else Element.EMPTY)
                    Element.STEAM -> set(x, y, if (rnd.nextFloat() < 0.3f) Element.WATER else Element.EMPTY)
                    else          -> set(x, y, Element.EMPTY)
                }
                return
            }
        }
        if (e == Element.FIRE) igniteNeighbors(x, y)
        if (rnd.nextFloat() < 0.7f) {
            val d = if (rnd.nextBoolean()) 1 else -1
            if (tryMove(x, y, x, y - 1, e)) return
            if (tryMove(x, y, x + d, y - 1, e)) return
            if (tryMove(x, y, x - d, y - 1, e)) return
        }
        if (rnd.nextFloat() < 0.3f) tryMove(x, y, x + (if (rnd.nextBoolean()) 1 else -1), y, e)
    }

    private fun igniteNeighbors(x: Int, y: Int) {
        for (dy in -1..1) for (dx in -1..1) {
            if (dx == 0 && dy == 0) continue
            val nx = x + dx; val ny = y + dy
            if (!inBounds(nx, ny)) continue
            val t = get(nx, ny)
            if (t.flammable && rnd.nextFloat() < 0.08f * reactionRate) set(nx, ny, Element.FIRE)
            if (t == Element.WATER && rnd.nextFloat() < 0.5f) { set(x, y, Element.STEAM); return }
        }
    }

    private fun phase(i: Int, e: Element, t: Float) {
        when (e) {
            Element.ICE   -> if (t > 0f) setByIndex(i, Element.WATER)
            Element.WATER -> when { t < 0f -> setByIndex(i, Element.ICE)
                                    t > 100f -> setByIndex(i, Element.STEAM) }
            Element.STEAM -> if (t < 90f) setByIndex(i, Element.WATER)
            Element.STONE -> if (t > 1100f) setByIndex(i, Element.LAVA)
            Element.LAVA  -> if (t < 700f) setByIndex(i, Element.STONE)
            Element.SAND  -> if (t > 1700f) setByIndex(i, Element.LAVA)
            Element.METAL -> if (t > 660f) setByIndex(i, Element.MOLTEN_METAL)
            Element.MOLTEN_METAL -> if (t < 660f) setByIndex(i, Element.METAL)
            else -> {}
        }
        if (e.flammable && t > ignitePt[e.ordinal]) setByIndex(i, Element.FIRE)
    }

    private fun updateTemperature() {
        val vals = Element.values()
        for (i in 0 until size) {
            when (vals[grid[i]]) {
                Element.FIRE -> temp[i] = 900f
                Element.LAVA -> if (temp[i] < 900f) temp[i] = 900f
                Element.MOLTEN_METAL -> if (temp[i] < 700f) temp[i] = 700f
                else -> temp[i] += (ambient - temp[i]) * 0.004f
            }
        }
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                val i = row + x
                val ci = conduct[grid[i]]
                var acc = 0f
                if (x > 0)          { val n = i - 1;     acc += minOf(ci, conduct[grid[n]]) * (temp[n] - temp[i]) }
                if (x < width - 1)  { val n = i + 1;     acc += minOf(ci, conduct[grid[n]]) * (temp[n] - temp[i]) }
                if (y > 0)          { val n = i - width; acc += minOf(ci, conduct[grid[n]]) * (temp[n] - temp[i]) }
                if (y < height - 1) { val n = i + width; acc += minOf(ci, conduct[grid[n]]) * (temp[n] - temp[i]) }
                newTemp[i] = temp[i] + acc
            }
        }
        System.arraycopy(newTemp, 0, temp, 0, size)
        for (i in 0 until size) phase(i, vals[grid[i]], temp[i])
    }

    fun render(pixels: IntArray) {
        val vals = Element.values()
        val fire = intArrayOf(0xFFff6b1a.toInt(), 0xFFffb31a.toInt(), 0xFFff3a1a.toInt())
        for (i in 0 until size) {
            val e = vals[grid[i]]
            pixels[i] = when {
                showTemperature -> heatColor(temp[i])
                e == Element.FIRE -> fire[rnd.nextInt(3)]
                else -> e.color
            }
        }
    }

    private fun heatColor(t: Float): Int {
        val k = ((t + 50f) / 1250f).coerceIn(0f, 1f)
        return Color.HSVToColor(floatArrayOf((1f - k) * 240f, 1f, 0.25f + 0.75f * k))
    }
}
