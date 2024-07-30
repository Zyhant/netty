package com.zyhant.netty.mqtt.core;

import com.zyhant.netty.common.domain.NettyConfig;
import com.zyhant.netty.mqtt.domain.CacheConstants;
import com.zyhant.netty.mqtt.domain.MqttClient;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 断开连接
 * @author zyhant
 * @date 2024/7/23 23:04
 */
public class DisconnectAckProcessor {

    private static final Logger log = LoggerFactory.getLogger(DisconnectAckProcessor.class);

    public static void handle(ChannelHandlerContext ctx, NettyConfig config) {
        ctx.close();
        String channelId = ctx.channel().id().asLongText();
        MqttClient client = config.getCacheObject(CacheConstants.MQTT_CLIENT_KEY + channelId);
        String clientId;
        if (client != null) {
            clientId = client.getClientId();
        } else {
            clientId = channelId;
        }
        Collection<String> topicList = config.keys(CacheConstants.MQTT_TOPIC_KEY);
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
                        log.info("MqttServer数据,设备:" + clientId + ",取消订阅主题:" + topic);
                    }
                } else {
                    config.deleteObject(CacheConstants.MQTT_TOPIC_KEY + topic);
                    log.info("MqttServer数据,移除主题:" + topic);
                }
            }
        }
        // 移除设备缓存
        if (client != null) {
            config.deleteObject(CacheConstants.MQTT_CLIENT_KEY + channelId);
        }
        log.info("MqttServer数据,设备:" + clientId + ",断开连接");
    }

}
