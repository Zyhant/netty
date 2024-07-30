package com.zyhant.netty.mqtt.core;

import com.zyhant.netty.mqtt.domain.CacheConstants;
import com.zyhant.netty.mqtt.domain.MqttClient;
import com.zyhant.netty.mqtt.domain.MqttConfig;
import com.zyhant.netty.mqtt.domain.MqttPublish;
import com.zyhant.netty.mqtt.utils.MessageIdFactory;
import com.zyhant.netty.mqtt.utils.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 消息处理
 * @author zyhant
 * @date 2024/7/23 23:08
 */
public class PublishAckProcessor {

    private static final Logger log = LoggerFactory.getLogger(PublishAckProcessor.class);

    public static void handle(ChannelHandlerContext context, MqttPublishMessage message, MqttConfig config, Callback callback) {
        try {
            String channelId = context.channel().id().asLongText();
            MqttClient client = config.getCacheObject(CacheConstants.MQTT_CLIENT_KEY + channelId);
            if (client == null) {
                context.close();
                log.error("MqttServer错误,未找到ChannelId为" + channelId + "的设备");
                return;
            }
            MqttFixedHeader mqttFixedHeaderInfo = message.fixedHeader();
            MqttPublishVariableHeader mqttPublishVariableHeader = message.variableHeader();
            MqttQoS qos = mqttFixedHeaderInfo.qosLevel();
            MqttPublish publish = new MqttPublish();
            Field fieldClient = publish.getClass().getDeclaredField("client");
            fieldClient.setAccessible(true);
            fieldClient.set(publish, client);
            fieldClient.setAccessible(false);
            Field fieldTopic = publish.getClass().getDeclaredField("topic");
            fieldTopic.setAccessible(true);
            fieldTopic.set(publish, mqttPublishVariableHeader.topicName());
            fieldTopic.setAccessible(false);
            Field fieldContent = publish.getClass().getDeclaredField("content");
            fieldContent.setAccessible(true);
            ByteBuf payload = message.payload();
            byte[] headBytes = new byte[payload.readableBytes()];
            payload.readBytes(headBytes);
            fieldContent.set(publish, new String(headBytes, StandardCharsets.UTF_8));
            fieldContent.setAccessible(false);
            switch (qos) {
                case AT_MOST_ONCE:        //	至多一次
                    break;
                case AT_LEAST_ONCE:        //	至少一次
                    //	构建返回报文， 可变报头
                    MqttMessageIdVariableHeader mqttMessageIdVariableHeaderBack = MqttMessageIdVariableHeader.from(mqttPublishVariableHeader.packetId());
                    //	构建返回报文， 固定报头
                    MqttFixedHeader mqttFixedHeaderBack = new MqttFixedHeader(MqttMessageType.PUBACK, mqttFixedHeaderInfo.isDup(), MqttQoS.AT_MOST_ONCE, mqttFixedHeaderInfo.isRetain(), 0x02);
                    //	构建PUBACK消息体
                    MqttPubAckMessage pubAck = new MqttPubAckMessage(mqttFixedHeaderBack, mqttMessageIdVariableHeaderBack);
                    context.writeAndFlush(pubAck);
                    break;
                case EXACTLY_ONCE:        //	刚好一次
                    //	构建返回报文， 固定报头
                    MqttFixedHeader mqttFixedHeaderBack2 = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_LEAST_ONCE, false, 0x02);
                    //	构建返回报文， 可变报头
                    MqttMessageIdVariableHeader mqttMessageIdVariableHeaderBack2 = MqttMessageIdVariableHeader.from(mqttPublishVariableHeader.packetId());
                    MqttMessage mqttMessageBack = new MqttMessage(mqttFixedHeaderBack2, mqttMessageIdVariableHeaderBack2);
                    context.writeAndFlush(mqttMessageBack);
                    break;
            }
            log.info("MqttServer数据,设备:" + publish.getClient().getClientId() + ",发布消息:\n主题:\n" + publish.getTopic() + "\n内容:\n" + publish.getContent());
            transmitMessage(publish.getTopic(), publish.getContent(), mqttFixedHeaderInfo, config);
            if (callback != null) {
                callback.publish(publish);
            }
        } catch (Exception e) {
            log.error("MqttServer错误", e);
        }
    }

    private static void transmitMessage(String topic, String content, MqttFixedHeader fixedHeader, MqttConfig config) {
        Collection<String> topicList = config.keys(CacheConstants.MQTT_TOPIC_KEY);
        String monitorTopic = config.onMonitorTopic();
        if (monitorTopic != null && !monitorTopic.isEmpty()) {
            topicList.add(monitorTopic);
        }
        Collection<String> topics = topicList.stream().filter(topicName -> topicName.equals(topic)).collect(Collectors.toList());
        if (topics.size() > 0) {
            for (String topicName : topics) {
                ArrayList<String> channelIds = config.getCacheObject(CacheConstants.MQTT_TOPIC_KEY + topicName);
                for (String channelId : channelIds) {
                    MqttClient client = config.getCacheObject(CacheConstants.MQTT_CLIENT_KEY + channelId);
                    if (client != null) {
                        int id = 0;
                        try {
                            id = MessageIdFactory.get();

                            MqttPublishMessage msg = ProtocolUtils.publishMessage(topic,
                                    content.getBytes(StandardCharsets.UTF_8),
                                    fixedHeader.qosLevel().value(),
                                    id,
                                    fixedHeader.isRetain()
                            );
                            log.info("MqttServer数据,设备:" + client.getClientId() + ",转发消息:\n主题:\n" + topic + "\n内容:\n" + content);
                            client.getChannel().writeAndFlush(msg);
                        } catch (Exception e) {
                            log.error("MqttServer错误", e);
                        } finally {
                            MessageIdFactory.release(id);
                        }
                    }
                }
            }
        }
    }

    public interface Callback {

        void publish(MqttPublish publish);

    }
}
