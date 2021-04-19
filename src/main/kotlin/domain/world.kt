package domain

import domain.placement.Collider
import domain.placement.Holder
import domain.placement.Position

class World(
    val width: Int,
    val height: Int,
    val holder: Holder<Organism>,
    val collider: Collider<Organism>) {

    fun add(organism: Organism): World {
        holder.add(organism)
        return this
    }

    fun update() {
        holder.update()
        holder.groups().forEach(collider::run)
    }
}

class Organism(val state: OrganismState, var position: Position) {

    private val targets: MutableSet<Organism> = mutableSetOf()

    fun uponSeeing(organism: Organism) {
        targets.add(organism)
    }
}

data class OrganismState(val vision: Int)