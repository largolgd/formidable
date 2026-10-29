package org.jahia.test.modules.formidable.nnmsync;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.utils.LanguageCodeConverters;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Keeps the NNM system's copy of a form's configuration equal to what the form publishes: read from
 * the live workspace, compared by hash with what the system last accepted, posted only when it
 * differs, as the next version.
 *
 * <p>A publication reaches the listener as several bursts of events, so a synchronisation asked for
 * waits {@link #COALESCE_SECONDS} and is replaced by any newer request for the same form. A system
 * that cannot be reached — connection down, timeout, 5xx — puts the form on a pending list, retried
 * every minute with the same version number, since the state is written only after a 2xx. A 4xx is
 * logged and dropped: the same body would be refused again.</p>
 *
 * <p>A form that is not connected (no sync action) or not in live is left alone. Nothing is sent
 * when a form is unpublished or loses its action: the system's API has no call for it.</p>
 */
@Component(service = ConfigSynchronizer.class, immediate = true)
public class ConfigSynchronizer {

    static final String WORKSPACE_LIVE = "live";
    static final long COALESCE_SECONDS = 2;
    static final long RETRY_SECONDS = 60;

    private static final Logger log = LoggerFactory.getLogger(ConfigSynchronizer.class);

    /** Where a configuration goes: the NNM system, or a fake in the tests. */
    @FunctionalInterface
    interface Gateway {
        int postConfig(String json) throws IOException;
    }

    /** What live says about a form: its definition, or empty when it is not published or not connected. */
    @FunctionalInterface
    interface LiveForms {
        Optional<FormDefinition> read(String formUuid) throws RepositoryException;
    }

    private final Set<String> pending = ConcurrentHashMap.newKeySet();
    private final Set<String> forced = ConcurrentHashMap.newKeySet();
    private final java.util.Map<String, ScheduledFuture<?>> coalesced = new ConcurrentHashMap<>();
    private Gateway gateway;
    private LiveForms live;
    private ScheduledExecutorService scheduler;

    @Reference
    private NnmClient client;

    @Reference
    private SyncState state;

    public ConfigSynchronizer() {
    }

    ConfigSynchronizer(Gateway gateway, LiveForms live, SyncState state, ScheduledExecutorService scheduler) {
        this.gateway = gateway;
        this.live = live;
        this.state = state;
        this.scheduler = scheduler;
    }

    @Activate
    public void activate() {
        gateway = client::postConfig;
        live = this::readLive;
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "formidable-nnm-config-sync");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::retryPending, RETRY_SECONDS, RETRY_SECONDS, TimeUnit.SECONDS);
    }

    @Deactivate
    public void deactivate() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /**
     * Synchronises the form once the publication has settled; a request replaces any earlier one still
     * waiting for the same form. {@code force} sends the current version again even if the system
     * already has it — what the submission path asks when the system answered that it does not know the form.
     */
    public void syncLater(String formUuid, boolean force) {
        if (force) {
            forced.add(formUuid);
        }
        ScheduledFuture<?> earlier = coalesced.put(formUuid, scheduler.schedule(() -> {
            coalesced.remove(formUuid);
            sync(formUuid, forced.remove(formUuid));
        }, COALESCE_SECONDS, TimeUnit.SECONDS));
        if (earlier != null) {
            earlier.cancel(false);
        }
    }

    void sync(String formUuid, boolean force) {
        try {
            Optional<FormDefinition> definition = live.read(formUuid);
            if (definition.isEmpty()) {
                pending.remove(formUuid);
                return;
            }
            send(formUuid, definition.get(), force);
        } catch (RepositoryException e) {
            pending.remove(formUuid);
            log.error("[ConfigSynchronizer] Could not read form {} in live, configuration not sent: {}", formUuid, e.getMessage(), e);
        }
    }

    private void send(String formUuid, FormDefinition definition, boolean force) throws RepositoryException {
        String hash = definition.hash();
        Optional<SyncState.Entry> known = state.get(formUuid);
        boolean unchanged = known.isPresent() && known.get().hash().equals(hash);
        if (unchanged && !force) {
            pending.remove(formUuid);
            log.debug("[ConfigSynchronizer] Form '{}' unchanged since version {}", definition.id(), known.get().version());
            return;
        }
        int version = unchanged ? known.get().version() : known.map(entry -> entry.version() + 1).orElse(1);
        int status;
        try {
            status = gateway.postConfig(definition.json(version));
        } catch (IOException e) {
            pending.add(formUuid);
            log.warn("[ConfigSynchronizer] Form '{}' version {} left pending, the NNM system cannot be reached: {}", definition.id(), version, e.getMessage());
            return;
        }
        if (status >= 200 && status < 300) {
            state.put(formUuid, new SyncState.Entry(version, hash));
            pending.remove(formUuid);
            log.info("[ConfigSynchronizer] Form '{}' version {} sent to the NNM system", definition.id(), version);
        } else if (status >= 500) {
            pending.add(formUuid);
            log.warn("[ConfigSynchronizer] Form '{}' version {} left pending, the NNM system answered {}", definition.id(), version, status);
        } else {
            pending.remove(formUuid);
            log.error("[ConfigSynchronizer] Form '{}' version {} refused by the NNM system with {}: not retried", definition.id(), version, status);
        }
    }

    /**
     * The forms whose last send found the system unreachable, retried in order and stopping at the
     * first still unreachable. Nothing may escape: the scheduler drops a task that throws, and with it
     * every later retry.
     */
    void retryPending() {
        for (String formUuid : java.util.List.copyOf(pending)) {
            try {
                sync(formUuid, false);
            } catch (RuntimeException e) {
                pending.remove(formUuid);
                log.error("[ConfigSynchronizer] Unexpected error retrying form {}, dropped from the pending list: {}", formUuid, e.getMessage(), e);
                continue;
            }
            if (pending.contains(formUuid)) {
                return;
            }
        }
    }

    Set<String> pending() {
        return Set.copyOf(pending);
    }

    private Optional<FormDefinition> readLive(String formUuid) throws RepositoryException {
        JCRTemplate template = JCRTemplate.getInstance();
        // the site's default language: where the options' values live
        String language = template.doExecuteWithSystemSessionAsUser(null, WORKSPACE_LIVE, null, session -> {
            try {
                JCRSiteNode site = session.getNodeByIdentifier(formUuid).getResolveSite();
                return site == null ? null : site.getDefaultLanguage();
            } catch (ItemNotFoundException e) {
                return null;
            }
        });
        if (language == null) {
            // not in live (unpublished, removed), or outside a site
            return Optional.empty();
        }
        try {
            return template.doExecuteWithSystemSessionAsUser(null, WORKSPACE_LIVE, LanguageCodeConverters.languageCodeToLocale(language), session -> {
                JCRNodeWrapper form = session.getNodeByIdentifier(formUuid);
                return FormDefinitionReader.carriesSyncAction(form) ? Optional.of(FormDefinitionReader.read(form)) : Optional.<FormDefinition>empty();
            });
        } catch (ItemNotFoundException e) {
            // gone between the two reads: an unpublication in progress
            return Optional.empty();
        }
    }
}
