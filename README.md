# UltimateLogger

UltimateLogger is a comprehensive logging plugin for Paper/Spigot Minecraft servers. It provides detailed logging of various player and block events, with a flexible and powerful API for developers to extend its functionality.

[![](https://jitpack.io/v/xef5000/UltimateLogger.svg)](https://jitpack.io/#xef5000/UltimateLogger)

## Features

- **Extensive Event Logging**: Logs various events including:
  - Player chat messages
  - Player commands
  - Player join/quit events
  - Player deaths
  - Block breaking
  - Block placement
- **Database Storage**: Store logs in either SQLite (local file) or MySQL (remote database)
- **In-game GUI**: View and filter logs directly in-game
- **Flexible API**: Easily extend the plugin with custom log definitions
- **Configurable**: Customize database settings, cache size, batch processing, and more
- **Performance Optimized**: Batch processing and connection pooling for minimal server impact

## Installation

1. Download the latest release from [GitHub Releases](https://github.com/xef5000/UltimateLogger/releases) or build from source
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in the `plugins/UltimateLogger/config.yml` file

## Configuration

The plugin's configuration file (`config.yml`) allows you to customize various aspects of the plugin:

```yaml
# Database settings
database:
  type: "SQLITE"  # Can be "SQLITE" or "MYSQL"
  
  # Settings for SQLite
  sqlite:
    filename: "logs.db"  # Database file in the plugin folder
  
  # Settings for MySQL
  mysql:
    host: "localhost"
    port: 3306
    database: "ultimatelogger"
    username: "user"
    password: "password"
  
  # Connection pool settings
  pool:
    max-size: 10  # Maximum number of database connections

# Log Manager settings
log-manager:
  # Cache settings
  cache:
    max-size: 100  # Maximum number of pages to store in cache
    expiry-minutes: 5  # Time in minutes after which cache entries expire
  
  # Batch processing settings
  batch:
    size: 100  # Maximum number of logs to process in a single batch
    interval: 100  # Interval in ticks between batch processing (20 ticks = 1 second)
```

## Usage

### Commands

- `/logger` - Opens the log viewing GUI (requires `ultimatelogger.view` permission)
- `/logger view <page>` - View logs in chat (requires `ultimatelogger.view` permission)
- `/logger stats` - View plugin statistics (requires `ultimatelogger.stats` permission)
- `/logger reload` - Reload the plugin configuration (requires `ultimatelogger.reload` permission)
- `/logger help` - Show help information

### Permissions

- `ultimatelogger.view` - Allows viewing logs
- `ultimatelogger.stats` - Allows viewing plugin statistics
- `ultimatelogger.reload` - Allows reloading the plugin configuration

## Developer API

UltimateLogger provides a flexible API for developers to extend its functionality.

### Adding as a Dependency

#### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.xef5000</groupId>
        <artifactId>UltimateLogger</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### Gradle

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.xef5000:UltimateLogger:1.0.0'
}
```

### Creating Custom Log Definitions

You can create custom log definitions by extending the `LogDefinition` class:

```java
import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

public class PlayerInteractLogDefinition extends LogDefinition<PlayerInteractEvent> {
    @Override
    public String getId() {
        return "player_interact";
    }

    @Override
    public Class<PlayerInteractEvent> getEventClass() {
        return PlayerInteractEvent.class;
    }

    @Override
    public boolean shouldLog(PlayerInteractEvent event) {
        // Only log right-clicks on blocks
        return event.getAction().isRightClick() && event.hasBlock();
    }

    @Override
    public LogData captureData(PlayerInteractEvent event) {
        return new LogData()
                .put("player_uuid", event.getPlayer().getUniqueId().toString())
                .put("player_name", event.getPlayer().getName())
                .put("block_type", event.getClickedBlock().getType().toString())
                .put("block_location", event.getClickedBlock().getLocation().toString());
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("block_type", "Block Type", ParameterType.STRING)
        );
    }
}
```

Then register your log definition in your plugin:

```java
import ca.xef5000.ultimateLogger.UltimateLogger;
import org.bukkit.plugin.java.JavaPlugin;

public class YourPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Get the UltimateLogger plugin
        UltimateLogger ultimateLogger = (UltimateLogger) getServer().getPluginManager().getPlugin("UltimateLogger");
        if (ultimateLogger != null) {
            // Register your custom log definition
            ultimateLogger.getLogManager().registerLogDefinition(new PlayerInteractLogDefinition());
            getLogger().info("Registered custom log definition with UltimateLogger!");
        }
    }
}
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support, please open an issue on the [GitHub repository](https://github.com/xef5000/UltimateLogger/issues).

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.