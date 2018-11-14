import java.util.*

data class Artifact(val value: Int, val weight: Int, val valueDensity: Float = value / weight.toFloat())

fun dynamicProgramming(artifacts: List<Artifact>, k: Int): Pair<Int, List<Char>> {
    val table = mutableListOf(List(k + 1) { 0 })
    for (a in artifacts) {
        val prev = table.last()
        val col = (0..k).map { if (it - a.weight >= 0 && a.value + prev[it - a.weight] > prev[it]) a.value + prev[it - a.weight] else prev[it] }
        table.add(col)
    }
    var i = k
    var sum = 0
    val taken = (artifacts.size downTo 1)
        .map { if (table[it][i] == table[it-1][i]) '0' else { val a = artifacts[it-1]; i -= a.weight; sum += a.value; '1' } }
        .reversed()
    return sum to taken
}

fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val n = input.nextInt()
    val k = input.nextInt()
    val artifacts = (0 until n).map { Artifact(input.nextInt(), input.nextInt()) }
    if ((n.toLong() * k) <= 300_000_000L) {
        val (value, taken) = dynamicProgramming(artifacts, k)
        println("$value 1")
        println(taken.joinToString(" "))
    } else {
        println("0 0")
        println(artifacts.map { "0" }.joinToString(" "))
    }
}
