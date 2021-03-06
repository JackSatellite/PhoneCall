package com.proposeme.seven.phonecall.net;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * Describe: 使用netty框架构建音频传输客户端
 * 此代理只是监听本地端口，然后在发送数据的时候需要指定对方ip和对方端口
 */
public class NettyClient {

    private NettyReceiverHandler handler;
    private int port = BaseData.PORT;  // 监听端口

    private EventLoopGroup group;

    private static NettyClient sClient;


    private NettyClient() {
        init();
    }

    /**
     * 获取Client 单例对象
     * @return NettyClient
     */
    public static NettyClient getClient(){
        if (sClient == null){
            sClient = new NettyClient();
        }

        return sClient;
    }

    /**
     *  注册回调
     * @param callback 回调变量。
     */
    public void setFrameResultedCallback(NettyReceiverHandler.FrameResultedCallback callback) {
        if (handler != null){
            handler.setOnFrameCallback(callback);
        }
    }

    /**
     * 初始化Netty对象。
     */
    private void init() {
        //初始化receiverHandler.
        handler = new NettyReceiverHandler();

        //启动客户端进行发送数据
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bootstrap b = new Bootstrap();
                group = new NioEventLoopGroup();
                try {
                    //设置netty的连接属性。
                    b.group(group)
                            .channel(NioDatagramChannel.class) //异步的 UDP 连接
                            .option(ChannelOption.SO_BROADCAST, true)
                            .option(ChannelOption.SO_RCVBUF, 1024 * 1024)//接收区2m缓存
                            .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(65535))//加上这个，里面是最大接收、发送的长度
                            .handler(handler); //设置数据的处理器

                    b.bind(port).sync().channel().closeFuture().await();
                    //构造一个劲监听本地端口的netty代理
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    group.shutdownGracefully();
                }
            }
        }).start();
    }


    /**
     *  发送数据到指定IP地址，
     * @param targetIp 目标IP
     * @param data 数据
     * @param msgType 数据类型
     */
    public void UserIPSendData(String targetIp, Object data, String msgType) {
        //这个就是NettyReceiverHandler.sendData（）方法的调用，也即是说，在NettyReceiverHandler这里面可以
        //即是处理接受数据的也是处理，发送数据的。
        handler.sendData(targetIp, port, data, msgType);
    }

    /**
     * 断开连接
     * @return true 成功断开  or  false 断开失败
     */
    public boolean DisConnect(){
        return  handler.DisConnect();
    }

    /**
     * 优雅的关闭Netty对象。
     */
    public void shutDownBootstrap(){
        group.shutdownGracefully();
    }
}
