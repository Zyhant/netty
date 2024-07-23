package com.zyhant.mqtt.core;

import com.zyhant.common.domain.NettyConfig;
import com.zyhant.mqtt.domain.CacheConstants;
import com.zyhant.mqtt.domain.MqttClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 心跳数据
 * @author zyhant
 * @date 2024/7/24 02:27
 */
public class PingAckProcessor {

    private static final Logger log = LoggerFactory.getLogger(PingAckProcessor.class);

    public static void handle(ChannelHandlerContext context, NettyConfig config) {
        String channelId = context.channel().id().asLongText();
        MqttClient client = config.getCacheObject(CacheConstants.MQTT_CLIENT_KEY + channelId);
        if (client == null) {
            context.close();
            log.error("MqttServer错误,未找到ChannelId为" + channelId + "的设备");
            return;
        }
        //	心跳响应报文	11010000 00000000  固定报文
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessage mqttMessageBack = new MqttMessage(fixedHeader);
        context.writeAndFlush(mqttMessageBack);
        // 刷新缓存
        config.refreshCacheObject(CacheConstants.MQTT_CLIENT_KEY + channelId);
        log.info("MqttServer数据,设备:" + client.getClientId() + ",心跳包:");
    }

}
