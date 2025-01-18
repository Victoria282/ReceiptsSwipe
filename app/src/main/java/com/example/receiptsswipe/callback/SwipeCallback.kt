package com.example.receiptsswipe.callback

import com.example.receiptsswipe.custom.ItemOfSelectableList
import com.example.receiptsswipe.custom.RecyclerElement
import com.example.receiptsswipe.custom.comparator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
interface SwipeCallback {
    fun getNextDocument(
        receiptNumber: String, countOfDocuments: Int? = 1
    ): List<DocumentParams>?

    fun getDocuments(): StateFlow<List<DocumentPresentation>?>
    fun getFilterDocuments(): MutableStateFlow<FilterData>
    fun getDocumentsState(): MutableStateFlow<ListState>
    fun setCashDeskParams(params: CashDeskParams)
    fun setFilterDocuments(filterData: FilterData)
    fun loadDocuments(isRefresh: Boolean? = null)
}

sealed class ListState {
    object Paging : ListState()
    object Loading : ListState()
    object NotLoading : ListState()
    class Error(val message: String) : ListState()
}

@Serializable
data class FilterData(
    val screen: ScreenToFilter,
    val period: PeriodFilter? = null,
    var filterIsApplied: Boolean = false,
    val outlet: ItemOfFilterList? = null,
    val division: ItemOfFilterList? = null,
    var isOnReturnReceipts: Boolean? = null,
    val documentType: ItemOfFilterList? = null,
    var cashDeskFilter: CashDeskFilter? = CashDeskFilter()
)

enum class ScreenToFilter {
    SHIFTS, DOCUMENTS, DIVISIONS_CASH_DESK, CASH_DESK
}

@Serializable
data class PeriodFilter(
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val id: Long = 0,
    val fromDatePresentation: String = "",
    val toDatePresentation: String = "",
)

@Serializable
data class CashDeskFilter(
    var revenueStart: String? = null,
    var revenueEnd: String? = null,
    var cashMoneyStart: String? = null,
    var cashMoneyEnd: String? = null,
    var nonCashMoneyStart: String? = null,
    var nonCashMoneyEnd: String? = null,
    var kktCondition: KktCondition = KktCondition.ALL,
    var countReceiptsStart: Int? = null,
    var countReceiptsEnd: Int? = null,
    var avgReceiptsStart: String? = null,
    var avgReceiptsEnd: String? = null,
    var countPositionsStart: Int? = null,
    var countPositionsEnd: Int? = null
)

enum class KktCondition(val id: Int) {
    ALL(R.string.cash_desk_kkt_conditions_all_title),
    OPENED(R.string.cash_desk_kkt_conditions_opened_title),
    CLOSED(R.string.cash_desk_kkt_conditions_closed_title)
}

@Serializable
data class ItemOfFilterList(
    override val id: String,
    override val title: String,
    override val isDisable: Boolean = false,
    override var isSelected: Boolean = false,
    override val chevronVisibility: Boolean = false,
    val period: PeriodFilter? = null
) : ItemOfSelectableList {

    override fun copyDiff(
        isDisable: Boolean,
        isSelected: Boolean,
        chevronVisibility: Boolean
    ): ItemOfSelectableList = this.copy(
        isDisable = isDisable,
        isSelected = isSelected,
        chevronVisibility = chevronVisibility
    )

    override fun encodeToStringJson(): String = Json.encodeToString(this)

    override fun compare(new: RecyclerElement): Boolean = this == new
}

data class DocumentPresentation(
    val number: String,
    val documentType: DocumentType,
    val cashier: String,
    val sum: String,
    val dateTime: String,
    val shiftNumber: Int,
    val fiscalSign: String,
    val kktId: String,
    val receiptNumber: String? = null,
    val paymentMethods: List<Int>? = null
) : RecyclerElement {

    enum class DocumentType {
        ALL, REGISTRATION_REPORT, REGISTRATION_CHANGE_REPORT,
        SHIFT_CLOSE_REPORT, REGISTRATION_CLOSE, OPERATOR_CONFIRMATION,
        PAYMENTS_STATUS_REPORT, RECEIPT, RECEIPT_CORRECTION, SHIFT_OPEN_REPORT,
        FORM_OF_STRICT_ACCOUNTABILITY, FORM_OF_STRICT_ACCOUNTABILITY_CORRECTION
    }

    override fun compare(new: RecyclerElement): Boolean = this.comparator<DocumentPresentation>(new)
}

data class CashDeskParams(
    val cashDesk: OutletCashDeskPresentation,
    var chartPeriod: TPSelectionHolder
)

@Serializable
data class OutletCashDeskPresentation(
    val id: Long,
    val isEnabled: Boolean,
    val nameString: String,
    val revenueString: String,
    val kktRegNumber: String,
    val outletId: Long,
    val utsOffset: Int? = null,
    val kktFactoryNumber: String? = null,
    val endFnActivationDateTime: Int? = null,
    val startFnActivationDateTime: Int? = null,
    val kktModelName: String? = null,
    val fnFactoryNumber: String? = null
) : RecyclerElement {
    override fun compare(new: RecyclerElement): Boolean =
        this.comparator<OutletCashDeskPresentation>(new)
}

@kotlinx.serialization.Serializable
data class TPSelectionHolder(
    val tpSelection: TPSelection,
    val from: Long,
    val to: Long,
    val title: String? = null
) : java.io.Serializable

@Serializable
sealed class TPSelection : java.io.Serializable {
    @Serializable
    open class CustomSelection(val from: Long? = null, val to: Long? = null) : TPSelection(),
        java.io.Serializable

    @Serializable
    object CurrentMonth : TPSelection(), java.io.Serializable

    @Serializable
    object Today : TPSelection(), java.io.Serializable

    @Serializable
    object Yesterday : TPSelection(), java.io.Serializable

    @Serializable
    object SevenDays : TPSelection(), java.io.Serializable

    @Serializable
    object ThirtyDays : TPSelection(), java.io.Serializable

    @Serializable
    class SampleText(val title: String? = null) : TPSelection(), java.io.Serializable
}

@Serializable
class DocumentParams(
    @SerialName("FiscalSign")
    val fiscalSign: String,
    @SerialName("CashdeskRegNum")
    val cashdeskRegistrationNumber: String,
    val receiptNumber: String? = null,
    var title: String
)