package ru.taxcom.cashdeskkit.document.controller

import ru.taxcom.cashdeskkit.data.cashdesk.request.DocumentParams
import ru.taxcom.cashdeskkit.data.cashdesk.response.PrintDocumentResponse
import ru.taxcom.cashdeskkit.data.qr.EmailResponse
import ru.taxcom.taxcomkit.data.resource.Resource

interface DocumentController {
    suspend fun sendReceiptByEmail(response: EmailResponse): Resource<Unit>
    suspend fun getDocument(docRequest: DocumentParams): Resource<PrintDocumentResponse>
}