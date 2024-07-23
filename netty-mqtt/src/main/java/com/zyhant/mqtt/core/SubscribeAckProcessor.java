package com.zyhant.mqtt.core;

import com.zyhant.common.domain.NettyConfig;
import com.zyhant.mqtt.domain.CacheConstants;
import com.zyhant.mqtt.domain.MqttClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 订阅主题
 * @author zyhant
 * @date 2024/7/24 01:29
 */
public class SubscribeAckProcessor {

    private static final Logger log = LoggerFactory.getLogger(SubscribeAckProcessor.class);

    public static void handle(ChannelHandlerContext context, MqttSubscribeMessage message, NettyConfig config, Callback callback) {
        String channelId = context.channel().id().asLongText();
        MqttClient client = config.getCacheObject(CacheConstants.MQTT_CLIENT_KEY + channelId);
        if (client == null) {
            context.close();
            log.error("MqttServer错误,未找到ChannelId为" + channelId + "的设备");
            return;
        }
        //	构建返回报文， 可变报头
        MqttMessageIdVariableHeader variableHeaderBack = MqttMessageIdVariableHeader.from(message.variableHeader().messageId());
        List<MqttTopicSubscription> topicSubscriptions = message.payload().topicSubscriptions();
        if (topicSubscriptions != null) {
            for (MqttTopicSubscription subscription : topicSubscriptions) {
                String topic = subscription.topicName();
                MqttQoS mqttQoS = subscription.qualityOfService();
                Collection<String> subscribeTopics;
                if (callback == null) {
                    mqttQoS = MqttQoS.FAILURE;
                    subscribeTopics = new HashSet<>();
                } else {
                    subscribeTopics = callback.subscribeTopics();
                }
                if (subscribeTopics.stream().noneMatch(topicName -> {
                    Pattern pattern = Pattern.compile(topicName);
                    Matcher matcher = pattern.matcher(topic);
                    return matcher.matches();
                })) {
                    mqttQoS = MqttQoS.FAILURE;
                }
                //	构建返回报文	有效负载
                MqttSubAckPayload payloadBack = new MqttSubAckPayload(mqttQoS.value());
                //	构建返回报文	固定报头
                MqttFixedHeader mqttFixedHeaderBack = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2 + topicSubscriptions.size());
                //	构建返回报文	订阅确认
                MqttSubAckMessage subAck = new MqttSubAckMessage(mqttFixedHeaderBack, variableHeaderBack, payloadBack);
                context.writeAndFlush(subAck);
                if (mqttQoS == MqttQoS.FAILURE) {
                    return;
                }
                Collection<String> topicList = config.keys(CacheConstants.MQTT_TOPIC_KEY);
                if (topicList != null) {
                    if (topicList.stream().anyMatch(topicName -> topicName.equals(topic))) {
                        // 存在订阅
                        ArrayList<String> channelIds = config.getCacheObject(CacheConstants.MQTT_TOPIC_KEY + topic);
                        if (channelIds.stream().noneMatch(id -> id.equals(channelId))) {
                            channelIds.add(channelId);
                            config.setCacheObject(CacheConstants.MQTT_TOPIC_KEY + topic, channelIds);
                            log.info("MqttServer数据,设备:" + client.getClientId() + ",订阅主题:" + topic);
                        }
                    } else {
                        // 不存在订阅
                        config.setCacheObject(CacheConstants.MQTT_TOPIC_KEY + topic, new ArrayList<>(Collections.singletonList(channelId)));
                        log.info("MqttServer数据,设备:" + client.getClientId() + ",订阅主题:" + topic);
                    }
                } else {
                    // 不存在订阅
                    config.setCacheObject(CacheConstants.MQTT_TOPIC_KEY + topic, new ArrayList<>(Collections.singletonList(channelId)));
                    log.info("MqttServer数据,设备:" + client.getClientId() + ",订阅主题:" + topic);
                }
            }
        }
    }

    public interface Callback {

        Collection<String> subscribeTopics();

    }

}
