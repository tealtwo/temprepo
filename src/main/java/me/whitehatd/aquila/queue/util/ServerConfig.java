package me.whitehatd.aquila.queue.util;

import gg.supervisor.configuration.yaml.YamlConfigService;
import gg.supervisor.core.annotation.Configuration;
import lombok.Data;

@Data
@Configuration(fileName = "server-config.yml", service = YamlConfigService.class)
public class ServerConfig {

    private String serverName;
}
