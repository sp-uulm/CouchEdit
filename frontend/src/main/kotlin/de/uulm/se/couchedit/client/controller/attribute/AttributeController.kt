package de.uulm.se.couchedit.client.controller.attribute

import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasAttributeManager
import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasCoordinator
import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasOperationHandler
import de.uulm.se.couchedit.client.viewmodel.attribute.AtomicAttributeContentViewModel
import de.uulm.se.couchedit.client.viewmodel.attribute.AttributeBagContentViewModel
import de.uulm.se.couchedit.client.viewmodel.attribute.AttributeViewModel
import de.uulm.se.couchedit.model.attribute.Attribute
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import de.uulm.se.couchedit.model.attribute.elements.AttributesFor
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.processing.attribute.factory.AggregateAttributeBagFactory
import de.uulm.se.couchedit.util.extensions.ref
import griffon.javafx.collections.ElementObservableList
import griffon.javafx.collections.GriffonFXCollections
import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import tornadofx.*

/**
 * [Controller] for the AttributeView. Given a [GraphicObject] instance, has the following tasks:
 *
 * * Show the element ID and type (including the shape type) in the [elementIdProperty] and [elementTypeProperty],
 *   respectively
 *
 * * Load [Attribute] values from the [CanvasCoordinator] and subscribe for live updates.
 *   The available AttributeBags and Attributes will be stored in the [attributeBagViewModels] property.
 *
 * * Maintain a list of possible [AttributeBag] types to create for this Element in the [possibleAttributeBags]
 *   ObservableList
 */
class AttributeController : Controller() {
    /**
     * Property containing an instance of the [GraphicObject] of which the Attributes should be shown / edited.
     */
    val editedElementProperty: ObjectProperty<GraphicObject<*>?> = SimpleObjectProperty()

    /**
     * Property containing the ID of the currently shown / edited GraphicObject
     */
    val elementIdProperty: ReadOnlyStringProperty
        get() = elementIdPropertyInternal.readOnlyProperty

    /**
     * Property containing the Type of the currently shown / edited GraphicObject as well as its Shape type
     */
    val elementTypeProperty: ReadOnlyStringProperty
        get() = elementTypePropertyInternal.readOnlyProperty

    /**
     * Property maintaining a List of the types of AttributeBag which may currently be inserted for the Element in
     * [editedElementProperty].
     */
    val possibleAttributeBags: ReadOnlyListProperty<Class<out AttributeBag>>
        get() = possibleAttributeBagsInternal.readOnlyProperty

    /**
     * Property maintaining a Map of AttributeViewModels representing the AttributeBags currently assigned to the edited
     * [GraphicObject] (and therefore, also the [AtomicAttributeContentViewModel]s contained in the bag).
     *
     * This is meant for display in a TreeView.
     */
    val attributeBagViewModels: ReadOnlyMapProperty<String, AttributeViewModel>
        get() = attributeBagViewModelsInternal.readOnlyProperty

    private val dirtyBagObservableList = ElementObservableList<ObservableValue<Boolean>>()

    /* ------------------------- INTERNAL R/W PROPERTIES ------------------------ */

    /**
     * Keeps the actual AttributeBag Elements that contain the values represented by the [attributeBagViewModels].
     */
    private val attributeBags = mutableMapOf<String, AttributeBag>()

    private val uncommittedAttributeBags = FXCollections.observableList<String>(mutableListOf())

    private val elementIdPropertyInternal = ReadOnlyStringWrapper("")

    private val elementTypePropertyInternal = ReadOnlyStringWrapper("")

    private val possibleAttributeBagsInternal = ReadOnlyListWrapper<Class<out AttributeBag>>(FXCollections.observableArrayList())

    /**
     * Property containing the [AttributeBag]s currently assigned to the Element given in the [editedElementProperty].
     */
    private val attributeBagViewModelsInternal = ReadOnlyMapWrapper<String, AttributeViewModel>(FXCollections.observableHashMap())

    /**
     * Bag types currently associated with the [editedElementProperty] Element.
     * Used to decide what AttributeBag types should be offered for adding by the [possibleAttributeBagsInternal]
     * property.
     */
    private val containedBagTypesSet = FXCollections.observableSet<Class<out AttributeBag>>()

    /**
     * All bag types available for the currently set [editedElementProperty] element.
     *
     * Used to decide what AttributeBag types should be offered for adding by the [possibleAttributeBagsInternal]
     * property.
     */
    private val availableBagTypesSet = FXCollections.observableSet<Class<out AttributeBag>>()

    /**
     * Whether this Controller has any changes to commit. The value of this is true, if at least one of these conditions
     * is met:
     *
     * * At least one of the attributes contained in the ViewModels is dirty
     * * There are entirely uncommitted AttributeBags
     */
    val isDirtyProperty = GriffonFXCollections
            .observableStream(dirtyBagObservableList)
            .anyMatch(ObservableValue<Boolean>::getValue)
            .or(this.uncommittedAttributeBags.sizeProperty.greaterThan(0))!!

