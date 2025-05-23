package com.kavun.backend.persistent.domain.base;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

/**
 * A custom sequence generator that can also accommodate manually assigned identifier.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public class AssignedSequenceStyleGenerator extends SequenceStyleGenerator {

  @Serial private static final long serialVersionUID = -2752406853588870681L;

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object) {
    if (object instanceof Identifiable<?> identifiable) {
      Serializable id = identifiable.getId();
      if (Objects.nonNull(id)) {
        return id;
      }
    }
    return (Serializable) super.generate(session, object);
  }
}
