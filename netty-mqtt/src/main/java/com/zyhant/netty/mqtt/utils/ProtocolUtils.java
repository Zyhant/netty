package com.zyhant.netty.mqtt.utils;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;

/**
 * MQTT消息工具
 * @author zyhant
 * @date 2024/7/24 00:51
 */
public class ProtocolUtils {

    public static MqttPublishMessage publishMessage(String topic, byte[] payload, int qosValue, int messageId, boolean isRetain) {
        return publishMessage(topic, payload, qosValue, isRetain, messageId, false);
    }

    public static MqttPublishMessage publishMessage(String topic, byte[] payload, int qosValue, boolean isRetain, int messageId, boolean isDup) {
        return (MqttPublishMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBLISH, isDup, MqttQoS.valueOf(qosValue), isRetain, 0),
                new MqttPublishVariableHeader(topic, messageId),
                Unpooled.buffer().writeBytes(payload));
    }

}
