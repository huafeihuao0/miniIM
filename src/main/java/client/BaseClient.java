package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import com.alibaba.fastjson.JSON;

import json.client.GetPubKey;
import client.message.ClientHttpRequestFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.example.http.websocketx.client.WebSocketClientHandler;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.codec.http2.DefaultHttp2Headers;


public class BaseClient {
    

	static final String HOST = System.getProperty("HOST", "127.0.0.1");
	static final int PORT =  Integer.parseInt(System.getProperty("port", "8080"));
	static final String URL = System.getProperty("url", "ws://127.0.0.1:8080/websocket");
    /*public static void main(String[] args) {
        
        EventLoopGroup group = new NioEventLoopGroup();
        try{
        	Bootstrap boot = new Bootstrap();
        	boot.group(group)
        		.channel(NioSocketChannel.class)
        		.handler(new BaseClientInitializer());
        	
        	Channel ch = boot.connect(HOST, PORT).sync().channel();

    		GetPubKey content = new GetPubKey();
    		content.setProcess("getPubKey");
    		ArrayList<String> supPubKey = new ArrayList<String>();
    		ArrayList<String> supSysKey = new ArrayList<String>();
    		supPubKey.add("RSA");
    		supSysKey.add("AES");
    		content.setSupPubKey(supPubKey);
    		content.setSupSysKey(supSysKey);
    		
        	ClientHttpRequestFactory initRequest = new ClientHttpRequestFactory();
        	initRequest.addUri("access/?process=getPubKey");
        	initRequest.addContent(JSON.toJSONString(content));
        	
        	HttpRequest request = initRequest.product();
            ch.writeAndFlush(request);

            ch.closeFuture().sync();
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
        	group.shutdownGracefully();
        }
	}*/
	
	public static void main(String[] args) throws URISyntaxException, InterruptedException, IOException {
		URI uri = new URI(URL);
		String scheme = uri.getScheme() ==null?"ws":uri.getScheme();
		final String host = uri.getHost() == null ?"127.0.0.1":uri.getHost();
		final int port;
		if(uri.getPort() == -1){
			if("ws".equalsIgnoreCase(scheme)){
				port = 80;
			}else{
				port = -1;
			}
		}else{
			port = uri.getPort();
		}
		
		if(!"ws".equalsIgnoreCase(scheme)){
			System.err.println("Only WS(S) is supported");
			return;
		}
		
		EventLoopGroup group = new NioEventLoopGroup();
		try{
			final MyWebSocketClientHandler handler = 
					new MyWebSocketClientHandler(
							WebSocketClientHandshakerFactory.newHandshaker(
									uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()));
			Bootstrap b = new Bootstrap();
			b.group(group)
			.channel(NioSocketChannel.class)
			.handler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline p = ch.pipeline();
					p.addLast(
							new HttpClientCodec(),
							new HttpObjectAggregator(8192),
							WebSocketClientCompressionHandler.INSTANCE,
							handler);
				}
			});
			
			Channel ch = b.connect(uri.getHost(),port).sync().channel();
			handler.handshakeFuture().sync();
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			while(true){
				String msg = console.readLine();
				if(msg == null){
					break;
				}else if("bye".equals(msg.toLowerCase())) {
                    ch.writeAndFlush(new CloseWebSocketFrame());
                    ch.closeFuture().sync();
                    break;
                } else if ("ping".equals(msg.toLowerCase())) {
                    WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[] { 8, 1, 8, 1 }));
                    ch.writeAndFlush(frame);
                } else {
                    WebSocketFrame frame = new TextWebSocketFrame(msg);
                    ch.writeAndFlush(frame);
                }
			}
		}finally{
			group.shutdownGracefully();
		}
	}
}