package org.jahia.test.modules.formidable.nnmsync;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Sends the submissions to the NNM system from threads of its own, after the visitor has been answered.
 * {@link #send} only queues the work: it never waits, and never throws. What happens afterwards is
 * logged and goes no further — the visitor is not told, and a failed submission is not retried
 * (it is still saved by the form's other actions).
 *
 * <p>The pool and its queue are bounded, so a system that stops answering cannot pile up work without
 * limit: each request is cut by the request timeout, and when the queue is full the submission is
 * dropped with a warning rather than held in memory.</p>
 */
@Component(service = SubmissionDispatcher.class, immediate = true)
public class SubmissionDispatcher {

    static final int THREADS = 2;
    static final int QUEUE_CAPACITY = 200;

    private static final Logger log = LoggerFactory.getLogger(SubmissionDispatcher.class);

    /** Where a submission goes: the NNM system, or a fake in the tests. */
    @FunctionalInterface
    interface Gateway {
        int postSubmission(String json) throws IOException;
    }

    private Gateway gateway;
    private ExecutorService executor;

    @Reference
    private NnmClient client;

    @Reference
    private ConfigSynchronizer synchronizer;

    public SubmissionDispatcher() {
    }

    SubmissionDispatcher(Gateway gateway, ExecutorService executor, ConfigSynchronizer synchronizer) {
        this.gateway = gateway;
        this.executor = executor;
        this.synchronizer = synchronizer;
    }

    @Activate
    public void activate() {
        gateway = client::postSubmission;
        executor = new ThreadPoolExecutor(THREADS, THREADS, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(QUEUE_CAPACITY), runnable -> {
            Thread thread = new Thread(runnable, "formidable-nnm-submission");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Deactivate
    public void deactivate() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /** Queues the submission and returns at once. */
    public void send(String formUuid, String submissionId, String json) {
        try {
            executor.execute(() -> deliver(formUuid, submissionId, json));
        } catch (RejectedExecutionException e) {
            log.warn("[SubmissionDispatcher] Submission {} dropped: {} are already waiting to be sent to the NNM system.", submissionId, QUEUE_CAPACITY);
        }
    }

    void deliver(String formUuid, String submissionId, String json) {
        try {
            int status = gateway.postSubmission(json);
            if (status == 201) {
                log.debug("[SubmissionDispatcher] Submission {} accepted by the NNM system", submissionId);
            } else if (status == 409) {
                log.debug("[SubmissionDispatcher] Submission {} was already known to the NNM system", submissionId);
            } else if (status == 404) {
                // the system does not know the form's configuration: send it again, the submission itself is lost
                log.warn("[SubmissionDispatcher] Submission {} refused with 404: the NNM system does not know the form {}, its configuration is sent again.", submissionId, formUuid);
                synchronizer.syncLater(formUuid, true);
            } else {
                log.warn("[SubmissionDispatcher] Submission {} refused by the NNM system with {}", submissionId, status);
            }
        } catch (IOException | RuntimeException e) {
            log.warn("[SubmissionDispatcher] Submission {} not sent to the NNM system: {}", submissionId, e.getMessage());
        }
    }
}
