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

package net.dv8tion.jda.internal.components;

import net.dv8tion.jda.api.components.MessageTopLevelComponentUnion;
import net.dv8tion.jda.api.components.UnknownComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponentUnion;
import net.dv8tion.jda.api.components.container.ContainerChildComponentUnion;
import net.dv8tion.jda.api.components.section.SectionAccessoryComponentUnion;
import net.dv8tion.jda.api.components.section.SectionContentComponentUnion;
import net.dv8tion.jda.api.interactions.modals.ModalTopLevelComponentUnion;
import net.dv8tion.jda.api.utils.data.DataObject;

import javax.annotation.Nonnull;

public class UnknownComponentImpl extends AbstractComponentImpl implements
        UnknownComponent,
        MessageTopLevelComponentUnion,
        ModalTopLevelComponentUnion,
        ActionRowChildComponentUnion,
        SectionContentComponentUnion,
        SectionAccessoryComponentUnion,
        ContainerChildComponentUnion
{
    private final DataObject data;

    public UnknownComponentImpl(DataObject data)
    {
        this.data = data;
    }

    @Nonnull
    @Override
    public UnknownComponentImpl withUniqueId(int uniqueId)
    {
        throw new UnsupportedOperationException("Cannot modify an unknown component");
    }

    @Override
    public int getUniqueId()
    {
        return data.getInt("id");
    }

    @Override
    @Nonnull
    public DataObject toData()
    {
        return data;
    }
}
