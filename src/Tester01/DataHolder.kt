package Tester01

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.nameWithoutExtension
import kotlin.math.max
import kotlin.math.min

const val MIN_PUMP = 0.75f
const val CANDLES_VOLUME_COUNT = 10
const val MIN_VOLUME_FACTOR = 20
const val COINS_COUNT_TO_IGNORE = 2
const val PATH = "D://OsEngine-master"
const val FIRST_SECONDS_TO_ANALYZE = 20
const val TRAILING_STOP = 1
const val TRAILING_LAG = 25

class DataHolder {
    private val coins = ArrayList<CoinHistory>()
    private val trades = ArrayList<Trade>()
    private val pumps = ArrayList<Pump>()

    fun loadHistoryFromFolder() {
        val resourcesPath = Paths.get(PATH, "/txt")
        Files.walk(resourcesPath)
            .filter { item -> Files.isRegularFile(item) }
            .filter { item -> item.toString().endsWith(".txt") }
            .forEach { item -> loadOneCoinHistory(item) }
    }

    private fun loadOneCoinHistory(path: Path) {
        val coinHistory = CoinHistory(path.fileName.nameWithoutExtension)
        coinHistory.loadFromFile(path.toString())
        coins.add(coinHistory)
        print(coins.size.toString() + ", ")
    }

    fun loadTrades() {
        val input = BufferedReader(FileReader("$PATH/trades.txt"));

        var line = input.readLine();
        while (line != null) {
            val parts = line.split(",")
            val dateParts = parts[1].split(".")
            val timeParts = parts[2].split(":")
            trades.add(
                Trade(
                    parts[0],
                    dateParts[2] + dateParts[1] + dateParts[0],
                    (timeParts[0] + timeParts[1] + "00").toLong() - 60000,
                    (timeParts[0] + timeParts[1] + "00").toLong() - 49000,
                    (timeParts[0] + timeParts[1] + "00").toLong() - 50000
                )
            )
            line = input.readLine()
        }
        input.close();
    }

    fun processTrades() {
        trades.forEach {
            println("TRADE " + it.coinName + " on " + it.date)
            processCoins(it)
        }
    }

    private fun processCoins(trade: Trade) {
        val filteredCoins = ArrayList<CoinHistory>()

        val indexEnd = trades.indexOf(trade)
        val indexStart = max(indexEnd - COINS_COUNT_TO_IGNORE, 0)
        coins.forEach { coin ->
            var ignore = false
            trades.forEach {
                if (trades.indexOf(it) in indexStart until indexEnd && it.coinName == coin.getName()) {
                    ignore = true
                }
            }
            if (!ignore) {
                filteredCoins.add(coin)
            }
        }

        filteredCoins.forEach {
            processCoin(it, trade)
            print("Coin " + it.getName() + ", ")
        }
        println()
    }

    private fun processCoin(coin: CoinHistory, trade: Trade) {
        val candlesOnDate = coin.getHistory().filter { item -> item.key.startsWith(trade.date) }
        var start = false
        var index = 0
        candlesOnDate.forEach { (key, value) ->
            val candleTime = key.substring(8).toLong()
            if (candleTime > trade.timeStart && !start) {
                start = true
            }
            if (start) {
                if (candleTime > trade.timeEnd) {
                    return
                }
//                var avgPrevVolume = 0.0f
//                if (index >= CANDLES_VOLUME_COUNT) {
//                    for (i in 1..CANDLES_VOLUME_COUNT) {
//                        avgPrevVolume += candlesOnDate.entries.elementAtOrNull(index - i)?.value?.volume ?: 0.0f
//                    }
//                }
//                avgPrevVolume /= CANDLES_VOLUME_COUNT
                val percentage = (value.exit - value.enter) / value.enter * 100
                if (percentage >= MIN_PUMP
                    && key.substring(12).toInt() <= FIRST_SECONDS_TO_ANALYZE
//                    && value.volume > avgPrevVolume * MIN_VOLUME_FACTOR
                ) {
                    pumps.forEach {
                        if (it.time in trade.timeTrade - 110..trade.timeTrade + 110 &&
                            it.date == trade.date &&
                            candleTime > trade.timeTrade + 110
                        ) {
                            it.success = true
                            return
                        }
                    }
                    val enterPrice = candlesOnDate.entries.elementAtOrNull(index + 1)?.value?.enter ?: 0.0f
                    pumps.add(Pump(coin.getName(),
                        trade.date,
                        candleTime,
                        value,
                        percentage,
                        false,
                        enterPrice,
                        getTrailingProfit(candlesOnDate,index,0,enterPrice),
                        getTrailingProfit(candlesOnDate,index, TRAILING_LAG,enterPrice)))
                }
            }
            index++
        }
    }

    private fun getTrailingProfit(candlesOnDate: Map<String, Candle>, index: Int, lag:Int, enterPrice: Float): Float {
        if (enterPrice == 0.0f) return 0.0f

        var step = lag
        var candleMax = 0.0f
        var candleMin = Float.MAX_VALUE
        var trailingStop = 0.0f
        do {
            step++
            val candle = candlesOnDate.entries.elementAtOrNull(index + step)
            candleMax = max(candle?.value?.max ?: 0.0f, candleMax)
            candleMin = min(candle?.value?.min ?: 0.0f, candleMin)
            trailingStop = candleMax * (100 - TRAILING_STOP) / 100
        } while (trailingStop <= candleMin)
        return (trailingStop - enterPrice) / enterPrice * 100
    }

    fun outputPumps() {
        val output = BufferedWriter(FileWriter("$PATH/pumps.txt"));

        output.write(
            "Дата," +
                    "Время," +
                    "Монета," +
                    "Откр," +
                    "Макс," +
                    "Мин," +
                    "Закр," +
                    "Объем," +
                    "Памп," +
                    "Вход," +
                    "Профит с трейл," +
                    "Профит с трейл +25"
        )
        output.newLine()
        pumps.forEach {
            output.write(
                it.date + "," +
                        it.time.toString().padStart(6, '0') + "," +
                        it.coinName + "," +
                        it.candle.enter + "," +
                        it.candle.max + "," +
                        it.candle.min + "," +
                        it.candle.exit + "," +
                        it.candle.volume + "," +
                        it.percentage + "," +
                        it.enterPrice + "," +
                        it.trailngProfit + "," +
                        it.trailngProfitWithLag
            )
            output.newLine()
        }
        output.close();
    }
}