import domain.Organism
import domain.OrganismState
import domain.World
import domain.placement.Position
import domain.placement.SimpleHolder
import domain.placement.VisionCollider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking<Unit> { // start main coroutine

    val world = World(10, 10, SimpleHolder(), VisionCollider())
        .add(Organism(OrganismState(10), Position(1, 1)))
    while (true) {
        launch {
            world.update()
            world.print()
        }
        delay(1000L)
    }
}

fun World.print() {
    for (i in 0 until width) {
        for (j in 0 until height) {
            print("* ")
        }
        println()
    }
}