package ru.taxcom.cashdeskkit.document

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PdfPrint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.taxcom.cashdeskkit.R
import ru.taxcom.cashdeskkit.WEB_RECEIPT_URL
import ru.taxcom.cashdeskkit.controller.data.ListState
import ru.taxcom.cashdeskkit.data.cashdesk.request.DocumentParams
import ru.taxcom.cashdeskkit.data.cashdesk.response.PrintDocumentResponse
import ru.taxcom.cashdeskkit.databinding.CashdeskDocumentFragmentBinding
import ru.taxcom.cashdeskkit.ui.loading.CashdeskLoadingFragment
import ru.taxcom.cashdeskkit.utils.generateUUID
import ru.taxcom.taxcomkit.data.resource.Resource
import ru.taxcom.taxcomkit.data.resource.ResourceStatus.ERROR
import ru.taxcom.taxcomkit.data.resource.ResourceStatus.LOADING
import ru.taxcom.taxcomkit.data.resource.ResourceStatus.SUCCESS
import ru.taxcom.taxcomkit.ui.snackbar.Snackbar
import ru.taxcom.taxcomkit.utils.getArgument
import ru.taxcom.taxcomkit.utils.launchWhenStarted
import ru.taxcom.taxcomkit.utils.pdf.PDF
import ru.taxcom.taxcomkit.utils.pdf.RECEIPT_NAME
import ru.taxcom.taxcomkit.utils.pdf.createWebPrintJob
import java.io.File
import kotlin.math.floor

class DocumentFragment : BaseDocumentFragment<CashdeskDocumentFragmentBinding>() {

    private val documentViewModel: DocumentViewModel by viewModels { viewModelFactory }

    private val shareClickListener = View.OnClickListener { createDocument() }

    private val callback = object : PdfPrint.PrintAdapterCallback {
        override fun callback(tag: Int) = sendDocument()
    }

    private val shareMailCLickListener = View.OnClickListener {
        getArgument<String>(RECEIPT_ID, false)?.let {
            val receiptId =
                if (isInitDocument == true) documentViewModel.documentResponse.value.data?.billId
                else getCurrentDocument()?.data?.billId
            receiptId ?: return@let
            innerNavigator?.navToEmailFragment(receiptId)
        }
    }

    private val swipe: () -> Unit = {
        CoroutineScope(Dispatchers.Main).launch {
            delay(300)
            if (isNeedSwipe) makeSwipe()
        }
    }

    private var isLoadAfterAsync: Boolean? = false
    private var isInitDocument: Boolean? = true
    private var isNeedSwipe: Boolean = false
    private var uri: Uri? = null

