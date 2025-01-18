package com.example.receiptsswipe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.receiptsswipe.activity.DocViewModel
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.taxcom.cashdesk.R
import ru.taxcom.cashdesk.databinding.ProfileNavBarActivityBinding
import ru.taxcom.cashdesk.menu.BottomMenu
import ru.taxcom.cashdesk.menu.ChangeItemMenu
import ru.taxcom.cashdesk.menu.getMenuId
import ru.taxcom.cashdesk.ui.profilescreen.ProfileScreenFragment
import ru.taxcom.cashdeskanalytics.data.ReportParam
import ru.taxcom.cashdeskanalytics.data.filter.Sort
import ru.taxcom.cashdeskanalytics.data.filter.SortType
import ru.taxcom.cashdeskanalytics.data.recycler.entity.ItemOfCashierList
import ru.taxcom.cashdeskanalytics.data.recycler.entity.ItemOfPeriodList
import ru.taxcom.cashdeskanalytics.ui.fragments.abcxyzreport.AbcXyzAnalysisFragment
import ru.taxcom.cashdeskanalytics.ui.fragments.abcxyzreport.AbcXyzAnalysisInfoFragment
import ru.taxcom.cashdeskanalytics.ui.fragments.listofcashiersreport.CashierDetailsFragment
import ru.taxcom.cashdeskanalytics.ui.fragments.listofcashiersreport.ListOfCashiersListFragment
import ru.taxcom.cashdeskanalytics.ui.fragments.listofcashiersreport.ListOfCashiersParametersFragment
import ru.taxcom.cashdeskanalytics.ui.fragments.listofcashiersreport.SelectSortFragment
import ru.taxcom.cashdeskanalytics.ui.fragments.main.AnalyticsFragment
import ru.taxcom.cashdeskanalytics.ui.fragments.revenuebyhoursreport.RevenueByHoursIndicatorFragment
import ru.taxcom.cashdeskanalytics.ui.fragments.revenuebyhoursreport.RevenueByHoursParametersFragment
import ru.taxcom.cashdeskanalytics.ui.fragments.selectlistoffilter.SelectListOfPeriodFragment
import ru.taxcom.cashdeskanalytics.ui.fragments.shiftindicatorsrepor.ShiftIndicatorsReportFragment
import ru.taxcom.cashdeskanalytics.ui.fragments.shiftindicatorsrepor.ShiftParametersFragment
import ru.taxcom.cashdeskdevices.ui.CashDeskDeviceListFragment
import ru.taxcom.cashdeskdivisions.data.cashdesk.CashDeskParams
import ru.taxcom.cashdeskdivisions.data.recycler.entities.DivisionElement
import ru.taxcom.cashdeskdivisions.data.recycler.entities.ShiftPresentation
import ru.taxcom.cashdeskdivisions.ui.filter.FilterFragment
import ru.taxcom.cashdeskdivisions.ui.fragments.CashDetailsFragment
import ru.taxcom.cashdeskdivisions.ui.fragments.DivisionsDetailsTPSelectionFragment
import ru.taxcom.cashdeskdivisions.ui.fragments.DivisionsFragment
import ru.taxcom.cashdeskdivisions.ui.fragments.filter.presentation.CashDeskFilterFragment
import ru.taxcom.cashdeskdivisions.ui.fragments.outlet.OutletSectionFragment
import ru.taxcom.cashdeskdivisions.ui.fragments.shift.ShiftFragment
import ru.taxcom.cashdeskkit.SwipeCallback
import ru.taxcom.cashdeskkit.controller.DocViewModel
import ru.taxcom.cashdeskkit.controller.data.ListState
import ru.taxcom.cashdeskkit.data.cashdesk.params.FilterParams
import ru.taxcom.cashdeskkit.data.cashdesk.params.OutletCashDeskPresentation
import ru.taxcom.cashdeskkit.data.cashdesk.request.DocumentParams
import ru.taxcom.cashdeskkit.data.filter.FilterData
import ru.taxcom.cashdeskkit.document.DocumentFragment
import ru.taxcom.cashdeskkit.document.navigator.DocumentNavigator
import ru.taxcom.cashdeskkit.email.SendReceiptByEmailFragment
import ru.taxcom.cashdeskkit.ui.tpselection.TPSelection
import ru.taxcom.cashdeskreport.data.filter.Filter
import ru.taxcom.cashdeskreport.ui.fragment.CashdeskReportChangesFragment
import ru.taxcom.cashdeskreport.ui.fragment.CashdeskReportFiscalFragment
import ru.taxcom.cashdeskreport.ui.fragment.CashdeskReportKktStatusFragment
import ru.taxcom.cashdeskreport.ui.fragment.CashdeskReportKttFragment
import ru.taxcom.cashdeskreport.ui.fragment.CashdeskReportReconciliationFragment
import ru.taxcom.cashdeskreport.ui.fragment.CashdeskReportSalesFragment
import ru.taxcom.cashdeskreport.ui.fragment.CashdeskReportSendCheckFragment
import ru.taxcom.cashdeskreport.ui.fragment.SelectListOfFilterFragment
import ru.taxcom.caskdesk_calendar.CashDeskCalendar
import ru.taxcom.caskdesk_calendar.config.CalendarConfig
import ru.taxcom.commonrecycler.selectablelist.IListProvider
import ru.taxcom.commonrecycler.selectablelist.ItemOfSelectableList
import ru.taxcom.commonrecycler.selectablelist.SelectListFragment
import ru.taxcom.feedback.ui.FeedbackScreenFragment
import ru.taxcom.feedback.ui.detail.DetailFeedbackFragment
import ru.taxcom.feedback.ui.history.FeedbackHistoryFragment
import ru.taxcom.filer.notificationmodule.data.informing.NotificationChannel
import ru.taxcom.filer.notificationmodule.data.notification.Notification
import ru.taxcom.filer.notificationmodule.data.notification.NotificationsFilter
import ru.taxcom.filer.notificationmodule.screens.NotificationNavigator
import ru.taxcom.filer.notificationmodule.screens.fragments.NotificationsChannelsSettingsFragment
import ru.taxcom.filer.notificationmodule.screens.fragments.NotificationsListFragment
import ru.taxcom.filer.notificationmodule.screens.fragments.NotificationsViewFragment
import ru.taxcom.filer.profilemodule.viewmodels.ProfileViewModel
import ru.taxcom.filer.settingsmodule.navigator.SettingsNavigator
import ru.taxcom.filer.settingsmodule.ui.SettingsFragment
import ru.taxcom.filer.settingsmodule.ui.fragments.configure.ScreenConfigureFragment
import ru.taxcom.taxcomkit.repository.settings.AppTheme
import ru.taxcom.taxcomkit.repository.settings.screen.DivisionTypes
import ru.taxcom.taxcomkit.ui.baseactivity.TaxcomDaggerAppCompatActivity
import ru.taxcom.taxcomkit.ui.snackbar.Snackbar
import ru.taxcom.taxcomkit.ui.view.activitybars.colorControls
import ru.taxcom.taxcomkit.utils.hideKeyboard
import ru.taxcom.taxcomkit.utils.launchWhenResumed
import ru.taxcom.taxcomkit.utils.permissions.requestAndroid13NotificationsPermissions
import ru.taxcom.taxcomkit.utils.preferences.Preferences
import ru.taxcom.taxcomkit.uuid.UuidStorage
import javax.inject.Inject
import kotlin.reflect.KClass
import ru.taxcom.cashdesk.ui.navigation.InnerNavigator as InnerNavigatorCashDesk
import ru.taxcom.cashdeskanalytics.ui.navigation.InnerNavigator as InnerNavigatorAnalytics
import ru.taxcom.cashdeskdevices.navigation.InnerNavigator as InnerCashdeskNavigator
import ru.taxcom.cashdeskdivisions.ui.navigation.InnerNavigator as InnerNavigatorDivision
import ru.taxcom.cashdeskreport.navigation.InnerNavigator as InnerNavigatorReports
import ru.taxcom.caskdesk_calendar.navigation.InnerNavigator as InnerCalendarNavigator
import ru.taxcom.feedback.navigation.InnerNavigator as FeedbackInnerNavigator

