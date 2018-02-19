package fredboat.feature;

import fredboat.main.BotController;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.utils.SessionControllerAdapter;
import net.dv8tion.jda.core.utils.tuple.ImmutablePair;
import net.dv8tion.jda.core.utils.tuple.Pair;

public class DikeSessionController extends SessionControllerAdapter {

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
        return BotController.INS.getCredentials().getDikeUrl();
    }

    @Override
    public Pair<String, Integer> getGatewayBot(JDA api) {
        Pair<String, Integer> pair = super.getGatewayBot(api);

        return new ImmutablePair<>(BotController.INS.getCredentials().getDikeUrl(), pair.getRight());
    }

}
