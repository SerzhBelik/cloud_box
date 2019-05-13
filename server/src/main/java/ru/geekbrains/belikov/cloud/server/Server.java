package ru.geekbrains.belikov.cloud.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import ru.geekbrains.belikov.cloud.common.Auth;

public class Server {


    private static class AuthHandler extends ChannelInboundHandlerAdapter{
        private static boolean isAuth = false;
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {



           if (msg instanceof Auth){
               Auth auth = (Auth) msg;
               auth.setAuth(AuthService.checkUser(auth.getLogin(), auth.getPassword()));
               isAuth = auth.isAuth();
               ctx.fireChannelRead(msg);
               ctx.writeAndFlush(msg);
           }

            if (isAuth){
                ctx.fireChannelRead(msg);
                return;
            }
        }
    }

    public void run() throws Exception {
        EventLoopGroup mainGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(mainGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(
                                    new ObjectDecoder(50 * 1024 * 1024, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new AuthHandler()
                                    ,new MainHandler()
                            );
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = b.bind(8189).sync();
            future.channel().closeFuture().sync();
        } finally {
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new Server().run();

    }
}
