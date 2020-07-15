package com.viewscenes.netsupervisor.netty.server;

import com.viewscenes.netsupervisor.annotation.RpcService;
import com.viewscenes.netsupervisor.netty.server.codec.json.JSONDecoder;
import com.viewscenes.netsupervisor.netty.server.codec.json.JSONEncoder;
import com.viewscenes.netsupervisor.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 *  生产者服务Server类-Netty实现
 */
@Component
public class NettyServer implements ApplicationContextAware, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
    private static final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup(4);

    private Map<String, Object> serviceMap = new HashMap<>();

    @Value("${rpc.server.address}")
    private String serverAddress;

    @Autowired
    ServiceRegistry registry;

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }

    /**
     * 将RpcService注解的服务类缓存起来，方便消费者调用
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(RpcService.class);
        for (Object serviceBean:
             beans.values()) {
            Class<?> clazz = serviceBean.getClass();
            Class<?>[] interfaces =  clazz.getInterfaces();
            for (Class<?> inter : interfaces){
                String interfaceName = inter.getName();
                logger.info("加载服务类: {}", interfaceName);
                serviceMap.put(interfaceName, serviceBean);
            }
        }
    }
    public void start(){
        final NettyServerHandler handler = new NettyServerHandler(serviceMap);
        new Thread(() ->{
           try {
               ServerBootstrap bootstrap = new ServerBootstrap();
               bootstrap.group(bossGroup,workerGroup).
                       channel(NioServerSocketChannel.class).
                       option(ChannelOption.SO_BACKLOG,1024).
                       childOption(ChannelOption.SO_KEEPALIVE,true).
                       childOption(ChannelOption.TCP_NODELAY,true).
                       childHandler(new ChannelInitializer<SocketChannel>() {
                           //创建NIOSocketChannel成功后，在进行初始化时，将它的ChannelHandler设置到ChannelPipeline中，用于处理网络IO事件
                           protected void initChannel(SocketChannel channel) throws Exception {
                               ChannelPipeline pipeline = channel.pipeline();
                               //超时检测
                               pipeline.addLast(new IdleStateHandler(0, 0, 60));
                               pipeline.addLast(new JSONEncoder());
                               pipeline.addLast(new JSONDecoder());
                              pipeline.addLast(handler);
                           }
                       });
               String[] array = serverAddress.split(":");
               String host = array[0];
               int port = Integer.parseInt(array[1]);
               ChannelFuture cf = bootstrap.bind(host,port).sync();
               logger.info("RPC 服务器启动.监听端口:"+port);
               registry.register(serverAddress);
               //等待服务端监听端口关闭
               cf.channel().closeFuture().sync();
           } catch (InterruptedException e) {
              logger.error(e.getMessage());
           } finally {
               bossGroup.shutdownGracefully();
               workerGroup.shutdownGracefully();
           }
        }).start();
    }
}