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

package net.dv8tion.jda.internal.audio;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.internal.utils.EntityString;

public class ConnectionRequest {
    protected final long guildId;
    protected long nextAttemptEpoch;
    protected ConnectionStage stage;
    protected long channelId;
    // [kamibot-patch] DISCONNECT/RECONNECT OP4가 게이트웨이 응답(VSU) 없이 재발사된 횟수.
    // 게이트웨이 프록시 장애로 VOICE_STATE_UPDATE(channel=null)이 영영 오지 않으면
    // 이 요청이 10초마다 무한 retry되어 큐가 포화되므로, 상한을 두고 포기(제거)하기 위한 카운터.
    protected int attemptCount;

    public ConnectionRequest(Guild guild) {
        this.stage = ConnectionStage.DISCONNECT;
        this.guildId = guild.getIdLong();
    }

    public ConnectionRequest(AudioChannel channel, ConnectionStage stage) {
        this.channelId = channel.getIdLong();
        this.guildId = channel.getGuild().getIdLong();
        this.stage = stage;
        this.nextAttemptEpoch = System.currentTimeMillis();
    }

    public void setStage(ConnectionStage stage) {
        // [kamibot-patch] stage 전환은 새로운 연결 시도 사이클의 시작이므로
        // stuck 재시도 카운터를 리셋한다 (직전 stage에서 누적된 실패를 이월하지 않음).
        if (this.stage != stage) {
            this.attemptCount = 0;
        }
        this.stage = stage;
    }

    public void setChannel(AudioChannel channel) {
        this.channelId = channel.getIdLong();
    }

    public void setNextAttemptEpoch(long epochMillis) {
        this.nextAttemptEpoch = epochMillis;
    }

    /** [kamibot-patch] 재발사 횟수를 1 증가시키고 누적값을 반환한다. */
    public int incrementAttemptCount() {
        return ++attemptCount;
    }

    /** [kamibot-patch] 누적 재발사 횟수. */
    public int getAttemptCount() {
        return attemptCount;
    }

    public AudioChannel getChannel(JDA api) {
        return (AudioChannel) api.getGuildChannelById(channelId);
    }

    public long getChannelId() {
        return channelId;
    }

    public ConnectionStage getStage() {
        return stage;
    }

    public long getNextAttemptEpoch() {
        return nextAttemptEpoch;
    }

    public long getGuildIdLong() {
        return guildId;
    }

    @Override
    public String toString() {
        return new EntityString(this)
                .setType(stage)
                .addMetadata("guildId", Long.toUnsignedString(guildId))
                .addMetadata("channelId", Long.toUnsignedString(channelId))
                .toString();
    }
}
