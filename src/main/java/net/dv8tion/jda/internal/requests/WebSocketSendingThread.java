/*
 * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.internal.requests;

import gnu.trove.map.TLongObjectMap;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.audio.ConnectionRequest;
import net.dv8tion.jda.internal.audio.ConnectionStage;
import org.slf4j.Logger;

import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

// Helper class delegated to WebSocketClient
class WebSocketSendingThread implements Runnable {
    private static final Logger LOG = WebSocketClient.LOG;

    // [kamibot-patch] stuck DISCONNECT/RECONNECT op4 재발사 상한.
    // 10초 간격 재시도이므로 100회 ≈ 약 1000초(~16분) 시도 후 포기한다.
    private static final int MAX_STUCK_ATTEMPTS = 100;

    private final WebSocketClient client;
    private final JDAImpl api;
    private final ReentrantLock queueLock;
    private final Queue<DataObject> chunkQueue;
    private final Queue<DataObject> ratelimitQueue;
    private final TLongObjectMap<ConnectionRequest> queuedAudioConnections;
    private final ScheduledExecutorService executor;
    private Future<?> handle;

    private boolean needRateLimit = false;
    private boolean attemptedToSend = false;
    private boolean shutdown = false;

    WebSocketSendingThread(WebSocketClient client) {
        this.client = client;
        this.api = client.api;
        this.queueLock = client.queueLock;
        this.chunkQueue = client.chunkSyncQueue;
        this.ratelimitQueue = client.ratelimitQueue;
        this.queuedAudioConnections = client.queuedAudioConnections;
        this.executor = client.executor;
    }

    public void shutdown() {
        shutdown = true;
        if (handle != null) {
            handle.cancel(false);
        }
    }

    public void start() {
        shutdown = false;
        handle = executor.submit(this);
    }

    private void scheduleIdle() {
        if (shutdown) {
            return;
        }
        handle = executor.schedule(this, 500, TimeUnit.MILLISECONDS);
    }

    private void scheduleSentMessage() {
        if (shutdown) {
            return;
        }
        handle = executor.schedule(this, 10, TimeUnit.MILLISECONDS);
    }

    private void scheduleRateLimit() {
        if (shutdown) {
            return;
        }
        handle = executor.schedule(this, 1, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        // Make sure that we don't send any packets before sending auth info.
        if (!client.sentAuthInfo) {
            scheduleIdle();
            return;
        }

        ConnectionRequest audioRequest = null;
        DataObject chunkRequest = null;

        boolean hasLock = false;

        try {
            api.setContext();
            attemptedToSend = false;
            needRateLimit = false;
            // We do this outside of the lock because otherwise we could potentially deadlock here
            audioRequest = client.getNextAudioConnectRequest();

            hasLock = queueLock.tryLock() || queueLock.tryLock(10, TimeUnit.SECONDS);
            if (!hasLock) {
                scheduleNext();
                return;
            }

            chunkRequest = chunkQueue.peek();
            if (chunkRequest != null) {
                handleChunkSync(chunkRequest);
            } else if (audioRequest != null) {
                handleAudioRequest(audioRequest);
            } else {
                handleNormalRequest();
            }
        } catch (InterruptedException ignored) {
            LOG.debug("Main WS send thread interrupted. Most likely JDA is disconnecting the websocket.");
            return;
        } catch (Throwable ex) {
            // Log error
            LOG.error("Encountered error in gateway worker", ex);

            if (!attemptedToSend) {
                // Try to remove the failed request
                if (chunkRequest != null) {
                    client.chunkSyncQueue.remove(chunkRequest);
                } else if (audioRequest != null) {
                    client.removeAudioConnection(audioRequest.getGuildIdLong());
                }
            }

            // Rethrow if error to kill thread
            if (ex instanceof Error) {
                throw (Error) ex;
            }
        } finally {
            if (hasLock) {
                queueLock.unlock();
            }
        }

        scheduleNext();
    }

    private void scheduleNext() {
        try {
            if (needRateLimit) {
                scheduleRateLimit();
            } else if (!attemptedToSend) {
                scheduleIdle();
            } else {
                scheduleSentMessage();
            }
        } catch (RejectedExecutionException ex) {
            if (api.getStatus() == JDA.Status.SHUTTING_DOWN || api.getStatus() == JDA.Status.SHUTDOWN) {
                LOG.debug("Rejected task after shutdown", ex);
            } else {
                LOG.error("Was unable to schedule next packet due to rejected execution by threadpool", ex);
            }
        }
    }

    private void handleChunkSync(DataObject chunkOrSyncRequest) {
        LOG.debug("Sending chunk/sync request {}", chunkOrSyncRequest);
        boolean success = send(
                DataObject.empty().put("op", WebSocketCode.MEMBER_CHUNK_REQUEST).put("d", chunkOrSyncRequest));

        if (success) {
            chunkQueue.remove();
        }
    }

    private void handleAudioRequest(ConnectionRequest audioRequest) {
        long channelId = audioRequest.getChannelId();
        long guildId = audioRequest.getGuildIdLong();
        Guild guild = api.getGuildById(guildId);
        if (guild == null) {
            LOG.debug("Discarding voice request due to null guild {}", guildId);
            // race condition on guild delete, avoid NPE on DISCONNECT requests
            queuedAudioConnections.remove(guildId);
            return;
        }
        ConnectionStage stage = audioRequest.getStage();
        AudioManager audioManager = guild.getAudioManager();
        DataObject packet;
        switch (stage) {
            case RECONNECT:
            case DISCONNECT:
                packet = newVoiceClose(guildId);
                break;
            default:
            case CONNECT:
                packet = newVoiceOpen(audioManager, channelId, guild.getIdLong());
        }
        LOG.debug("Sending voice request {}", packet);
        if (send(packet)) {
            // If we didn't get RateLimited, Next request attempt will be 10 seconds from now
            // we remove it in VoiceStateUpdateHandler once we hear that it has updated our status
            // in 10 seconds we will attempt again in case we did not receive an update
            audioRequest.setNextAttemptEpoch(System.currentTimeMillis() + 10000);
            // If we are already in the correct state according to voice state
            // we will not receive a VOICE_STATE_UPDATE that would remove it
            // thus we update it here
            GuildVoiceState voiceState = guild.getSelfMember().getVoiceState();
            client.updateAudioConnection0(guild.getIdLong(), voiceState.getChannel());

            // [kamibot-patch] stuck-queue 상한 포기.
            // DISCONNECT/RECONNECT를 보냈는데 voiceState상 봇이 여전히 채널에 있으면
            // VOICE_STATE_UPDATE(channel=null)이 오지 않아 큐에서 영원히 retry 돈다
            // (게이트웨이 프록시 장애 시). 이 상태가 MAX_STUCK_ATTEMPTS회 이상 지속되면
            // 요청을 큐에서 제거해 무한 재발사로 인한 큐 포화를 막는다.
            // updateAudioConnection0가 위에서 이미 정상 완료된 요청은 제거했으므로,
            // 여기서 큐에 남아있다는 것은 아직 반영되지 않았다는 뜻이다.
            if (stage == ConnectionStage.DISCONNECT || stage == ConnectionStage.RECONNECT) {
                net.dv8tion.jda.api.entities.channel.middleman.AudioChannel voiceCh =
                        voiceState != null ? voiceState.getChannel() : null;
                if (voiceCh != null) {
                    int attempts = audioRequest.incrementAttemptCount();
                    if (attempts >= MAX_STUCK_ATTEMPTS) {
                        LOG.warn("[kamibot-patch] Giving up stuck op4 after {} attempts, removing from queue: "
                                        + "stage={} guild={} requestCh={} voiceCh={}",
                                attempts, stage, guildId, channelId, voiceCh.getIdLong());
                        client.removeAudioConnection(guildId);
                    } else {
                        LOG.warn("[STUCK-CANDIDATE] op4 stage={} sent but voiceState still in channel "
                                        + "(attempt {}/{}): guild={} requestCh={} voiceCh={}",
                                stage, attempts, MAX_STUCK_ATTEMPTS, guildId, channelId, voiceCh.getIdLong());
                    }
                }
            }
        }
    }

    private void handleNormalRequest() {
        DataObject message = ratelimitQueue.peek();
        if (message != null) {
            LOG.debug("Sending normal message {}", message);
            if (send(message)) {
                ratelimitQueue.remove();
            }
        }
    }

    // returns true if send was successful
    private boolean send(DataObject request) {
        needRateLimit = !client.send(request, false);
        attemptedToSend = true;
        return !needRateLimit;
    }

    protected DataObject newVoiceClose(long guildId) {
        return DataObject.empty()
                .put("op", WebSocketCode.VOICE_STATE)
                .put(
                        "d",
                        DataObject.empty()
                                .put("guild_id", Long.toUnsignedString(guildId))
                                .putNull("channel_id")
                                .put("self_mute", false)
                                .put("self_deaf", false));
    }

    protected DataObject newVoiceOpen(AudioManager manager, long channel, long guild) {
        return DataObject.empty()
                .put("op", WebSocketCode.VOICE_STATE)
                .put(
                        "d",
                        DataObject.empty()
                                .put("guild_id", guild)
                                .put("channel_id", channel)
                                .put("self_mute", manager.isSelfMuted())
                                .put("self_deaf", manager.isSelfDeafened()));
    }
}