    override fun inflateViewBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): CashdeskDocumentFragmentBinding =
        CashdeskDocumentFragmentBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLoadingDocsState()
        setWebViewClient()
        initObservers()
        initArguments()
        initDocuments()
        setUpToolbar()
    }

    private fun initObservers() = with(documentViewModel) {
        params.observe(viewLifecycleOwner) {
            if (it?.size == 0) return@observe
            if (isInitDocument == false) {
                if (it?.size == 1) return@observe
                setUpToolbar(it?.elementAt(FIRST_DOCUMENT_POSITION)?.title)
            }
            if (isInitDocument == false) {
                if (it?.size == 1) return@observe
                setFooterText(it?.elementAt(NEXT_DOCUMENT_POSITION)?.title)
            } else setFooterText(it?.elementAt(FIRST_DOCUMENT_POSITION)?.title)
        }
        isLoading.observe(viewLifecycleOwner) {
            when {
                it && params.value?.size == 1 -> binding.progressBar.isVisible = true
                !it -> binding.progressBar.isVisible = false
            }
        }
    }

    private fun setFooterText(text: String? = null) = with(binding) {
        if (getArgument<Boolean?>(IS_ENABLE_SWIPE, removeArgument = false) == false) return@with
        webViewStepLet.title.text = text
    }

    private fun setUpToolbar(toolbarText: String? = null) = with(binding.toolbar) {
        title = toolbarText ?: documentViewModel.documentParams.replayCache.firstOrNull()?.title
        action1Init = R.drawable.share_mail_icon
        action2Init = R.drawable.share_icon
        action1Tint = R.color.SecondaryForegroundColor
        action2Tint = R.color.SecondaryForegroundColor
        toolbarBackgroundColor = R.color.screen_background_light_color
        onClickBackSpase = View.OnClickListener { innerNavigator?.navToBack() }
        onClickAction1 = shareMailCLickListener
        onClickAction2 = shareClickListener
        init()
    }

    private fun setWebViewClient() = with(binding) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                CoroutineScope(Dispatchers.Main).launch {
                    delay(100)
                    val webViewLocation = IntArray(2)
                    webView.getLocationOnScreen(webViewLocation)
                    val bottomYWebView = webViewLocation[1] + webView.height

                    val frameViewLocation = IntArray(2)
                    frameLayout.getLocationOnScreen(frameViewLocation)
                    val bottomYFrameView = frameViewLocation[1] + frameLayout.height

                    val difference = bottomYFrameView - bottomYWebView
                    val bottomPadding =
                        if (difference == 0) BOTTOM_PADDING_VALUE else difference + BOTTOM_PADDING_VALUE

                    val jsonBottomPadding =
                        "javascript:(function(){ document.body.style.paddingBottom = '${bottomPadding}px'})();"
                    webView.loadUrl(jsonBottomPadding)
                }
            }
        }
    }

    private fun initScrollListener() = with(binding) {
        webView.setCallback(swipe)
        webView.setOnScrollChangeListener { _, _, _, _, _ ->
            val scaledWebViewHeight = webViewScaledHeight()
            val scrolledValue = scaledWebViewHeight - webView.scrollY - BOTTOM_PADDING_VALUE
            val webViewHeight = webView.height
            when {
                webViewHeight - scrolledValue > 120 -> setLetView()

                webViewHeight - scrolledValue > 30 -> setPullView()
                else -> removeView()
            }
        }
    }

    private fun webViewScaledHeight(): Int = floor(
        binding.webView.getContentHeight() * binding.webView.scale
    ).toInt()

    private fun setPullView() = with(binding) {
        isNeedSwipe = false
        webViewStepPull.root.visibility = View.VISIBLE
        webViewStepLet.root.visibility = View.GONE
    }

    private fun setLetView() = with(binding) {
        isNeedSwipe = true
        webViewStepLet.root.visibility = View.VISIBLE
        webViewStepPull.root.visibility = View.GONE
    }

    private fun removeView() = with(binding) {
        isNeedSwipe = false
        webViewStepLet.root.visibility = View.GONE
        webViewStepPull.root.visibility = View.GONE
    }

    private fun initLoadingDocsState() = with(binding) {
        lifecycleScope.launch {
            swipeCallback?.getDocumentsState()?.collect {
                when (it) {
                    ListState.Loading -> {
                        hideFooter()
                        progressBar.isVisible = true
                    }

                    is ListState.Error -> {
                        progressBar.isVisible = false
                        showErrorMessage(
                            getString(R.string.error_title), it.message
                        )
                    }

                    ListState.NotLoading -> {
                        progressBar.isVisible = false
                        if (isLoadAfterAsync == false) return@collect
                        makeSwipe(needRemove = false)
                        isLoadAfterAsync = false
                    }

                    else -> {}
                }
            }
        }
    }

    private fun makeSwipe(needRemove: Boolean = true) = with(documentViewModel) {
        if (params.value?.size == null || params.value?.size == 1) return@with
        val nextDocument = swipeCallback?.getNextDocument(params.value?.last()?.receiptNumber ?: "")
        if (needRemove && isInitDocument == false) {
            val first = params.value?.first()
            val list = params.value
            list?.removeAt(0)
            params.value = (list)
            webViews.remove(first)
        }

        nextDocument?.let {
            makeCashedNextDocument(it.firstOrNull())
            loadNextWebView()
        } ?: run {
            binding.progressBar.isVisible = true
            swipeCallback?.loadDocuments()
            isLoadAfterAsync = true
            hideFooter()
        }
    }

    private fun loadNextWebView() = with(binding) {
        isInitDocument = false
        val animation = AnimationUtils.loadAnimation(context, R.anim.slide_down)
        webView.startAnimation(animation)
        webView.scrollTo(0, 0)
        renderState(getCurrentDocument())
        removeView()
    }

    private fun initCachedDocuments(receiptNumber: String) {
        if (getArgument<Boolean?>(IS_ENABLE_SWIPE, removeArgument = false) == false) return
        val ids =
            swipeCallback?.getNextDocument(receiptNumber, DEFAULT_INIT_COUNT_OF_CASHED_DOCUMENTS)
        documentViewModel.makeCachedDocuments(ids)
    }

    private fun initDocuments() = with(documentViewModel) {
        documentResponse.onEach(::renderState).launchWhenStarted(lifecycleScope)
    }

    private fun initArguments() {
        getArgument<DocumentParams>(
            SHOW_DOC_PARAM_KEY, removeArgument = false
        )?.let {
            initCachedDocuments(it.receiptNumber ?: "")
            documentViewModel.init(it)
        }
        val isEnableSwipe = getArgument<Boolean?>(IS_ENABLE_SWIPE, removeArgument = false) ?: false
        if (isEnableSwipe) initScrollListener()
    }

    private fun hideFooter() = with(binding) {
        webViewStepPull.root.visibility = View.GONE
        webViewStepLet.root.visibility = View.GONE
    }

    private fun renderState(stateLoading: Resource<PrintDocumentResponse>?) = with(binding) {
        when (stateLoading?.status) {
            SUCCESS -> {
                dismissLoadingFragment()
                val content = stateLoading.data?.html ?: ""
                webView.visibility = View.VISIBLE
                renderWebView(content)
                emptyContentMsg.isVisible = content.isNotBlank()
                if (content.isBlank()) {
                    emptyContentMsg.isVisible = true
                    emptyContentMsg.text = getString(R.string.casdesk_divisions_empty)
                } else emptyContentMsg.isVisible = false
            }

            ERROR -> errorState(stateLoading)

            LOADING -> {
                showLoadingFragment()
                emptyContentMsg.isVisible = false
            }

            else -> {}
        }
    }

    private fun errorState(documentResponse: Resource<PrintDocumentResponse>) = with(binding) {
        val errorTitle =
            documentResponse.messageTitle ?: getString(R.string.cashdesk_analytics_loading_error)

        val errorMessage =
            documentResponse.message ?: getString(R.string.cashdesk_analytics_try_later)
        hideFooter()
        dismissLoadingFragment()
        progressBar.isVisible = false
        showErrorMessage(errorTitle, errorMessage)
    }

    private fun showErrorMessage(errorTitle: String, errorMessage: String) = with(binding) {
        Snackbar.showNegativeSnackBar(
            activity = requireActivity(),
            subtitleText = errorMessage,
            rootView = requireView(),
            titleText = errorTitle
        )
        webView.visibility = View.GONE
        emptyContentMsg.isVisible = true
        emptyContentMsg.text = errorMessage
    }

    private fun changedHeaderHtml(htmlText: String): String =
        "$FULL_WEB_VIEW_OPEN_TAG $htmlText $FULL_WEB_VIEW_CLOSE_TAG"

    private fun renderWebView(content: String) = with(binding) {
        val mimeType = "text/html; charset=UTF-8"
        val encoding = "utf-8"
        with(webView) {
            settings.useWideViewPort = true
            settings.displayZoomControls = false
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = true
            settings.javaScriptEnabled = true

            val changeFontHtml: String = changedHeaderHtml(content)
            loadDataWithBaseURL(null, changeFontHtml, mimeType, encoding, null)
        }
        toolbar.visibility = View.VISIBLE
    }

    private fun createDocument() = with(binding.webView) {
        val uuid = generateUUID()
        val file = createWebPrintJob(this, uuid, callback, requireContext())
        val fullPath = File("${file}/$RECEIPT_NAME$uuid.$PDF")
        uri = FileProvider.getUriForFile(
            requireContext(), requireContext().packageName, fullPath
        )
    }

    private fun sendDocument() = with(documentViewModel) {
        val id = if (isInitDocument == true) documentResponse.value.data?.billId
        else getCurrentDocument()?.data?.billId
        id ?: return@with
        val webReceiptUrl = WEB_RECEIPT_URL.plus(id)
        val intent = Intent(Intent.ACTION_SEND)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_TEXT, webReceiptUrl)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(intent)
    }

    private fun getCurrentDocument(): Resource<PrintDocumentResponse>? {
        val currentDocumentParams = documentViewModel.params.value?.first()
        return documentViewModel.webViews[currentDocumentParams]
    }

    private fun showLoadingFragment() =
        CashdeskLoadingFragment().show(childFragmentManager, LOADING_FRAGMENT_KEY)

    private fun dismissLoadingFragment() {
        val loadingFragment = childFragmentManager.findFragmentByTag(LOADING_FRAGMENT_KEY)
        if (loadingFragment != null) (loadingFragment as DialogFragment).dismissNow()
    }

    companion object {
        private const val SHOW_DOC_PARAM_KEY = "ru.taxcom.cashdeskkit.document.DocumentFragment"
        private const val RECEIPT_ID = "receipt_id"
        private const val FULL_WEB_VIEW_OPEN_TAG =
            "<head><meta name=\"viewport\" content=\"width=device-width, user-scalable=yes\" /></head>"
        private const val FULL_WEB_VIEW_CLOSE_TAG = "</body></html>"
        private const val LOADING_FRAGMENT_KEY = "LOADING_FRAGMENT_KEY"
        private const val FIRST_DOCUMENT_POSITION = 0
        private const val NEXT_DOCUMENT_POSITION = 1
        private const val DEFAULT_INIT_COUNT_OF_CASHED_DOCUMENTS = 3
        private const val BOTTOM_PADDING_VALUE = 200
        private const val IS_ENABLE_SWIPE = "is_enable_swipe"

        fun newFragment(
            document: DocumentParams, receiptId: String, isEnableSwipe: Boolean? = false
        ): DocumentFragment = DocumentFragment().apply {
            arguments = bundleOf(
                SHOW_DOC_PARAM_KEY to Json.encodeToString(document),
                RECEIPT_ID to Json.encodeToString(receiptId),
                IS_ENABLE_SWIPE to isEnableSwipe
            )
        }
    }
}