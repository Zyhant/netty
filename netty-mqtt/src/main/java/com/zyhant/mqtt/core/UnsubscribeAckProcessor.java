package com.zyhant.mqtt.core;

import com.zyhant.common.domain.NettyConfig;
import com.zyhant.mqtt.domain.CacheConstants;
import com.zyhant.mqtt.domain.MqttClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 取消订阅
 * @author zyhant
 * @date 2024/7/24 02:22
 */
public class UnsubscribeAckProcessor {

    private static final Logger log = LoggerFactory.getLogger(UnsubscribeAckProcessor.class);

    public static void handle(ChannelHandlerContext context, MqttUnsubscribeMessage message, NettyConfig config) {
        String channelId = context.channel().id().asLongText();
        MqttClient client = config.getCacheObject(CacheConstants.MQTT_CLIENT_KEY + channelId);
        if (client == null) {
            context.close();
            log.error("MqttServer错误,未找到ChannelId为" + channelId + "的设备");
            return;
        }
        MqttUnsubscribePayload unsubscribePayload = message.payload();
        //	构建返回报文	可变报头
        MqttMessageIdVariableHeader variableHeaderBack = MqttMessageIdVariableHeader.from(message.variableHeader().messageId());
        //	构建返回报文	固定报头
        MqttFixedHeader mqttFixedHeaderBack = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2);
        //	构建返回报文	取消订阅确认
        MqttUnsubAckMessage unSubAck = new MqttUnsubAckMessage(mqttFixedHeaderBack, variableHeaderBack);
        context.writeAndFlush(unSubAck);
        // 取消订阅信息
        List<String> topicList = unsubscribePayload.topics();
        if (topicList != null) {
            for (String topic : topicList) {
                ArrayList<String> channelIds = config.getCacheObject(CacheConstants.MQTT_TOPIC_KEY + topic);
                if (channelIds != null) {
                    if (channelIds.stream().anyMatch(id -> id.equals(channelId))) {
                        channelIds.remove(channelId);
                        config.deleteObject(CacheConstants.MQTT_TOPIC_KEY + topic);
                        if (channelIds.size() > 0) {
                            config.setCacheObject(CacheConstants.MQTT_TOPIC_KEY + topic, channelIds);
                        }
                        log.info("MqttServer数据,设备:" + client.getClientId() + ",取消订阅主题:" + topic);
                    }
                } else {
                    config.deleteObject(CacheConstants.MQTT_TOPIC_KEY + topic);
                    log.info("MqttServer数据,移除主题:" + topic);
                }
            }
        }
    }

}
