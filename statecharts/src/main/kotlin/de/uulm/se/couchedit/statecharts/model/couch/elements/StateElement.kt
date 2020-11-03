package de.uulm.se.couchedit.statecharts.model.couch.elements

import de.uulm.se.couchedit.model.base.AbstractElement

/**
 * Parent class for all Elements that can be the source or target of state transitions (currently this means states and
 * pseudostates).
 */
abstract class StateElement(override var name: String?) : StateHierarchyElement, AbstractElement()
