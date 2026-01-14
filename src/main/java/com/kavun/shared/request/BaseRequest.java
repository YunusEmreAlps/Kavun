package com.kavun.shared.request;

import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The BaseRequest provides base fields to be extended by all Requests.
 *
 * <p>
 * This class contains common audit fields that map to {@link
 * com.boss.backend.persistent.domain.base.BaseEntity}.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseRequest implements Serializable {

}
