package com.example.chemsandbox

enum class Element(
    val color: Int,
    val density: Int,
    val state: State,
    val flammable: Boolean = false,
    val baseLife: Int = 0,
    val conductivity: Float = 0.06f,
    val ignitePoint: Float = Float.POSITIVE_INFINITY
) {
    EMPTY(0xFF16161e.toInt(), 0, State.GAS, conductivity = 0.01f),
    SAND (0xFFe0c068.toInt(), 5, State.POWDER, conductivity = 0.05f),
    WATER(0xFF3a7bd5.toInt(), 3, State.LIQUID, conductivity = 0.15f),
    ICE  (0xFFa8d8ff.toInt(), 4, State.SOLID,  conductivity = 0.20f),
    STONE(0xFF7a7a7a.toInt(), 10, State.SOLID, conductivity = 0.08f),
    METAL(0xFFb8c0c8.toInt(), 12, State.SOLID, conductivity = 0.90f),
    MOLTEN_METAL(0xFFffae3a.toInt(), 11, State.LIQUID, conductivity = 0.80f),
    WOOD (0xFF8b5a2b.toInt(), 9, State.SOLID, flammable = true, ignitePoint = 300f, conductivity = 0.03f),
    OIL  (0xFF5a4015.toInt(), 2, State.LIQUID, flammable = true, ignitePoint = 160f, conductivity = 0.05f),
    PLANT(0xFF3aaa3a.toInt(), 8, State.SOLID, flammable = true, ignitePoint = 200f, conductivity = 0.03f),
    FIRE (0xFFff6b1a.toInt(), 1, State.GAS, baseLife = 60, conductivity = 0.60f),
    LAVA (0xFFff4500.toInt(), 6, State.LIQUID, conductivity = 0.40f),
    ACID (0xFF7cfc00.toInt(), 3, State.LIQUID, conductivity = 0.10f),
    SMOKE(0xFF606060.toInt(), 0, State.GAS, baseLife = 120, conductivity = 0.05f),
    STEAM(0xFFbcd8e0.toInt(), 0, State.GAS, baseLife = 200, conductivity = 0.10f);

    enum class State { SOLID, POWDER, LIQUID, GAS }
}
