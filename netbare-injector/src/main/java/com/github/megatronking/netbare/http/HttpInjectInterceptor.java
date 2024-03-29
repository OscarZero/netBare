/*  NetBare - An android network capture and injection library.
 *  Copyright (C) 2018-2019 Megatron King
 *  Copyright (C) 2018-2019 GuoShi
 *
 *  NetBare is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU General Public License as published by the Free Software Found-
 *  ation, either version 3 of the License, or (at your option) any later version.
 *
 *  NetBare is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with NetBare.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.megatronking.netbare.http;

import android.net.Uri;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.megatronking.netbare.injector.HttpInjector;
import com.github.megatronking.netbare.io.ByteBufferInputStream;
import com.github.megatronking.netbare.io.HttpBodyInputStream;
import com.github.megatronking.netbare.utils.TransferEncoding;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * A HTTP inject interceptor holds a {@link HttpInjector}.
 *
 * @author Megatron King
 * @since 2018-12-29 22:25
 */
public final class HttpInjectInterceptor extends HttpIndexedInterceptor {

    private boolean mShouldInjectRequest;
    private boolean mShouldInjectResponse;

    public static HashSet<String> blackList;

    private HttpInjector mHttpInjector;

    private HttpInjectInterceptor(HttpInjector httpInjector) {
        this.mHttpInjector = httpInjector;
    }

    @Override
    protected final void intercept(@NonNull final HttpRequestChain chain,
                                   @NonNull ByteBuffer buffer, int index) throws IOException {
        if (chain.request().uid() == Process.myUid()) {
            chain.process(buffer);
            return;
        }
        Log.e("netbare","@@@@@@@@ host : " + chain.request().host()+"/ip"+chain.request().ip()+"/port"+chain.request().port());

        if(HttpInjectInterceptor.blackList.contains(chain.request().host())){
            Log.e("netbare","@@@@@@@@ host : " + chain.request().host()+"  reject request");
            return;
        }
        if (index == 0) {
            mShouldInjectRequest = mHttpInjector.sniffRequest(chain.request());
        }
        if (!mShouldInjectRequest) {
            mHttpInjector.intercept(chain.request(),buffer,index,buildHeader(chain.request()).toHeaders());
            chain.process(buffer);
            return;
        }
        if (index == 0) {
            mHttpInjector.onRequestInject(buildHeader(chain.request()), new HttpRequestInjectorCallback(chain));
        } else {
            mHttpInjector.onRequestInject(chain.request(), new HttpRawBody(buffer), new HttpRequestInjectorCallback(chain));
        }
    }

    @Override
    protected final void intercept(@NonNull final HttpResponseChain chain,
                                   @NonNull ByteBuffer buffer, int index) throws IOException {
        if (chain.response().uid() == Process.myUid()) {
            chain.process(buffer);
            return;
        }
        if (index == 0) {
            mShouldInjectResponse = mHttpInjector.sniffResponse(chain.response());
        }
        //Transfer-Encoding: chunked
//        {
//            String headers = buildHeader(chain.response()).toHeaders();
//
//            StringBuilder stringBuffer = new StringBuilder();
//            try {
////                    ByteBufferInputStream bais = new ByteBufferInputStream(buffer);
//                HttpBodyInputStream his = null;
//                Reader reader = null;
//                his = new HttpBodyInputStream(new HttpRawBody(ByteBuffer.wrap(buffer.array())));
//                try {
//                    if(headers.contains("gzip")){
//                        Log.e("netbare","#################-----@@@@@@@--");
//                        String body = TransferEncoding.readGzipBodyToString(new GZIPInputStream(his));
//                        Log.e("netbare","#################-------"+body);
//                    }
//                    reader = new InputStreamReader(new GZIPInputStream(his), "UTF-8");
//                } catch (Exception ex) {
//                    reader = new InputStreamReader(his, "UTF-8");
//                }
//                BufferedReader in = new BufferedReader(reader);
//
//                String line = " ";
//                while ((line = in.readLine()) != null) {
////                        String succeedStr = new String(line.getBytes(), StandardCharsets.UTF_8);
//                    stringBuffer.append(line);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            Log.e("netbare", "############ headers : " + headers);
//            Log.e("netbare", "############ host : " + chain.response().host() + "  /  ip= " + chain.response().ip() + "/body = " + stringBuffer.toString());
//        }

        if (!mShouldInjectResponse) {
            if (index == 0) {
                mHttpInjector.intercept(chain.response(),buffer,index,buildHeader(chain.response()).toHeaders());
            }else {
                StringBuilder stringBuffer = new StringBuilder();
                try{
//                    ByteBufferInputStream bais = new ByteBufferInputStream(buffer);
                    HttpBodyInputStream his = null;
                    Reader reader = null;
                    his = new HttpBodyInputStream(new HttpRawBody(ByteBuffer.wrap(buffer.array())));

                    try {
                        reader = new InputStreamReader(new GZIPInputStream(his),"UTF-8");
                    } catch (Exception ex) {
//                        Log.e("netbare", "123123_b1"+ex );
                        reader = new InputStreamReader(his,"UTF-8");
                    }
                    BufferedReader in = new BufferedReader(reader);

                    String line = " ";
                    while ((line = in.readLine()) != null) {
//                        Log.e("netbare", "123123 : "+line );
                        stringBuffer.append(line);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                mHttpInjector.intercept(chain.response(),buffer,index,stringBuffer.toString());
            }
            chain.process(buffer);
            return;
        }
        if (index == 0) {
            mHttpInjector.onResponseInject(buildHeader(chain.response()),
                    new HttpResponseInjectorCallback(chain));
        } else {
            mHttpInjector.onResponseInject(chain.response(), new HttpRawBody(buffer),
                    new HttpResponseInjectorCallback(chain));
        }
    }

    @Override
    public void onRequestFinished(@NonNull HttpRequest request) {
        super.onRequestFinished(request);
        if (mShouldInjectRequest) {
            mHttpInjector.onRequestFinished(request);
        }
        mShouldInjectRequest = false;
    }

    @Override
    public void onResponseFinished(@NonNull HttpResponse response) {
        super.onResponseFinished(response);
        if (mShouldInjectResponse) {
            mHttpInjector.onResponseFinished(response);
        }
        mShouldInjectResponse = false;
    }

    private HttpRequestHeaderPart buildHeader(HttpRequest request) {
        return new HttpRequestHeaderPart.Builder(request.httpProtocol(), Uri.parse(request.url()),
                request.requestHeaders(), request.method(),request.id())
                .build();
    }

    private HttpResponseHeaderPart buildHeader(HttpResponse response) {
        return new HttpResponseHeaderPart.Builder(response.httpProtocol(), Uri.parse(response.url()),
                response.responseHeaders(), response.code(), response.message(),response.id())
                .build();
    }

    /**
     * A factory produces {@link HttpInjectInterceptor} instance.
     *
     * @param httpInjector A HTTP injector.
     * @return An instance of {@link HttpInjectInterceptor}.
     */
    public static HttpInterceptorFactory createFactory(final HttpInjector httpInjector) {
        return new HttpInterceptorFactory() {

            @NonNull
            @Override
            public HttpInterceptor create() {
                return new HttpInjectInterceptor(httpInjector);
            }

        };
    }

}