    /* ------------------------- SERVICE DEPENDENCIES -------------------------- */

    /**
     * The system's current [CanvasCoordinator]. Used to subscribe to Attribute updates as well as to publish changes
     * to the rest of the system.
     */
    private val operationHandler: CanvasOperationHandler by di()

    /**
     * The system's current [CanvasAttributeManager]. Used to query the
     */
    private val attributeManager: CanvasAttributeManager by di()

    private val attributeBagFactory: AggregateAttributeBagFactory by di()

    init {
        // Get notified when the edited element is changed
        this.editedElementProperty.addListener { _, _, newValue ->
            this.onEditedElementChange(newValue)
        }

        // Get notified when the attributes of the currently edited Element change
        this.attributeManager.addAttributeChangeListener(this::class.java.name) { id, _ ->
            if (this.editedElementProperty.value?.id == id) {
                this.loadAttributes()
            }
        }

        this.attributeBagViewModelsInternal.addListener { change: MapChangeListener.Change<out String, out AttributeViewModel> ->
            if (change.wasAdded()) {
                change.valueAdded.content?.let { dirtyBagObservableList.add(it.isDirty) }
            } else if (change.wasRemoved()) {
                change.valueRemoved.content?.let { dirtyBagObservableList.remove(it.isDirty) }
            }
        }
    }

    fun insertAttributeBag(bagClass: Class<out AttributeBag>) {
        val bag = this.editedElementProperty.value?.let {
            this.attributeBagFactory.createBag(bagClass, it)
        } ?: return

        this.attributeBagViewModelsInternal[bag.id] = generateViewModel(bag)
        this.attributeBags[bag.id] = bag
        this.uncommittedAttributeBags.add(bag.id)

        this.loadAttributes()
    }

    fun commit() {
        val elementSetToStore = mutableSetOf<AttributeBag>()
        val relationSetToStore = mutableSetOf<AttributesFor>()

        for ((id, attributeBag) in this.attributeBagViewModels) {
            var isUncommitted = this.uncommittedAttributeBags.remove(id)
            var mustBeStored = isUncommitted

            val vmContent = attributeBag.content as? AttributeBagContentViewModel ?: continue

            for ((ref, attribute) in vmContent.attributeObservable) {
                val vmAttributeContent = attribute.content as? AtomicAttributeContentViewModel<*> ?: continue

                if (vmAttributeContent.dirtyByUser.value) {
                    mustBeStored = true

                    /*
                     * This will apply the value to the backing Attribute. As this is the same object reference as the
                     * one stored in the attributeBags map, that one will automatically be updated as well.
                     */
                    vmAttributeContent.commit()
                }
            }

            if (mustBeStored) {
                attributeBags[id]?.let {
                    elementSetToStore.add(it)

                    // for new AttributeBags, also create the association relation
                    if (isUncommitted) {
                        this.editedElementProperty.value?.let { element ->
                            relationSetToStore.add(AttributesFor(it.ref(), element.ref()))
                        }
                    }
                }
            }
        }

        operationHandler.updateAttributes(elementSetToStore, relationSetToStore)
    }

    fun rollback() {
        for ((_, attributeBag) in this.attributeBagViewModels) {
            val vmContent = attributeBag.content as? AttributeBagContentViewModel ?: continue

            for ((_, attribute) in vmContent.attributeObservable) {
                (attribute.content as? AtomicAttributeContentViewModel<*>)?.rollback()
            }

            for (uncommittedId in this.uncommittedAttributeBags) {
                this.attributeBagViewModelsInternal.remove(uncommittedId)
            }

            this.uncommittedAttributeBags.clear()
        }
    }

    /**
     * Callback for when the currently edited Element in the [editedElementProperty] is changed for the given
     * [newElement].
     */
    private fun onEditedElementChange(newElement: GraphicObject<*>?) {
        this.elementIdPropertyInternal.value = newElement?.id ?: ""
        this.elementTypePropertyInternal.value = newElement?.let { it::class.java.simpleName + " (${it.shapeClass.simpleName})" }
                ?: ""

        possibleAttributeBags.clear()
        this.editedElementProperty.value?.let { graphicObject ->
            possibleAttributeBags.addAll(attributeBagFactory.availableBagTypes(graphicObject))
        }

        loadAttributes()
        loadPossibleBagTypes()
    }

