package com.kongzhong.mrpc.transport;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.kongzhong.mrpc.client.RpcFuture;
import com.kongzhong.mrpc.client.cluster.Connections;
import com.kongzhong.mrpc.common.thread.RpcThreadPool;
import com.kongzhong.mrpc.model.RpcRequest;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 抽象客户端请求处理器
 *
 * @author biezhi
 *         2017/4/19
 */
public abstract class SimpleClientHandler<T> extends SimpleChannelInboundHandler<T> {

    public static final Logger log = LoggerFactory.getLogger(SimpleClientHandler.class);

    private static ListeningExecutorService TPE = MoreExecutors.listeningDecorator((ThreadPoolExecutor) RpcThreadPool.getExecutor(16, -1));

    protected Map<String, RpcFuture> mapCallBack = new ConcurrentHashMap<>();

    protected volatile Channel channel;

    protected SocketAddress socketAddress;

    protected String serverAddress;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.socketAddress = this.channel.remoteAddress();
        log.debug("Channel actived");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Connections.me().remove(this);

        log.debug("Channel inactive: {}", this.channel);
        // 创建异步重连
//        final EventLoop eventLoopGroup = this.channel.eventLoop();
//        Set<String> referNames = Sets.newHashSet();
//        List<Class<?>> referers = ClientConfig.me().getReferers();
//        if (null != referers && !referers.isEmpty()) {
//            referers.forEach(type -> referNames.add(type.getName()));
//        }
//
//        System.out.println("提交重连请求");
//        TPE.submit(new SimpleRequestCallback(referNames, eventLoopGroup, this.channel.remoteAddress()));
    }

    /**
     * handler 中出现异常才会执行这个函数
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("", cause);
        ctx.close();
    }

    /**
     * 客户端关闭时调用
     */
    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    public abstract RpcFuture sendRequest(RpcRequest request);

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }
}