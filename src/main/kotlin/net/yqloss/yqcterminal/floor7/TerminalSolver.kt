package net.yqloss.yqcterminal.floor7

import com.odtheking.mixin.accessors.AbstractContainerScreenAccessor
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.EventPriority
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color.Companion.darker
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.skyblock.dungeon.terminals.TerminalTypes
import com.odtheking.odin.utils.skyblock.dungeon.terminals.TerminalUtils
import com.odtheking.odin.utils.ui.rendering.NVGPIPRenderer
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.yqloss.yqcterminal.betterterminal.BetterTerminal
import net.yqloss.yqcterminal.betterterminal.SlotType
import net.yqloss.yqcterminal.betterterminal.SlotType.*
import net.yqloss.yqcterminal.betterterminal.Terminal

object TerminalSolver : Module(
    name = "[YQ] Terminal Solver",
    description = "Renders solution for terminals in floor 7. QTERM"
) {
    // val renderType by SelectorSetting("Mode", "Normal", arrayListOf("Normal", "Custom GUI"), desc = "How the terminal solver should render.")
    // val customTermSize by NumberSetting("Term Size", 2f, 1f, 3f, 0.1f, desc = "The size of the custom terminal GUI.").withDependency { renderType == 1 }
    // private val normalTermSize by NumberSetting("Normal Term Size", 3, 1, 5, 1, desc = "The GUI scale increase for normal terminal GUI.").withDependency { renderType != 1 }
    // val roundness by NumberSetting("Roundness", 9f, 0f, 15f, 1f, desc = "The roundness of the custom terminal gui.").withDependency { renderType == 1 }
  val renderType=0
    // val gap by NumberSetting("Gap", 5f, 0f, 15f, 1f, desc = "The gap between the slots in the custom terminal gui.").withDependency { renderType == 1 }

    private val solverSettings by DropdownSetting("Solver Functionality")
    private val cancelToolTip by BooleanSetting("Stop Tooltips", true, desc = "Stops rendering tooltips in terminals.").withDependency { renderType == 0 && solverSettings }
    private val cancelMelodySolver by BooleanSetting("Stop Melody Solver", false, desc = "Stops rendering the melody solver.").withDependency { solverSettings }
    val debug by BooleanSetting("Debug", false, desc = "Shows debug terminals.").withDependency { solverSettings }

    private val showColors by DropdownSetting("Color Settings")
    val backgroundColor by ColorSetting("Background", Colors.gray26, true, desc = "Background color of the terminal solver.").withDependency { showColors }

    val panesColor by ColorSetting("Panes", Colors.MINECRAFT_GREEN, true, desc = "Color of the panes terminal solver.").withDependency { showColors }

    val rubixColor1 by ColorSetting("Rubix 1", Colors.MINECRAFT_GREEN, true, desc = "Color of the rubix terminal solver for 1 click.").withDependency { showColors }
    val rubixColor2 by ColorSetting("Rubix 2", Colors.MINECRAFT_GREEN.darker(), true, desc = "Color of the rubix terminal solver for 2 click.").withDependency { showColors }
    val oppositeRubixColor1 by ColorSetting("Rubix -1", Colors.MINECRAFT_DARK_RED, true, desc = "Color of the rubix terminal solver for -1 click.").withDependency { showColors }
    val oppositeRubixColor2 by ColorSetting("Rubix -2", Colors.MINECRAFT_DARK_RED.darker(), true, desc = "Color of the rubix terminal solver for -2 click.").withDependency { showColors }

    val orderColor by ColorSetting("Order 1", Colors.MINECRAFT_GREEN, true, desc = "Color of the order terminal solver for 1st item.").withDependency { showColors }
    val orderColor2 by ColorSetting("Order 2", Colors.MINECRAFT_GREEN.darker(), true, desc = "Color of the order terminal solver for 2nd item.").withDependency { showColors }
    val orderColor3 by ColorSetting("Order 3", Colors.MINECRAFT_GREEN.darker().darker(), true, desc = "Color of the order terminal solver for 3rd item.").withDependency { showColors }

    val startsWithColor by ColorSetting("Starts With", Colors.MINECRAFT_GREEN, true, desc = "Color of the starts with terminal solver.").withDependency { showColors }

    val selectColor by ColorSetting("Select", Colors.MINECRAFT_GREEN, true, desc = "Color of the select terminal solver.").withDependency { showColors }

    val melodyColumColor by ColorSetting("Melody Column", Colors.MINECRAFT_DARK_PURPLE, true, desc = "Color of the colum indicator for melody.").withDependency { showColors && !cancelMelodySolver }
    val melodyPointerColor by ColorSetting("Melody Pointer", Colors.MINECRAFT_GREEN, true, desc = "Color of the location for pressing for melody.").withDependency { showColors && !cancelMelodySolver }
    val melodyBackgroundColor by ColorSetting("Melody Background", Colors.gray38, true, desc = "Color of the background slot in melody.").withDependency { showColors && !cancelMelodySolver }

    // @JvmStatic val termSize get() = if (enabled && renderType == 0 && TerminalUtils.currentTerm != null) normalTermSize else 1
    private val renderMelody get() = !(cancelMelodySolver && TerminalUtils.currentTerm?.type == TerminalTypes.MELODY)

    init {
        on<GuiEvent.SlotClick> (EventPriority.HIGH) {
          BetterTerminal.parsedState ?: return@on
          cancel()
          if (slotId == 0) {
            BetterTerminal.forceNonQueue()
          }else if (slotId == 8) {
            BetterTerminal.reloadTerminal()
          }else
          BetterTerminal.onClick(slotId, button)

//            if (
//                System.currentTimeMillis() - term.timeOpened < firstClickProt ||
//                (blockIncorrectClicks && !term.canClick(slotId, button))
//            ) return@on cancel()
//
//            if (middleClickGUI) {
//                term.click(slotId, if (button == 0) GLFW.GLFW_MOUSE_BUTTON_3 else button, hideClicked && !term.isClicked)
//                return@on cancel()
//            }
//
//            if (hideClicked && !term.isClicked) term.simulateClick(slotId, button)
        }

      on<TickEvent.End> {
        BetterTerminal.update()
      }

      var renderSlots: List<Terminal.SlotRenderInfo>? = null

        on<GuiEvent.Draw> {

          BetterTerminal.data ?: return@on
            if (!renderMelody) return@on
          BetterTerminal.data?.run {
            renderSlots = terminal.draw(state)
          }
            if (debug) TerminalUtils.currentTerm?.let { term ->
                val menu = (mc.screen as? AbstractContainerScreen<*>)?.menu ?: return@let
                val debugInfo = listOf(
                    "§7Type: §f${term.type.name}",
                    "§7Window Name: §f${mc.screen?.title?.string}",
                    "§7Container ID: §f${menu.containerId}",
                    "§7Time Open: §f${System.currentTimeMillis() - term.timeOpened}ms",
                    "§7Is Clicked: §f${term.isClicked}",
                    "§7Window Count: §f${term.windowCount}",
                    "§7Solution: §f${term.solution.joinToString(", ")}",
                )

                debugInfo.forEachIndexed { index, line ->
                    guiGraphics.drawWordWrap(mc.font, Component.literal(line), 5, 20 + (index * 10), 300, Colors.WHITE.rgba)
                }

                menu.items?.forEachIndexed { index, stack ->
                    guiGraphics.renderItem(stack, 5 + (index % 9) * 18, 250 + (index / 9) * 18)
                    guiGraphics.renderItemDecorations(mc.font, stack, 5 + (index % 9) * 18, 250 + (index / 9) * 18)
                }
            }
            if (renderType == 1) cancel()
            else {
                val screen = (screen as? AbstractContainerScreen<*>) as? AbstractContainerScreenAccessor ?: return@on
                guiGraphics.fill(screen.x + 7, screen.y + 16, screen.x + screen.width - 7, screen.y + screen.height - 96, backgroundColor.rgba)
            }
        }

        on<GuiEvent.DrawSlot> {
          val data = BetterTerminal.data ?: return@on
            if (!renderMelody) return@on

          val renderSlots = renderSlots ?: return@on
          cancel()
          var slotInfo = renderSlots.getOrNull(slot.index) ?: return@on
          if (slot.index == 0) {
            slotInfo = Terminal.SlotRenderInfo(
              if (data.enableQueue) SlotType.EMPTY else SlotType.NQ,
              "NQ"
            )
          }

          if (slot.index == 8) {
            slotInfo = Terminal.SlotRenderInfo(
              SlotType.EMPTY,
              if (data.isQueueEmpty) "RS" else "${data.queueSize}"
            )
          }

          val color = when (slotInfo.type) {
            // Order terminal colors
            ORDER_1 -> orderColor.rgba
            ORDER_2 -> orderColor2.rgba
            ORDER_3 -> orderColor3.rgba

            // Panes terminal - highlight on
            PANES_OFF -> panesColor.rgba
            // Panes off - unknown mapping, leave TODO

            // Starts-with / start correct
            START_CORRECT -> startsWithColor.rgba
            // Color-correct terminal mapping unknown, leave TODO
            COLOR_CORRECT -> selectColor.rgba

            // Rubix colors (right = positive, left = negative)
            RUBIX_LEFT_2 -> rubixColor2.rgba
            RUBIX_LEFT_1 -> rubixColor1.rgba
            RUBIX_RIGHT_1 -> oppositeRubixColor1.rgba
            RUBIX_RIGHT_2 -> oppositeRubixColor2.rgba

            // ALIGN-related colors not represented by settings yet - leave TODO
            ALIGN_TARGET -> melodyColumColor.rgba
            ALIGN_ACTIVE_CURRENT -> melodyPointerColor.rgba
            ALIGN_INACTIVE_BUTTON -> melodyBackgroundColor.rgba
            ALIGN_ACTIVE_OTHER -> melodyBackgroundColor.rgba
            ALIGN_ACTIVE_BUTTON -> melodyPointerColor.rgba
            NQ -> Colors.MINECRAFT_RED.rgba
             else -> null
           }

          color?.let { guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color) }
          slotInfo.text?.let {
            guiGraphics.drawCenteredString(screen.font, it, slot.x + 8, slot.y + 4, Colors.WHITE.rgba)
          }


//            val slotIndex = slot.index
//            val inventorySize = (screen as? AbstractContainerScreen<*>)?.menu?.slots?.size ?: return@on
//
//            if (slotIndex <= inventorySize - 37) cancel()
//            if (slotIndex !in term.solution) return@on
//
//            when (term.type) {
//                TerminalTypes.PANES -> guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, panesColor.rgba)
//
//                TerminalTypes.STARTS_WITH ->
//                    guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, startsWithColor.rgba)
//
//                TerminalTypes.SELECT ->
//                    guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, selectColor.rgba)
//
//                TerminalTypes.NUMBERS -> {
//                    val index = term.solution.indexOf(slotIndex)
//                    if (index < 3) {
//                        val color = when (index) {
//                            0 -> orderColor
//                            1 -> orderColor2
//                            else -> orderColor3
//                        }.rgba
//                        guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color)
//                        cancel()
//                    }
//                    val amount = slot.item?.count?.toString() ?: ""
//                    if (showNumbers) guiGraphics.drawCenteredString(screen.font, amount, slot.x + 8, slot.y + 4, Colors.WHITE.rgba)
//                }
//
//                TerminalTypes.RUBIX -> {
//                    val needed = term.solution.count { it == slotIndex }
//                    val text = if (needed < 3) needed else (needed - 5)
//                    if (text != 0) {
//                        val color = when (text) {
//                            2 -> rubixColor2
//                            1 -> rubixColor1
//                            -2 -> oppositeRubixColor2
//                            else -> oppositeRubixColor1
//                        }
//
//                        guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color.rgba)
//                        guiGraphics.drawCenteredString(screen.font, text.toString(), slot.x + 8, slot.y + 4, Colors.WHITE.rgba)
//                    }
//                }
//
//                TerminalTypes.MELODY -> {
//                    guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, when {
//                        (slotIndex / 9).equalsOneOf(0, 5) -> melodyColumColor
//                        (slotIndex % 9).equalsOneOf(1, 2, 3, 4, 5) -> melodyPointerColor
//                        else -> melodyPointerColor
//                    }.rgba)
//                }
//            }
        }

        on<GuiEvent.DrawTooltip> {
            if (cancelToolTip && TerminalUtils.currentTerm != null) cancel()
        }

        on<GuiEvent.DrawBackground> {
            if (TerminalUtils.currentTerm == null || renderType != 1 || !renderMelody) return@on

            NVGPIPRenderer.draw(guiGraphics, 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight()) {
                TerminalUtils.currentTerm?.type?.getGUI()?.render()
            }

            cancel()
        }

//        on<GuiEvent.KeyPress> {
//            if (TerminalUtils.currentTerm != null && renderType == 1 && renderMelody && mc.options.keyDrop.matches(input)) {
//                TerminalUtils.currentTerm?.type?.getGUI()?.mouseClicked(screen, if (input.hasControlDown()) GLFW.GLFW_MOUSE_BUTTON_2 else GLFW.GLFW_MOUSE_BUTTON_3)
//                cancel()
//            }
//        }
//
//        on<GuiEvent.MouseClick> (EventPriority.HIGH) {
//            if (TerminalUtils.currentTerm != null && renderType == 1 && renderMelody) {
//                TerminalUtils.currentTerm?.type?.getGUI()?.mouseClicked(screen, if (click.button() == 0) GLFW.GLFW_MOUSE_BUTTON_3 else click.button())
//                cancel()
//            }
//        }
//
//        on<TerminalEvent.Open> {
//            devMessage("§aNew terminal: §6${terminal.type.name}")
//            if (renderType == 0) mc.execute { mc.resizeDisplay() }
//        }
//
//        on<TerminalEvent.Close> {
//            devMessage("§cLeft terminal: §6${terminal.type.name}")
//            if (renderType == 0) mc.execute { mc.resizeDisplay() }
//        }
    }
}
