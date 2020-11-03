package de.uulm.se.couchedit.processing.common.model.diffcollection

import de.uulm.se.couchedit.processing.common.model.ModelDiff

/**
 * A type of DiffCollection which does not carry version information.
 *
 * If the diffs are executed, the Processor should probably time-stamp them as if they were just executed.
 */
class PreparedDiffCollection : DiffCollectionImpl() {
    fun putDiff(md: ModelDiff) {
        this.diffsInternal[md.affected.id] = md
    }
}
