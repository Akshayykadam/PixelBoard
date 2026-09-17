package com.akshaykadam.pixelboard.patches.gboard.shared

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

internal const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

internal fun Document.findSoftkeyTemplate(id: String): Element =
    getElementsByTagName("softkey_template").elements().firstOrNull {
        it.getAttribute("id") == id
    } ?: error("Could not find softkey_template with id $id")

internal fun Document.findSoftkeyList(templateId: String): Element =
    getElementsByTagName("softkey_list").elements().firstOrNull {
        it.getAttribute("template_id") == templateId
    } ?: error("Could not find softkey_list with template_id $templateId")

internal fun ensureTemplateAction(
    templateElement: Element,
    type: String,
    data: String,
    popupLabel: String
) {
    val action = templateElement.childElements("action").firstOrNull {
        it.getAttribute("type") == type
    } ?: templateElement.ownerDocument.createElement("action").also { createdAction ->
        val longPressAction = templateElement.childElements("action").firstOrNull {
            it.getAttribute("type") == "LONG_PRESS"
        }
        if (longPressAction != null) {
            templateElement.insertBefore(createdAction, longPressAction)
        } else {
            templateElement.appendChild(createdAction)
        }
    }

    action.setAttribute("type", type)
    action.setAttribute("data", data)
    action.setAttribute("intention", "COMMIT")
    action.setAttribute("keycode", "PLAIN_TEXT")
    action.setAttribute("popup_label", popupLabel)
}

internal fun setFooterLabelValue(templateElement: Element, location: String, value: String) {
    val footerLabel = templateElement.childElements("label").firstOrNull {
        it.getAttribute("location") == location
    } ?: templateElement.ownerDocument.createElement("label").also { label ->
        templateElement.appendChild(label)
    }

    footerLabel.setAttribute("location", location)
    footerLabel.setAttribute("value", value)
}

internal fun Node.insertAfter(newNode: Node, referenceNode: Node) {
    val parent = if (this.nodeType == Node.DOCUMENT_NODE) {
        (this as Document).documentElement
    } else {
        this
    }
    val nextSibling = referenceNode.nextSibling
    if (nextSibling == null) {
        parent.appendChild(newNode)
    } else {
        parent.insertBefore(newNode, nextSibling)
    }
}

internal fun NodeList.elements(): Sequence<Element> =
    (0 until length).asSequence().mapNotNull { item(it) as? Element }

internal fun Element.childElements(tagName: String? = null): Sequence<Element> =
    childNodes.elements().filter { tagName == null || it.tagName == tagName }
