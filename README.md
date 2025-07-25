# UltimateLogger

UltimateLogger is a comprehensive logging plugin for Paper/Spigot Minecraft servers. It provides detailed logging of various player and block events, with a flexible and powerful API for developers to extend its functionality.

[![](https://jitpack.io/v/xef5000/UltimateLogger.svg)](https://jitpack.io/#xef5000/UltimateLogger)
![](https://img.shields.io/github/downloads/xef5000/UltimateLogger/total)


## Features

- **Extensive Event Logging**: Logs various events including:
  - Player chat messages
  - Player commands
  - Player join/quit events (including IP address)
  - Player deaths
  - Block breaking
  - Block placement
  - etc.
- **Database Storage**: Store logs in either SQLite (local file) or MySQL (remote database)
- **In-game GUI**: View and filter logs directly in-game, including single log view
- **Log Management**: Ability to archive logs to protect them from auto-deletion, and automatic log cleanup
- **Discord Integration**: Send log notifications to Discord via webhooks
- **Flexible API**: Easily extend the plugin with custom log definitions
- **Configurable**: Customize database settings, cache size, batch processing, enable/disable specific log types, and more
- **Performance Optimized**: Batch processing and connection pooling for minimal server impact

## Installation

1. Download the latest release from [GitHub Releases](https://github.com/xef5000/UltimateLogger/releases) or build from source
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in the `plugins/UltimateLogger/config.yml` file

## Configuration

The plugin's configuration file (`config.yml`) allows you to customize various aspects of the plugin:

```yaml
# UltimateLogger Configuration

# Database settings
# type: can be "SQLITE" or "MYSQL"
database:
  type: "SQLITE"

  # Settings for SQLite
  sqlite:
    # The name of the database file.
    # It will be created in the UltimateLogger plugin folder.
    filename: "logs.db"

  # Settings for MySQL
  mysql:
    host: "localhost"
    port: 3306
    database: "ultimatelogger"
    username: "user"
    password: "password"

  # Connection pool settings
  pool:
    # Maximum number of database connections in the pool
    max-size: 10

# Log Manager settings
logs:
  # Cache settings
  cache:
    # Maximum number of pages to store in the cache
    max-size: 100
    # Time in minutes after which cache entries expire if not accessed
    expiry-minutes: 5

  # Batch processing settings
  batch:
    # Maximum number of logs to process in a single batch
    size: 100
    # Interval in ticks between batch processing (20 ticks = 1 second)
    interval: 100

  # Log types to disable
  disabled-log-types:
    - block_break
    - block_place

  # Webhook settings
  webhooks:
    #block_break: "https://discord.com/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz"

  # How many days to keep logs before they are automatically deleted.
  # Set to -1 to disable auto-deletion.
  retention-period-days: 30
  # How often (in minutes) the server should run the cleanup task to delete old logs.
  cleanup-interval-minutes: 60
```

## Usage

### Commands

- `/logger` - Opens the log viewing GUI (requires `ultimatelogger.view` permission)
- `/logger view <page>` - View logs in chat (requires `ultimatelogger.view` permission)
- `/logger log <id>` - View a specific log by ID in a detailed GUI (requires `ultimatelogger.view` permission)
- `/logger stats` - View plugin statistics (requires `ultimatelogger.stats` permission)
- `/logger reload` - Reload the plugin configuration (requires `ultimatelogger.reload` permission)
- `/logger help` - Show help information

### Permissions

- `ultimatelogger.view` - Allows viewing logs
- `ultimatelogger.stats` - Allows viewing plugin statistics
- `ultimatelogger.reload` - Allows reloading the plugin configuration
- `ultimatelogger.archive` - Allows archiving logs to protect them from auto-deletion
- `ultimatelogger.unarchive` - Allows unarchiving logs to make them expire normally

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
        <version>1.1.2</version>
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
    compileOnly 'com.github.xef5000:UltimateLogger:1.1.2'
}
```

### Creating Custom Log Definitions

You can create custom log definitions by extending the `LogDefinition` class. Each log definition is responsible for capturing data from a specific Bukkit event type.

Here's an example of a custom log definition for TNT explosions:

```java
import ca.xef5000.ultimateLogger.api.LogData;
import ca.xef5000.ultimateLogger.api.LogDefinition;
import ca.xef5000.ultimateLogger.api.ParameterDefinition;
import ca.xef5000.ultimateLogger.api.ParameterType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;

public class CustomTNTLogDefinition extends LogDefinition<EntityExplodeEvent> {
    @Override
    public String getId() {
        return "custom_tnt_explosion";
    }

    @Override
    public Class<EntityExplodeEvent> getEventClass() {
        return EntityExplodeEvent.class;
    }

    @Override
    public boolean shouldLog(EntityExplodeEvent event) {
        // Only log TNT explosions
        return event.getEntity() instanceof TNTPrimed;
    }

    @Override
    public LogData captureData(EntityExplodeEvent event) {
        TNTPrimed tnt = (TNTPrimed) event.getEntity();
        
        // Create a LogData object with structured information about the explosion
        LogData data = new LogData()
                .put("location_world", event.getLocation().getWorld().getName())
                .put("location_x", event.getLocation().getBlockX())
                .put("location_y", event.getLocation().getBlockY())
                .put("location_z", event.getLocation().getBlockZ())
                .put("blocks_destroyed", event.blockList().size())
                .put("yield", event.getYield());

        // Conditionally add player information if a player ignited the TNT
        if (tnt.getSource() instanceof Player) {
            Player player = (Player) tnt.getSource();
            data.put("player_uuid", player.getUniqueId().toString())
                .put("player_name", player.getName());
        }

        return data;
    }

    @Override
    public List<ParameterDefinition> getFilterableParameters() {
        // Define which parameters can be used for filtering in the GUI
        return List.of(
                new ParameterDefinition("player_name", "Player Name", ParameterType.STRING),
                new ParameterDefinition("player_uuid", "Player UUID", ParameterType.UUID),
                new ParameterDefinition("location_world", "World", ParameterType.STRING),
                new ParameterDefinition("blocks_destroyed", "Blocks Destroyed", ParameterType.INTEGER),
                new ParameterDefinition("yield", "Explosion Yield", ParameterType.STRING)
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
            ultimateLogger.getLogManager().registerLogDefinition(new CustomTNTLogDefinition());
            getLogger().info("Registered custom log definition with UltimateLogger!");
        } else {
            getLogger().warning("UltimateLogger plugin not found! Custom log definitions will not be registered.");
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
