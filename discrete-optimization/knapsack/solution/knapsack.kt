import java.util.*
import kotlinx.coroutines.*
import kotlin.math.min
import kotlin.system.measureTimeMillis

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

const val BATCH_SIZE = 5000

fun dynamicProgrammingMemoryEfficientCols(artifacts: List<Artifact>, k: Int): Pair<Int, List<Char>> = runBlocking(Dispatchers.Default) {
    var prev = List(k + 1) { 0 to setOf<Artifact>() }
    for (a in artifacts) {
        val parts = (k / BATCH_SIZE) + 1
        val col = (0..parts).map { async {
            val start = it * BATCH_SIZE
            val end = min(start + BATCH_SIZE, k + 1)
            (start until end).map {
                if (it - a.weight >= 0 && a.value + prev[it - a.weight].first > prev[it].first)
                    a.value + prev[it - a.weight].first to prev[it - a.weight].second.plus(a)
                else
                    prev[it]
            }
        }} .flatMap { it.await() }
        prev = col
    }
    val taken = prev.last().second
    taken.sumBy { it.value } to artifacts.map { if (taken.contains(it)) '1' else '0' }
}

typealias Heuristic = (List<Artifact>, Int) -> Int

data class Node(
    val value: Int, val weightAvailable: Int, val estimatedMax: Int,
    val artifactsTaken: Set<Artifact>, val artifactsLeft: List<Artifact>, val artifactsProcessed: List<Artifact> = listOf()
) {
    fun take(heuristic: Heuristic): Node {
        val a = artifactsLeft.first()
        val artLeft = artifactsLeft.drop(1)
        val weightLeft = weightAvailable - a.weight
        val newValue = value + a.value
        return Node(newValue, weightLeft, heuristic(artLeft, weightLeft) + newValue, artifactsTaken.plus(a), artLeft, artifactsProcessed.plus(a))
    }

    fun drop(heuristic: Heuristic): Node {
        val a = this.artifactsLeft.first()
        val artLeft = this.artifactsLeft.drop(1)
        return this.copy(estimatedMax = heuristic(artLeft, weightAvailable) + value, artifactsLeft = artLeft, artifactsProcessed = artifactsProcessed.plus(a))
    }

    fun moveUp(heuristic: Heuristic): Node {
        val dropped = artifactsProcessed.reversed().takeWhile { !artifactsTaken.contains(it) }
        val processed = artifactsProcessed.dropLast(dropped.size)
        val a = processed.last()
        val newValue = value - a.value
        val newWeightAllowance = weightAvailable + a.weight
        val artLeft = dropped.plus(a).reversed().plus(artifactsLeft)
        val estimated = heuristic(artLeft, newWeightAllowance) + newValue
        return Node(newValue, newWeightAllowance, estimated, artifactsTaken.minus(a), artLeft, processed.dropLast(1))
    }
}

fun pickEverything(artifacts: List<Artifact>, k: Int): Int = artifacts.sumBy { it.value }
fun pickHighestDensity(artifacts: List<Artifact>, k: Int): Int {
    var allowance = k
    return artifacts
        .sortedByDescending { it.valueDensity }
        .takeWhile { if (allowance > 0) { allowance -= it.weight; true } else false }
        .sumBy { it.value }
}

fun branchAndBoundBestFirst(artifacts: List<Artifact>, k: Int, heuristic: Heuristic): Pair<Int, List<Char>> {
    val queue = PriorityQueue<Node>(1024, { a, b -> b.estimatedMax.compareTo(a.estimatedMax)})
    var best = Node(0, 0, 0, setOf(), artifacts)
    queue.add(Node(0, k, heuristic(artifacts, k), setOf(), artifacts))
    while (queue.peek() != null) {
        val n = queue.poll()
        if (n.estimatedMax > best.value && n.artifactsLeft.isNotEmpty()) {
            val drop = n.drop(heuristic)
            if (drop.estimatedMax > best.value) queue.add(drop)
            val take = n.take(heuristic)
            if (take.weightAvailable >= 0 && take.estimatedMax > best.value) queue.add(take)
        } else if (n.value > best.value && n.artifactsLeft.isEmpty()) {
            best = n
        }
    }
    return best.value to artifacts.map { if (best.artifactsTaken.contains(it)) '1' else '0' }
}

fun branchAndBoundDepthFirst(artifacts: List<Artifact>, k: Int, heuristic: Heuristic): Pair<Int, List<Char>> {
    tailrec fun takeAll(node: Node): Node =
        if (node.artifactsLeft.isEmpty())
            node
        else
            takeAll(if (node.artifactsLeft.first().weight <= node.weightAvailable) node.take(heuristic) else node.drop(heuristic))

    var best = takeAll(Node(0, k, 0, setOf(), artifacts))
    var node = best
    while (node.artifactsTaken.isNotEmpty() || node.estimatedMax > best.value) {
        node = node.moveUp(heuristic)
        while (node.estimatedMax <= best.value && node.artifactsTaken.isNotEmpty()) {
            node = node.moveUp(heuristic)
        }
        node = takeAll(node.drop(heuristic))
        if (node.value > best.value) {
            best = node
        }
    }
    return best.value to artifacts.map { if (best.artifactsTaken.contains(it)) '1' else '0' }
}

fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val n = input.nextInt()
    val k = input.nextInt()
    val artifacts = (0 until n).map { Artifact(input.nextInt(), input.nextInt()) }
    val elapsed = measureTimeMillis {
        val (value, taken) = if (n.toLong() * k <= 100_000_000)
            dynamicProgramming(artifacts, k)
        else if (k <= 1_000_000)
            dynamicProgrammingMemoryEfficientCols(artifacts, k)
        else
            0 to artifacts.map { '0' }
        println("$value 1")
        println(taken.joinToString(" "))
    }

    System.err.println("took $elapsed ms")
}
