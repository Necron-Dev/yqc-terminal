package net.yqloss.yqcterminal

import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.features.ModuleManager
import net.yqloss.yqcterminal.floor7.TerminalSolver

fun init() {
  ModuleManager.registerModules(ModuleConfig("yqc-terminal.json"), TerminalSolver)
}
