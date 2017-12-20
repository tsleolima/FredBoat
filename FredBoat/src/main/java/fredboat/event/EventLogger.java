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

package fredboat.event;

import fredboat.FredBoat;
import fredboat.messaging.CentralMessaging;
import fredboat.util.Emojis;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.*;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This overengineered class logs some events via a webhook into Discord.
 * <p>
 * Shards status events and guild join/leave events are collected. In regular intervals messages are created containing
 * the collected information. The created messages are then attempted to be posted via the webhook. A small buffer is
 * used to try to not drop occasionally failed messages, since the reporting of status events is especially important
 * during ongoing connection issues. If the amount of status events is too damn high, summaries are posted.
 */
public class EventLogger extends ListenerAdapter {

    public static final Logger log = LoggerFactory.getLogger(EventLogger.class);

    private final static int MAX_MESSAGE_QUEUE_SIZE = 30;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            runnable -> new Thread(runnable, "eventlogger"));
    @Nullable
    private WebhookClient eventLoggerWebhook;

    //saves some messages, so that in case we run into occasional connection issues we dont just drop them due to the webhook timing out
    private final Queue<Message> toBeSent = new ConcurrentLinkedQueue<>();

    private final List<ShardStatusEvent> statusStats = new CopyOnWriteArrayList<>();
    private final AtomicInteger guildsJoinedEvents = new AtomicInteger(0);
    private final AtomicInteger guildsLeftEvents = new AtomicInteger(0);

    public EventLogger(long id, String token) {
        this(new WebhookClientBuilder(id, token).build());
    }

    public EventLogger(String eventLogWebhookUrl) {
        this(new WebhookClientBuilder(eventLogWebhookUrl).build());
    }


    @Override
    public void onReady(ReadyEvent event) {
        statusStats.add(new ShardStatusEvent(event.getJDA().getShardInfo().getShardId(),
                ShardStatusEvent.StatusEvent.READY, ""));
    }

    @Override
    public void onResume(ResumedEvent event) {
        statusStats.add(new ShardStatusEvent(event.getJDA().getShardInfo().getShardId(),
                ShardStatusEvent.StatusEvent.RESUME, ""));
    }

    @Override
    public void onReconnect(ReconnectedEvent event) {
        statusStats.add(new ShardStatusEvent(event.getJDA().getShardInfo().getShardId(),
                ShardStatusEvent.StatusEvent.RECONNECT, ""));
    }

    @Override
    public void onDisconnect(DisconnectEvent event) {
        String closeCodeStr = "close code " + (event.getCloseCode() == null ? "null" : event.getCloseCode().getCode());
        statusStats.add(new ShardStatusEvent(event.getJDA().getShardInfo().getShardId(),
                ShardStatusEvent.StatusEvent.DISCONNECT, "with " + closeCodeStr));
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        String closeCodeStr = "close code " + (event.getCloseCode() == null ? "null" : event.getCloseCode().getCode());
        statusStats.add(new ShardStatusEvent(event.getJDA().getShardInfo().getShardId(),
                ShardStatusEvent.StatusEvent.SHUTDOWN, "with " + closeCodeStr));
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        guildsJoinedEvents.incrementAndGet();
        log.info("Joined guild {} with {} users", event.getGuild(), event.getGuild().getMemberCache().size());
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        guildsLeftEvents.incrementAndGet();
        log.info("Left guild {} with {} users", event.getGuild(), event.getGuild().getMemberCache().size());
    }

    private final Runnable ON_SHUTDOWN = () -> {
        String message;
        if (FredBoat.shutdownCode != FredBoat.UNKNOWN_SHUTDOWN_CODE) {
            message = Emojis.DOOR + "Exiting with code " + FredBoat.shutdownCode + ".";
        } else {
            message = Emojis.DOOR + "Exiting with unknown code.";
        }
        log.info(message);
        if (eventLoggerWebhook != null) {
            eventLoggerWebhook.send(message);
        }
    };

    //actual constructor
    private EventLogger(WebhookClient eventLoggerWebhook) {
        Runtime.getRuntime().addShutdownHook(new Thread(ON_SHUTDOWN, EventLogger.class.getSimpleName() + " shutdownhook"));

        //test the provided webhook before assigning it, otherwise it will spam our logs with exceptions
        WebhookClient workingWebhook = null;
        if (eventLoggerWebhook.getIdLong() > 0) { //id is 0 when there is no webhookid configured in the config; skip this in that case
            try {
                eventLoggerWebhook.send(Emojis.PENCIL + "Eventlogger started.")
                        .get();
                workingWebhook = eventLoggerWebhook; //webhook test was successful; FIXME occasionally this might fail during the start due to connection issues, while the provided values are actually valid
            } catch (Exception e) {
                log.error("Failed to create event log webhook. Event logs will not be available. Doublecheck your configuration values.");
                eventLoggerWebhook.close();
            }
        } else {
            eventLoggerWebhook.close();
        }

        this.eventLoggerWebhook = workingWebhook;

        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendShardStatusSummary();
            } catch (Exception e) {
                log.error("Failed to send shard status summary to event log webhook", e);
            }
        }, 0, 1, TimeUnit.MINUTES);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendGuildsSummary();
            } catch (Exception e) {
                log.error("Failed to send guilds summary to event log webhook", e);
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    private void sendGuildsSummary() {
        MessageEmbed embed = CentralMessaging.getColoredEmbedBuilder()
                .setTimestamp(LocalDateTime.now())
                .setTitle("Joins and Leaves since the last update")
                .addField("Guilds joined", Integer.toString(guildsJoinedEvents.getAndSet(0)), true)
                .addField("Guilds left", Integer.toString(guildsLeftEvents.getAndSet(0)), true)
                .build();

        addMessageToWebhookQueue(CentralMessaging.getClearThreadLocalMessageBuilder().setEmbed(embed).build());
        drainMessageQueue();
    }

    private void sendShardStatusSummary() {
        List<ShardStatusEvent> events = new ArrayList<>(statusStats);
        statusStats.removeAll(events);

        if (events.isEmpty()) {
            return;//nothing to report
        }

        //split into messages of acceptable size (2k chars max)
        StringBuilder msg = new StringBuilder();
        for (ShardStatusEvent event : events) {
            String eventStr = event.toString();
            if (msg.length() + eventStr.length() > 1900) {
                addMessageToWebhookQueue(CentralMessaging.getClearThreadLocalMessageBuilder()
                        .appendCodeBlock(msg.toString(), "diff").build());
                msg = new StringBuilder();
            }
            msg.append("\n").append(eventStr);
        }
        if (msg.length() > 0) {//any leftovers?
            addMessageToWebhookQueue(CentralMessaging.getClearThreadLocalMessageBuilder()
                    .appendCodeBlock(msg.toString(), "diff").build());
        }
        drainMessageQueue();
    }

    private synchronized void drainMessageQueue() {
        if (eventLoggerWebhook == null) {
            return;
        }
        try {
            while (!toBeSent.isEmpty()) {
                Message message = toBeSent.peek();
                eventLoggerWebhook.send(message).get();
                toBeSent.poll();
            }
        } catch (Exception e) {
            log.warn("Event log webhook failed to send a message. Will try again later.", e);
        }
    }

    private void addMessageToWebhookQueue(Message message) {
        while (toBeSent.size() > MAX_MESSAGE_QUEUE_SIZE) {
            toBeSent.poll(); //drop messages above max size
        }
        toBeSent.add(message);
    }

    private static class ShardStatusEvent {

        final int shardId;
        @Nonnull
        final StatusEvent event;
        @Nonnull
        final String additionalInfo;
        final long timestamp;

        private ShardStatusEvent(int shardId, @Nonnull StatusEvent event, @Nonnull String additionalInfo) {
            this.shardId = shardId;
            this.event = event;
            this.additionalInfo = additionalInfo;
            this.timestamp = System.currentTimeMillis();
        }

        enum StatusEvent {
            READY("readied", "+"),
            RESUME("resumed", "+"),
            RECONNECT("reconnected", "+"),
            DISCONNECT("disconnected", "-"),
            SHUTDOWN("shut down", "-");

            String str;
            String diff;

            StatusEvent(String str, String diff) {
                this.str = str;
                this.diff = diff;
            }
        }

        @Override
        public String toString() {
            return String.format("%s [%s] Shard %s %s %s",
                    event.diff, TextUtils.asTimeInCentralEurope(timestamp),
                    TextUtils.forceNDigits(shardId, 3), event.str, additionalInfo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(shardId, event, timestamp);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ShardStatusEvent)) return false;
            ShardStatusEvent other = (ShardStatusEvent) obj;
            return other.shardId == this.shardId
                    && other.event == this.event
                    && other.timestamp == this.timestamp;
        }
    }
}
