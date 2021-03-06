package com.kongzhong.mrpc.transport.http;

import com.kongzhong.mrpc.client.RpcFuture;
import com.kongzhong.mrpc.exception.HttpException;
import com.kongzhong.mrpc.model.RequestBody;
import com.kongzhong.mrpc.model.RpcRequest;
import com.kongzhong.mrpc.model.RpcResponse;
import com.kongzhong.mrpc.transport.SimpleClientHandler;
import com.kongzhong.mrpc.utils.JSONUtils;
import com.kongzhong.mrpc.utils.ReflectUtils;
import com.kongzhong.mrpc.utils.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author biezhi
 *         2017/4/19
 */
@Slf4j
public class HttpClientHandler extends SimpleClientHandler<FullHttpResponse> {

    /**
     * 每次客户端发送一次RPC请求的 时候调用.
     *
     * @param request
     * @return
     */
    @Override
    public RpcFuture sendRequest(RpcRequest rpcRequest) {

        RpcFuture rpcFuture = new RpcFuture(rpcRequest);
        mapCallBack.put(rpcRequest.getRequestId(), rpcFuture);

        RequestBody requestBody = new RequestBody();
        requestBody.setRequestId(rpcRequest.getRequestId());
        requestBody.setService(rpcRequest.getClassName());
        requestBody.setMethod(rpcRequest.getMethodName());
        requestBody.setParameters(Arrays.asList(rpcRequest.getParameters()));

        Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
        if (null != parameterTypes) {
            List<String> parameterTypesJSON = new ArrayList<>();
            for (Class<?> type : parameterTypes) {
                parameterTypesJSON.add(type.getName());
            }
            requestBody.setParameterTypes(parameterTypesJSON);
        }

        try {
            String sendBody = JSONUtils.toJSONString(requestBody);
            log.debug("request: {}", sendBody);

            DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/rpc");
            req.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE); // or HttpHeaders.Values.CLOSE
            req.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
            req.headers().add(HttpHeaders.Names.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
            ByteBuf bbuf = Unpooled.copiedBuffer(sendBody, StandardCharsets.UTF_8);
            req.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bbuf.readableBytes());
            req.content().clear().writeBytes(bbuf);

            channel.writeAndFlush(req);
        } catch (Exception e) {
            log.error("", e);
        }

        return rpcFuture;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse httpResponse) throws Exception {
        try {

            ByteBuf buf = httpResponse.content();
            byte[] resp = new byte[buf.readableBytes()];
            buf.readBytes(resp);
            String body = new String(resp, "UTF-8");

            if (StringUtils.isEmpty(body)) {
                return;
            }
            RpcResponse rpcResponse = JSONUtils.parseObject(body, RpcResponse.class);
            if (rpcResponse.getSuccess()) {
                log.debug("response: {}", body);
                Object result = rpcResponse.getResult();
                if (null != result && null != rpcResponse.getReturnType() && !rpcResponse.getReturnType().equals(Void.class)) {
                    Class<?> re = ReflectUtils.getClassType(rpcResponse.getReturnType());
                    rpcResponse.setResult(JSONUtils.parseObject(JSONUtils.toJSONString(result), re));
                }
            }
            RpcFuture rpcFuture = mapCallBack.get(rpcResponse.getRequestId());
            if (rpcFuture != null) {
                mapCallBack.remove(rpcResponse.getRequestId());
                rpcFuture.done(rpcResponse);
            }
        } catch (Exception e) {
            throw new HttpException("client read response error", e);
        }
    }
}