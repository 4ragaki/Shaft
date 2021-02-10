package ceui.lisa.fragments;

import android.content.Intent;
import android.net.SSLCertificateSocketFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import com.google.android.material.snackbar.Snackbar;
import com.just.agentweb.AgentWeb;
import com.just.agentweb.WebViewClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import ceui.lisa.R;
import ceui.lisa.activities.OutWakeActivity;
import ceui.lisa.activities.UserActivity;
import ceui.lisa.databinding.FragmentWebviewBinding;
import ceui.lisa.http.HttpDns;
import ceui.lisa.http.RubySSLSocketFactory;
import ceui.lisa.utils.ClipBoardUtils;
import ceui.lisa.utils.Common;
import ceui.lisa.utils.Params;
import ceui.lisa.utils.PixivOperate;
import ceui.lisa.view.ContextMenuTitleView;

import static ceui.lisa.activities.Shaft.sUserModel;

public class FragmentWebView extends BaseFragment<FragmentWebviewBinding> {

    //private static final String ILLUST_HEAD = "https://www.pixiv.net/member_illust.php?mode=medium&illust_id=";
    private static final String USER_HEAD = "https://www.pixiv.net/member.php?id=";
    private static final String WORKS_HEAD = "https://www.pixiv.net/artworks/";
    private static final String PIXIV_HEAD = "https://www.pixiv.net/";
    private static final String TAG = "FragmentWebView";
    private String title;
    private String url;
    private String response = null;
    private String mime = null;
    private String encoding = null;
    private String historyUrl = null;
    private boolean preferPreserve = false;
    private AgentWeb mAgentWeb;
    private WebView mWebView;
    private String mIntentUrl;
    private WebViewClickHandler handler = new WebViewClickHandler();
    private HttpDns httpDns = HttpDns.getInstance();

    @Override
    public void initBundle(Bundle bundle) {
        title = bundle.getString(Params.TITLE);
        url = bundle.getString(Params.URL);
        response = bundle.getString(Params.RESPONSE);
        mime = bundle.getString(Params.MIME);
        encoding = bundle.getString(Params.ENCODING);
        historyUrl = bundle.getString(Params.HISTORY_URL);
        preferPreserve = bundle.getBoolean(Params.PREFER_PRESERVE);
    }

    public static FragmentWebView newInstance(String title, String url) {
        Bundle args = new Bundle();
        args.putString(Params.TITLE, title);
        args.putString(Params.URL, url);
        FragmentWebView fragment = new FragmentWebView();
        fragment.setArguments(args);
        return fragment;
    }

    public static FragmentWebView newInstance(String title, String url, boolean preferPreserve) {
        Bundle args = new Bundle();
        args.putString(Params.TITLE, title);
        args.putString(Params.URL, url);
        args.putBoolean(Params.PREFER_PRESERVE, preferPreserve);
        FragmentWebView fragment = new FragmentWebView();
        fragment.setArguments(args);
        return fragment;
    }

