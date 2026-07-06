package com.example.tieuluanandroids.ui.webview

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ApiWebViewFragment : Fragment() {

    private lateinit var webView: WebView
    private val capturedPortalTokens = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_api_webview, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.web_view_api)
        lifecycleScope.launch {
            val credentials = app.sessionManager.getCredentials()
            if (credentials == null) {
                Toast.makeText(
                    requireContext(),
                    "Login is required before opening WebView",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().navigate(R.id.LoginFragment)
                return@launch
            }
            configureWebView()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.addJavascriptInterface(PortalApiHook(), "PortalApiHook")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                injectLoginApiHook()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return false
            }
        }
        webView.loadUrl(PORTAL_URL)
    }

    private inner class PortalApiHook {
        @JavascriptInterface
        fun onLoginApi(
            url: String,
            method: String,
            requestBody: String?,
            status: Int,
            responseText: String,
            authorizationHeader: String?
        ) {
            val prettyResponse = runCatching {
                JSONObject(responseText).toString(2)
            }.getOrDefault(responseText)

            Log.d(TAG, "Login API $method $url")
            Log.d(TAG, "Login API request: ${requestBody.orEmpty()}")
            Log.d(TAG, "Login API status: $status")
            Log.d(TAG, "Login API response: $prettyResponse")
            Log.d(TAG, "Login API authorization header captured: ${!authorizationHeader.isNullOrBlank()}")

            val authorization = extractPortalToken(responseText) ?: authorizationHeader
            if (status !in 200..299) {
                Log.w(TAG, "Skip portal credential save because login API failed with status=$status")
            } else if (authorization.isNullOrBlank()) {
                Log.w(TAG, "Skip portal credential save because Authorization was not captured")
            } else {
                savePortalCredential(authorization)
            }

            activity?.runOnUiThread {
                val context = context ?: return@runOnUiThread
                Toast.makeText(
                    context,
                    "Login API status: $status",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun savePortalCredential(authorization: String) {
        val credentialKey = authorization.trim()
        synchronized(capturedPortalTokens) {
            if (!capturedPortalTokens.add(credentialKey)) {
                Log.d(TAG, "Skip duplicated Authorization credential")
                return
            }
        }

        lifecycleScope.launch {
            val result = app.data.savePortalCredential(
                authorization = authorization,
                cookie = null,
                csrfToken = null
            )
            val message = if (result.success) {
                "Portal credential saved"
            } else {
                "Cannot save portal credential: ${result.message}"
            }
            context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
            if (result.success) {
                Log.d(TAG, message)
            } else {
                Log.w(TAG, message)
            }
        }
    }

    private fun extractPortalToken(responseText: String): String? {
        val json = runCatching { JSONObject(responseText) }.getOrNull() ?: return null
        return findToken(json)
    }

    private fun findToken(value: Any?): String? {
        return when (value) {
            is JSONObject -> {
                TOKEN_KEYS.firstNotNullOfOrNull { key ->
                    value.optString(key).takeIf { it.isNotBlank() }
                } ?: value.keys().asSequence()
                    .mapNotNull { key -> findToken(value.opt(key)) }
                    .firstOrNull()
            }
            is JSONArray -> (0 until value.length())
                .asSequence()
                .mapNotNull { index -> findToken(value.opt(index)) }
                .firstOrNull()
            is String -> value.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            else -> null
        }
    }

    private val app: SmartCalendarApplication
        get() = requireActivity().application as SmartCalendarApplication

    private fun injectLoginApiHook() {
        webView.evaluateJavascript(loginApiHookScript(), null)
    }

    private fun loginApiHookScript(): String = """
        (function() {
            if (window.__portalLoginHookInstalled) return;
            window.__portalLoginHookInstalled = true;

            const LOGIN_PATH = '/api/v1/user/login';

            function isLoginUrl(url) {
                try {
                    return String(url || '').indexOf(LOGIN_PATH) !== -1;
                } catch (e) {
                    return false;
                }
            }

            function bodyToText(body) {
                if (body == null) return '';
                if (typeof body === 'string') return body;
                try { return JSON.stringify(body); } catch (e) { return String(body); }
            }

            function safeHeader(headers, name) {
                try { return headers && headers.get ? (headers.get(name) || '') : ''; } catch (e) { return ''; }
            }

            const originalFetch = window.fetch;
            if (originalFetch) {
                window.fetch = function(input, init) {
                    const url = typeof input === 'string' ? input : (input && input.url);
                    const method = (init && init.method) || (input && input.method) || 'GET';
                    const requestBody = init && init.body ? bodyToText(init.body) : '';

                    return originalFetch.apply(this, arguments).then(function(response) {
                        if (isLoginUrl(url)) {
                            response.clone().text().then(function(text) {
                                PortalApiHook.onLoginApi(
                                    String(url),
                                    String(method),
                                    requestBody,
                                    response.status,
                                    text,
                                    safeHeader(response.headers, 'Authorization')
                                );
                            }).catch(function(error) {
                                PortalApiHook.onLoginApi(
                                    String(url),
                                    String(method),
                                    requestBody,
                                    response.status,
                                    'Cannot read response: ' + error,
                                    safeHeader(response.headers, 'Authorization')
                                );
                            });
                        }
                        return response;
                    });
                };
            }

            const originalOpen = XMLHttpRequest.prototype.open;
            const originalSend = XMLHttpRequest.prototype.send;

            XMLHttpRequest.prototype.open = function(method, url) {
                this.__portalHookMethod = method;
                this.__portalHookUrl = url;
                return originalOpen.apply(this, arguments);
            };

            XMLHttpRequest.prototype.send = function(body) {
                const xhr = this;
                const requestBody = bodyToText(body);
                xhr.addEventListener('load', function() {
                    if (isLoginUrl(xhr.__portalHookUrl)) {
                        PortalApiHook.onLoginApi(
                            String(xhr.__portalHookUrl),
                            String(xhr.__portalHookMethod || 'GET'),
                            requestBody,
                            xhr.status,
                            xhr.responseText || '',
                            xhr.getResponseHeader('Authorization') || ''
                        );
                    }
                });
                return originalSend.apply(this, arguments);
            };
        })();
    """.trimIndent()

    companion object {
        private const val TAG = "PortalWebView"
        private const val PORTAL_URL = "https://portal.ut.edu.vn/"
        private val TOKEN_KEYS = listOf(
            "access_token",
            "accessToken",
            "token",
            "authToken",
            "authorization",
            "Authorization"
        )
    }
}
