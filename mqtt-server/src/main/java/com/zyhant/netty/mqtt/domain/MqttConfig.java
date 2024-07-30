package com.zyhant.netty.mqtt.domain;

import com.zyhant.netty.common.domain.NettyConfig;

/**
 * @author zyhant
 * @date 2024/7/24 01:05
 */
public abstract class MqttConfig extends NettyConfig {

    public abstract String onMonitorTopic();

}
