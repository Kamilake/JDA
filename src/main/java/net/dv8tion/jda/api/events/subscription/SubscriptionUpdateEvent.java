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

package net.dv8tion.jda.api.events.subscription;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Indicates a Premium App subscription was updated ({@code SUBSCRIPTION_UPDATE}).
 *
 * <p>Fired on renewal (period timestamps change), cancellation ({@code status=2}), resume,
 * upgrade/downgrade and end ({@code status=1}). Entitlement events are <b>not</b> sent on renewal.
 *
 * @see <a href="https://docs.discord.com/developers/resources/subscription">Subscription Resource</a>
 */
public class SubscriptionUpdateEvent extends Event {
    /** Subscription is active and scheduled to renew. */
    public static final int STATUS_ACTIVE = 0;
    /** Subscription is inactive and not being charged. */
    public static final int STATUS_INACTIVE = 1;
    /** Subscription is active but will not renew. */
    public static final int STATUS_ENDING = 2;

    private final DataObject payload;

    public SubscriptionUpdateEvent(@Nonnull JDA api, long responseNumber, @Nonnull DataObject payload) {
        super(api, responseNumber);
        this.payload = payload;
    }

    /** The raw subscription object. */
    @Nonnull
    public DataObject getPayload() {
        return payload;
    }

    public long getSubscriptionIdLong() {
        return payload.getUnsignedLong("id");
    }

    public long getUserIdLong() {
        return payload.getUnsignedLong("user_id");
    }

    @Nonnull
    public List<String> getSkuIds() {
        return strings("sku_ids");
    }

    @Nonnull
    public List<String> getEntitlementIds() {
        return strings("entitlement_ids");
    }

    @Nonnull
    public OffsetDateTime getCurrentPeriodStart() {
        return payload.getOffsetDateTime("current_period_start");
    }

    @Nonnull
    public OffsetDateTime getCurrentPeriodEnd() {
        return payload.getOffsetDateTime("current_period_end");
    }

    /** One of {@link #STATUS_ACTIVE}, {@link #STATUS_INACTIVE}, {@link #STATUS_ENDING}, or an unknown future value. */
    public int getStatus() {
        return payload.getInt("status", -1);
    }

    @Nullable
    public OffsetDateTime getCanceledAt() {
        return payload.getOffsetDateTime("canceled_at", null);
    }

    private List<String> strings(String key) {
        DataArray array = payload.optArray(key).orElse(null);
        if (array == null) return Collections.emptyList();
        List<String> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) list.add(array.getString(i));
        return Collections.unmodifiableList(list);
    }
}
