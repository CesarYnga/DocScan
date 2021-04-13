package com.cesarynga.docscan.exception

class NoDocumentDetectedException(message: String?, cause: Throwable?) : Exception(message, cause) {

    constructor(message: String?) : this(message, null)
}