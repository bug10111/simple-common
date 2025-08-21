package com.simple.common.websocket.initializer;

import com.simple.common.websocket.common.constant.WebSocketConstant;
import com.simple.common.websocket.handler.WebSocketAuthHandler;
import com.simple.common.websocket.handler.WebSocketServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
public class WebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

    protected void initChannel(SocketChannel socketChannel) {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast("http-codec", new HttpServerCodec());
        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
        pipeline.addLast("http-chunked", new ChunkedWriteHandler());
        pipeline.addLast(new IdleStateHandler(120, 0, 0));
        pipeline.addLast(new WebSocketAuthHandler());
        pipeline.addLast(new WebSocketServerHandler());
        pipeline.addLast(new WebSocketServerProtocolHandler(WebSocketConstant.WEBSOCKET_PATH, true, 65536 * 10));
    }
}