    public static FragmentWebView newInstance(String title, String url, String response,
                                              String mime, String encoding, String history_url) {
        Bundle args = new Bundle();
        args.putString(Params.TITLE, title);
        args.putString(Params.URL, url);
        args.putString(Params.RESPONSE, response);
        args.putString(Params.MIME, mime);
        args.putString(Params.ENCODING, encoding);
        args.putString(Params.HISTORY_URL, history_url);
        FragmentWebView fragment = new FragmentWebView();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void initLayout() {
        mLayoutID = R.layout.fragment_webview;
    }

    @Override
    public void initView() {
        baseBind.toolbarTitle.setText(title);
        baseBind.toolbar.setNavigationOnClickListener(v -> mActivity.finish());
    }

    @Override
    protected void initData() {
        AgentWeb.PreAgentWeb ready = AgentWeb.with(this)
                .setAgentWebParent(baseBind.webViewParent, new RelativeLayout.LayoutParams(-1, -1))
                .useDefaultIndicator()
                .setWebViewClient(new WebViewClient() {
//                    @Override
//                    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
//                        String scheme = request.getUrl().getScheme().trim();
//                        String method = request.getMethod();
//                        Map<String, String> headerFields = request.getRequestHeaders();
//                        String url = request.getUrl().toString();
//                        Log.e(TAG, "url:" + url);
//                        // 无法拦截body，拦截方案只能正常处理不带body的请求；
//                        if ((scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
//                                && method.equalsIgnoreCase("get")) {
//                            try {
//                                URLConnection connection = recursiveRequest(url, headerFields, null);
//
//                                if (connection == null) {
//                                    Log.e(TAG, "connection null");
//                                    return super.shouldInterceptRequest(view, request);
//                                }
//
//                                // 注*：对于POST请求的Body数据，WebResourceRequest接口中并没有提供，这里无法处理
//                                String contentType = connection.getContentType();
//                                String mime = getMime(contentType);
//                                String charset = getCharset(contentType);
//                                HttpURLConnection httpURLConnection = (HttpURLConnection)connection;
//                                int statusCode = httpURLConnection.getResponseCode();
//                                String response = httpURLConnection.getResponseMessage();
//                                Map<String, List<String>> headers = httpURLConnection.getHeaderFields();
//                                Set<String> headerKeySet = headers.keySet();
//                                Log.e(TAG, "code:" + httpURLConnection.getResponseCode());
//                                Log.e(TAG, "mime:" + mime + "; charset:" + charset);
//
//
//                                // 无mime类型的请求不拦截
//                                if (TextUtils.isEmpty(mime)) {
//                                    Log.e(TAG, "no MIME");
//                                    return super.shouldInterceptRequest(view, request);
//                                } else {
//                                    // 二进制资源无需编码信息
//                                    if (!TextUtils.isEmpty(charset) || (isBinaryRes(mime))) {
//                                        WebResourceResponse resourceResponse = new WebResourceResponse(mime, charset, httpURLConnection.getInputStream());
//                                        resourceResponse.setStatusCodeAndReasonPhrase(statusCode, response);
//                                        Map<String, String> responseHeader = new HashMap<String, String>();
//                                        for (String key: headerKeySet) {
//                                            // HttpUrlConnection可能包含key为null的报头，指向该http请求状态码
//                                            responseHeader.put(key, httpURLConnection.getHeaderField(key));
//                                        }
//                                        resourceResponse.setResponseHeaders(responseHeader);
//                                        return resourceResponse;
//                                    } else {
//                                        Log.e(TAG, "non binary resource for " + mime);
//                                        return super.shouldInterceptRequest(view, request);
//                                    }
//                                }
//                            } catch (MalformedURLException e) {
//                                e.printStackTrace();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        return super.shouldInterceptRequest(view, request);
//                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        String destiny = request.getUrl().toString();
                        Common.showLog(className + "destiny " + destiny);
                        if (destiny.contains(PIXIV_HEAD)) {
                            if (destiny.contains("logout.php")) {
                                return false;
                            } else {
                                try {

                                    Intent intent = new Intent(mContext, OutWakeActivity.class);
                                    intent.setData(Uri.parse(destiny));
                                    startActivity(intent);
                                    if (!preferPreserve) {
                                        finish();
                                    }
                                } catch (Exception e) {
                                    Common.showToast(e.toString());
                                    e.printStackTrace();
                                }
                                return true;
                            }
                        }

                        return super.shouldOverrideUrlLoading(view, request);
                    }
                })
                .createAgentWeb()
                .ready();

        if (response == null) {
            mAgentWeb = ready.go(url);
        } else {
            mAgentWeb = ready.get();
            mAgentWeb.getUrlLoader().loadDataWithBaseURL(url, response, mime, encoding, historyUrl);
        }

        mWebView = mAgentWeb.getWebCreator().getWebView();
        WebSettings settings = mWebView.getSettings();
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        registerForContextMenu(mWebView);
    }

    @Override
    public void onPause() {
        mAgentWeb.getWebLifeCycle().onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mAgentWeb.getWebLifeCycle().onDestroy();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        mAgentWeb.getWebLifeCycle().onResume();
        super.onResume();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        WebView.HitTestResult result = mWebView.getHitTestResult();
        mIntentUrl = result.getExtra();
        menu.setHeaderView(new ContextMenuTitleView(mContext, mIntentUrl, Common.resolveThemeAttribute(mContext, R.attr.colorPrimary)));

        if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
            mIntentUrl = result.getExtra();
            //menu.setHeaderTitle(mIntentUrl);
            menu.add(Menu.NONE, WebViewClickHandler.OPEN_IN_BROWSER, 0, R.string.webview_handler_open_in_browser).setOnMenuItemClickListener(handler);
            menu.add(Menu.NONE, WebViewClickHandler.COPY_LINK_ADDRESS, 1, R.string.webview_handler_copy_link_addr).setOnMenuItemClickListener(handler);
            menu.add(Menu.NONE, WebViewClickHandler.COPY_LINK_TEXT, 1, R.string.webview_handler_copy_link_text).setOnMenuItemClickListener(handler);
            menu.add(Menu.NONE, WebViewClickHandler.DOWNLOAD_LINK, 1, R.string.webview_handler_download_link).setOnMenuItemClickListener(handler);
            menu.add(Menu.NONE, WebViewClickHandler.SHARE_LINK, 1, R.string.webview_handler_share).setOnMenuItemClickListener(handler);
        }

