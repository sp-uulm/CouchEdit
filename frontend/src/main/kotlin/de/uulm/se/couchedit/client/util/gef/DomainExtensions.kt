package de.uulm.se.couchedit.client.util.gef

import org.eclipse.gef.common.adapt.AdapterKey
import org.eclipse.gef.mvc.fx.domain.IDomain
import org.eclipse.gef.mvc.fx.viewer.IViewer

/**
 * The [IViewer] assigned to this [IDomain]
 */
val IDomain.contentViewer: IViewer
    get() {
        return this.getAdapter(AdapterKey.get(IViewer::class.java, IDomain.CONTENT_VIEWER_ROLE))
    }