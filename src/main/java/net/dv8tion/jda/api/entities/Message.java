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
package net.dv8tion.jda.api.entities;

import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.Component;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.MessageTopLevelComponentUnion;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.tree.ComponentTree;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.components.utils.ComponentIterator;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.entities.messages.MessagePoll;
import net.dv8tion.jda.api.entities.messages.MessageSnapshot;
import net.dv8tion.jda.api.entities.sticker.GuildSticker;
import net.dv8tion.jda.api.entities.sticker.Sticker;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.interactions.IntegrationOwners;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import net.dv8tion.jda.api.requests.restaction.pagination.PollVotersPaginationAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;
import net.dv8tion.jda.api.utils.AttachedFile;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.NamedAttachmentProxy;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.dv8tion.jda.api.utils.messages.MessagePollData;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.ReceivedMessage;
import net.dv8tion.jda.internal.entities.channel.mixin.middleman.MessageChannelMixin;
import net.dv8tion.jda.internal.requests.restaction.MessageCreateActionImpl;
import net.dv8tion.jda.internal.requests.restaction.pagination.PollVotersPaginationActionImpl;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.Helpers;
import okhttp3.MultipartBody;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a Text message received from Discord.
 * <br>This represents messages received from {@link MessageChannel MessageChannels}.
 *
 * <p><b>This type is not updated. JDA does not keep track of changes to messages, it is advised to do this via events such
 * as {@link net.dv8tion.jda.api.events.message.MessageUpdateEvent MessageUpdateEvent} and similar.</b>
 *
 * <p><b>Message Differences</b><br>
 * There are 2 implementations of this interface in JDA.
 * <ol>
 *     <li><b>Received Message</b>
 *     <br>Messages received through events or history query.
 *         These messages hold information of <b>existing</b> messages and
 *         can be modified or deleted.</li>
 *     <li><b>System Message</b>
 *     <br>Specification of <b>Received Messages</b> that are generated by Discord
 *         on certain events. Commonly this is used in groups or to indicate a pin within a MessageChannel.
 *         The different types can be found in the {@link net.dv8tion.jda.api.entities.MessageType MessageType} enum.</li>
 * </ol>
 *
 * <p>When a feature is not available it will throw an {@link java.lang.UnsupportedOperationException UnsupportedOperationException}
 * as per interface specifications.
 * <br>Specific operations may have specified information available in the {@code throws} javadoc.
 *
 * <p><b>Formattable</b><br>
 * This interface extends {@link java.util.Formattable Formattable} and can be used with a {@link java.util.Formatter Formatter}
 * such as used by {@link String#format(String, Object...) String.format(String, Object...)}
 * or {@link java.io.PrintStream#printf(String, Object...) PrintStream.printf(String, Object...)}.
 *
 * <p>This will use {@link #getContentDisplay()} rather than {@link Object#toString()}!
 * <br>Supported Features:
 * <ul>
 *     <li><b>Alternative</b>
 *     <br>   - Using {@link #getContentRaw()}
 *              (Example: {@code %#s} - uses {@link #getContentDisplay()})</li>
 *
 *     <li><b>Width/Left-Justification</b>
 *     <br>   - Ensures the size of a format
 *              (Example: {@code %20s} - uses at minimum 20 chars;
 *              {@code %-10s} - uses left-justified padding)</li>
 *
 *     <li><b>Precision</b>
 *     <br>   - Cuts the content to the specified size
 *              (replacing last 3 chars with {@code ...}; Example: {@code %.20s})</li>
 * </ul>
 *
 * <p>More information on formatting syntax can be found in the {@link java.util.Formatter format syntax documentation}!
 *
 * @see MessageChannel#getIterableHistory()
 * @see MessageChannel#getHistory()
 * @see MessageChannel#getHistoryAfter(String, int)
 * @see MessageChannel#getHistoryBefore(String, int)
 * @see MessageChannel#getHistoryAround(String, int)
 * @see MessageChannel#getHistoryFromBeginning(int)
 * @see MessageChannel#retrieveMessageById(String)
 *
 * @see MessageChannel#deleteMessageById(String)
 * @see MessageChannel#editMessageById(String, CharSequence)
 */
public interface Message extends ISnowflake, Formattable
{
    /** Template for {@link #getJumpUrl()}.*/
    String JUMP_URL = "https://discord.com/channels/%s/%s/%s";

    /**
     * The maximum sendable file size (10 MiB)
     *
     *  @see MessageRequest#setFiles(Collection)
     */
    int MAX_FILE_SIZE = 10 << 20;

    /**
     * The maximum amount of files sendable within a single message ({@value})
     *
     * @see MessageRequest#setFiles(Collection)
     */
    int MAX_FILE_AMOUNT = 10;

    /**
     * The maximum amount of characters sendable in one message. ({@value})
     * <br>This only applies to the raw content and not embeds!
     *
     * @see MessageRequest#setContent(String)
     */
    int MAX_CONTENT_LENGTH = 2000;

    /**
     * The maximum amount of characters sendable in one message. ({@value})
     * <br>This only applies when {@linkplain #isUsingComponentsV2() V2 Components} are enabled.
     *
     * <p>Unlike {@link #MAX_CONTENT_LENGTH}, the amount of characters is calculated from all the components.
     *
     * @see MessageRequest#useComponentsV2()
     */
    int MAX_CONTENT_LENGTH_COMPONENT_V2 = 4000;

   /**
    * The maximum amount of reactions that can be added to one message ({@value})
    *
    * @see Message#addReaction(Emoji)
    */
    int MAX_REACTIONS = 20;

    /**
     * The maximum amount of Embeds that can be added to one message ({@value})
     *
     * @see    MessageChannel#sendMessageEmbeds(Collection)
     * @see    MessageRequest#setEmbeds(Collection)
     */
    int MAX_EMBED_COUNT = 10;

    /**
     * The maximum amount of {@link Sticker Stickers} that can be added to a message ({@value})
     *
     * @see GuildMessageChannel#sendStickers(StickerSnowflake...)
     * @see MessageCreateAction#setStickers(StickerSnowflake...)
     */
    int MAX_STICKER_COUNT = 3;

    /**
     * The maximum amount of {@link MessageTopLevelComponent MessageTopLevelComponents} that can be added to a message's {@link #getComponents() root component list} when using the legacy component system.  ({@value})
     */
    int MAX_COMPONENT_COUNT = 5;

    /**
     * The maximum amount of {@link Component components} that can be added to a message including nested components. ({@value})
     */
    int MAX_COMPONENT_COUNT_IN_COMPONENT_TREE = 40;

    /**
     * The maximum character length for a {@link #getNonce() nonce} ({@value})
     */
    int MAX_NONCE_LENGTH = 25;

    /**
     * Pattern used to find instant invites in strings.
     *
     * <p>The only named group is at index 1 with the name {@code "code"}.
     *
     * @see #getInvites()
     */
    Pattern INVITE_PATTERN = Pattern.compile(
            "(?:https?://)?" +                     // Scheme
            "(?:\\w+\\.)?" +                       // Subdomain
            "discord(?:(?:app)?\\.com" +           // Discord domain
            "/invite[/\\\\]|\\.gg/)(?<code>[a-z0-9-]+)" + // Path
            "(?:\\?\\S*)?(?:#\\S*)?",              // Useless query or URN appendix
            Pattern.CASE_INSENSITIVE);

    /**
     * Pattern used to find {@link #getJumpUrl() Jump URLs} in strings.
     *
     * <p><b>Groups</b><br>
     * <table>
     *   <caption style="display: none">Javadoc is stupid, this is not a required tag</caption>
     *   <tr>
     *     <th>Index</th>
     *     <th>Name</th>
     *     <th>Description</th>
     *   </tr>
     *   <tr>
     *     <td>0</td>
     *     <td>N/A</td>
     *     <td>The entire link</td>
     *   </tr>
     *   <tr>
     *     <td>1</td>
     *     <td>guild</td>
     *     <td>The ID of the target guild</td>
     *   </tr>
     *   <tr>
     *     <td>2</td>
     *     <td>channel</td>
     *     <td>The ID of the target channel</td>
     *   </tr>
     *   <tr>
     *     <td>3</td>
     *     <td>message</td>
     *     <td>The ID of the target message</td>
     *   </tr>
     * </table>
     * You can use the names with {@link java.util.regex.Matcher#group(String) Matcher.group(String)}
     * and the index with {@link java.util.regex.Matcher#group(int) Matcher.group(int)}.
     *
     * @see #getJumpUrl()
     */
    Pattern JUMP_URL_PATTERN = Pattern.compile(
            "(?:https?://)?" +                                             // Scheme
            "(?:\\w+\\.)?" +                                               // Subdomain
            "discord(?:app)?\\.com" +                                      // Discord domain
            "/channels/(?<guild>\\d+)/(?<channel>\\d+)/(?<message>\\d+)" + // Path
            "(?:\\?\\S*)?(?:#\\S*)?",                                      // Useless query or URN appendix
            Pattern.CASE_INSENSITIVE);

    /**
     * Suppresses the warning for missing the {@link net.dv8tion.jda.api.requests.GatewayIntent#MESSAGE_CONTENT MESSAGE_CONTENT} intent and using one of the dependent getters.
     */
    static void suppressContentIntentWarning()
    {
        ReceivedMessage.didContentIntentWarning = true;
    }

    /**
     * Returns the {@link MessageReference} for this Message. This will be null if this Message has no reference.
     *
     * <p>This will have different meaning depending on the message {@link #getType() type}.
     * The following are the message types where a reference will be present:
     * <ul>
     *     <li>{@link MessageType#INLINE_REPLY INLINE_REPLY}</li>
     *     <li>{@link MessageType#THREAD_STARTER_MESSAGE THREAD_STARTER_MESSAGE}</li>
     *     <li>{@link MessageType#CONTEXT_COMMAND CONTEXT_COMMAND} (message context command)</li>
     * </ul>
     *
     * <p>You can access all the information about a reference through this object.
     * Additionally, you can retrieve the referenced Message if discord did not load it in time. This can be done with {@link MessageReference#resolve()}.
     *
     * @return The message reference, or null.
     */
    @Nullable
    MessageReference getMessageReference();

    /**
     * Referenced message.
     *
     * <p>This will have different meaning depending on the message {@link #getType() type}.
     * The following are the message types where a reference can be present:
     * <ul>
     *     <li>{@link MessageType#INLINE_REPLY INLINE_REPLY}</li>
     *     <li>{@link MessageType#THREAD_STARTER_MESSAGE THREAD_STARTER_MESSAGE}</li>
     *     <li>{@link MessageType#CONTEXT_COMMAND CONTEXT_COMMAND} (message context command)</li>
     * </ul>
     *
     * <p>This can be null even if the type is {@link MessageType#INLINE_REPLY INLINE_REPLY}, when the message it references doesn't exist or discord wasn't able to resolve it in time.
     *
     * <p>This differs from a {@link MessageReference}, which contains the raw IDs attached to the reference, and allows you to retrieve the referenced message
     *
     * @return The referenced message, or null
     *
     * @see #getMessageReference()
     */
    @Nullable
    default Message getReferencedMessage()
    {
        return getMessageReference() != null
                ? getMessageReference().getMessage()
                : null;
    }

    /**
     * The {@link Mentions} used in this message.
     *
     * <p>This includes {@link Member Members}, {@link GuildChannel GuildChannels}, {@link Role Roles}, and {@link CustomEmoji CustomEmojis}.
     * Can also be used to check if a message mentions {@code @everyone} or {@code @here}.
     *
     * <p><b>Example</b><br>
     * {@code
     * System.out.println("Message mentioned these users: " + message.getMentions().getUsers());
     * System.out.println("Message used these custom emojis: " + message.getMentions().getCustomEmojis());
     * }
     *
     * @return {@link Mentions} for this message.
     */
    @Nonnull
    Mentions getMentions();

    /**
     * Returns whether or not this Message has been edited before.
     *
     * @return True if this message has been edited.
     */
    boolean isEdited();

    /**
     * Provides the {@link java.time.OffsetDateTime OffsetDateTime} defining when this Message was last
     * edited. If this Message has not been edited ({@link #isEdited()} is {@code false}), then this method
     * will return {@code null}.
     *
     * @return Time of the most recent edit, or {@code null} if the Message has never been edited.
     */
    @Nullable
    OffsetDateTime getTimeEdited();

    /**
     * The author of this Message
     *
     * @return Message author
     */
    @Nonnull
    User getAuthor();

    /**
     * Returns the author of this Message as a {@link net.dv8tion.jda.api.entities.Member member}.
     * <br><b>This is only valid if the Message was actually sent in a GuildMessageChannel.</b> This will return {@code null}
     * if the message was not sent in a GuildMessageChannel, or if the message was sent by a Webhook (including apps).
     * <br>You can check the type of channel this message was sent from using {@link #isFromType(ChannelType)} or {@link #getChannelType()}.
     *
     * <p>Discord does not provide a member object for messages returned by {@link RestAction RestActions} of any kind.
     * This will return null if the message was retrieved through {@link MessageChannel#retrieveMessageById(long)} or similar means,
     * unless the member is already cached.
     *
     * @return Message author, or {@code null} if the message was not sent in a GuildMessageChannel, or if the message was sent by a Webhook.
     *
     * @see    #isWebhookMessage()
     */
    @Nullable
    Member getMember();

    /**
     * Returns the approximate position of this message in a {@link ThreadChannel}.
     * <br>This can be used to estimate the relative position of a message in a thread, by comparing against {@link ThreadChannel#getTotalMessageCount()}.
     *
     * <p><b>Notes:</b>
     * <ul>
     *     <li>The position might contain gaps or duplicates.</li>
     *     <li>The position is not set on messages sent earlier than July 19th, 2022, and will return -1.</li>
     * </ul>
     *
     * @throws IllegalStateException
     *         If this message was not sent in a {@link ThreadChannel}.
     *
     * @return The approximate position of this message, or {@code -1} if this message is too old.
     *
     * @see    <a href="https://discord.com/developers/docs/resources/channel#message-object" target="_blank">Discord docs: <code>position</code> property on the message object</a>
     */
    int getApproximatePosition();

    /**
     * Returns the jump-to URL for the received message. Clicking this URL in the Discord client will cause the client to
     * jump to the specified message.
     *
     * @return A String representing the jump-to URL for the message
     */
    @Nonnull
    String getJumpUrl();

    /**
     * The textual content of this message in the format that would be shown to the Discord client. All
     * {@link net.dv8tion.jda.api.entities.IMentionable IMentionable} entities will be resolved to the format
     * shown by the Discord client instead of the {@literal <id>} format.
     *
     * <p>This includes resolving:
     * <br>{@link User Users} / {@link net.dv8tion.jda.api.entities.Member Members}
     * to their @Username/@Nickname format,
     * <br>{@link GuildChannel GuildChannels} to their #ChannelName format,
     * <br>{@link net.dv8tion.jda.api.entities.Role Roles} to their @RoleName format
     * <br>{@link CustomEmoji Custom Emojis} (not unicode emojis!) to their {@code :name:} format.
     *
     * <p>If you want the actual Content (mentions as {@literal <@id>}), use {@link #getContentRaw()} instead
     *
     * <p><b>Requires {@link net.dv8tion.jda.api.requests.GatewayIntent#MESSAGE_CONTENT GatewayIntent.MESSAGE_CONTENT}</b>
     *
     * @return The textual content of the message with mentions resolved to be visually like the Discord client.
     */
    @Nonnull
    String getContentDisplay();

    /**
     * The raw textual content of this message. Does not resolve {@link net.dv8tion.jda.api.entities.IMentionable IMentionable}
     * entities like {@link #getContentDisplay()} does. This means that this is the completely raw textual content of the message
     * received from Discord and can contain mentions specified by
     * <a href="https://discord.com/developers/docs/resources/channel#message-formatting" target="_blank">Discord's Message Formatting</a>.
     *
     * <p><b>Requires {@link net.dv8tion.jda.api.requests.GatewayIntent#MESSAGE_CONTENT GatewayIntent.MESSAGE_CONTENT}</b>
     *
     * @return The raw textual content of the message, containing unresolved Discord message formatting.
     */
    @Nonnull
    String getContentRaw();

    /**
     * Gets the textual content of this message using {@link #getContentDisplay()} and then strips it of markdown characters
     * like {@literal *, **, __, ~~, ||} that provide text formatting. Any characters that match these but are not being used
     * for formatting are escaped to prevent possible formatting.
     *
     * <p><b>Requires {@link net.dv8tion.jda.api.requests.GatewayIntent#MESSAGE_CONTENT GatewayIntent.MESSAGE_CONTENT}</b>
     *
     * @return The textual content from {@link #getContentDisplay()} with all text formatting characters removed or escaped.
     */
    @Nonnull
    String getContentStripped();

    /**
     * Creates an immutable List of {@link net.dv8tion.jda.api.entities.Invite Invite} codes
     * that are included in this Message.
     * <br>This will use the {@link java.util.regex.Pattern Pattern} provided
     * under {@link #INVITE_PATTERN} to construct a {@link java.util.regex.Matcher Matcher} that will
     * parse the {@link #getContentRaw()} output and include all codes it finds in a list.
     *
     * <p>You can use the codes to retrieve/validate invites via
     * {@link net.dv8tion.jda.api.entities.Invite#resolve(JDA, String) Invite.resolve(JDA, String)}
     *
     * @return Immutable list of invite codes
     */
    @Nonnull
    @Unmodifiable
    List<String> getInvites();

    /**
     * Validation <a href="https://en.wikipedia.org/wiki/Cryptographic_nonce" target="_blank" >nonce</a> for this Message
     * <br>This can be used to validate that a Message was properly sent to the Discord Service.
     * <br>To set a nonce before sending you may use {@link MessageCreateAction#setNonce(String) MessageCreateAction.setNonce(String)}!
     *
     * @return The validation nonce
     *
     * @see    MessageCreateAction#setNonce(String)
     * @see    <a href="https://en.wikipedia.org/wiki/Cryptographic_nonce" target="_blank">Cryptographic Nonce - Wikipedia</a>
     */
    @Nullable
    String getNonce();

    /**
     * Used to determine if this Message was received from a {@link MessageChannel}
     * of the {@link net.dv8tion.jda.api.entities.channel.ChannelType ChannelType} specified.
     *
     * <p>Useful for restricting functionality to a certain type of channels.
     *
     * @param  type
     *         The {@link ChannelType ChannelType} to check against.
     *
     * @return True if the {@link net.dv8tion.jda.api.entities.channel.ChannelType ChannelType} which this message was received
     *         from is the same as the one specified by {@code type}.
     */
    boolean isFromType(@Nonnull ChannelType type);

    /**
     * Whether this message was sent in a {@link Guild Guild}.
     * <br>If this is {@code false} then {@link #getGuild()} will throw an {@link java.lang.IllegalStateException}.
     *
     * @return True, if {@link #getChannelType()}.{@link ChannelType#isGuild() isGuild()} is true.
     */
    boolean isFromGuild();

    /**
     * Gets the {@link net.dv8tion.jda.api.entities.channel.ChannelType ChannelType} that this message was received from.
     *
     * @return The ChannelType which this message was received from.
     */
    @Nonnull
    ChannelType getChannelType();

    /**
     * Indicates if this Message was sent either by a {@link net.dv8tion.jda.api.entities.Webhook Webhook} or an app,
     * instead of a {@link User User}.
     * <br>Useful if you want to ignore non-users.
     *
     * @return True if this message was sent by a {@link net.dv8tion.jda.api.entities.Webhook Webhook}.
     */
    boolean isWebhookMessage();

    /**
     * If this message is from an application-owned {@link net.dv8tion.jda.api.entities.Webhook Webhook} or
     * is a response to an {@link net.dv8tion.jda.api.interactions.Interaction Interaction}, this will return
     * the application's id.
     * 
     * @return The application's id or {@code null} if this message was not sent by an application
     */
    @Nullable
    default String getApplicationId()
    {
        return getApplicationIdLong() == 0 ? null : Long.toUnsignedString(getApplicationIdLong());
    }

    /**
     * If this message is from an application-owned {@link net.dv8tion.jda.api.entities.Webhook Webhook} or
     * is a response to an {@link net.dv8tion.jda.api.interactions.Interaction Interaction}, this will return
     * the application's id.
     * 
     * @return The application's id or 0 if this message was not sent by an application
     */
    long getApplicationIdLong();

    /**
     * Whether this message instance has an available {@link #getChannel()}.
     *
     * <p>This can be {@code false} for messages sent via webhooks, or in the context of interactions.
     *
     * @return True, if {@link #getChannel()} is available
     */
    boolean hasChannel();

    /**
     * The ID for the channel this message was sent in.
     * <br>This is useful when {@link #getChannel()} is unavailable, for instance on webhook messages.
     *
     * @return The channel id
     */
    long getChannelIdLong();

    /**
     * The ID for the channel this message was sent in.
     * <br>This is useful when {@link #getChannel()} is unavailable, for instance on webhook messages.
     *
     * @return The channel id
     */
    @Nonnull
    default String getChannelId()
    {
        return Long.toUnsignedString(getChannelIdLong());
    }

    /**
     * Returns the {@link MessageChannel} that this message was sent in.
     *
     * @throws IllegalStateException
     *         If the channel is not available (see {@link #hasChannel()})
     *
     * @return The MessageChannel of this Message
     *
     * @see    #hasChannel()
     * @see    #getChannelIdLong()
     */
    @Nonnull
    MessageChannelUnion getChannel();

    /**
     * Returns the {@link GuildMessageChannel} that this message was sent in
     *  if it was sent in a Guild.
     *
     * @throws java.lang.IllegalStateException
     *         If this was not sent in a {@link Guild} or the channel is not available (see {@link #hasChannel()}).
     *
     * @return The MessageChannel of this Message
     *
     * @see    #hasChannel()
     * @see    #getChannelIdLong()
     */
    @Nonnull
    GuildMessageChannelUnion getGuildChannel();

    /**
     * The {@link Category Category} this
     * message was sent in. This will always be {@code null} for DMs.
     * <br>Equivalent to {@code getGuildChannel().getParentCategory()} if this was sent in a {@link GuildMessageChannel}.
     *
     * @return {@link net.dv8tion.jda.api.entities.channel.concrete.Category Category} for this message
     */
    @Nullable
    Category getCategory();

    /**
     * Whether this message instance provides a guild instance via {@link #getGuild()}.
     * <br>This is different from {@link #isFromGuild()}, which checks whether the message was sent in a guild.
     * This method describes whether {@link #getGuild()} is usable.
     *
     * <p>This can be {@code false} for messages sent via webhooks, or in the context of interactions.
     *
     * @return True, if {@link #getGuild()} is provided
     */
    boolean hasGuild();

    /**
     * The ID for the guild this message was sent in.
     * <br>This is useful when {@link #getGuild()} is not provided, for instance on webhook messages.
     *
     * @return The guild id, or 0 if this message was not sent in a guild
     */
    long getGuildIdLong();

    /**
     * The ID for the guild this message was sent in.
     * <br>This is useful when {@link #getGuild()} is not provided, for instance on webhook messages.
     *
     * @return The guild id, or null if this message was not sent in a guild
     */
    @Nullable
    default String getGuildId()
    {
        return isFromGuild() ? Long.toUnsignedString(getGuildIdLong()) : null;
    }

    /**
     * Returns the {@link Guild Guild} that this message was sent in.
     * <br>This is just a shortcut to {@link #getGuildChannel()}{@link GuildChannel#getGuild() .getGuild()}.
     * <br><b>This is only valid if the Message was actually sent in a GuildMessageChannel.</b>
     * <br>You can check the type of channel this message was sent from using {@link #isFromType(ChannelType)} or {@link #getChannelType()}.
     *
     * @throws java.lang.IllegalStateException
     *         If this was not sent in a {@link GuildChannel} or the guild instance is not provided
     *
     * @return The Guild this message was sent in
     *
     * @see    #isFromGuild()
     * @see    #isFromType(ChannelType)
     * @see    #getChannelType()
     */
    @Nonnull
    Guild getGuild();

    /**
     * An immutable list of {@link net.dv8tion.jda.api.entities.Message.Attachment Attachments} that are attached to this message.
     * <br>Most likely this will only ever be 1 {@link net.dv8tion.jda.api.entities.Message.Attachment Attachment} at most.
     *
     * <p><b>Requires {@link net.dv8tion.jda.api.requests.GatewayIntent#MESSAGE_CONTENT GatewayIntent.MESSAGE_CONTENT}</b>
     *
     * @return Immutable list of {@link net.dv8tion.jda.api.entities.Message.Attachment Attachments}.
     */
    @Nonnull
    @Unmodifiable
    List<Attachment> getAttachments();

    /**
     * An immutable list of {@link net.dv8tion.jda.api.entities.MessageEmbed MessageEmbeds} that are part of this Message.
     *
     * <p><b>Requires {@link net.dv8tion.jda.api.requests.GatewayIntent#MESSAGE_CONTENT GatewayIntent.MESSAGE_CONTENT}</b>
     *
     * @return Immutable list of all given MessageEmbeds.
     */
    @Nonnull
    @Unmodifiable
    List<MessageEmbed> getEmbeds();

    /**
     * Layouts of interactive components, usually {@link ActionRow ActionRows}.
     * <br>You can use {@link MessageRequest#setComponents(MessageTopLevelComponent...)} to update these.
     *
     * <p><b>Requires {@link net.dv8tion.jda.api.requests.GatewayIntent#MESSAGE_CONTENT GatewayIntent.MESSAGE_CONTENT}</b>
     *
     * @return Immutable {@link List} of {@link MessageTopLevelComponent}
     *
     * @see    #getActionRows()
     * @see    #getButtons()
     * @see    #getButtonById(String)
     */
    @Nonnull
    @Unmodifiable
    List<MessageTopLevelComponentUnion> getComponents();

    /**
     * Whether this message can contain V2 components.
     * <br>This checks for {@link MessageFlag#IS_COMPONENTS_V2}.
     *
     * @return {@code true} if this message has the components V2 flag
     *
     * @see MessageRequest#useComponentsV2()
     * @see MessageRequest#useComponentsV2(boolean)
     */
    boolean isUsingComponentsV2();

    /**
     * A {@link MessageComponentTree} constructed from {@link #getComponents()}.
     *
     * @return {@link MessageComponentTree}
     */
    @Nonnull
    default MessageComponentTree getComponentTree()
    {
        return MessageComponentTree.of(getComponents());
    }

    /**
     * The {@link MessagePoll} attached to this message.
     *
     * @return Possibly-null poll instance for this message
     *
     * @see    #endPoll()
     */
    @Nullable
    MessagePoll getPoll();

    /**
     * End the poll attached to this message.
     *
     * @throws IllegalStateException
     *         If this poll was not sent by the currently logged in account or no poll was attached to this message
     *
     * @return {@link AuditableRestAction} - Type: {@link Message}
     */
    @Nonnull
    @CheckReturnValue
    AuditableRestAction<Message> endPoll();

    /**
     * Paginate the users who voted for a poll answer.
     *
     * @param  answerId
     *         The id of the poll answer, usually the ordinal position of the answer (first is 1)
     *
     * @return {@link PollVotersPaginationAction}
     */
    @Nonnull
    @CheckReturnValue
    default PollVotersPaginationAction retrievePollVoters(long answerId)
    {
        return new PollVotersPaginationActionImpl(getJDA(), getChannelId(), getId(), answerId);
    }

    /**
     * Rows of interactive components such as {@link Button Buttons}.
     * <br>You can use {@link MessageRequest#setComponents(MessageTopLevelComponent...)} to update these.
     *
     * <p><b>Requires {@link net.dv8tion.jda.api.requests.GatewayIntent#MESSAGE_CONTENT GatewayIntent.MESSAGE_CONTENT}</b>
     *
     * @return Immutable {@link List} of {@link ActionRow}
     *
     * @see    #getButtons()
     * @see    #getButtonById(String)
     *
     * @deprecated
     *         Can be replaced with {@link ComponentTree#findAll(Class) getComponentTree().findAll(ActionRow.class)}
     */
    @Nonnull
    @Unmodifiable
    @Deprecated
    @ForRemoval
    default List<ActionRow> getActionRows()
    {
        return ComponentIterator.createStream(getComponents())
                .filter(ActionRow.class::isInstance)
                .map(ActionRow.class::cast)
                .collect(Helpers.toUnmodifiableList());
    }

    /**
     * All {@link Button Buttons} attached to this message.
     *
     * <p><b>Requires {@link net.dv8tion.jda.api.requests.GatewayIntent#MESSAGE_CONTENT GatewayIntent.MESSAGE_CONTENT}</b>
     *
     * @return Immutable {@link List} of {@link Button Buttons}
     *
     * @deprecated
     *         Can be replaced with {@link ComponentTree#findAll(Class) getComponentTree().findAll(Button.class)}
     */
    @Nonnull
    @Unmodifiable
    @Deprecated
    @ForRemoval
    default List<Button> getButtons()
    {
        return ComponentIterator.createStream(getComponents())
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Gets the {@link Button} with the specified ID.
     *
     * <p><b>Requires {@link net.dv8tion.jda.api.requests.GatewayIntent#MESSAGE_CONTENT GatewayIntent.MESSAGE_CONTENT}</b>
     *
     * @param  id
     *         The id of the button
     *
     * @throws IllegalArgumentException
     *         If the id is null
     *
     * @return The {@link Button} or null if no button with that ID is present on this message
     *
     * @deprecated
     *         Can be replaced with {@link ComponentTree#find(Class, Predicate) getComponentTree().find(Button.class, button -> id.equals(button.getCustomId()).orElse(null)}
     */
    @Nullable
    @Deprecated
    @ForRemoval
    default Button getButtonById(@Nonnull String id)
    {
        Checks.notNull(id, "Button ID");
        return getButtons().stream()
                .filter(it -> id.equals(it.getCustomId()))
                .findFirst().orElse(null);
    }

    /**
     * All {@link Button Buttons} with the specified label attached to this message.
     *
     * <p><b>Requires {@link net.dv8tion.jda.api.requests.GatewayIntent#MESSAGE_CONTENT GatewayIntent.MESSAGE_CONTENT}</b>
     *
     * @param  label
     *         The button label
     * @param  ignoreCase
     *         Whether to use {@link String#equalsIgnoreCase(String)} instead of {@link String#equals(Object)}
     *
     * @throws IllegalArgumentException
     *         If the provided label is null
     *
     * @return Immutable {@link List} of {@link Button Buttons} with the specified label
     *
     * @deprecated
     *         Can be replaced with {@link ComponentTree#findAll(Class, Predicate) getComponentTree().findAll(Button.class, button -> id.equals(button.getCustomId())},
     *         usage is discouraged since content displayed to an user can change, prefer using data owned by the bot,
     *         for example, in custom IDs or stored by the bot
     */
    @Nonnull
    @Unmodifiable
    @Deprecated
    @ForRemoval
    default List<Button> getButtonsByLabel(@Nonnull String label, boolean ignoreCase)
    {
        Checks.notNull(label, "Label");
        Predicate<Button> filter;
        if (ignoreCase)
            filter = b -> label.equalsIgnoreCase(b.getLabel());
        else
            filter = b -> label.equals(b.getLabel());
        return getButtons().stream()
                .filter(filter)
                .collect(Helpers.toUnmodifiableList());
    }

    /**
     * All {@link MessageReaction MessageReactions} that are on this Message.
     *
     * @return Immutable list of all MessageReactions on this message.
     *
     * @see    MessageReaction
     */
    @Nonnull
    @Unmodifiable
    List<MessageReaction> getReactions();

    /**
     * All {@link StickerItem StickerItems} that are in this Message.
     * <br>The returned StickerItems may only contain necessary information such as the sticker id, format type, name, and icon url.
     *
     * @return Immutable list of all StickerItems in this message.
     */
    @Nonnull
    @Unmodifiable
    List<StickerItem> getStickers();

    /**
     * The {@link MessageSnapshot MessageSnaphots} attached to this message.
     *
     * <p>This is used primarily for message forwarding.
     * The content of the forwarded message is provided as a snapshot at the time of forwarding.
     * When the message is edited or deleted, this snapshot remains unchanged.
     *
     * @return Immutable {@link List} of {@link MessageSnapshot}
     */
    @Nonnull
    @Unmodifiable
    List<MessageSnapshot> getMessageSnapshots();

    /**
     * Defines whether or not this Message triggers TTS (Text-To-Speech).
     *
     * @return If this message is TTS.
     */
    boolean isTTS();

    /**
     * A {@link net.dv8tion.jda.api.entities.MessageActivity MessageActivity} that contains its type and party id.
     *
     * @return The activity, or {@code null} if no activity was added to the message.
     */
    @Nullable
    MessageActivity getActivity();

    /**
     * Edits this message and updates the content.
     * <br>Any other fields of the message will remain unchanged,
     * you can use {@link net.dv8tion.jda.api.utils.messages.MessageEditRequest#setReplace(boolean) replace(true)} to remove everything else (embeds/attachments/components).
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The request was attempted after the account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed, or after {@link net.dv8tion.jda.api.Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL}
     *         was revoked in the {@link GuildMessageChannel GuildMessageChannel}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>The provided {@code messageId} is unknown in this MessageChannel, either due to the id being invalid, or
     *         the message it referred to has already been deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_CHANNEL UNKNOWN_CHANNEL}
     *     <br>The request was attempted after the channel was deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#INVALID_FORM_BODY INVALID_FORM_BODY}
     *     <br>{@linkplain MessageRequest#useComponentsV2(boolean) Components V2} is used by the to-be-edited message, and this request has non-empty content or embeds.</li>
     * </ul>
     *
     * @param  newContent
     *         The new content of the message, or empty string to remove content (assumes other fields exist like embeds)
     *
     * @throws UnsupportedOperationException
     *         If this is a system message
     * @throws IllegalStateException
     *         If the message is not authored by this bot
     * @throws IllegalArgumentException
     *         If null is provided or the new content is longer than {@value #MAX_CONTENT_LENGTH} characters
     *
     * @return {@link MessageEditAction}
     *
     * @see    MessageChannel#editMessageById(long, CharSequence)
     */
    @Nonnull
    @CheckReturnValue
    MessageEditAction editMessage(@Nonnull CharSequence newContent);

    /**
     * Edits this message using the provided {@link MessageEditData}.
     * <br>You can use {@link net.dv8tion.jda.api.utils.messages.MessageEditBuilder MessageEditBuilder} to create a {@link MessageEditData} instance.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The request was attempted after the account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed, or after {@link net.dv8tion.jda.api.Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL}
     *         was revoked in the {@link GuildMessageChannel}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>The provided {@code messageId} is unknown in this MessageChannel, either due to the id being invalid, or
     *         the message it referred to has already been deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_CHANNEL UNKNOWN_CHANNEL}
     *     <br>The request was attempted after the channel was deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#INVALID_FORM_BODY INVALID_FORM_BODY}
     *     <br>{@linkplain MessageRequest#useComponentsV2(boolean) Components V2} is used by the to-be-edited message, and this request has non-empty content or embeds.</li>
     * </ul>
     *
     * @param  data
     *         The {@link MessageEditData} used to update the message
     *
     * @throws UnsupportedOperationException
     *         If this is a system message
     * @throws IllegalStateException
     *         If the message is not authored by this bot
     * @throws IllegalArgumentException
     *         If null is provided
     *
     * @return {@link MessageEditAction}
     *
     * @see    net.dv8tion.jda.api.utils.messages.MessageEditBuilder MessageEditBuilder
     * @see    MessageChannel#editMessageById(long, MessageEditData)
     */
    @Nonnull
    @CheckReturnValue
    MessageEditAction editMessage(@Nonnull MessageEditData data);

    /**
     * Edits this message using the provided {@link MessageEmbed MessageEmbeds}.
     * <br>You can use {@link net.dv8tion.jda.api.EmbedBuilder EmbedBuilder} to create a {@link MessageEmbed} instance.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The request was attempted after the account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed, or after {@link net.dv8tion.jda.api.Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL}
     *         was revoked in the {@link GuildMessageChannel}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>The provided {@code messageId} is unknown in this MessageChannel, either due to the id being invalid, or
     *         the message it referred to has already been deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_CHANNEL UNKNOWN_CHANNEL}
     *     <br>The request was attempted after the channel was deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#INVALID_FORM_BODY INVALID_FORM_BODY}
     *     <br>{@linkplain MessageRequest#useComponentsV2(boolean) Components V2} is used by the to-be-edited message, and this request has non-empty content or embeds.</li>
     * </ul>
     *
     * @param  embeds
     *         The new {@link MessageEmbed MessageEmbeds} of the message, empty list to remove embeds
     *
     * @throws UnsupportedOperationException
     *         If this is a system message
     * @throws IllegalStateException
     *         If the message is not authored by this bot
     * @throws IllegalArgumentException
     *         <ul>
     *             <li>If {@code null} is provided</li>
     *             <li>If more than {@value Message#MAX_EMBED_COUNT} embeds are provided</li>
     *         </ul>
     *
     * @return {@link MessageEditAction}
     *
     * @see    net.dv8tion.jda.api.EmbedBuilder EmbedBuilder
     * @see    MessageChannel#editMessageEmbedsById(long, Collection)
     */
    @Nonnull
    @CheckReturnValue
    MessageEditAction editMessageEmbeds(@Nonnull Collection<? extends MessageEmbed> embeds);

    /**
     * Edits this message using the provided {@link MessageEmbed MessageEmbeds}.
     * <br>You can use {@link net.dv8tion.jda.api.EmbedBuilder EmbedBuilder} to create a {@link MessageEmbed} instance.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The request was attempted after the account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed, or after {@link net.dv8tion.jda.api.Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL}
     *         was revoked in the {@link GuildMessageChannel}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>The provided {@code messageId} is unknown in this MessageChannel, either due to the id being invalid, or
     *         the message it referred to has already been deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_CHANNEL UNKNOWN_CHANNEL}
     *     <br>The request was attempted after the channel was deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#INVALID_FORM_BODY INVALID_FORM_BODY}
     *     <br>{@linkplain MessageRequest#useComponentsV2(boolean) Components V2} is used by the to-be-edited message, and this request has non-empty content or embeds.</li>
     * </ul>
     *
     * @param  embeds
     *         The new {@link MessageEmbed MessageEmbeds} of the message, or an empty list to remove all embeds
     *
     * @throws UnsupportedOperationException
     *         If this is a system message
     * @throws IllegalStateException
     *         If the message is not authored by this bot
     * @throws IllegalArgumentException
     *         <ul>
     *             <li>If {@code null} is provided</li>
     *             <li>If more than {@value Message#MAX_EMBED_COUNT} embeds are provided</li>
     *         </ul>
     *
     * @return {@link MessageEditAction}
     *
     * @see    net.dv8tion.jda.api.EmbedBuilder EmbedBuilder
     * @see    MessageChannel#editMessageEmbedsById(long, Collection)
     */
    @Nonnull
    @CheckReturnValue
    default MessageEditAction editMessageEmbeds(@Nonnull MessageEmbed... embeds)
    {
        Checks.noneNull(embeds, "MessageEmbeds");
        return editMessageEmbeds(Arrays.asList(embeds));
    }

    /**
     * Edits this message using the provided {@link MessageTopLevelComponent MessageTopLevelComponents}.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The request was attempted after the account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed, or after {@link net.dv8tion.jda.api.Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL}
     *         was revoked in the {@link GuildMessageChannel}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>The provided {@code messageId} is unknown in this MessageChannel, either due to the id being invalid, or
     *         the message it referred to has already been deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_CHANNEL UNKNOWN_CHANNEL}
     *     <br>The request was attempted after the channel was deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#INVALID_FORM_BODY INVALID_FORM_BODY}
     *     <br>{@linkplain MessageRequest#useComponentsV2(boolean) Components V2} is used by the to-be-edited message, and this request has non-empty content or embeds.</li>
     * </ul>
     *
     * @param  components
     *         The {@link MessageTopLevelComponent MessageTopLevelComponents} to set, can be empty to remove components,
     *         can contain up to {@value Message#MAX_COMPONENT_COUNT} V1 components.
     *         There are no limits for {@linkplain MessageRequest#isUsingComponentsV2() V2 components}
     *         outside the {@linkplain Message#MAX_COMPONENT_COUNT_IN_COMPONENT_TREE total tree size} ({@value Message#MAX_COMPONENT_COUNT_IN_COMPONENT_TREE}).
     *
     * @throws UnsupportedOperationException
     *         If this is a system message
     * @throws IllegalStateException
     *         If the message is not authored by this bot
     * @throws IllegalArgumentException
     *         <ul>
     *             <li>If {@code null} is provided</li>
     *             <li>If any of the provided components are not {@linkplain Component.Type#isMessageCompatible() compatible with messages}</li>
     *         </ul>
     *
     * @return {@link MessageEditAction}
     *
     * @see    MessageChannel#editMessageComponentsById(long, Collection)
     */
    @Nonnull
    @CheckReturnValue
    MessageEditAction editMessageComponents(@Nonnull Collection<? extends MessageTopLevelComponent> components);

    /**
     * Edits this message using the provided {@link MessageTopLevelComponent MessageTopLevelComponents}.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The request was attempted after the account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed, or after {@link net.dv8tion.jda.api.Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL}
     *         was revoked in the {@link GuildMessageChannel}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>The provided {@code messageId} is unknown in this MessageChannel, either due to the id being invalid, or
     *         the message it referred to has already been deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_CHANNEL UNKNOWN_CHANNEL}
     *     <br>The request was attempted after the channel was deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#INVALID_FORM_BODY INVALID_FORM_BODY}
     *     <br>{@linkplain MessageRequest#useComponentsV2(boolean) Components V2} is used by the to-be-edited message, and this request has non-empty content or embeds.</li>
     * </ul>
     *
     * @param  components
     *         The {@link MessageTopLevelComponent MessageTopLevelComponents} to set, can be empty to remove components,
     *         can contain up to {@value Message#MAX_COMPONENT_COUNT} V1 components.
     *         There are no limits for {@linkplain MessageRequest#isUsingComponentsV2() V2 components}
     *         outside the {@linkplain Message#MAX_COMPONENT_COUNT_IN_COMPONENT_TREE total tree size} ({@value Message#MAX_COMPONENT_COUNT_IN_COMPONENT_TREE}).
     *
     * @throws UnsupportedOperationException
     *         If this is a system message
     * @throws IllegalStateException
     *         If the message is not authored by this bot
     * @throws IllegalArgumentException
     *         <ul>
     *             <li>If {@code null} is provided</li>
     *             <li>If any of the provided components are not {@linkplain Component.Type#isMessageCompatible() compatible with messages}</li>
     *         </ul>
     *
     * @return {@link MessageEditAction}
     *
     * @see    MessageChannel#editMessageComponentsById(long, Collection)
     */
    @Nonnull
    @CheckReturnValue
    default MessageEditAction editMessageComponents(@Nonnull MessageTopLevelComponent... components)
    {
        Checks.noneNull(components, "Components");
        return editMessageComponents(Arrays.asList(components));
    }

    /**
     * Edits this message using the provided {@link ComponentTree}.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The request was attempted after the account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed, or after {@link net.dv8tion.jda.api.Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL}
     *         was revoked in the {@link GuildMessageChannel}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>The provided {@code messageId} is unknown in this MessageChannel, either due to the id being invalid, or
     *         the message it referred to has already been deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_CHANNEL UNKNOWN_CHANNEL}
     *     <br>The request was attempted after the channel was deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#INVALID_FORM_BODY INVALID_FORM_BODY}
     *     <br>{@linkplain MessageRequest#useComponentsV2(boolean) Components V2} is used by the to-be-edited message, and this request has non-empty content or embeds.</li>
     * </ul>
     *
     * @param  tree
     *         The {@link ComponentTree} to set, can be empty to remove components,
     *         can contain up to {@value Message#MAX_COMPONENT_COUNT} V1 components.
     *         There are no limits for {@linkplain MessageRequest#isUsingComponentsV2() V2 components}
     *         outside the {@linkplain Message#MAX_COMPONENT_COUNT_IN_COMPONENT_TREE total tree size} ({@value Message#MAX_COMPONENT_COUNT_IN_COMPONENT_TREE}).
     *
     * @throws UnsupportedOperationException
     *         If this is a system message
     * @throws IllegalStateException
     *         If the message is not authored by this bot
     * @throws IllegalArgumentException
     *         <ul>
     *             <li>If {@code null} is provided</li>
     *             <li>If any of the provided components are not {@linkplain Component.Type#isMessageCompatible() compatible with messages}</li>
     *         </ul>
     *
     * @return {@link MessageEditAction}
     *
     * @see    MessageChannel#editMessageComponentsById(long, ComponentTree)
     * @see    net.dv8tion.jda.api.components.tree.MessageComponentTree MessageComponentTree
     */
    @Nonnull
    @CheckReturnValue
    default MessageEditAction editMessageComponents(@Nonnull ComponentTree<? extends MessageTopLevelComponent> tree)
    {
        Checks.notNull(tree, "ComponentTree");
        return editMessageComponents(tree.getComponents());
    }

    /**
     * Edits this message using the provided format arguments.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The request was attempted after the account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed, or after {@link net.dv8tion.jda.api.Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL}
     *         was revoked in the {@link GuildMessageChannel}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>The provided {@code messageId} is unknown in this MessageChannel, either due to the id being invalid, or
     *         the message it referred to has already been deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_CHANNEL UNKNOWN_CHANNEL}
     *     <br>The request was attempted after the channel was deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#INVALID_FORM_BODY INVALID_FORM_BODY}
     *     <br>{@linkplain MessageRequest#useComponentsV2(boolean) Components V2} is used by the to-be-edited message, and this request has non-empty content or embeds.</li>
     * </ul>
     *
     * @param  format
     *         Format String used to generate new Content
     * @param  args
     *         The arguments which should be used to format the given format String
     *
     * @throws IllegalArgumentException
     *         If provided {@code format} is {@code null} or blank.
     * @throws UnsupportedOperationException
     *         If this is a system message
     * @throws IllegalStateException
     *         If the message is not authored by this bot
     * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
     *         If this is a {@link GuildMessageChannel} and this account does not have
     *         {@link net.dv8tion.jda.api.Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL}
     * @throws java.util.IllegalFormatException
     *         If a format string contains an illegal syntax,
     *         a format specifier that is incompatible with the given arguments,
     *         insufficient arguments given the format string, or other illegal conditions.
     *         For specification of all possible formatting errors,
     *         see the <a href="../util/Formatter.html#detail">Details</a>
     *         section of the formatter class specification.
     *
     * @return {@link MessageEditAction}
     *
     * @see    MessageChannel#editMessageFormatById(long, String, Object...)
     */
    @Nonnull
    @CheckReturnValue
    MessageEditAction editMessageFormat(@Nonnull String format, @Nonnull Object... args);

    /**
     * Edits this message using the provided files.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#REQUEST_ENTITY_TOO_LARGE REQUEST_ENTITY_TOO_LARGE}
     *     <br>If any of the provided files is bigger than {@link Guild#getMaxFileSize()}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The request was attempted after the account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed, or after {@link net.dv8tion.jda.api.Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL}
     *         was revoked in the {@link GuildMessageChannel}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>The provided {@code messageId} is unknown in this MessageChannel, either due to the id being invalid, or
     *         the message it referred to has already been deleted. This might also be triggered for ephemeral messages, if the interaction expired.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_CHANNEL UNKNOWN_CHANNEL}
     *     <br>The request was attempted after the channel was deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#INVALID_FORM_BODY INVALID_FORM_BODY}
     *     <br>{@linkplain MessageRequest#useComponentsV2(boolean) Components V2} is used by the to-be-edited message, and this request has non-empty content or embeds.</li>
     * </ul>
     *
     * <p><b>Resource Handling Note:</b> Once the request is handed off to the requester, for example when you call {@link RestAction#queue()},
     * the requester will automatically clean up all opened files by itself. You are only responsible to close them yourself if it is never handed off properly.
     * For instance, if an exception occurs after using {@link FileUpload#fromData(File)}, before calling {@link RestAction#queue()}.
     * You can safely use a try-with-resources to handle this, since {@link FileUpload#close()} becomes ineffective once the request is handed off.
     *
     * @param  attachments
     *         The new attachments of the message (Can be {@link FileUpload FileUploads} or {@link net.dv8tion.jda.api.utils.AttachmentUpdate AttachmentUpdates})
     *
     * @throws UnsupportedOperationException
     *         If this is a system message
     * @throws IllegalStateException
     *         If the message is not authored by this bot
     * @throws IllegalArgumentException
     *         If {@code null} is provided
     *
     * @return {@link MessageEditAction} that can be used to further update the message
     *
     * @see    AttachedFile#fromAttachment(Message.Attachment)
     * @see    FileUpload#fromData(InputStream, String)
     */
    @Nonnull
    @CheckReturnValue
    MessageEditAction editMessageAttachments(@Nonnull Collection<? extends AttachedFile> attachments);

    /**
     * Edits this message using the provided files.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#REQUEST_ENTITY_TOO_LARGE REQUEST_ENTITY_TOO_LARGE}
     *     <br>If any of the provided files is bigger than {@link Guild#getMaxFileSize()}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The request was attempted after the account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed, or after {@link net.dv8tion.jda.api.Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL}
     *         was revoked in the {@link GuildMessageChannel}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>The provided {@code messageId} is unknown in this MessageChannel, either due to the id being invalid, or
     *         the message it referred to has already been deleted. This might also be triggered for ephemeral messages, if the interaction expired.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_CHANNEL UNKNOWN_CHANNEL}
     *     <br>The request was attempted after the channel was deleted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#INVALID_FORM_BODY INVALID_FORM_BODY}
     *     <br>{@linkplain MessageRequest#useComponentsV2(boolean) Components V2} is used by the to-be-edited message, and this request has non-empty content or embeds.</li>
     * </ul>
     *
     * <p><b>Resource Handling Note:</b> Once the request is handed off to the requester, for example when you call {@link RestAction#queue()},
     * the requester will automatically clean up all opened files by itself. You are only responsible to close them yourself if it is never handed off properly.
     * For instance, if an exception occurs after using {@link FileUpload#fromData(File)}, before calling {@link RestAction#queue()}.
     * You can safely use a try-with-resources to handle this, since {@link FileUpload#close()} becomes ineffective once the request is handed off.
     *
     * @param  attachments
     *         The new attachments of the message (Can be {@link FileUpload FileUploads} or {@link net.dv8tion.jda.api.utils.AttachmentUpdate AttachmentUpdates})
     *
     * @throws UnsupportedOperationException
     *         If this is a system message
     * @throws IllegalStateException
     *         If the message is not authored by this bot
     * @throws IllegalArgumentException
     *         If {@code null} is provided
     *
     * @return {@link MessageEditAction} that can be used to further update the message
     *
     * @see    AttachedFile#fromAttachment(Message.Attachment)
     * @see    FileUpload#fromData(InputStream, String)
     */
    @Nonnull
    @CheckReturnValue
    default MessageEditAction editMessageAttachments(@Nonnull AttachedFile... attachments)
    {
        Checks.noneNull(attachments, "Attachments");
        return editMessageAttachments(Arrays.asList(attachments));
    }

    /**
     * Replies and references this message.
     * <br>This is identical to {@code message.getGuildChannel().sendStickers(stickers).reference(message)}.
     * You can use {@link MessageCreateAction#mentionRepliedUser(boolean) mentionRepliedUser(false)} to not mention the author of the message.
     * <br>By default there won't be any error thrown if the referenced message does not exist.
     * This behavior can be changed with {@link MessageCreateAction#failOnInvalidReply(boolean)}.
     *
     * <p>For further info, see {@link GuildMessageChannel#sendStickers(Collection)} and {@link MessageCreateAction#setMessageReference(Message)}.
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} include:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If this message no longer exists</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_AUTOMOD MESSAGE_BLOCKED_BY_AUTOMOD}
     *     <br>If this message was blocked by an {@link net.dv8tion.jda.api.entities.automod.AutoModRule AutoModRule}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER}
     *     <br>If this message was blocked by the harmful link filter</li>
     * </ul>
     *
     * @param  stickers
     *         The 1-3 stickers to send
     *
     * @throws MissingAccessException
     *         If the currently logged in account does not have {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL} in this channel
     * @throws InsufficientPermissionException
     *         <ul>
     *           <li>If this is a {@link ThreadChannel} and the bot does not have {@link Permission#MESSAGE_SEND_IN_THREADS Permission.MESSAGE_SEND_IN_THREADS}</li>
     *           <li>If this is not a {@link ThreadChannel} and the bot does not have {@link Permission#MESSAGE_SEND Permission.MESSAGE_SEND}</li>
     *         </ul>
     * @throws IllegalArgumentException
     *         <ul>
     *           <li>If any of the provided stickers is a {@link GuildSticker},
     *               which is either {@link GuildSticker#isAvailable() unavailable} or from a different guild.</li>
     *           <li>If the list is empty or has more than 3 stickers</li>
     *           <li>If null is provided</li>
     *         </ul>
     * @throws IllegalStateException
     *         If this message was not sent in a {@link Guild}
     *
     * @return {@link MessageCreateAction}
     *
     * @see    Sticker#fromId(long)
     */
    @Nonnull
    @CheckReturnValue
    default MessageCreateAction replyStickers(@Nonnull Collection<? extends StickerSnowflake> stickers)
    {
        return getGuildChannel().sendStickers(stickers).setMessageReference(this);
    }

    /**
     * Replies and references this message.
     * <br>This is identical to {@code message.getGuildChannel().sendStickers(stickers).reference(message)}.
     * You can use {@link MessageCreateAction#mentionRepliedUser(boolean) mentionRepliedUser(false)} to not mention the author of the message.
     * <br>By default there won't be any error thrown if the referenced message does not exist.
     * This behavior can be changed with {@link MessageCreateAction#failOnInvalidReply(boolean)}.
     *
     * <p>For further info, see {@link GuildMessageChannel#sendStickers(Collection)} and {@link MessageCreateAction#setMessageReference(Message)}.
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} include:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If this message no longer exists</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_AUTOMOD MESSAGE_BLOCKED_BY_AUTOMOD}
     *     <br>If this message was blocked by an {@link net.dv8tion.jda.api.entities.automod.AutoModRule AutoModRule}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER}
     *     <br>If this message was blocked by the harmful link filter</li>
     * </ul>
     *
     * @param  stickers
     *         The 1-3 stickers to send
     *
     * @throws MissingAccessException
     *         If the currently logged in account does not have {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL} in this channel
     * @throws InsufficientPermissionException
     *         <ul>
     *           <li>If this is a {@link ThreadChannel} and the bot does not have {@link Permission#MESSAGE_SEND_IN_THREADS Permission.MESSAGE_SEND_IN_THREADS}</li>
     *           <li>If this is not a {@link ThreadChannel} and the bot does not have {@link Permission#MESSAGE_SEND Permission.MESSAGE_SEND}</li>
     *         </ul>
     * @throws IllegalArgumentException
     *         <ul>
     *           <li>If any of the provided stickers is a {@link GuildSticker},
     *               which is either {@link GuildSticker#isAvailable() unavailable} or from a different guild.</li>
     *           <li>If the list is empty or has more than 3 stickers</li>
     *           <li>If null is provided</li>
     *         </ul>
     * @throws IllegalStateException
     *         If this message was not sent in a {@link Guild}
     *
     * @return {@link MessageCreateAction}
     *
     * @see    Sticker#fromId(long)
     */
    @Nonnull
    @CheckReturnValue
    default MessageCreateAction replyStickers(@Nonnull StickerSnowflake... stickers)
    {
        return getGuildChannel().sendStickers(stickers).setMessageReference(this);
    }

    /**
     * Shortcut for {@code getChannel().sendMessage(content).setMessageReference(this)}.
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} include:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If this message no longer exists</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_AUTOMOD MESSAGE_BLOCKED_BY_AUTOMOD}
     *     <br>If this message was blocked by an {@link net.dv8tion.jda.api.entities.automod.AutoModRule AutoModRule}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER}
     *     <br>If this message was blocked by the harmful link filter</li>
     * </ul>
     *
     * @param  content
     *         The reply content
     *
     * @throws InsufficientPermissionException
     *         If {@link MessageChannel#sendMessage(CharSequence)} throws
     * @throws IllegalArgumentException
     *         If {@link MessageChannel#sendMessage(CharSequence)} throws
     *
     * @return {@link MessageCreateAction}
     */
    @Nonnull
    @CheckReturnValue
    default MessageCreateAction reply(@Nonnull CharSequence content)
    {
        return getChannel().sendMessage(content).setMessageReference(this);
    }

    /**
     * Shortcut for {@code getChannel().sendMessage(data).setMessageReference(this)}.
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} include:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If this message no longer exists</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_AUTOMOD MESSAGE_BLOCKED_BY_AUTOMOD}
     *     <br>If this message was blocked by an {@link net.dv8tion.jda.api.entities.automod.AutoModRule AutoModRule}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER}
     *     <br>If this message was blocked by the harmful link filter</li>
     * </ul>
     *
     * @param  msg
     *         The {@link MessageCreateData} to send
     *
     * @throws InsufficientPermissionException
     *         If {@link MessageChannel#sendMessage(MessageCreateData)} throws
     * @throws IllegalArgumentException
     *         If {@link MessageChannel#sendMessage(MessageCreateData)} throws
     *
     * @return {@link MessageCreateAction}
     */
    @Nonnull
    @CheckReturnValue
    default MessageCreateAction reply(@Nonnull MessageCreateData msg)
    {
        return getChannel().sendMessage(msg).setMessageReference(this);
    }

    /**
     * Shortcut for {@code getChannel().sendMessagePoll(data).setMessageReference(this)}.
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} include:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_CHANNEL UNKNOWN_CHANNEL}
     *     <br>if this channel was deleted</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#CANNOT_SEND_TO_USER CANNOT_SEND_TO_USER}
     *     <br>If this is a {@link PrivateChannel} and the currently logged in account
     *         does not share any Guilds with the recipient User</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_AUTOMOD MESSAGE_BLOCKED_BY_AUTOMOD}
     *     <br>If this message was blocked by an {@link net.dv8tion.jda.api.entities.automod.AutoModRule AutoModRule}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER}
     *     <br>If this message was blocked by the harmful link filter</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#POLL_INVALID_CHANNEL_TYPE POLL_INVALID_CHANNEL_TYPE}
     *     <br>This channel does not allow polls</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#POLL_WITH_UNUSABLE_EMOJI POLL_WITH_UNUSABLE_EMOJI}
     *     <br>This poll uses an external emoji that the bot is not allowed to use</li>
     * </ul>
     *
     * @param  poll
     *         The poll to send
     *
     * @throws InsufficientPermissionException
     *         If {@link MessageChannel#sendMessage(MessageCreateData)} throws
     * @throws IllegalArgumentException
     *         If {@link MessageChannel#sendMessage(MessageCreateData)} throws
     *
     * @return {@link MessageCreateAction}
     */
    @Nonnull
    @CheckReturnValue
    default MessageCreateAction replyPoll(@Nonnull MessagePollData poll)
    {
        return getChannel().sendMessagePoll(poll).setMessageReference(this);
    }

    /**
     * Shortcut for {@code getChannel().sendMessageEmbeds(embed, other).setMessageReference(this)}.
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} include:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If this message no longer exists</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_AUTOMOD MESSAGE_BLOCKED_BY_AUTOMOD}
     *     <br>If this message was blocked by an {@link net.dv8tion.jda.api.entities.automod.AutoModRule AutoModRule}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER}
     *     <br>If this message was blocked by the harmful link filter</li>
     * </ul>
     *
     * @param  embed
     *         The {@link MessageEmbed} to send
     * @param  other
     *         Any addition {@link MessageEmbed MessageEmbeds} to send
     *
     * @throws InsufficientPermissionException
     *         If {@link MessageChannel#sendMessageEmbeds(MessageEmbed, MessageEmbed...)} throws
     * @throws IllegalArgumentException
     *         If {@link MessageChannel#sendMessageEmbeds(MessageEmbed, MessageEmbed...)} throws
     *
     * @return {@link MessageCreateAction}
     */
    @Nonnull
    @CheckReturnValue
    default MessageCreateAction replyEmbeds(@Nonnull MessageEmbed embed, @Nonnull MessageEmbed... other)
    {
        Checks.notNull(embed, "MessageEmbeds");
        Checks.noneNull(other, "MessageEmbeds");
        List<MessageEmbed> embeds = new ArrayList<>(1 + other.length);
        embeds.add(embed);
        Collections.addAll(embeds, other);
        return replyEmbeds(embeds);
    }

    /**
     * Shortcut for {@code getChannel().sendMessageEmbeds(embeds).setMessageReference(this)}.
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} include:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If this message no longer exists</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_AUTOMOD MESSAGE_BLOCKED_BY_AUTOMOD}
     *     <br>If this message was blocked by an {@link net.dv8tion.jda.api.entities.automod.AutoModRule AutoModRule}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER}
     *     <br>If this message was blocked by the harmful link filter</li>
     * </ul>
     *
     * @param  embeds
     *         The {@link MessageEmbed MessageEmbeds} to send
     *
     * @throws InsufficientPermissionException
     *         If {@link MessageChannel#sendMessageEmbeds(Collection)} throws
     * @throws IllegalArgumentException
     *         If {@link MessageChannel#sendMessageEmbeds(Collection)} throws
     *
     * @return {@link MessageCreateAction}
     */
    @Nonnull
    @CheckReturnValue
    default MessageCreateAction replyEmbeds(@Nonnull Collection<? extends MessageEmbed> embeds)
    {
        return getChannel().sendMessageEmbeds(embeds).setMessageReference(this);
    }

    /**
     * Shortcut for {@code getChannel().sendMessageComponents(components).setMessageReference(this)}.
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} include:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If this message no longer exists</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_AUTOMOD MESSAGE_BLOCKED_BY_AUTOMOD}
     *     <br>If this message was blocked by an {@link net.dv8tion.jda.api.entities.automod.AutoModRule AutoModRule}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER}
     *     <br>If this message was blocked by the harmful link filter</li>
     * </ul>
     *
     * @param  components
     *         The {@link MessageTopLevelComponent MessageTopLevelComponents} to send
     *         can contain up to {@value Message#MAX_COMPONENT_COUNT} V1 components.
     *         There are no limits for {@linkplain MessageRequest#isUsingComponentsV2() V2 components}
     *         outside the {@linkplain Message#MAX_COMPONENT_COUNT_IN_COMPONENT_TREE total tree size} ({@value Message#MAX_COMPONENT_COUNT_IN_COMPONENT_TREE}).
     *
     * @throws InsufficientPermissionException
     *         If {@link MessageChannel#sendMessageComponents(Collection)} throws
     * @throws IllegalArgumentException
     *         If {@link MessageChannel#sendMessageComponents(Collection)} throws
     *
     * @return {@link MessageCreateAction}
     */
    @Nonnull
    @CheckReturnValue
    default MessageCreateAction replyComponents(@Nonnull Collection<? extends MessageTopLevelComponent> components)
    {
        Checks.noneNull(components, "MessageTopLevelComponents");
        return getChannel().sendMessageComponents(components).setMessageReference(this);
    }

    /**
     * Shortcut for {@code getChannel().sendMessageComponents(component, other).setMessageReference(this)}.
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} include:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If this message no longer exists</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_AUTOMOD MESSAGE_BLOCKED_BY_AUTOMOD}
     *     <br>If this message was blocked by an {@link net.dv8tion.jda.api.entities.automod.AutoModRule AutoModRule}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER}
     *     <br>If this message was blocked by the harmful link filter</li>
     * </ul>
     *
     * @param  component
     *         The {@link MessageTopLevelComponent} to send
     * @param  other
     *         Additional {@link MessageTopLevelComponent MessageTopLevelComponents} to send
     *         can contain up to {@value Message#MAX_COMPONENT_COUNT} V1 components.
     *         There are no limits for {@linkplain MessageRequest#isUsingComponentsV2() V2 components}
     *         outside the {@linkplain Message#MAX_COMPONENT_COUNT_IN_COMPONENT_TREE total tree size} ({@value Message#MAX_COMPONENT_COUNT_IN_COMPONENT_TREE}).
     *
     * @throws InsufficientPermissionException
     *         If {@link MessageChannel#sendMessageComponents(MessageTopLevelComponent, MessageTopLevelComponent...)} throws
     * @throws IllegalArgumentException
     *         If {@link MessageChannel#sendMessageComponents(MessageTopLevelComponent, MessageTopLevelComponent...)} throws
     *
     * @return {@link MessageCreateAction}
     */
    @Nonnull
    @CheckReturnValue
    default MessageCreateAction replyComponents(@Nonnull MessageTopLevelComponent component, @Nonnull MessageTopLevelComponent... other)
    {
        Checks.notNull(component, "MessageTopLevelComponents");
        Checks.noneNull(other, "MessageTopLevelComponents");
        List<MessageTopLevelComponent> components = new ArrayList<>(1 + other.length);
        components.add(component);
        Collections.addAll(components, other);
        return replyComponents(components);
    }

    /**
     * Shortcut for {@code getChannel().sendMessageComponents(tree).setMessageReference(this)}.
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} include:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If this message no longer exists</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_AUTOMOD MESSAGE_BLOCKED_BY_AUTOMOD}
     *     <br>If this message was blocked by an {@link net.dv8tion.jda.api.entities.automod.AutoModRule AutoModRule}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER}
     *     <br>If this message was blocked by the harmful link filter</li>
     * </ul>
     *
     * @param  tree
     *         The {@link ComponentTree} to send,
     *         containing up to {@value Message#MAX_COMPONENT_COUNT} V1 components.
     *         There are no limits for {@linkplain MessageRequest#isUsingComponentsV2() V2 components}
     *         outside the {@linkplain Message#MAX_COMPONENT_COUNT_IN_COMPONENT_TREE total tree size} ({@value Message#MAX_COMPONENT_COUNT_IN_COMPONENT_TREE}).
     *
     * @throws InsufficientPermissionException
     *         If {@link MessageChannel#sendMessageComponents(ComponentTree)} throws
     * @throws IllegalArgumentException
     *         If {@link MessageChannel#sendMessageComponents(ComponentTree)} throws
     *
     * @return {@link MessageCreateAction}
     *
     * @see    net.dv8tion.jda.api.components.tree.MessageComponentTree MessageComponentTree
     */
    @Nonnull
    @CheckReturnValue
    default MessageCreateAction replyComponents(@Nonnull ComponentTree<? extends MessageTopLevelComponent> tree)
    {
        Checks.notNull(tree, "ComponentTree");
        return replyComponents(tree.getComponents());
    }

    /**
     * Shortcut for {@code getChannel().sendMessageFormat(format, args).setMessageReference(this)}.
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} include:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If this message no longer exists</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_AUTOMOD MESSAGE_BLOCKED_BY_AUTOMOD}
     *     <br>If this message was blocked by an {@link net.dv8tion.jda.api.entities.automod.AutoModRule AutoModRule}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER}
     *     <br>If this message was blocked by the harmful link filter</li>
     * </ul>
     *
     * @param  format
     *         The format string
     * @param  args
     *         The arguments to use in the format string
     *
     * @throws InsufficientPermissionException
     *         If {@link MessageChannel#sendMessageFormat(String, Object...)} throws
     * @throws IllegalArgumentException
     *         If {@link MessageChannel#sendMessageFormat(String, Object...)} throws
     *
     * @return {@link MessageCreateAction}
     */
    @Nonnull
    @CheckReturnValue
    default MessageCreateAction replyFormat(@Nonnull String format, @Nonnull Object... args)
    {
        return getChannel().sendMessageFormat(format, args).setMessageReference(this);
    }

    /**
     * Shortcut for {@code getChannel().sendFiles(files).setMessageReference(this)}.
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} include:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If this message no longer exists</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_AUTOMOD MESSAGE_BLOCKED_BY_AUTOMOD}
     *     <br>If this message was blocked by an {@link net.dv8tion.jda.api.entities.automod.AutoModRule AutoModRule}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER}
     *     <br>If this message was blocked by the harmful link filter</li>
     * </ul>
     *
     * @param  files
     *         The {@link FileUpload FileUploads} to send
     *
     * @throws InsufficientPermissionException
     *         If {@link MessageChannel#sendFiles(FileUpload...)} throws
     * @throws IllegalArgumentException
     *         If {@link MessageChannel#sendFiles(FileUpload...)} throws
     *
     * @return {@link MessageCreateAction}
     */
    @Nonnull
    @CheckReturnValue
    default MessageCreateAction replyFiles(@Nonnull FileUpload... files)
    {
        return getChannel().sendFiles(files).setMessageReference(this);
    }

    /**
     * Shortcut for {@code getChannel().sendFiles(files).setMessageReference(this)}.
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} include:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If this message no longer exists</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_AUTOMOD MESSAGE_BLOCKED_BY_AUTOMOD}
     *     <br>If this message was blocked by an {@link net.dv8tion.jda.api.entities.automod.AutoModRule AutoModRule}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER MESSAGE_BLOCKED_BY_HARMFUL_LINK_FILTER}
     *     <br>If this message was blocked by the harmful link filter</li>
     * </ul>
     *
     * @param  files
     *         The {@link FileUpload FileUploads} to send
     *
     * @throws InsufficientPermissionException
     *         If {@link MessageChannel#sendFiles(Collection)} throws
     * @throws IllegalArgumentException
     *         If {@link MessageChannel#sendFiles(Collection)} throws
     *
     * @return {@link MessageCreateAction}
     */
    @Nonnull
    @CheckReturnValue
    default MessageCreateAction replyFiles(@Nonnull Collection<? extends FileUpload> files)
    {
        return getChannel().sendFiles(files).setMessageReference(this);
    }

    /**
     * Forwards this message into the provided channel.
     *
     * <p><b>A message forward request cannot contain additional content.</b>
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} from forwarding include:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#REFERENCED_MESSSAGE_NOT_FOUND REFERENCED_MESSSAGE_NOT_FOUND}
     *     <br>If the provided reference cannot be resolved to a message</li>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#FORWARD_CANNOT_HAVE_CONTENT FORWARD_CANNOT_HAVE_CONTENT}
     *     <br>If additional content is sent alongside a forwarded message</li>
     * </ul>
     *
     * @param  channel
     *         The target channel to forward to
     *
     * @throws InsufficientPermissionException
     *         If the bot is missing {@link Permission#MESSAGE_SEND} in the target channel
     * @throws IllegalArgumentException
     *         If the target channel is null
     *
     * @return {@link MessageCreateAction}
     */
    @Nonnull
    @CheckReturnValue
    default MessageCreateAction forwardTo(@Nonnull MessageChannel channel)
    {
        Checks.notNull(channel, "Target channel");
        if (channel instanceof MessageChannelMixin)
            ((MessageChannelMixin<?>) channel).checkCanSendMessage();
        return new MessageCreateActionImpl(channel)
                .setMessageReference(MessageReference.MessageReferenceType.FORWARD, this);
    }

    /**
     * Deletes this Message from Discord.
     * <br>If this Message was not sent by the currently logged in account, then this will fail unless the Message is from
     * a {@link GuildChannel} and the current account has
     * {@link Permission#MESSAGE_MANAGE Permission.MESSAGE_MANAGE} in the channel.
     *
     * <p><u>To delete many messages at once in a {@link MessageChannel MessageChannel}
     * you should use {@link MessageChannel#purgeMessages(List) MessageChannel.purgeMessages(List)} instead.</u>
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The delete was attempted after the account lost access to the {@link GuildChannel}
     *         due to {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL} being revoked, or the
     *         account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_PERMISSIONS MISSING_PERMISSIONS}
     *     <br>The delete was attempted after the account lost {@link Permission#MESSAGE_MANAGE Permission.MESSAGE_MANAGE} in
     *         the {@link GuildChannel} when deleting another Member's message
     *         or lost {@link Permission#MESSAGE_MANAGE}.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If the message has already been deleted. This might also be triggered for ephemeral messages, if the interaction expired.</li>
     * </ul>
     *
     * @throws MissingAccessException
     *         If the currently logged in account does not have {@link Member#hasAccess(GuildChannel) access} in this channel.
     * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
     *         If this Message was not sent by the currently logged in account, the Message was sent in a
     *         {@link GuildChannel GuildChannel}, and the currently logged in account
     *         does not have {@link Permission#MESSAGE_MANAGE Permission.MESSAGE_MANAGE} in
     *         the channel.
     * @throws java.lang.IllegalStateException
     *         <ul>
     *              <li>If this Message was not sent by the currently logged in account and it was <b>not</b> sent in a
     *              {@link GuildChannel GuildChannel}.</li>
     *              <li>If this message type cannot be deleted. (See {@link MessageType#canDelete()})</li>
     *              <li>If this Message is ephemeral and the interaction expired.</li>
     *         </ul>
     *
     * @return {@link net.dv8tion.jda.api.requests.restaction.AuditableRestAction AuditableRestAction}
     *
     * @see    TextChannel#deleteMessages(java.util.Collection) TextChannel.deleteMessages(Collection)
     * @see    MessageChannel#purgeMessages(java.util.List) MessageChannel.purgeMessages(List)
     */
    @Nonnull
    @CheckReturnValue
    AuditableRestAction<Void> delete();

    /**
     * Returns the {@link net.dv8tion.jda.api.JDA JDA} instance related to this Message.
     *
     * @return  the corresponding JDA instance
     */
    @Nonnull
    JDA getJDA();

    /**
     * Whether or not this Message has been pinned in its parent channel.
     *
     * @return True - if this message has been pinned.
     */
    boolean isPinned();

    /**
     * Used to add the Message to the {@link #getChannel() MessageChannel's} pinned message list.
     * <br>This is a shortcut method to {@link MessageChannel#pinMessageById(String)}.
     *
     * <p>The success or failure of this action will not affect the return of {@link #isPinned()}.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The pin request was attempted after the account lost access to the {@link GuildChannel}
     *         due to {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL} being revoked, or the
     *         account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_PERMISSIONS MISSING_PERMISSIONS}
     *     <br>The pin request was attempted after the account lost {@link Permission#MESSAGE_MANAGE Permission.MESSAGE_MANAGE} in
     *         the {@link GuildChannel}.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If the message has already been deleted. This might also be triggered for ephemeral messages.</li>
     * </ul>
     *
     * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
     *         If this Message is from a {@link GuildChannel} and:
     *         <br><ul>
     *             <li>Missing {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL}.
     *             <br>The account needs access the the channel to pin a message in it.</li>
     *             <li>Missing {@link Permission#MESSAGE_MANAGE Permission.MESSAGE_MANAGE}.
     *             <br>Required to actually pin the Message.</li>
     *         </ul>
     * @throws IllegalStateException
     *         If this Message is ephemeral
     *
     * @return {@link RestAction RestAction} - Type: {@link java.lang.Void}
     */
    @Nonnull
    @CheckReturnValue
    RestAction<Void> pin();

    /**
     * Used to remove the Message from the {@link #getChannel() MessageChannel's} pinned message list.
     * <br>This is a shortcut method to {@link MessageChannel#unpinMessageById(String)}.
     *
     * <p>The success or failure of this action will not affect the return of {@link #isPinned()}.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The unpin request was attempted after the account lost access to the {@link GuildChannel}
     *         due to {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL} being revoked, or the
     *         account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_PERMISSIONS MISSING_PERMISSIONS}
     *     <br>The unpin request was attempted after the account lost {@link Permission#MESSAGE_MANAGE Permission.MESSAGE_MANAGE} in
     *         the {@link GuildChannel}.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If the message has already been deleted. This might also be triggered for ephemeral messages.</li>
     * </ul>
     *
     * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
     *         If this Message is from a {@link GuildChannel} and:
     *         <br><ul>
     *             <li>Missing {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL}.
     *             <br>The account needs access the the channel to pin a message in it.</li>
     *             <li>Missing {@link Permission#MESSAGE_MANAGE Permission.MESSAGE_MANAGE}.
     *             <br>Required to actually pin the Message.</li>
     *         </ul>
     * @throws IllegalStateException
     *         If this Message is ephemeral
     *
     * @return {@link RestAction RestAction} - Type: {@link java.lang.Void}
     */
    @Nonnull
    @CheckReturnValue
    RestAction<Void> unpin();

    /**
     * Adds a reaction to this Message using an {@link Emoji}.
     *
     * <p>This message instance will not be updated by this operation.
     *
     * <p>Reactions are the small emoji below a message that have a counter beside them
     * showing how many users have reacted with the same emoji.
     *
     * <p><b>Neither success nor failure of this request will affect this Message's {@link #getReactions()} return as Message is immutable.</b>
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The reaction request was attempted after the account lost access to the {@link GuildChannel}
     *         due to {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL} being revoked
     *     <br>Also can happen if the account lost the {@link Permission#MESSAGE_HISTORY Permission.MESSAGE_HISTORY}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#REACTION_BLOCKED REACTION_BLOCKED}
     *     <br>The user has blocked the currently logged in account and the reaction failed</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#TOO_MANY_REACTIONS TOO_MANY_REACTIONS}
     *     <br>The message already has too many reactions to proceed</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_PERMISSIONS MISSING_PERMISSIONS}
     *     <br>The reaction request was attempted after the account lost {@link Permission#MESSAGE_ADD_REACTION Permission.MESSAGE_ADD_REACTION}
     *         or {@link Permission#MESSAGE_HISTORY Permission.MESSAGE_HISTORY}
     *         in the {@link GuildChannel} when adding the reaction.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_EMOJI UNKNOWN_EMOJI}
     *     <br>The provided emoji was deleted, doesn't exist, or is not available to the currently logged-in account in this channel.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If the message has already been deleted. This might also be triggered for ephemeral messages.</li>
     * </ul>
     *
     * @param  emoji
     *         The {@link Emoji} to add as a reaction to this Message.
     *
     * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
     *         If the MessageChannel this message was sent in was a {@link GuildChannel}
     *         and the logged in account does not have
     *         <ul>
     *             <li>{@link Permission#MESSAGE_ADD_REACTION Permission.MESSAGE_ADD_REACTION}</li>
     *             <li>{@link Permission#MESSAGE_HISTORY Permission.MESSAGE_HISTORY}</li>
     *         </ul>
     * @throws java.lang.IllegalArgumentException
     *         <ul>
     *             <li>If the provided {@link Emoji} is null.</li>
     *             <li>If the provided {@link Emoji} is a custom emoji and cannot be used in the current channel.
     *                 See {@link RichCustomEmoji#canInteract(User, MessageChannel)} or {@link RichCustomEmoji#canInteract(Member)} for more information.</li>
     *         </ul>
     * @throws IllegalStateException
     *         If this message is ephemeral
     *
     * @return {@link RestAction}
     */
    @Nonnull
    @CheckReturnValue
    RestAction<Void> addReaction(@Nonnull Emoji emoji);

    /**
     * Removes all reactions from this Message.
     * <br>This is useful for moderator commands that wish to remove all reactions at once from a specific message.
     *
     * <p>Please note that you <b>can't</b> clear reactions if this message was sent in a {@link PrivateChannel PrivateChannel}!
     *
     * <p><b>Neither success nor failure of this request will affect this Message's {@link #getReactions()} return as Message is immutable.</b>
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The clear-reactions request was attempted after the account lost access to the {@link GuildChannel}
     *         due to {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL} being revoked, or the
     *         account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_PERMISSIONS MISSING_PERMISSIONS}
     *     <br>The clear-reactions request was attempted after the account lost {@link Permission#MESSAGE_MANAGE Permission.MESSAGE_MANAGE}
     *         in the {@link GuildChannel} when adding the reaction.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If the message has already been deleted. This might also be triggered for ephemeral messages.</li>
     * </ul>
     *
     * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
     *         If the MessageChannel this message was sent in was a {@link GuildChannel}
     *         and the currently logged in account does not have {@link Permission#MESSAGE_MANAGE Permission.MESSAGE_MANAGE}
     *         in the channel.
     * @throws java.lang.IllegalStateException
     *         <ul>
     *             <li>If this message was <b>not</b> sent in a {@link Guild Guild}.</li>
     *             <li>If this message is ephemeral</li>
     *         </ul>
     *
     *
     * @return {@link RestAction}
     */
    @Nonnull
    @CheckReturnValue
    RestAction<Void> clearReactions();

    /**
     * Removes all reactions for the specified {@link Emoji}.
     *
     * <p>Please note that you <b>can't</b> clear reactions if this message was sent in a {@link PrivateChannel PrivateChannel}!
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The currently logged in account lost access to the channel by either being removed from the guild
     *         or losing the {@link Permission#VIEW_CHANNEL VIEW_CHANNEL} permission</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_EMOJI UNKNOWN_EMOJI}
     *     <br>The provided emoji was deleted, doesn't exist, or is not available to the currently logged-in account in this channel.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If the message has already been deleted. This might also be triggered for ephemeral messages.</li>
     * </ul>
     *
     * @param  emoji
     *         The {@link Emoji} to remove reactions for
     *
     * @throws InsufficientPermissionException
     *         If the currently logged in account does not have {@link Permission#MESSAGE_MANAGE} in the channel
     * @throws IllegalArgumentException
     *         If provided with null
     * @throws java.lang.IllegalStateException
     *         <ul>
     *             <li>If this message was <b>not</b> sent in a {@link Guild Guild}.</li>
     *             <li>If this message is ephemeral</li>
     *         </ul>
     *
     * @return {@link RestAction}
     */
    @Nonnull
    @CheckReturnValue
    RestAction<Void> clearReactions(@Nonnull Emoji emoji);

    /**
     * Removes own reaction from this Message using an {@link Emoji},
     * you can use {@link #removeReaction(Emoji, User)} to remove reactions from other users,
     * or {@link #clearReactions(Emoji)} to remove all reactions for the specified emoji.
     *
     * <p>This message instance will not be updated by this operation.
     *
     * <p>Reactions are the small emojis below a message that have a counter beside them
     * showing how many users have reacted with the same emoji.
     *
     * <p><b>Neither success nor failure of this request will affect this Message's {@link #getReactions()} return as Message is immutable.</b>
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The reaction request was attempted after the account lost access to the {@link GuildChannel}
     *         due to {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL} being revoked
     *     <br>Also can happen if the account lost the {@link Permission#MESSAGE_HISTORY Permission.MESSAGE_HISTORY}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_EMOJI UNKNOWN_EMOJI}
     *     <br>The provided emoji was deleted, doesn't exist, or is not available to the currently logged-in account in this channel.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If the message has already been deleted. This might also be triggered for ephemeral messages.</li>
     * </ul>
     *
     * @param  emoji
     *         The {@link Emoji} reaction to remove as a reaction from this Message.
     *
     * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
     *         If the MessageChannel this message was sent in was a {@link GuildChannel}
     *         and the logged in account does not have {@link Permission#MESSAGE_HISTORY Permission.MESSAGE_HISTORY}
     * @throws java.lang.IllegalArgumentException
     *         <ul>
     *             <li>If the provided {@link Emoji} is null.</li>
     *             <li>If the provided {@link Emoji} is a custom emoji and cannot be used in the current channel.
     *                 See {@link RichCustomEmoji#canInteract(User, MessageChannel)} or {@link RichCustomEmoji#canInteract(Member)} for more information.</li>
     *         </ul>
     * @throws IllegalStateException
     *         If this is an ephemeral message
     *
     * @return {@link RestAction}
     */
    @Nonnull
    @CheckReturnValue
    RestAction<Void> removeReaction(@Nonnull Emoji emoji);

    /**
     * Removes a {@link User User's} reaction from this Message using an {@link Emoji}.
     *
     * <p>Please note that you <b>can't</b> remove reactions of other users if this message was sent in a {@link PrivateChannel PrivateChannel}!
     *
     * <p>This message instance will not be updated by this operation.
     *
     * <p>Reactions are the small emojis below a message that have a counter beside them
     * showing how many users have reacted with the same emoji.
     *
     * <p><b>Neither success nor failure of this request will affect this Message's {@link #getReactions()} return as Message is immutable.</b>
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The reaction request was attempted after the account lost access to the {@link GuildChannel}
     *         due to {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL} being revoked
     *     <br>Also can happen if the account lost the {@link Permission#MESSAGE_HISTORY Permission.MESSAGE_HISTORY}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_PERMISSIONS MISSING_PERMISSIONS}
     *     <br>The reaction request was attempted after the account lost {@link Permission#MESSAGE_MANAGE Permission.MESSAGE_MANAGE}
     *         in the {@link GuildChannel} when removing the reaction.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_EMOJI UNKNOWN_EMOJI}
     *     <br>The provided emoji was deleted, doesn't exist, or is not available to the currently logged-in account in this channel.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If the message has already been deleted. This might also be triggered for ephemeral messages.</li>
     * </ul>
     *
     * @param  emoji
     *         The {@link Emoji} reaction to remove as a reaction from this Message.
     * @param  user
     *         The {@link User} to remove the reaction for.
     *
     * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
     *         If the MessageChannel this message was sent in was a {@link GuildChannel}
     *         and the logged in account does not have {@link Permission#MESSAGE_HISTORY Permission.MESSAGE_HISTORY}.
     * @throws java.lang.IllegalArgumentException
     *         <ul>
     *             <li>If the provided {@code emoji} is null.</li>
     *             <li>If the provided {@code emoji} cannot be used in the current channel.
     *                 See {@link RichCustomEmoji#canInteract(User, MessageChannel)} or {@link RichCustomEmoji#canInteract(Member)} for more information.</li>
     *             <li>If the provided user is null</li>
     *         </ul>
     * @throws java.lang.IllegalStateException
     *         <ul>
     *             <li>If this message was <b>not</b> sent in a
     *                 {@link Guild Guild}
     *                 <b>and</b> the given user is <b>not</b> the {@link SelfUser}.</li>
     *             <li>If this message is ephemeral</li>
     *         </ul>
     *
     *
     * @return {@link RestAction}
     */
    @Nonnull
    @CheckReturnValue
    RestAction<Void> removeReaction(@Nonnull Emoji emoji, @Nonnull User user);

    /**
     * This obtains the {@link User users} who reacted using the given {@link Emoji}.
     *
     * <br>By default, this only includes users that reacted with {@link MessageReaction.ReactionType#NORMAL}.
     * Use {@link #retrieveReactionUsers(Emoji, MessageReaction.ReactionType) retrieveReactionUsers(emoji, ReactionType.SUPER)}
     * to retrieve the users that used a super reaction instead.
     *
     * <p>Messages maintain a list of reactions, alongside a list of users who added them.
     *
     * <p>Using this data, we can obtain a {@link ReactionPaginationAction}
     * of the users who've reacted to this message.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The retrieve request was attempted after the account lost access to the {@link GuildChannel}
     *         due to {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL} being revoked
     *     <br>Also can happen if the account lost the {@link Permission#MESSAGE_HISTORY Permission.MESSAGE_HISTORY}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_EMOJI UNKNOWN_EMOJI}
     *     <br>The provided emoji was deleted, doesn't exist, or is not available to the currently logged-in account in this channel.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If the message has already been deleted. This might also be triggered for ephemeral messages.</li>
     * </ul>
     *
     * @param  emoji
     *         The {@link Emoji} to retrieve users for.
     *
     * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
     *         If the MessageChannel this message was sent in was a {@link GuildChannel} and the
     *         logged in account does not have {@link Permission#MESSAGE_HISTORY Permission.MESSAGE_HISTORY} in the channel.
     * @throws java.lang.IllegalArgumentException
     *         If the provided {@link Emoji} is null.
     * @throws IllegalStateException
     *         If this Message is ephemeral
     *
     * @return The {@link ReactionPaginationAction} of the users who reacted with the provided emoji
     */
    @Nonnull
    @CheckReturnValue
    default ReactionPaginationAction retrieveReactionUsers(@Nonnull Emoji emoji)
    {
        return retrieveReactionUsers(emoji, MessageReaction.ReactionType.NORMAL);
    }

    /**
     * This obtains the {@link User users} who reacted using the given {@link Emoji}.
     *
     * <p>Messages maintain a list of reactions, alongside a list of users who added them.
     *
     * <p>Using this data, we can obtain a {@link ReactionPaginationAction}
     * of the users who've reacted to this message.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The retrieve request was attempted after the account lost access to the {@link GuildChannel}
     *         due to {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL} being revoked
     *     <br>Also can happen if the account lost the {@link Permission#MESSAGE_HISTORY Permission.MESSAGE_HISTORY}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_EMOJI UNKNOWN_EMOJI}
     *     <br>The provided emoji was deleted, doesn't exist, or is not available to the currently logged-in account in this channel.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If the message has already been deleted. This might also be triggered for ephemeral messages.</li>
     * </ul>
     *
     * @param  emoji
     *         The {@link Emoji} to retrieve users for.
     * @param  type
     *         The specific type of reaction
     *
     * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
     *         If the MessageChannel this message was sent in was a {@link GuildChannel} and the
     *         logged in account does not have {@link Permission#MESSAGE_HISTORY Permission.MESSAGE_HISTORY} in the channel.
     * @throws java.lang.IllegalArgumentException
     *         If the provided null is provided.
     * @throws IllegalStateException
     *         If this Message is ephemeral
     *
     * @return The {@link ReactionPaginationAction} of the users who reacted with the provided emoji
     */
    @Nonnull
    @CheckReturnValue
    ReactionPaginationAction retrieveReactionUsers(@Nonnull Emoji emoji, @Nonnull MessageReaction.ReactionType type);

    /**
     * This obtains the {@link MessageReaction} for the given {@link Emoji} on this message.
     * <br>The reaction instance also store which users reacted with the specified emoji.
     *
     * <p>Messages store reactions by keeping a list of reaction names.
     *
     * @param  emoji
     *         The unicode or custom emoji of the reaction emoji
     *
     * @throws java.lang.IllegalArgumentException
     *         If the provided emoji is null
     *
     * @return The {@link MessageReaction} or null if not present.
     */
    @Nullable
    @CheckReturnValue
    MessageReaction getReaction(@Nonnull Emoji emoji);

    /**
     * Enables/Disables suppression of Embeds on this Message.
     * <br>Suppressing Embeds is equivalent to pressing the {@code X} in the top-right corner of an Embed inside the Discord client.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The clear-reactions request was attempted after the account lost access to the {@link GuildChannel}
     *         due to {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL} being revoked, or the
     *         account lost access to the {@link Guild Guild}
     *         typically due to being kicked or removed.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_PERMISSIONS MISSING_PERMISSIONS}
     *     <br>The suppress-embeds request was attempted after the account lost {@link Permission#MESSAGE_MANAGE Permission.MESSAGE_MANAGE}
     *         in the {@link GuildChannel} when adding the reaction.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If the message has already been deleted. This might also be triggered for ephemeral messages.</li>
     * </ul>
     *
     * @param  suppressed
     *         Whether the embed should be suppressed
     *
     * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
     *         If the MessageChannel this message was sent in was a {@link GuildChannel}
     *         and the currently logged in account does not have
     *         {@link Permission#MESSAGE_MANAGE Permission.MESSAGE_MANAGE} in the channel.
     * @throws net.dv8tion.jda.api.exceptions.PermissionException
     *         If the MessageChannel this message was sent in was a {@link PrivateChannel PrivateChannel}
     *         and the message was not sent by the currently logged in account.
     * @throws IllegalStateException
     *         If this Message is ephemeral
     * @return {@link net.dv8tion.jda.api.requests.restaction.AuditableRestAction AuditableRestAction} - Type: {@link java.lang.Void}
     *
     * @see    #isSuppressedEmbeds()
     */
    @Nonnull
    @CheckReturnValue
    AuditableRestAction<Void> suppressEmbeds(boolean suppressed);

    /**
     * Attempts to crosspost this message.
     *
     * <p>The following {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} are possible:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#ALREADY_CROSSPOSTED ALREADY_CROSSPOSTED}
     *     <br>The target message has already been crossposted.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_ACCESS MISSING_ACCESS}
     *     <br>The request was attempted after the account lost access to the
     *         {@link Guild Guild}
     *         typically due to being kicked or removed, or after {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL}
     *         was revoked in the {@link GuildChannel}</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_PERMISSIONS MISSING_PERMISSIONS}
     *     <br>The request was attempted after the account lost
     *         {@link Permission#MESSAGE_MANAGE Permission.MESSAGE_MANAGE} in the GuildMessageChannel.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_MESSAGE UNKNOWN_MESSAGE}
     *     <br>If the message has already been deleted. This might also be triggered for ephemeral messages.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#UNKNOWN_CHANNEL UNKNOWN_CHANNEL}
     *     <br>The request was attempted after the channel was deleted.</li>
     * </ul>
     *
     * @throws IllegalStateException
     *         <ul>
     *             <li>If the channel is not a {@link NewsChannel}.</li>
     *             <li>If the message is ephemeral.</li>
     *         </ul>
     * @throws MissingAccessException
     *         If the currently logged in account does not have {@link Member#hasAccess(GuildChannel) access} in this channel.
     * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
     *         If the currently logged in account does not have
     *         {@link Permission#VIEW_CHANNEL Permission.VIEW_CHANNEL} in this channel
     *         or if this message is from another user and we don't have {@link Permission#MESSAGE_MANAGE Permission.MESSAGE_MANAGE}.
     *
     * @return {@link RestAction} - Type: {@link Message}
     *
     * @since  4.2.1
     */
    @Nonnull
    @CheckReturnValue
    RestAction<Message> crosspost();

    /**
     * Whether embeds are suppressed for this message.
     * When Embeds are suppressed, they are not displayed on clients nor provided via API until un-suppressed.
     * <br>This is a shortcut method for checking if {@link #getFlags() getFlags()} contains
     * {@link net.dv8tion.jda.api.entities.Message.MessageFlag#EMBEDS_SUPPRESSED MessageFlag#EMBEDS_SUPPRESSED}
     *
     * @return Whether or not Embeds are suppressed for this Message.
     *
     * @see    #suppressEmbeds(boolean)
     */
    boolean isSuppressedEmbeds();

    /**
     * Returns an EnumSet of all {@link Message.MessageFlag MessageFlags} present for this Message.
     *
     * @return Never-Null EnumSet of present {@link Message.MessageFlag MessageFlags}
     *
     * @see    Message.MessageFlag
     */
    @Nonnull
    EnumSet<MessageFlag> getFlags();

    /**
     * Returns the raw message flags of this message
     *
     * @return The raw message flags
     *
     * @see    #getFlags()
     */
    long getFlagsRaw();

    /**
     * Whether this message is ephemeral.
     * <br>The message being ephemeral means it is only visible to the bot and the interacting user
     * <br>This is a shortcut method for checking if {@link #getFlags()} contains {@link MessageFlag#EPHEMERAL}
     *
     * @return Whether the message is ephemeral
     */
    boolean isEphemeral();

    /**
     * Whether this message is silent.
     * <br>The message being silent means it will not trigger push and desktop notifications
     * <br>This is a shortcut method for checking if {@link #getFlags()} contains {@link MessageFlag#NOTIFICATIONS_SUPPRESSED}
     *
     * @return Whether the message is silent
     */
    boolean isSuppressedNotifications();

    /**
     * Whether this message is a voice message.
     *
     * @return True, if this is a voice message
     */
    boolean isVoiceMessage();

    /**
     * Returns a possibly {@code null} {@link ThreadChannel ThreadChannel} that was started from this message.
     * This can be {@code null} due to no ThreadChannel being started from it or the ThreadChannel later being deleted.
     *
     * @return The {@link ThreadChannel ThreadChannel} that was started from this message.
     */
    @Nullable
    ThreadChannel getStartedThread();

    /**
     * This specifies the {@link net.dv8tion.jda.api.entities.MessageType MessageType} of this Message.
     *
     * <p>Messages can represent more than just simple text sent by Users, they can also be special messages that
     * inform about events that occur. Messages can either be {@link net.dv8tion.jda.api.entities.MessageType#DEFAULT default messages}
     * or special messages like {@link net.dv8tion.jda.api.entities.MessageType#GUILD_MEMBER_JOIN welcome messages}.
     *
     * @return The {@link net.dv8tion.jda.api.entities.MessageType MessageType} of this message.
     */
    @Nonnull
    MessageType getType();

    /**
     * This is sent on the message object when the message is a response to an {@link net.dv8tion.jda.api.interactions.Interaction Interaction} without an existing message.
     *
     * <p>This means responses to Message Components do not include this property, instead including a message reference object as components always exist on preexisting messages.
     *
     * @return The {@link net.dv8tion.jda.api.entities.Message.Interaction Interaction} of this message.
     *
     * @deprecated
     *         Replaced with {@link #getInteractionMetadata()}
     */
    @Nullable
    @Deprecated
    Interaction getInteraction();

    /**
     * Returns the interaction metadata,
     * available when the message is a response or a followup to an {@link net.dv8tion.jda.api.interactions.Interaction Interaction}.
     *
     * @return The {@link InteractionMetadata} of this message, or {@code null}
     */
    @Nullable
    InteractionMetadata getInteractionMetadata();

    /**
     * Creates a new, public {@link ThreadChannel} spawning/starting at this {@link Message} inside the {@link IThreadContainer} this message was sent in.
     * <br>The starting message will copy this message, and will be of type {@link MessageType#THREAD_STARTER_MESSAGE MessageType.THREAD_STARTER_MESSAGE}.
     *
     * <p>The resulting {@link ThreadChannel ThreadChannel} may be one of:
     * <ul>
     *     <li>{@link ChannelType#GUILD_PUBLIC_THREAD}</li>
     *     <li>{@link ChannelType#GUILD_NEWS_THREAD}</li>
     * </ul>
     *
     * <p>Possible {@link net.dv8tion.jda.api.requests.ErrorResponse ErrorResponses} caused by
     * the returned {@link net.dv8tion.jda.api.requests.RestAction RestAction} include the following:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MISSING_PERMISSIONS MISSING_PERMISSIONS}
     *     <br>The channel could not be created due to a permission discrepancy</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MAX_CHANNELS MAX_CHANNELS}
     *     <br>The maximum number of channels were exceeded</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#THREAD_WITH_THIS_MESSAGE_ALREADY_EXISTS}
     *     <br>This message has already been used to create a thread</li>
     *
     *     <li>{@link net.dv8tion.jda.api.requests.ErrorResponse#MAX_ACTIVE_THREADS}
     *     <br>The maximum number of active threads has been reached, and no more may be created.</li>
     * </ul>
     *
     * @param  name
     *         The name of the new ThreadChannel (up to {@value Channel#MAX_NAME_LENGTH} characters)
     *
     * @throws IllegalArgumentException
     *         If the provided name is null, blank, empty, or longer than {@value Channel#MAX_NAME_LENGTH} characters
     * @throws IllegalStateException
     *         If the message's channel is not actually a {@link net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer}.
     * @throws UnsupportedOperationException
     *         If this is a forum channel.
     *         You must use {@link net.dv8tion.jda.api.entities.channel.concrete.ForumChannel#createForumPost(String, MessageCreateData) createForumPost(...)} instead.
     * @throws InsufficientPermissionException
     *         If the bot does not have {@link net.dv8tion.jda.api.Permission#CREATE_PUBLIC_THREADS Permission.CREATE_PUBLIC_THREADS} in this channel
     *
     * @return A specific {@link ThreadChannelAction} that may be used to configure the new ThreadChannel before its creation.
     */
    @Nonnull
    @CheckReturnValue
    ThreadChannelAction createThreadChannel(@Nonnull String name);

    /**
     * Mention constants, useful for use with {@link java.util.regex.Pattern Patterns}
     */
    enum MentionType
    {
        /**
         * Represents a mention for a {@link User User}/{@link net.dv8tion.jda.api.entities.Member Member}
         * <br>The first and only group matches the id of the mention.
         */
        USER("<@!?(\\d+)>", "users"),
        /**
         * Represents a mention for a {@link net.dv8tion.jda.api.entities.Role Role}
         * <br>The first and only group matches the id of the mention.
         */
        ROLE("<@&(\\d+)>", "roles"),
        /**
         * Represents a mention for a {@link GuildChannel}
         * <br>The first and only group matches the id of the mention.
         */
        CHANNEL("<#(\\d+)>", null),
        /**
         * Represents a mention for a {@link CustomEmoji}
         * <br>The first group matches the name of the emoji and the second the id of the mention.
         */
        EMOJI("<a?:([a-zA-Z0-9_]+):([0-9]+)>", null),
        /**
         * Represents a mention for all active users, literal {@code @here}
         */
        HERE("@here", "everyone"),
        /**
         * Represents a mention for all users in a server, literal {@code @everyone}.
         */
        EVERYONE("@everyone", "everyone"),
        /**
         * Represents a mention for a slash command.
         * <br>The first group is the command name, the second group is the subcommand group name (nullable),
         * the third group is the subcommand name (nullable), and the fourth group is the command ID.
         */
        SLASH_COMMAND("</([\\w-]+)(?> ([\\w-]+))??(?> ([\\w-]+))?:(\\d+)>", null);

        private final Pattern pattern;
        private final String parseKey;

        MentionType(String regex, String parseKey)
        {
            this.pattern = Pattern.compile(regex);
            this.parseKey = parseKey;
        }

        @Nonnull
        public Pattern getPattern()
        {
            return pattern;
        }

        /**
         * The Key returned by this method is used to determine the group or parsable mention group they are part of.
         * <br>It is used internally in methods like {@link MessageRequest#setAllowedMentions(Collection)}.
         * <p>
         * Returns {@code null}, when they don't belong to any mention group.
         *
         * @return Nullable group key for mention parsing
         */
        @Nullable
        public String getParseKey()
        {
            return parseKey;
        }
    }

    /**
     * Enum representing the flags on a Message.
     * <p>
     * Note: The Values defined in this Enum are not considered final and only represent the current State of <i>known</i> Flags.
     */
    enum MessageFlag
    {
        /**
         * The Message has been published to subscribed Channels (via Channel Following)
         */
        CROSSPOSTED(0),
        /**
         * The Message originated from a Message in another Channel (via Channel Following)
         */
        IS_CROSSPOST(1),
        /**
         * Embeds are suppressed on the Message.
         * @see net.dv8tion.jda.api.entities.Message#isSuppressedEmbeds() Message#isSuppressedEmbeds()
         */
        EMBEDS_SUPPRESSED(2),
        /**
         * Indicates, that the source message of this crosspost was deleted.
         * This should only be possible in combination with {@link #IS_CROSSPOST}
         */
        SOURCE_MESSAGE_DELETED(3),
        /**
         * Indicates, that this Message came from the urgent message system
         */
        URGENT(4),
        /**
         * Indicates, that this Message is ephemeral, the Message is only visible to the bot and the interacting user
         * @see Message#isEphemeral
         */
        EPHEMERAL(6),
        /**
         * Indicates, that this Message is an interaction response and the bot is "thinking"
         */
        LOADING(7),
        /**
         * Indicates, that this message will not trigger push and desktop notifications
         * @see Message#isSuppressedNotifications
         */
        NOTIFICATIONS_SUPPRESSED(12),
        /**
         * The Message is a voice message, containing an audio attachment
         */
        IS_VOICE_MESSAGE(13),
        /**
         * Indicates this message is using V2 components.
         *
         * <p>Using V2 components has no top-level component limit,
         * and allows more components in total ({@value Message#MAX_COMPONENT_COUNT_IN_COMPONENT_TREE}).
         * <br>They also allow you to use a larger choice of components,
         * such as any component extending {@link MessageTopLevelComponent},
         * as long as they are {@linkplain Component.Type#isMessageCompatible() compatible}.
         * <br>The character limit for the messages also gets changed to {@value Message#MAX_CONTENT_LENGTH_COMPONENT_V2}.
         *
         * <p>This, however, comes with a few drawbacks:
         * <ul>
         *     <li>You cannot send content, embeds, polls or stickers</li>
         *     <li>It does not support voice messages</li>
         *     <li>It does not support previewing files</li>
         *     <li>URLs don't create embeds</li>
         *     <li>You cannot switch this message back to not using Components V2 (you can however upgrade a message to V2)</li>
         * </ul>
         *
         * @see MessageRequest#useComponentsV2()
         * @see MessageRequest#useComponentsV2(boolean)
         * @see MessageRequest#setDefaultUseComponentsV2(boolean)
         */
        IS_COMPONENTS_V2(15);

        private final int value;

        MessageFlag(int offset)
        {
            this.value = 1 << offset;
        }

        /**
         * Returns the value of the MessageFlag as represented in the bitfield. It is always a power of 2 (single bit)
         *
         * @return Non-Zero bit value of the field
         */
        public int getValue()
        {
            return value;
        }

        /**
         * Given a bitfield, this function extracts all Enum values according to their bit values and returns
         * an EnumSet containing all matching MessageFlags
         * @param  bitfield
         *         Non-Negative integer representing a bitfield of MessageFlags
         * @return Never-Null EnumSet of MessageFlags being found in the bitfield
         */
        @Nonnull
        public static EnumSet<MessageFlag> fromBitField(int bitfield)
        {
            Set<MessageFlag> set = Arrays.stream(MessageFlag.values())
                .filter(e -> (e.value & bitfield) > 0)
                .collect(Collectors.toSet());
            return set.isEmpty() ? EnumSet.noneOf(MessageFlag.class) : EnumSet.copyOf(set);
        }

        /**
         * Converts a Collection of MessageFlags back to the integer representing the bitfield.
         * This is the reverse operation of {@link #fromBitField(int)}.
         * @param  coll
         *         A Non-Null Collection of MessageFlags
         * @throws IllegalArgumentException
         *         If the provided Collection is {@code null}
         * @return Integer value of the bitfield representing the given MessageFlags
         */
        public static int toBitField(@Nonnull Collection<MessageFlag> coll)
        {
            Checks.notNull(coll, "Collection");
            int flags = 0;
            for (MessageFlag messageFlag : coll)
            {
                flags |= messageFlag.value;
            }
            return flags;
        }
    }

    /**
     * Represents a {@link net.dv8tion.jda.api.entities.Message Message} file attachment.
     */
    class Attachment implements ISnowflake, AttachedFile
    {
        private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList("jpg",
                "jpeg", "png", "gif", "webp", "tiff", "svg", "apng"));
        private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList("webm",
                "flv", "vob", "avi", "mov", "wmv", "amv", "mp4", "mpg", "mpeg", "gifv"));
        private final long id;
        private final String url;
        private final String proxyUrl;
        private final String fileName;
        private final String contentType;
        private final String description;
        private final int size;
        private final int height;
        private final int width;
        private final boolean ephemeral;
        private final String waveform;
        private final double duration;

        private final JDAImpl jda;

        public Attachment(long id, String url, String proxyUrl, String fileName, String contentType, String description, int size, int height, int width, boolean ephemeral, String waveform, double duration, JDAImpl jda)
        {
            this.id = id;
            this.url = url;
            this.proxyUrl = proxyUrl;
            this.fileName = fileName;
            this.contentType = contentType;
            this.description = description;
            this.size = size;
            this.height = height;
            this.width = width;
            this.ephemeral = ephemeral;
            this.waveform = waveform;
            this.duration = duration;
            this.jda = jda;
        }

        /**
         * The corresponding JDA instance for this Attachment
         *
         * @return The corresponding JDA instance for this Attachment
         */
        @Nonnull
        public JDA getJDA()
        {
            return jda;
        }

        @Override
        public long getIdLong()
        {
            return id;
        }

        /**
         * The url of the Attachment, most likely on the Discord servers.
         *
         * @return Non-null String containing the Attachment URL.
         */
        @Nonnull
        public String getUrl()
        {
            return url;
        }

        /**
         * Url to the resource proxied by the Discord CDN.
         *
         * @return Non-null String containing the proxied Attachment url.
         */
        @Nonnull
        public String getProxyUrl()
        {
            return proxyUrl;
        }

        /**
         * Returns an {@link NamedAttachmentProxy} for this attachment.
         *
         * @return Non-null {@link NamedAttachmentProxy} of this attachment
         *
         * @see    #getProxyUrl()
         */
        @Nonnull
        public NamedAttachmentProxy getProxy()
        {
            return new NamedAttachmentProxy(width > 0 && height > 0 ? proxyUrl : url, fileName);
        }

        /**
         * The file name of the Attachment when it was first uploaded.
         *
         * @return Non-null String containing the Attachment file name.
         */
        @Nonnull
        public String getFileName()
        {
            return fileName;
        }

        /**
         * The file extension of the Attachment when it was first uploaded.
         * <br>Null is returned if no characters follow the last occurrence of the '{@code .}' character
         * (or if the character is not present in {@link #getFileName()}).
         *
         * @return Non-null String containing the Attachment file extension, or null if it can't be determined.
         */
        @Nullable
        public String getFileExtension()
        {
            int index = fileName.lastIndexOf('.') + 1;
            return index == 0 || index == fileName.length() ? null : fileName.substring(index);
        }

        /**
         * The Content-Type of this file.
         * <br>This is the  <a href="https://en.wikipedia.org/wiki/Media_type" target="_blank">Media type</a> of the file that would be used in an HTTP request or similar.
         *
         * @return The content-type, or null if this isn't provided
         */
        @Nullable
        public String getContentType()
        {
            return contentType;
        }

        /**
         * The description (alt text) of this attachment.
         * <br>This description is shown when hovering over the attachment in the client.
         *
         * @return The description, or null if this isn't provided
         */
        @Nullable
        public String getDescription()
        {
            return description;
        }

        /**
         * The size of the attachment in bytes.
         * <br>Example: if {@code getSize()} returns 1024, then the attachment is 1024 bytes, or 1KiB, in size.
         *
         * @return Positive int containing the size of the Attachment.
         */
        public int getSize()
        {
            return size;
        }

        /**
         * The height of the Attachment if this Attachment is an image/video.
         * <br>If this Attachment is neither an image, nor a video, this returns -1.
         *
         * @return int containing image/video Attachment height, or -1 if attachment is neither image nor video.
         */
        public int getHeight()
        {
            return height;
        }

        /**
         * The width of the Attachment if this Attachment is an image/video.
         * <br>If this Attachment is neither an image, nor a video, this returns -1.
         *
         * @return int containing image/video Attachment width, or -1 if attachment is neither image nor video.
         */
        public int getWidth()
        {
            return width;
        }

        /**
         * Whether or not this attachment is from an ephemeral Message.
         * <br>If this Attachment is ephemeral, it will automatically be removed after 2 weeks. The attachment is guaranteed to be available as long as the message itself exists.
         *
         * @return True if this attachment is from an ephemeral message
         */
        public boolean isEphemeral()
        {
            return ephemeral;
        }

        /**
         * Gets the waveform data encoded in this attachment. This is currently only present on
         * {@link MessageFlag#IS_VOICE_MESSAGE voice messages}.
         *
         * @return A possibly-{@code null} array of integers representing the amplitude of the
         *         audio over time. Amplitude is sampled at 10Hz, but the client will decrease
         *         this to keep the waveform to at most 256 bytes.
         *         The values in this array are <b>unsigned</b>.
         */
        @Nullable
        public byte[] getWaveform()
        {
            if (waveform == null)
                return null;
            return Base64.getDecoder().decode(waveform);
        }

        /**
         * Gets the duration of this attachment. This is currently only nonzero on
         * {@link MessageFlag#IS_VOICE_MESSAGE voice messages}.
         *
         * @return The duration of this attachment's audio in seconds, or {@code 0}
         *         if this is not a voice message.
         */
        public double getDuration()
        {
            return duration;
        }

        /**
         * Whether or not this attachment is an Image,
         * based on {@link #getWidth()}, {@link #getHeight()}, and {@link #getFileExtension()}.
         *
         * @return True if this attachment is an image
         */
        public boolean isImage()
        {
            if (width < 0) return false; //if width is -1, so is height
            String extension = getFileExtension();
            return extension != null && IMAGE_EXTENSIONS.contains(extension.toLowerCase());
        }

        /**
         * Whether or not this attachment is a video,
         * based on {@link #getWidth()}, {@link #getHeight()}, and {@link #getFileExtension()}.
         *
         * @return True if this attachment is a video
         */
        public boolean isVideo()
        {
            if (width < 0) return false; //if width is -1, so is height
            String extension = getFileExtension();
            return extension != null && VIDEO_EXTENSIONS.contains(extension.toLowerCase());
        }

        /**
         * Whether or not this attachment is marked as spoiler,
         * based on {@link #getFileName()}.
         *
         * @return True if this attachment is marked as spoiler
         *
         * @since  4.2.1
         */
        public boolean isSpoiler()
        {
            return getFileName().startsWith("SPOILER_");
        }

        @Override
        public void close() {}

        @Override
        public void forceClose() {}

        @Override
        public void addPart(@Nonnull MultipartBody.Builder builder, int index) {}

        @Nonnull
        @Override
        public DataObject toAttachmentData(int index)
        {
            return DataObject.empty().put("id", id);
        }
    }

    /**
     * Represents an {@link net.dv8tion.jda.api.interactions.Interaction Interaction} provided with a {@link net.dv8tion.jda.api.entities.Message Message}.
     *
     * @deprecated Replaced with {@link InteractionMetadata}
     */
    @Deprecated
    class Interaction implements ISnowflake
    {
        private final long id;
        private final int type;
        private final String name;
        private final User user;
        private final Member member;

        public Interaction(long id, int type, String name, User user, Member member)
        {
            this.id = id;
            this.type = type;
            this.name = name;
            this.user = user;
            this.member = member;
        }

        @Override
        public long getIdLong()
        {
            return id;
        }

        /**
         * The raw interaction type.
         * <br>It is recommended to use {@link #getType()} instead.
         *
         * @return The raw interaction type
         */
        public int getTypeRaw()
        {
            return type;
        }

        /**
         * The {@link net.dv8tion.jda.api.interactions.InteractionType} for this interaction.
         *
         * @return The {@link net.dv8tion.jda.api.interactions.InteractionType} or {@link net.dv8tion.jda.api.interactions.InteractionType#UNKNOWN}
         */
        @Nonnull
        public InteractionType getType()
        {
            return InteractionType.fromKey(getTypeRaw());
        }

        /**
         * The command name.
         *
         * @return The command name
         */
        @Nonnull
        public String getName()
        {
            return name;
        }

        /**
         * The {@link User} who caused this interaction.
         *
         * @return The {@link User}
         */
        @Nonnull
        public User getUser()
        {
            return user;
        }

        /**
         * The {@link Member} who caused this interaction.
         * <br>This is null if the interaction is not from a guild.
         *
         * @return The {@link Member}
         */
        @Nullable
        public Member getMember()
        {
            return member;
        }
    }

    /**
     * Metadata about the interaction, including the source of the interaction and relevant server and user IDs.
     *
     * @see Message#getInteractionMetadata()
     */
    class InteractionMetadata implements ISnowflake
    {
        private final long id;
        private final int type;
        private final User user;
        private final IntegrationOwners integrationOwners;
        private final long originalResponseMessageId;
        private final long interactedMessageId;
        private final InteractionMetadata triggeringInteraction;
        private final User targetUser;
        private final long targetMessageId;

        public InteractionMetadata(long id, int type, User user, IntegrationOwners integrationOwners, long originalResponseMessageId, long interactedMessageId, InteractionMetadata triggeringInteraction, User targetUser, long targetMessageId)
        {
            this.id = id;
            this.type = type;
            this.user = user;
            this.integrationOwners = integrationOwners;
            this.originalResponseMessageId = originalResponseMessageId;
            this.interactedMessageId = interactedMessageId;
            this.triggeringInteraction = triggeringInteraction;
            this.targetUser = targetUser;
            this.targetMessageId = targetMessageId;
        }

        @Override
        public long getIdLong()
        {
            return id;
        }

        /**
         * The raw interaction type.
         * <br>It is recommended to use {@link #getType()} instead.
         *
         * @return The raw interaction type
         */
        public int getTypeRaw()
        {
            return type;
        }

        /**
         * The {@link net.dv8tion.jda.api.interactions.InteractionType} for this interaction.
         *
         * @return The {@link net.dv8tion.jda.api.interactions.InteractionType} or {@link net.dv8tion.jda.api.interactions.InteractionType#UNKNOWN}
         */
        @Nonnull
        public InteractionType getType()
        {
            return InteractionType.fromKey(type);
        }

        /**
         * The {@link User} who caused this interaction.
         *
         * @return The {@link User}
         */
        @Nonnull
        public User getUser()
        {
            return user;
        }

        /**
         * Returns the integration owners of this interaction, which depends on how the app was installed.
         *
         * @return The integration owners of this interaction
         */
        @Nonnull
        public IntegrationOwners getIntegrationOwners()
        {
            return integrationOwners;
        }

        /**
         * The ID of the original response message, present only on followup messages.
         *
         * @return The ID of the original response message, or {@code 0}
         */
        public long getOriginalResponseMessageIdLong()
        {
            return originalResponseMessageId;
        }

        /**
         * The ID of the original response message, present only on followup messages.
         *
         * @return The ID of the original response message, or {@code null}
         */
        @Nullable
        public String getOriginalResponseMessageId()
        {
            if (originalResponseMessageId == 0) return null;
            return Long.toUnsignedString(originalResponseMessageId);
        }

        /**
         * The ID of the message containing the component which created this message.
         *
         * @return the ID of the message containing the component which created this message, or {@code 0}
         */
        public long getInteractedMessageIdLong()
        {
            return interactedMessageId;
        }

        /**
         * The ID of the message containing the component which created this message.
         *
         * @return the ID of the message containing the component which created this message, or {@code null}
         */
        @Nullable
        public String getInteractedMessageId()
        {
            if (interactedMessageId == 0) return null;
            return Long.toUnsignedString(interactedMessageId);
        }

        /**
         * Metadata for the interaction that was used to open the modal,
         * present only on modal submit interactions.
         *
         * @return Metadata for the interaction that was used to open the modal, or {@code null}
         */
        @Nullable
        public InteractionMetadata getTriggeringInteraction()
        {
            return triggeringInteraction;
        }

        /**
         * The user the command was run on, present only on user interaction commands.
         *
         * @return The user the command was run on, or {@code null}
         */
        @Nullable
        public User getTargetUser()
        {
            return targetUser;
        }

        /**
         * The ID of the message the command was run on, present only on message interaction commands.
         *
         * <p>If this is present, {@link Message#getMessageReference()} will also be present.
         *
         * @return The ID of the message the command was run on, or {@code 0}
         */
        public long getTargetMessageIdLong()
        {
            return targetMessageId;
        }

        /**
         * The ID of the message the command was run on, present only on message interaction commands.
         *
         * <p>If this is present, {@link Message#getMessageReference()} will also be present.
         *
         * @return The ID of the message the command was run on, or {@code null}
         */
        @Nullable
        public String getTargetMessageId()
        {
            if (targetMessageId == 0) return null;
            return Long.toUnsignedString(targetMessageId);
        }
    }
}
