package ru.taxcom.cashdeskkit.document

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.taxcom.cashdeskkit.data.cashdesk.request.DocumentParams
import ru.taxcom.cashdeskkit.data.cashdesk.response.PrintDocumentResponse
import ru.taxcom.cashdeskkit.document.controller.DocumentController
import ru.taxcom.taxcomkit.data.resource.Resource
import ru.taxcom.taxcomkit.utils.viewmodel.TaxcomViewModel
import javax.inject.Inject

class DocumentViewModel @Inject constructor(
    private val controller: DocumentController
) : TaxcomViewModel() {

    val documentParams: MutableSharedFlow<DocumentParams> = MutableSharedFlow(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val documentResponse: StateFlow<Resource<PrintDocumentResponse>> =
        documentParams.flatMapLatest {
            flow {
                emit(Resource.loading(null))
                val result = controller.getDocument(it)
                emit(result)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, Resource.success(null))

    val webViews: LinkedHashMap<DocumentParams, Resource<PrintDocumentResponse>> = linkedMapOf()

    val params = MutableLiveData<MutableList<DocumentParams?>?>()

    private var _isLoading = MutableLiveData<Boolean>()
    val isLoading get() = _isLoading

    fun init(docRequestParam: DocumentParams) = documentParams.tryEmit(docRequestParam)

    fun makeCachedDocuments(ids: List<DocumentParams>?) = viewModelScope.launch {
        params.value = ids?.toMutableList()
        val deferredDocuments = ids?.map { docParams ->
            async {
                webViews.put(docParams, controller.getDocument(docParams))
            }
        }
        deferredDocuments?.awaitAll()
    }

    fun makeCashedNextDocument(docParams: DocumentParams?) = viewModelScope.launch {
        docParams ?: return@launch
        _isLoading.value = true
        val deferredDocument = async {
            webViews.put(docParams, controller.getDocument(docParams))
        }
        deferredDocument.await()
        val list = params.value
        if (list?.contains(docParams) == false) list.add(docParams)
        params.value = list
        _isLoading.value = false
    }
}