    /**
     * Fetches the currently available [AttributeBag]s for the [editedElementProperty] Element and writes their
     * [AttributeBagContentViewModel]s with the correct [AtomicAttributeContentViewModel]s into the [attributeBagViewModels] Map.
     */
    private fun loadAttributes() {
        synchronized(this) {
            val elementId = this.editedElementProperty.value?.id

            if (elementId == null) {
                this.attributeBagViewModels.clear()
                this.containedBagTypesSet.clear()
                updatePossibleAttributeTypesList()

                return
            }

            // Fetch all currently available AttributeBags for the edited Element.
            val foundBags = attributeManager.getBagsForElement(elementId).toMutableMap()

            for (id in uncommittedAttributeBags) {
                val bag = attributeBags[id] ?: continue

                foundBags.getOrPut(bag::class.java) {
                    mutableSetOf()
                }.add(bag)
            }

            val bagTypesToRemove = containedBagTypesSet.toMutableSet()
            /*
             * Mark all current bag IDs for removal. They are removed from this set if found to be still present.
             */
            val bagsToRemove = attributeBagViewModelsInternal.keys.toMutableSet()

            // don't remove things we have yet to commit
            bagsToRemove.removeAll(uncommittedAttributeBags)

            /*
             * Iterate over all bags. As the bags are additionally wrapped in a map of their types, do this for each
             * of the elements of this map
             */
            bagTypeLoop@ for ((bagType, bags) in foundBags) {
                bagTypesToRemove.remove(bagType)

                // BagType is now contained, so add it to the contained types and remove it from the available to add
                // types
                containedBagTypesSet.add(bagType)
                possibleAttributeBagsInternal.remove(bagType)

                bagLoop@ for (bag in bags) {
                    // bag still present, don't remove.
                    bagsToRemove.remove(bag.id)

                    this.attributeBags[bag.id] = bag

                    val values = bag.readOnlyValues

                    // get existing AttributeBag representation in this Controller...
                    val vmBag = attributeBagViewModelsInternal.getOrPut(bag.id, {
                        // ... or insert a new one if it did not yet exist.
                        generateViewModel(bag)
                    })

                    val vmBagContent = (vmBag.content as? AttributeBagContentViewModel) ?: run {
                        val valueViewModel = generateValueViewModel(bag)

                        vmBag.content = valueViewModel

                        return@run valueViewModel
                    }

                    /*
                     * Mark all attribute references for removal.
                     */
                    val attributesToRemove = vmBagContent.attributeObservable.keys.toMutableSet()

                    attributeLoop@ for ((ref, attribute) in values) {
                        attributesToRemove.remove(ref)

                        val vmAttribute = vmBagContent.attributeObservable[ref]

                        // If an old representation of the given value already exists, update that one
                        if (vmAttribute != null) {
                            val vmAttributeContent = (vmAttribute.content as? AtomicAttributeContentViewModel<*>)
                                    ?: run {
                                        val valueViewModel = generateValueViewModel(attribute)

                                        vmBag.content = valueViewModel

                                        return@run valueViewModel
                                    }

                            // if the attribute's value is out of sync, update it.
                            if (vmAttributeContent.backingAttribute != attribute) {
                                vmAttributeContent.setBackingAttributeUnsafe(attribute)
                            }

                            continue@attributeLoop
                        }

                        // if no representation exists, add that Attribute.
                        val newAttributeViewModel = AttributeViewModel(
                                ref.attrId,
                                attribute::class.java.simpleName,
                                AtomicAttributeContentViewModel(
                                        attribute
                                )
                        )

                        vmBagContent.attributeObservable[ref] = newAttributeViewModel
                    }

                    attributesToRemove.forEach {
                        vmBagContent.attributeObservable.remove(it)
                    }
                }
            }

            bagsToRemove.forEach { idToRemove ->
                attributeBagViewModelsInternal.remove(idToRemove)
                attributeBags.remove(idToRemove)
            }

            bagTypesToRemove.forEach {
                containedBagTypesSet.remove(it)
            }
        }

        updatePossibleAttributeTypesList()
    }

    private fun loadPossibleBagTypes() {
        this.editedElementProperty.value?.let {
            val availableTypes = this.attributeBagFactory.availableBagTypes(it)

            val attributeBagTypesToRemove = availableBagTypesSet.toMutableSet()

            for (type in availableTypes) {
                if (!attributeBagTypesToRemove.remove(type)) {
                    availableBagTypesSet.add(type)
                }
            }
        } ?: availableBagTypesSet.clear()

        updatePossibleAttributeTypesList()
    }

    /**
     * Syncs the value of [containedBagTypesSet] and [availableBagTypesSet] with that of
     * [possibleAttributeBagsInternal].
     */
    private fun updatePossibleAttributeTypesList() {
        val possibleAttributeTypesToRemove = possibleAttributeBagsInternal.toMutableSet()

        val possibleAttributeTypes = availableBagTypesSet.minus(containedBagTypesSet)

        for (type in possibleAttributeTypes) {
            if (!possibleAttributeTypesToRemove.remove(type)) {
                possibleAttributeBagsInternal.add(type)
            }
        }

        possibleAttributeBagsInternal.removeAll(possibleAttributeTypesToRemove)
    }

    private fun generateViewModel(bag: AttributeBag): AttributeViewModel {
        return AttributeViewModel(bag.id, bag::class.java.simpleName, generateValueViewModel(bag))
    }

    private fun generateValueViewModel(bag: AttributeBag): AttributeBagContentViewModel {
        return AttributeBagContentViewModel(bag.id, bag::class.java, emptyMap())
    }

    private fun generateValueViewModel(attribute: Attribute<*>): AtomicAttributeContentViewModel<*> {
        return AtomicAttributeContentViewModel(attribute)
    }
}
