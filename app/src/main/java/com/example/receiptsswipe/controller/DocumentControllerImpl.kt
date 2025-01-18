package ru.taxcom.cashdeskkit.document.controller

import ru.taxcom.cashdeskkit.data.cashdesk.request.DocumentParams
import ru.taxcom.cashdeskkit.data.cashdesk.response.PrintDocumentResponse
import ru.taxcom.cashdeskkit.data.qr.EmailResponse
import ru.taxcom.cashdeskkit.network.api.CashdeskApi
import ru.taxcom.taxcomkit.data.resource.Resource
import ru.taxcom.taxcomkit.utils.internet.processHttpResponse
import javax.inject.Inject

class DocumentControllerImpl @Inject constructor(
    private val api: CashdeskApi
) : DocumentController {
    override suspend fun getDocument(docRequest: DocumentParams): Resource<PrintDocumentResponse> =
        processHttpResponse(
            response = { api.loadDocument(docRequest) }
        )

    override suspend fun sendReceiptByEmail(response: EmailResponse): Resource<Unit> =
        processHttpResponse(
            response = { api.sendReceiptByEmail(response) }
        )
}