package Tester01

import java.io.BufferedReader
import java.io.FileReader
import java.util.*
import kotlin.collections.HashMap

class CoinHistory(private val name: String) {
    private val history = TreeMap<String, Candle>()

    fun getHistory():TreeMap<String, Candle>{
        return history
    }

    fun getName():String{
        return name
    }

    fun loadFromFile(fileName: String) {
        val input = BufferedReader(FileReader(fileName));

        var line = input.readLine();
        while (line != null) {
            val parts = line.split(",")
            history[parts[0]+parts[1]] = Candle(
                parts[2].toFloat(),
                parts[3].toFloat(),
                parts[4].toFloat(),
                parts[5].toFloat(),
                parts[6].toFloat())
            line = input.readLine()
        }
        input.close();
    }
}