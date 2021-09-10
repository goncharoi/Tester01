package Tester01

data class Pump(val coinName: String,
                val date: String,
                val time: Long,
                val candle: Candle,
                val percentage:Float,
                var success:Boolean,
                var enterPrice:Float,
                var trailngProfit:Float,
                var trailngProfitWithLag:Float
)