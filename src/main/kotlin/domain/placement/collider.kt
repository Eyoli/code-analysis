package domain.placement

import domain.Organism
import kotlin.math.sqrt

interface Collider<T> {
    fun run(objects: List<T>)
}

abstract class PairCollider<T> : Collider<T> {
    override fun run(objects: List<T>) {
        for (i in objects.indices) {
            for (j in i until objects.size) {
                if (objectsAreColliding(objects[i], objects[j])) {
                    onCollision(objects[i], objects[j])
                }
            }
        }
    }

    abstract fun objectsAreColliding(obj1: T, obj2: T): Boolean
    abstract fun onCollision(obj1: T, obj2: T)
}

class VisionCollider : PairCollider<Organism>() {

    override fun objectsAreColliding(obj1: Organism, obj2: Organism): Boolean {
        val d = obj1.position.distanceTo(obj2.position)
        return d <= obj1.state.vision || d <= obj2.state.vision
    }

    override fun onCollision(obj1: Organism, obj2: Organism) {
        if (obj1.state.vision > obj2.state.vision) {
            obj1.uponSeeing(obj2)
        } else {
            obj2.uponSeeing(obj1)
        }
    }
}

interface Holder<T> {
    fun add(obj: T)
    fun remove(obj: T)
    fun groups(): List<List<T>>
    fun update()
}

class SimpleHolder<T> : Holder<T> {
    private val container = mutableListOf<T>()

    override fun add(obj: T) {
        container.add(obj)
    }

    override fun remove(obj: T) {
        container.remove(obj)
    }

    override fun groups(): List<List<T>> {
        return listOf(container)
    }

    override fun update() {}
}

data class Position(val x: Int, val y: Int) {

    fun distanceTo(p: Position): Double {
        val dx = this.x - p.x
        val dy = this.y - p.y
        return sqrt((dx * dx + dy * dy).toDouble())
    }
}