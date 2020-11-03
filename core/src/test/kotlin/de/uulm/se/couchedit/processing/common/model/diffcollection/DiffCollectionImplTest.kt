package de.uulm.se.couchedit.processing.common.model.diffcollection

import de.uulm.se.couchedit.processing.common.model.ModelDiff
import org.junit.jupiter.api.BeforeEach

class DiffCollectionImplTest: DiffCollectionTest() {
    override val systemUnderTest = DiffCollectionImpl(diffList.map { it.affected.id to it }.toMap())
}