        if (result.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

            mIntentUrl = result.getExtra();
            //menu.setHeaderTitle(mIntentUrl);
            menu.add(Menu.NONE, WebViewClickHandler.OPEN_IN_BROWSER, 0, R.string.webview_handler_open_in_browser).setOnMenuItemClickListener(handler);
            menu.add(Menu.NONE, WebViewClickHandler.OPEN_IMAGE, 1, R.string.webview_handler_open_image).setOnMenuItemClickListener(handler);
            menu.add(Menu.NONE, WebViewClickHandler.DOWNLOAD_LINK, 2, R.string.webview_handler_download_link).setOnMenuItemClickListener(handler);
            menu.add(Menu.NONE, WebViewClickHandler.SEARCH_GOOGLE, 2, R.string.webview_handler_search_with_ggl).setOnMenuItemClickListener(handler);
            menu.add(Menu.NONE, WebViewClickHandler.SHARE_LINK, 2, R.string.webview_handler_share).setOnMenuItemClickListener(handler);

        }
    }

    public AgentWeb getAgentWeb() {
        return mAgentWeb;
    }

    public void setAgentWeb(AgentWeb agentWeb) {
        mAgentWeb = agentWeb;
    }

    public final class WebViewClickHandler implements MenuItem.OnMenuItemClickListener {
        static final int OPEN_IN_BROWSER = 0x0;
        static final int OPEN_IMAGE = 0x1;
        static final int COPY_LINK_ADDRESS = 0x2;
        static final int COPY_LINK_TEXT = 0x3;
        static final int DOWNLOAD_LINK = 0x4;
        static final int SEARCH_GOOGLE = 0x5;
        static final int SHARE_LINK = 0x6;

        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {

                case OPEN_IN_BROWSER: {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mIntentUrl));
                    mActivity.startActivity(intent);
                    break;
                }
                case OPEN_IMAGE: {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(mIntentUrl), "image/*");
                    mActivity.startActivity(intent);
                    break;
                }
                case COPY_LINK_ADDRESS: {
                    ClipBoardUtils.putTextIntoClipboard(mContext, mIntentUrl);
                    Snackbar.make(rootView, R.string.copy_to_clipboard, Snackbar.LENGTH_SHORT).show();
                    break;
                }
                case COPY_LINK_TEXT: {
                    Common.showToast("不会");
                    break;
                }
                case SEARCH_GOOGLE: {
                    mWebView.loadUrl("https://www.google.com/searchbyimage?image_url=" + mIntentUrl);
                    break;
                }
                case SHARE_LINK: {
                    Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse(mIntentUrl));
                    mActivity.startActivity(intent);
                    break;
                }
            }

            return true;
        }
    }





    /**
     * 从contentType中获取MIME类型
     * @param contentType
     * @return
     */
    private String getMime(String contentType) {
        if (contentType == null) {
            return null;
        }
        return contentType.split(";")[0];
    }

    /**
     * 从contentType中获取编码信息
     * @param contentType
     * @return
     */
    private String getCharset(String contentType) {
        if (contentType == null) {
            return null;
        }

        String[] fields = contentType.split(";");
        if (fields.length <= 1) {
            return null;
        }

        String charset = fields[1];
        if (!charset.contains("=")) {
            return null;
        }
        charset = charset.substring(charset.indexOf("=") + 1);
        return charset;
    }

    /**
     * 是否是二进制资源，二进制资源可以不需要编码信息
     * @param mime
     * @return
     */
    private boolean isBinaryRes(String mime) {
        if (mime.startsWith("image")
                || mime.startsWith("audio")
                || mime.startsWith("video")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * header中是否含有cookie
     * @param headers
     */
    private boolean containCookie(Map<String, String> headers) {
        for (Map.Entry<String, String> headerField : headers.entrySet()) {
            if (headerField.getKey().contains("Cookie")) {
                return true;
            }
        }
        return false;
    }

    public URLConnection recursiveRequest(String path, Map<String, String> headers, String reffer) {
        HttpURLConnection conn;
        URL url = null;
        try {
            url = new URL(path);
            conn = (HttpURLConnection) url.openConnection();
            // 异步接口获取IP
            String ip = "210.140.131.188";
            if (ip != null) {
                // 通过HTTPDNS获取IP成功，进行URL替换和HOST头设置
                Log.d(TAG, "Get IP: " + ip + " for host: " + url.getHost() + " from HTTPDNS successfully!");
                String newUrl = path.replaceFirst(url.getHost(), ip);
                conn = (HttpURLConnection) new URL(newUrl).openConnection();

                if (headers != null) {
                    for (Map.Entry<String, String> field : headers.entrySet()) {
                        conn.setRequestProperty(field.getKey(), field.getValue());
                    }
                }
                // 设置HTTP请求头Host域
                conn.setRequestProperty("Host", url.getHost());
            } else {
                return null;
            }
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(false);
            if (conn instanceof HttpsURLConnection) {
                final HttpsURLConnection httpsURLConnection = (HttpsURLConnection)conn;
                // sni场景，创建SSLScocket
                WebviewTlsSniSocketFactory sslSocketFactory = new WebviewTlsSniSocketFactory((HttpsURLConnection) conn);
                httpsURLConnection.setSSLSocketFactory(sslSocketFactory);
                // https场景，证书校验
                httpsURLConnection.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        String host = httpsURLConnection.getRequestProperty("Host");
                        if (null == host) {
                            host = httpsURLConnection.getURL().getHost();
                        }
                        return HttpsURLConnection.getDefaultHostnameVerifier().verify(host, session);
                    }
                });
            }
            int code = conn.getResponseCode();// Network block
            if (needRedirect(code)) {
                // 原有报头中含有cookie，放弃拦截
                if (containCookie(headers)) {
                    return null;
                }

                String location = conn.getHeaderField("Location");
                if (location == null) {
                    location = conn.getHeaderField("location");
                }

                if (location != null) {
                    if (!(location.startsWith("http://") || location
                            .startsWith("https://"))) {
                        //某些时候会省略host，只返回后面的path，所以需要补全url
                        URL originalUrl = new URL(path);
                        location = originalUrl.getProtocol() + "://"
                                + originalUrl.getHost() + location;
                    }
                    Log.e(TAG, "code:" + code + "; location:" + location + "; path" + path);
                    return recursiveRequest(location, headers, path);
                } else {
                    // 无法获取location信息，让浏览器获取
                    return null;
                }
            } else {
                // redirect finish.
                Log.e(TAG, "redirect finish");
                return conn;
            }
        } catch (MalformedURLException e) {
            Log.w(TAG, "recursiveRequest MalformedURLException");
        } catch (IOException e) {
            Log.w(TAG, "recursiveRequest IOException");
        } catch (Exception e) {
            Log.w(TAG, "unknow exception");
        }
        return null;
    }

    private boolean needRedirect(int code) {
        return code >= 300 && code < 400;
    }

    class WebviewTlsSniSocketFactory extends SSLSocketFactory {
        private final String TAG = WebviewTlsSniSocketFactory.class.getSimpleName();
        HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        private HttpsURLConnection conn;

        public WebviewTlsSniSocketFactory(HttpsURLConnection conn) {
            this.conn = conn;
        }

        @Override
        public Socket createSocket() throws IOException {
            return null;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            return null;
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
            return null;
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return null;
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return null;
        }

        // TLS layer

        @Override
        public String[] getDefaultCipherSuites() {
            return new String[0];
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return new String[0];
        }

        @Override
        public Socket createSocket(Socket plainSocket, String host, int port, boolean autoClose) throws IOException {
            String peerHost = this.conn.getRequestProperty("Host");
            if (peerHost == null)
                peerHost = host;
            Log.i(TAG, "customized createSocket. host: " + peerHost);
            InetAddress address = plainSocket.getInetAddress();
            if (autoClose) {
                // we don't need the plainSocket
                plainSocket.close();
            }
            // create and connect SSL socket, but don't do hostname/certificate verification yet
            SSLCertificateSocketFactory sslSocketFactory = (SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(0);
            SSLSocket ssl = (SSLSocket) sslSocketFactory.createSocket(address, port);

            // enable TLSv1.1/1.2 if available
            ssl.setEnabledProtocols(ssl.getSupportedProtocols());

            // set up SNI before the handshake
            Log.i(TAG, "Setting SNI hostname");
            sslSocketFactory.setHostname(ssl, peerHost);

            // verify hostname and certificate
            SSLSession session = ssl.getSession();

            if (!hostnameVerifier.verify(peerHost, session))
                throw new SSLPeerUnverifiedException("Cannot verify hostname: " + peerHost);

            Log.i(TAG, "Established " + session.getProtocol() + " connection with " + session.getPeerHost() +
                    " using " + session.getCipherSuite());

            return ssl;
        }
    }
}
