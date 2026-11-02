package org.jahia.test.modules.formidable.nnmsync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSynchronizerTest {

    private static final String UUID = "form-1";

    private final Map<String, SyncState.Entry> stored = new HashMap<>();
    private final List<String> sent = new ArrayList<>();
    private int status = 201;
    private boolean unreachable;
    private FormDefinition live;
    private ConfigSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        live = new FormDefinition("contact", List.of(new FormDefinition.Field("email", "email", true, List.of())));
        SyncState state = new SyncState() {
            @Override
            public Optional<Entry> get(String formUuid) {
                return Optional.ofNullable(stored.get(formUuid));
            }

            @Override
            public void put(String formUuid, Entry entry) {
                stored.put(formUuid, entry);
            }
        };
        synchronizer = new ConfigSynchronizer(json -> {
            if (unreachable) {
                throw new IOException("timeout");
            }
            sent.add(json);
            return status;
        }, formUuid -> Optional.ofNullable(live), state, null);
    }

    @Test
    void theFirstPublicationSendsVersionOne() {
        synchronizer.sync(UUID, false);

        assertEquals(1, sent.size());
        assertTrue(sent.get(0).contains("\"version\":1"));
        assertEquals(1, stored.get(UUID).version());
    }

    @Test
    void aRepublicationWithoutChangeSendsNothing() {
        synchronizer.sync(UUID, false);
        synchronizer.sync(UUID, false);

        assertEquals(1, sent.size());
    }

    @Test
    void aChangeSendsTheNextVersion() {
        synchronizer.sync(UUID, false);
        live = new FormDefinition("contact", List.of(new FormDefinition.Field("email", "email", false, List.of())));
        synchronizer.sync(UUID, false);

        assertEquals(2, sent.size());
        assertTrue(sent.get(1).contains("\"version\":2"));
        assertEquals(2, stored.get(UUID).version());
    }

    @Test
    void forcingSendsTheCurrentVersionAgain() {
        synchronizer.sync(UUID, false);
        synchronizer.sync(UUID, true);

        assertEquals(2, sent.size());
        assertTrue(sent.get(1).contains("\"version\":1"));
    }

    @Test
    void aServerErrorLeavesTheFormPendingAndTheRetryReusesTheNumber() {
        status = 503;
        synchronizer.sync(UUID, false);
        assertTrue(synchronizer.pending().contains(UUID));
        assertTrue(stored.isEmpty());

        status = 201;
        synchronizer.retryPending();

        assertTrue(synchronizer.pending().isEmpty());
        assertEquals(2, sent.size());
        assertTrue(sent.get(1).contains("\"version\":1"));
        assertEquals(1, stored.get(UUID).version());
    }

    @Test
    void anUnreachableSystemLeavesTheFormPending() {
        unreachable = true;
        synchronizer.sync(UUID, false);

        assertTrue(synchronizer.pending().contains(UUID));
        assertTrue(stored.isEmpty());
    }

    @Test
    void aRefusalIsNotRetried() {
        status = 400;
        synchronizer.sync(UUID, false);

        assertTrue(synchronizer.pending().isEmpty());
        assertTrue(stored.isEmpty());
    }

    @Test
    void aFormThatIsNotConnectedOrNotPublishedIsLeftAlone() {
        live = null;
        synchronizer.sync(UUID, false);

        assertTrue(sent.isEmpty());
    }
}
