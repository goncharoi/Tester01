package Tester01

import kotlin.jvm.JvmStatic

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val dataHolder = DataHolder()
        println("HISTORY LOADING...")
        dataHolder.loadHistoryFromFolder()
        println("\n\nTRADES LOADING...")
        dataHolder.loadTrades()
        println("\n\nTRADES PROCESSING...")
        dataHolder.processTrades()
        println("\n\nPUMPS OUTPUT...")
        dataHolder.outputPumps()
    }
}