package net.yqloss.yqcterminal.betterterminal

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks

interface Terminal {
    val enableQueue: Boolean
    val enableRightClick: Boolean
    val lines: Int
    val beginLine: Int
    val chestLines: Int
    val title: String

    fun parse(items: List<ItemStack?>): List<Int>?

    fun draw(state: List<Int>): List<SlotRenderInfo>

    data class Prediction(
      val state: List<Int>,
      val clickType: ClickType,
      val button: Int,
    )

    fun predict(
        state: List<Int>,
        slotID: Int,
        button: Int,
    ): Prediction

    data class SlotRenderInfo(
      val type: SlotType,
      val text: String?,
    )

    fun MutableList<SlotRenderInfo>.add(type: SlotType) {
        add(SlotRenderInfo(type, null))
    }

    fun MutableList<SlotRenderInfo>.add(
      type: SlotType,
      text: String,
    ) {
        add(SlotRenderInfo(type, text))
    }
}

interface TerminalFactory<T : Terminal> {
    fun createIfMatch(title: String): T?
}

val glassPanes by lazy {
  listOf(
    Blocks.WHITE_STAINED_GLASS_PANE.asItem(),
    Blocks.ORANGE_STAINED_GLASS_PANE.asItem(),
    Blocks.MAGENTA_STAINED_GLASS_PANE.asItem(),
    Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.asItem(),
    Blocks.YELLOW_STAINED_GLASS_PANE.asItem(),
    Blocks.LIME_STAINED_GLASS_PANE.asItem(),
    Blocks.PINK_STAINED_GLASS_PANE.asItem(),
    Blocks.GRAY_STAINED_GLASS_PANE.asItem(),
    Blocks.LIGHT_GRAY_STAINED_GLASS_PANE.asItem(),
    Blocks.CYAN_STAINED_GLASS_PANE.asItem(),
    Blocks.PURPLE_STAINED_GLASS_PANE.asItem(),
    Blocks.BLUE_STAINED_GLASS_PANE.asItem(),
    Blocks.BROWN_STAINED_GLASS_PANE.asItem(),
    Blocks.GREEN_STAINED_GLASS_PANE.asItem(),
    Blocks.RED_STAINED_GLASS_PANE.asItem(),
    Blocks.BLACK_STAINED_GLASS_PANE.asItem(),
  )
}

val ItemStack.metadata: Int get() {
  if (item in glassPanes) return glassPanes.indexOf(item)
  return 0
}

inline fun Terminal.mapSlots(
  items: List<ItemStack?>,
  range: List<Int>,
  glassPane: Boolean,
  function: (ItemStack) -> Int,
): List<Int>? {
    return range
        .mapNotNull { if (it in items.indices) items[it] else null }
        .filter { it.item != Items.AIR && ( !glassPane || it.item in glassPanes) }
        .takeIf { it.size == range.size }
        ?.map(function)
}

inline fun Terminal.mapSlotsWithSlotId(
  items: List<ItemStack?>,
  range: List<Int>,
  function: (Pair<Int, ItemStack>) -> Int,
): List<Int>? {
  return range
    .mapNotNull { if (it in items.indices) items[it] else null }
    .mapIndexed { index, stack -> range[index] to stack }
    .filter { it.second.item != Items.AIR }
    .takeIf { it.size == range.size }
    ?.map(function)
}

fun rectSlots(
    x1: Int,
    y1: Int,
    x2: Int,
    y2: Int,
) = buildList {
    (y1..y2).forEach { y ->
        (x1..x2).forEach { x ->
            add(x + y * 9)
        }
    }
}

