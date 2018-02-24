package fredboat.audio.player;

import fredboat.commandmeta.abs.CommandContext;
import fredboat.shared.constant.BotConstants;
import net.dv8tion.jda.core.entities.Guild;

public class PlayerLimitManager {

    // A negative limit means unlimited
    private static int limit = -1;

    public static boolean checkLimit(Guild guild, PlayerRegistry playerRegistry) {
        GuildPlayer guildPlayer = playerRegistry.getExisting(guild);
        //noinspection SimplifiableIfStatement
        if (guildPlayer != null && guildPlayer.getTrackCount() > 0)
            return true;

        return limit < 0
                || playerRegistry.getPlayingPlayers().size() < limit;

    }

    public static boolean checkLimitResponsive(CommandContext context, PlayerRegistry playerRegistry) {
        boolean b = checkLimit(context.guild, playerRegistry);

        if (!b) {
            String patronUrl = "<" + BotConstants.DOCS_DONATE_URL + ">";
            String msg = context.i18nFormat("playersLimited", limit, patronUrl);
            context.reply(msg);
        }

        return b;
    }

    public static int getLimit() {
        return limit;
    }

    public static void setLimit(int limit) {
        PlayerLimitManager.limit = limit;
    }
}
