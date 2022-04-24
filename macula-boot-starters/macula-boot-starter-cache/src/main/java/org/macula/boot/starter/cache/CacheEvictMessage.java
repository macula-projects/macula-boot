package org.macula.boot.starter.cache;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Small message used by this caching library in order to sync eviction of entries when necessary
 * @author Rain
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheEvictMessage implements Serializable {

    private String cacheName;
    private String entryKey;

}
