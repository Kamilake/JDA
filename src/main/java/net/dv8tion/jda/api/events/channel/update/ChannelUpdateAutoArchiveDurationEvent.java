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

package net.dv8tion.jda.api.events.channel.update;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelField;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import javax.annotation.Nonnull;

/**
 * Indicates that a {@link Channel Channel's} auto archive duration has been updated.
 *
 * <p>Can be used to retrieve the old auto archive duration and the new one.
 *
 * <p>Limited to {@link ThreadChannel Thread Channels}.
 *
 * @see ThreadChannel#getAutoArchiveDuration()
 * @see ThreadChannel.AutoArchiveDuration
 * @see ChannelField#AUTO_ARCHIVE_DURATION
 */
public class ChannelUpdateAutoArchiveDurationEvent extends GenericChannelUpdateEvent<ThreadChannel.AutoArchiveDuration>
{
    public static final ChannelField FIELD = ChannelField.AUTO_ARCHIVE_DURATION;
    public static final String IDENTIFIER = FIELD.getFieldName();

    public ChannelUpdateAutoArchiveDurationEvent(@Nonnull JDA api, long responseNumber, Channel channel, ThreadChannel.AutoArchiveDuration oldValue, ThreadChannel.AutoArchiveDuration newValue)
    {
        super(api, responseNumber, channel, FIELD, oldValue, newValue);
    }
}
