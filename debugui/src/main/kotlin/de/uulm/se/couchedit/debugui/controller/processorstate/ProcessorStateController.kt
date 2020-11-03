package de.uulm.se.couchedit.debugui.controller.processorstate

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.processing.common.modbus.ModificationBusManager
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.controller.ProcessorModificationPort
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import tornadofx.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Controller providing functionality to get available [ModelRepository]s from the current [ModificationBusManager] of
 * the system.
 */
class ProcessorStateController : Controller() {
    private val modificationBusManager: ModificationBusManager by di()

    internal val availableProcessors = FXCollections.observableArrayList(
            modificationBusManager.debugPorts.filterIsInstance(ProcessorModificationPort::class.java).map { it.debugProcessor }.toMutableList()
    )

    internal val currentProcessor = SimpleObjectProperty<Processor?>(null)

    internal val currentModelRepository = currentProcessor.objectBinding {
        it?.let(this@ProcessorStateController::getModelRepositoryOfProcessor)
    }

    fun <T: Element>getElementState(ref: ElementReference<T>): T? {
        return this.currentModelRepository.value?.get(ref)
    }

    private fun getModelRepositoryOfProcessor(processor: Processor): ModelRepository {
        val reflection = processor::class

        val repositories = reflection.memberProperties
                .filter { it.returnType.isSubtypeOf(ModelRepository::class.createType()) }
                .mapNotNull {
                    (it as? KProperty1<Any, *>)?.run {
                        isAccessible = true
                        return@run get(processor) as? ModelRepository
                    }
                }

        if (repositories.size != 1) {
            throw IllegalStateException("The processor $processor has ${repositories.size} repositories, expected 1")
        }

        return repositories.first()
    }
}
