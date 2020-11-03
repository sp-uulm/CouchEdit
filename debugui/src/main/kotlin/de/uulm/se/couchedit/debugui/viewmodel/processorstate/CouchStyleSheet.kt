package de.uulm.se.couchedit.debugui.viewmodel.processorstate

import com.mxgraph.util.mxConstants
import com.mxgraph.view.mxPerimeter
import com.mxgraph.view.mxStylesheet
import java.util.*

object CouchStyleSheet: mxStylesheet() {
    const val ELEMENT_VERTEX_STYLE_KEY = "ELEMENT"
    const val RELATION_VERTEX_STYLE_KEY = "RELATION"
    const val EDGE_STYLE_DIRECTED = "EDGE_DIRECTED"
    const val EDGE_STYLE_DIRECTED_REVERSE = "EDGE_REVERSE"
    const val EDGE_STYLE_BIDIRECTIONAL = "EDGE_BIDIRECTIONAL"
    const val EDGE_STYLE_UNDIRECTED = "EDGE_UNDIRECTED"

    init {
        this.putCellStyle(ELEMENT_VERTEX_STYLE_KEY, createElementVertexStyle())
        this.putCellStyle(RELATION_VERTEX_STYLE_KEY, createRelationDiamondVertexStyle())
        this.putCellStyle(EDGE_STYLE_DIRECTED, createDirectedEdgeStyle(fromArrow = false, toArrow = true))
        this.putCellStyle(EDGE_STYLE_DIRECTED_REVERSE, createDirectedEdgeStyle(fromArrow = true, toArrow = false))
        this.putCellStyle(EDGE_STYLE_BIDIRECTIONAL, createDirectedEdgeStyle(fromArrow = true, toArrow = true))
        this.putCellStyle(EDGE_STYLE_UNDIRECTED, createDirectedEdgeStyle(fromArrow = false, toArrow = false))
    }

    private fun createElementVertexStyle(): Hashtable<String, Any> {
        val style = Hashtable<String, Any>()

        style[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_RECTANGLE
        style[mxConstants.STYLE_PERIMETER] = mxPerimeter.RectanglePerimeter
        style[mxConstants.STYLE_VERTICAL_ALIGN] = mxConstants.ALIGN_MIDDLE
        style[mxConstants.STYLE_ALIGN] = mxConstants.ALIGN_CENTER
        style[mxConstants.STYLE_FILLCOLOR] = "#ff704d"
        style[mxConstants.STYLE_STROKECOLOR] = "#330000"
        style[mxConstants.STYLE_FONTCOLOR] = "#000000"

        return style
    }

    private fun createRelationDiamondVertexStyle(): Hashtable<String, Any> {
        val style = Hashtable<String, Any>()

        style[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_RHOMBUS
        style[mxConstants.STYLE_PERIMETER] = mxPerimeter.RhombusPerimeter

        style[mxConstants.STYLE_VERTICAL_ALIGN] = mxConstants.ALIGN_MIDDLE
        style[mxConstants.STYLE_ALIGN] = mxConstants.ALIGN_CENTER
        style[mxConstants.STYLE_FILLCOLOR] = "#009999"
        style[mxConstants.STYLE_STROKECOLOR] = "#004d4d"
        style[mxConstants.STYLE_FONTCOLOR] = "#000000"

        return style
    }

    private fun createDirectedEdgeStyle(fromArrow: Boolean, toArrow: Boolean): Hashtable<String, Any> {
        val style = Hashtable<String, Any>()

        style[mxConstants.STYLE_ALIGN] = mxConstants.ALIGN_CENTER
        style[mxConstants.STYLE_STARTARROW] = if(fromArrow) mxConstants.ARROW_CLASSIC else mxConstants.NONE
        style[mxConstants.STYLE_ENDARROW] = if(toArrow) mxConstants.ARROW_CLASSIC else mxConstants.NONE
        style[mxConstants.STYLE_STROKECOLOR] = "#808080"
        style[mxConstants.STYLE_FONTCOLOR] = "#000000"

        return style
    }


}