class ProfileNavBarActivity : TaxcomDaggerAppCompatActivity(), InnerNavigatorDivision,
    InnerNavigatorAnalytics, InnerNavigatorReports, InnerCashdeskNavigator, InnerNavigatorCashDesk,
    NotificationNavigator, InnerCalendarNavigator, DocumentNavigator, SettingsNavigator,
    SwipeCallback, FeedbackInnerNavigator {

    @Inject
    lateinit var uuidStorage: UuidStorage

    private lateinit var binding: ProfileNavBarActivityBinding

    private val profileViewModel: ProfileViewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[ProfileViewModel::class.java]
    }
    private val viewModel: ProfileNavBarViewModel by viewModels { viewModelFactory }
    private val docViewModel: DocViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileNavBarActivityBinding.inflate(layoutInflater)
        initFeedbackUuid()
        initTheme()
        val view = binding.root
        viewModel.getRemoteControl()
        login = intent.getStringExtra(LOGIN)
        url = intent.getStringExtra(URL)
        val notificationId = intent.getStringExtra(NOTIFICATION_ID)
        viewModel.pushNotification(login, url)
        notificationPref.saveShowNotification(
            login.isNullOrEmpty().not() && url.isNullOrEmpty().not()
        )
        notificationId?.let { notificationPref.savePushNotificationID(it) }
        initBottomMenu()
        setContentView(view)
        this.colorControls()
        tokenExpiredSnackbar()
        this.requestAndroid13NotificationsPermissions()
    }

    private fun initFeedbackUuid() {
        if (uuidStorage.getUuid().isEmpty()) uuidStorage.saveUuid()
    }

    private fun initTheme() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                profileViewModel.appTheme.observe(this@ProfileNavBarActivity) { theme ->
                    when (theme) {
                        AppTheme.LIGHT ->
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

                        AppTheme.DARK ->
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

                        else -> return@observe
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) ?: return
        outState.putString(FRAGMENT_KEY, fragment::class.java.simpleName)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val fragment = savedInstanceState.getString(FRAGMENT_KEY)
        savedInstanceState.clear()
        if (fragment == SETTINGS_KEY) supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, SettingsFragment())
            .disallowAddToBackStack()
            .commit()
    }

    override fun onStop() {
        super.onStop()
        viewModel.clearSavedDivisionsTabs()
    }

    private fun initBottomMenu() = with(binding.bottomNavigation) {
        setVisibleBottomItemMenu()
        selectedItemId = viewModel.getStartSelectMenuItem()
        setOnItemSelectedListener { item ->
            viewModel.clearSavedDivisionsTabs()
            viewModel.setBottomMenu(BottomMenu.findMenuById(item.itemId))
        }
        viewModel.currentMainFragment
            .onEach(::changeItemMenu)
            .launchWhenResumed(lifecycleScope)
        lifecycleScope.launch {
            viewModel.permissionMainScreen.observe(this@ProfileNavBarActivity) {
                supportFragmentManager.beginTransaction()
                    .replace(binding.fragmentContainer.id, getStartFragment(it))
                    .commit()
            }
        }
    }

    private fun setVisibleBottomItemMenu() = with(binding.bottomNavigation) {
        menu.findItem(BottomMenu.DIVISION.getMenuId()).isVisible =
            viewModel.getAvailableMainScreen()
        menu.findItem(BottomMenu.REPORT.getMenuId()).isVisible = viewModel.getAvailableMainScreen()
        menu.findItem(BottomMenu.ANALYTICS.getMenuId()).isVisible =
            viewModel.getAvailableAnalytics()
    }

    private fun changeItemMenu(changeItemMenu: ChangeItemMenu) {
        val fragmentToAttach = changeItemMenu.itemToAttach.toFragmentClass()
        val fragmentToDetach = changeItemMenu.itemToDetach.toFragmentClass()
        setFragment(fragmentToAttach, fragmentToDetach)
    }

    private fun BottomMenu?.toFragmentClass(): KClass<out Fragment>? = when (this) {
        BottomMenu.DIVISION -> DivisionsFragment::class
        BottomMenu.REPORT -> CashDeskDeviceListFragment::class
        BottomMenu.ANALYTICS -> AnalyticsFragment::class
        BottomMenu.PROFILE -> ProfileScreenFragment::class
        BottomMenu.SETTINGS -> SettingsFragment::class
        else -> null
    }

    private fun getStartFragment(permissionMainScreen: Boolean): DaggerFragment {
        return when {
            viewModel.getNotification() -> ProfileScreenFragment()
            permissionMainScreen -> DivisionsFragment()
            viewModel.getAvailableAnalytics() -> AnalyticsFragment()
            else -> ProfileScreenFragment()
        }
    }

    private fun setFragment(
        toAttach: KClass<out Fragment>?,
        toDetach: KClass<out Fragment>?
    ): Boolean {
        toAttach ?: return false
        val tagToAttach = toAttach.qualifiedName
        val tagToDetach = toDetach?.qualifiedName
        val fragmentToDetach = tagToDetach?.let { supportFragmentManager.findFragmentByTag(it) }
        val existFragment = supportFragmentManager.findFragmentByTag(tagToAttach)
        tagToDetach?.let { supportFragmentManager.saveBackStack(it) }
        supportFragmentManager.commit {
            if (supportFragmentManager.backStackEntryCount > EMPTY_FRAGMENT_IN_BACK_STACK)
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            if (fragmentToDetach != null)
                detach(fragmentToDetach)
            if (existFragment != null)
                attach(existFragment)
            else
                replace(binding.fragmentContainer.id, toAttach.java, null, tagToAttach)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            setReorderingAllowed(true)
        }
        tagToAttach?.let { supportFragmentManager.restoreBackStack(it) }
        return true
    }

    private fun Fragment.navigateTo(name: String? = null) {
        supportFragmentManager
            .beginTransaction()
            .replace(binding.fragmentContainer.id, this)
            .addToBackStack(name)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commitAllowingStateLoss()
    }

    override fun navToBack(name: String?) {
        hideKeyboard()
        if (name == null)
            supportFragmentManager.popBackStack()
        else
            supportFragmentManager.popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        showBottomMenu()
    }

    override fun navToDocument(
        document: DocumentParams, receiptId: String
    ) = DocumentFragment.newFragment(document, receiptId, true).navigateTo()

    override fun loadDocuments(isRefresh: Boolean?) = docViewModel.loadDocuments(isRefresh)

    override fun getDocuments() = docViewModel.documents
    override fun navigateToChatHistory() = FeedbackHistoryFragment.newInstance().navigateTo()
    override fun navigateToDetails(id: String?) = DetailFeedbackFragment.newInstance(id).navigateTo()
    override fun getNextDocument(
        receiptNumber: String,
        countOfDocuments: Int?
    ): List<DocumentParams>? =
        docViewModel.getNextDocument(receiptNumber, countOfDocuments ?: 1)

    override fun setFilterDocuments(filterData: FilterData) =
        docViewModel.setFilterData(filterData)

    override fun setCashDeskParams(params: CashDeskParams) =
        docViewModel.setCashDeskParams(params)

    override fun getFilterDocuments(): MutableStateFlow<FilterData> =
        docViewModel.filterDocuments

    override fun getDocumentsState(): MutableStateFlow<ListState> =
        docViewModel.documentsListState

    companion object {
        private const val packageName = "ru.taxcom.filer.profilemodule.ui"
        private const val accountRemovedKey = "$packageName.accountRemovedKey"
        private const val DIVISION_KEY = "Divisions"
        private const val CASH_DESK_KEY = "CashDesk"
        private const val OUTLET_KEY = "Outlet"
        private const val EMPTY_FRAGMENT_IN_BACK_STACK = 0
        private const val SETTINGS_KEY = "SettingsFragment"
        private const val FRAGMENT_KEY = "fragment"

        fun start(context: Context, clearStack: Boolean, accountRemoved: Boolean) {
            val intent = Intent(context, ProfileNavBarActivity::class.java)
            if (clearStack) intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (accountRemoved) intent.putExtra(accountRemovedKey, true)
            context.startActivity(intent)
        }
    }
}