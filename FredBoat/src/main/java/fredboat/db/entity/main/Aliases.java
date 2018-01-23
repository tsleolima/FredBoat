/*
 *
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
 */

package fredboat.db.entity.main;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import space.npstr.sqlsauce.converters.PostgresHStoreConverter;
import space.npstr.sqlsauce.entities.SaucedEntity;
import space.npstr.sqlsauce.fp.types.EntityKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by napster on 23.01.18.
 */
@Entity
@Table(name = "aliases")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "aliases")
public class Aliases extends SaucedEntity<Long, Aliases> {

    //can be a guild or a user id; needs to be a snowflake id or otherwise some kind of unique identification
    @Id
    @Column(name = "id", nullable = false)
    private long id;

    @SuppressWarnings("JpaAttributeTypeInspection")
    @Column(name = "aliases", columnDefinition = "hstore")
    @Convert(converter = PostgresHStoreConverter.class)
    private Map<String, String> aliases = new HashMap<>();

    //for jpa / database wrapper
    public Aliases() {
    }

    @Nonnull
    private static EntityKey<Long, Aliases> key(long id) {
        return EntityKey.of(id, Aliases.class);
    }

    @Nonnull
    public static EntityKey<Long, Aliases> key(@Nonnull User user) {
        return key(user.getIdLong());
    }

    @Nonnull
    public static EntityKey<Long, Aliases> key(@Nonnull Member member) {
        return key(member.getUser());
    }

    @Nonnull
    public static EntityKey<Long, Aliases> key(@Nonnull Guild guild) {
        return key(guild.getIdLong());
    }

    @Nonnull
    @Override
    public Aliases setId(@Nonnull Long id) {
        this.id = id;
        return this;
    }

    @Nonnull
    @Override
    public Long getId() {
        return this.id;
    }


    @Nullable
    public String getAlias(@Nonnull String aliasKey) {
        return aliases.get(aliasKey);
    }

    @Nonnull
    public Aliases setAlias(@Nonnull String aliasKey, @Nonnull String aliasValue) {
        aliases.put(aliasKey, aliasValue); //todo any limits?
        return this;
    }

    @Nonnull
    public Aliases removeAlias(@Nonnull String aliasKey) {
        aliases.remove(aliasKey);
        return this;
    }

    public int size() {
        return aliases.size();
    }

    /**
     * @return all aliases in an unmodifiable representation. Please use other methods like
     * {@link Aliases#setAlias(String, String)} or {@link Aliases#removeAlias(String)} to manipulate the data
     */
    @Nonnull
    public Map<String, String> getAliases() {
        return Collections.unmodifiableMap(aliases);
    }
}
