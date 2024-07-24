package com.zyhant.netty.mqtt;

import com.zyhant.netty.common.NettyServer;
import com.zyhant.netty.mqtt.domain.MqttConfig;
import com.zyhant.netty.mqtt.domain.MqttLogin;
import com.zyhant.netty.mqtt.domain.MqttPublish;
import com.zyhant.netty.mqtt.core.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * @author zyhant
 * @date 2024/7/23 20:28
 */
public abstract class MqttServer extends NettyServer<MqttConfig> {

    private static final Logger log = LoggerFactory.getLogger(MqttServer.class);

    public MqttServer(MqttConfig config) {
        super(config);
    }

    @Override
    protected void initPipeline(ChannelPipeline pipeline) {
        pipeline.addLast("encoder", MqttEncoder.INSTANCE);
        pipeline.addLast("decoder", new MqttDecoder());
        pipeline.addLast(new MqttHandler());
    }

    protected abstract boolean onLogin(MqttLogin login);

    protected abstract void onPublish(MqttPublish publish);

    protected abstract Collection<String> onSubscribeTopics();

    private class MqttHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof MqttMessage)) {
                return;
            }
            MqttMessage message = (MqttMessage) msg;
            if (message.fixedHeader() == null) {
                log.error("MqttServer错误,Header为空");
                return;
            }
            MqttFixedHeader header = message.fixedHeader();
            switch (header.messageType()) {
                case CONNECT:
                    if (message instanceof MqttConnectMessage) {
                        ConnectAckProcessor.handle(ctx, (MqttConnectMessage) message, config, MqttServer.this::onLogin);
                    }
                    break;
                case PUBLISH:
                    if (message instanceof MqttPublishMessage) {
                        PublishAckProcessor.handle(ctx, (MqttPublishMessage) message, config, MqttServer.this::onPublish);
                    }
                    break;
                case PUBREL:
                    MqttMessageIdVariableHeader messageIdVariableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
                    //	构建返回报文， 固定报头
                    MqttFixedHeader mqttFixedHeaderBack = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0x02);
                    //	构建返回报文， 可变报头
                    MqttMessageIdVariableHeader mqttMessageIdVariableHeaderBack = MqttMessageIdVariableHeader.from(messageIdVariableHeader.messageId());
                    MqttMessage mqttMessageBack = new MqttMessage(mqttFixedHeaderBack, mqttMessageIdVariableHeaderBack);
                    ctx.channel().writeAndFlush(mqttMessageBack);
                    break;
                case SUBSCRIBE:
                    if (message instanceof MqttSubscribeMessage) {
                        SubscribeAckProcessor.handle(ctx, (MqttSubscribeMessage) message, config, MqttServer.this::onSubscribeTopics);
                    }
                    break;
                case UNSUBSCRIBE:
                    if (message instanceof MqttUnsubscribeMessage) {
                        UnsubscribeAckProcessor.handle(ctx, (MqttUnsubscribeMessage) message, config);
                    }
                    break;
                case PINGREQ:
                    PingAckProcessor.handle(ctx, config);
                    break;
                case DISCONNECT:
                    DisconnectAckProcessor.handle(ctx, config);
                    break;
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            DisconnectAckProcessor.handle(ctx, config);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            super.userEventTriggered(ctx, evt);
            DisconnectAckProcessor.handle(ctx, config);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            DisconnectAckProcessor.handle(ctx, config);
        }
    }
}
