/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.api;

import fredboat.audio.player.PlayerRegistry;
import fredboat.feature.metrics.BotMetrics;
import fredboat.feature.metrics.MetricsServletAdapter;
import fredboat.jda.ShardProvider;
import fredboat.main.Launcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class API {

    private static final Logger log = LoggerFactory.getLogger(API.class);
    private final ShardProvider shardProvider;
    private final PlayerRegistry playerRegistry;
    private final BotMetrics botMetrics;
    private final MetricsServletAdapter metricsServlet;

    public API(ShardProvider shardProvider, PlayerRegistry playerRegistry, BotMetrics botMetrics, MetricsServletAdapter metricsServlet) {
        this.shardProvider = shardProvider;
        this.playerRegistry = playerRegistry;
        this.botMetrics = botMetrics;
        this.metricsServlet = metricsServlet;
    }


    @ExceptionHandler
    public ResponseEntity<String> onException(Exception exception, HttpServletRequest request) {
        log.error(request.getMethod() + " " + request.getPathInfo(), exception);
        return new ResponseEntity<>("Sorry! An error happened.\n" + exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @GetMapping(path = "/stats")
    public Object getStats(HttpServletResponse response) {
        response.setContentType("application/json");

        JSONObject root = new JSONObject();
        JSONArray a = new JSONArray();

        shardProvider.streamShards().forEach(shard -> {
            JSONObject fbStats = new JSONObject();
            fbStats.put("id", shard.getShardInfo().getShardId())
                    .put("guilds", shard.getGuildCache().size())
                    .put("users", shard.getUserCache().size())
                    .put("status", shard.getStatus());

            a.put(fbStats);
        });

        JSONObject g = new JSONObject();
        g.put("playingPlayers", playerRegistry.getPlayingPlayers().size())
                .put("totalPlayers", playerRegistry.getRegistry().size())
                .put("distribution", Launcher.getBotController().getAppConfig().getDistribution())
                .put("guilds", botMetrics.getTotalGuildsCount())
                .put("users", botMetrics.getTotalUniqueUsersCount());

        root.put("shards", a);
        root.put("global", g);

        return root.toString();
    }

    @GetMapping(path = "/metrics")
    public void getMetrics(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        metricsServlet.servletGet(request, response);
    }
}
