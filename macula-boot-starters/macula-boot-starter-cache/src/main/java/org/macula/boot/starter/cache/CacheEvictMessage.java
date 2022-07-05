package org.macula.boot.starter.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Small message used by this caching library in order to sync eviction of entries when necessary
 *
 * @author Rain
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheEvictMessage implements Serializable {

    private String cacheName;
    private String entryKey;

}
