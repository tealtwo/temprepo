package me.whitehatd.aquila.queue.util;

import gg.supervisor.util.chat.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

@gg.supervisor.core.annotation.Component
public class TitleUtils {
    public void showTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component t = Text.translate(title);
        Component sub = Text.translate(subtitle);
        Title.Times times = Title.Times.times(
            java.time.Duration.ofMillis(fadeIn * 50L),
            java.time.Duration.ofMillis(stay * 50L),
            java.time.Duration.ofMillis(fadeOut * 50L)
        );
        player.showTitle(Title.title(t, sub, times));
    }
}
