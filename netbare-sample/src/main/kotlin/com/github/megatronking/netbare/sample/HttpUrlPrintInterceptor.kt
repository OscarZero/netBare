package com.github.megatronking.netbare.sample

import com.github.megatronking.netbare.http.HttpIndexedInterceptor
import com.github.megatronking.netbare.http.HttpInterceptorFactory
import com.github.megatronking.netbare.http.HttpRequestChain
import com.github.megatronking.netbare.http.HttpResponseChain
import com.github.megatronking.netbare.sample.util.Logger
import java.nio.ByteBuffer


/**
 * 拦截器反例1：打印请求url
 *
 * @author Megatron King
 * @since 2019/1/2 22:05
 */
class HttpUrlPrintInterceptor : HttpIndexedInterceptor() {

    companion object {
        const val TAG = "URL"

        fun createFactory(): HttpInterceptorFactory {
            return HttpInterceptorFactory { HttpUrlPrintInterceptor() }
        }
    }


    override fun intercept(chain: HttpRequestChain, buffer: ByteBuffer, index: Int) {
        if (index == 0) {
            // 一个请求可能会有多个数据包，故此方法会多次触发，这里只在收到第一个包的时候打印
            Logger.e("url==========indxt $index:" + chain.request().url())
        }
        // 调用process将数据发射给下一个拦截器，否则数据将不会发给服务器
        chain.process(buffer)
    }

    override fun intercept(chain: HttpResponseChain, buffer: ByteBuffer, index: Int) {
        Logger.e("Response "+chain.response().toString())
        chain.process(buffer)
    }

}
