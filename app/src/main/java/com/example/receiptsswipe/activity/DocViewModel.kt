package com.example.receiptsswipe.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.taxcom.cashdeskdivisions.data.cashdesk.CashDeskParams
import ru.taxcom.cashdeskkit.controller.data.DocumentPresentation
import ru.taxcom.cashdeskkit.controller.data.ListState
import ru.taxcom.cashdeskkit.controller.data.getStringInt
import ru.taxcom.cashdeskkit.controller.data.toModule
import ru.taxcom.cashdeskkit.data.cashdesk.request.DocumentParams
import ru.taxcom.cashdeskkit.data.cashdesk.request.PagingModel
import ru.taxcom.cashdeskkit.data.filter.FilterData
import ru.taxcom.cashdeskkit.data.filter.ScreenToFilter
import ru.taxcom.taxcomkit.data.resource.ResourceStatus
import ru.taxcom.taxcomkit.utils.resource.ResourceProvider
import javax.inject.Inject

class DocViewModel @Inject constructor(
    private val docController: DocController, private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _documents = MutableStateFlow<List<DocumentPresentation>?>(null)
    val documents get() = _documents.asStateFlow()

    private val _filterDocuments = MutableStateFlow(FilterData(screen = ScreenToFilter.DOCUMENTS))
    val filterDocuments get() = _filterDocuments

    private val cashDeskParams: MutableSharedFlow<CashDeskParams> = MutableSharedFlow(
        replay = 1, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val pagingModel = MutableStateFlow(
        PagingModel(
            currentPage = INIT_PAGE, pageSize = INIT_PAGE_SIZE, isDescOrder = true, order = 0
        )
    )
    private val canPaginate = MutableStateFlow(false)

    val documentsListState = MutableStateFlow<ListState>(ListState.Loading)

    init {
        setDocumentsListener()
    }

    fun setFilterData(filterData: FilterData) {
        _filterDocuments.value = filterData
        loadDocuments(isRefresh = true)
    }

    fun setCashDeskParams(params: CashDeskParams) {
        cashDeskParams.tryEmit(params)
    }

    fun loadDocuments(isRefresh: Boolean? = false) {
        viewModelScope.launch {
            if (isRefresh == true) pagingModel.value = pagingModel.value.copy(
                currentPage = INIT_PAGE
            )
            documentsListState.emit(ListState.Paging)
            docController.pagingDocuments(
                cashDeskParams.replayCache.firstOrNull() ?: return@launch,
                filterDocuments.value,
                pagingModel.value
            )
        }
    }

    private fun setDocumentsListener() = viewModelScope.launch {
        docController.docs.collect { response ->
            when (response?.status ?: return@collect) {
                ResourceStatus.LOADING -> documentsListState.emit(ListState.Paging)
                ResourceStatus.SUCCESS -> {
                    val listData =
                        response.data?.documentModels?.map { it.toModule(cashDeskParams.replayCache.firstOrNull()) }
                            ?: emptyList()

                    val updatedList =
                        if (pagingModel.value.currentPage == INIT_PAGE) mutableListOf()
                        else _documents.value?.toMutableList()
                    updatedList?.addAll(listData)
                    _documents.emit(updatedList)


                    documentsListState.emit(ListState.NotLoading)
                    canPaginate.emit(listData.size >= DEFAULT_PAGE_SIZE)
                    if (canPaginate.value) pagingModel.value = pagingModel.value.copy(
                        currentPage = pagingModel.value.currentPage + 1,
                        pageSize = DEFAULT_PAGE_SIZE
                    )
                }

                ResourceStatus.ERROR -> documentsListState.emit(
                    ListState.Error(response.message ?: response.messageTitle ?: "")
                )
            }
        }
    }

    fun getNextDocument(receiptNumber: String, countOfDocuments: Int): List<DocumentParams>? {
        val currentScreenDocument = documents.value?.find { it.receiptNumber == receiptNumber }
        val indexOfCurrentDocument =
            documents.value?.lastIndexOf(currentScreenDocument) ?: return null

        return if (indexOfCurrentDocument + countOfDocuments >= (documents.value?.size ?: 0)) null
        else when (countOfDocuments) {
            1 -> {
                val param = documents.value?.get(indexOfCurrentDocument + 1)
                listOf(
                    DocumentParams(
                        param?.fiscalSign ?: "",
                        param?.kktId ?: "",
                        receiptNumber = param?.receiptNumber,
                        getTitle(param ?: return emptyList())
                    )
                )
            }

            else -> {
                val sublist = documents.value?.subList(
                    indexOfCurrentDocument + 1, indexOfCurrentDocument + 1 + countOfDocuments
                )
                sublist?.map {
                    DocumentParams(
                        it.fiscalSign,
                        it.kktId,
                        receiptNumber = it.receiptNumber,
                        getTitle(it)
                    )
                } ?: emptyList()
            }
        }
    }

    private fun getTitle(doc: DocumentPresentation): String {
        val titleId = doc.documentType.getStringInt()
        return doc.number.takeIf { it.isNotEmpty() }
            ?.let { "${resourceProvider.getString(titleId)} №${doc.number}" }
            ?: resourceProvider.getString(titleId)
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
        private const val INIT_PAGE_SIZE = 30
        private const val INIT_PAGE = 1
    }
}