package fredboat;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.utils.SessionControllerAdapter;
import net.dv8tion.jda.core.utils.tuple.ImmutablePair;
import net.dv8tion.jda.core.utils.tuple.Pair;

public class FredBoatSessionController extends SessionControllerAdapter {

    @Override
    public void appendSession(SessionConnectNode node) {
        try {
            node.run(true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeSession(SessionConnectNode node) {
        // Nop
    }

    @Override
    public String getGateway(JDA api) {
        return Config.CONFIG.getDikeUrl() == null
                ? super.getGateway(api)
                : Config.CONFIG.getDikeUrl();
    }

    @Override
    public Pair<String, Integer> getGatewayBot(JDA api) {
        Pair<String, Integer> pair = super.getGatewayBot(api);

        if (Config.CONFIG.getDikeUrl() != null) {
            pair = new ImmutablePair<>(Config.CONFIG.getDikeUrl(), pair.getRight());
        }

        return pair;
    }

}
